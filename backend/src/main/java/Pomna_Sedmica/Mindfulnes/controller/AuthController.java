package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.Auth0TokenResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.LoginRequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.RegisterRequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
@RequiredArgsConstructor
public class AuthController {

    private final WebClient.Builder webClientBuilder;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 🔐 Injected from .env / application.properties
    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.clientId}")
    private String clientId;

    @Value("${auth0.clientSecret}")
    private String clientSecret;

    @Value("${auth0.audience}")
    private String audience;

    // 🧠 LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        if (req.isSocialLogin()) {
            // 🔹 Auth0 login flow
            Map<String, Object> body = Map.of(
                    "grant_type", "password",
                    "username", req.getEmail(),
                    "password", req.getPassword(),
                    "audience", audience,
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "scope", "openid profile email"
            );

            WebClient webClient = webClientBuilder.baseUrl(auth0Domain).build();

            Auth0TokenResponse token = webClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Auth0TokenResponse.class)
                    .block();

            if (token == null || token.getAccess_token() == null) {
                return ResponseEntity.status(401).body("Invalid Auth0 credentials");
            }

            // Find or create user by Auth0 ID
            String auth0Id = decodeAuth0Sub(token.getId_token());
            User user = userRepository.findByAuth0Id(auth0Id)
                    .orElseGet(() -> {
                        User u = new User(
                                req.getEmail(),
                                auth0Id,
                                "Auth0User",
                                "",
                                LocalDate.now(),
                                Role.USER,
                                true
                        );
                        return userRepository.save(u);
                    });

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("access_token", token.getAccess_token()));
        } else {
            // 🔹 Local login
            User user = userRepository.findByEmail(req.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                return ResponseEntity.status(401).body("Wrong password");
            }

            String jwt = jwtUtil.generateToken(user);

            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("access_token", jwt));
        }
    }

    // 🧾 REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.status(400).body("Email already exists");
        }

        String encoded = passwordEncoder.encode(req.getPassword());

        User user = new User(
                req.getEmail(),
                encoded,
                req.getName(),
                req.getSurname(),
                req.getDateOfBirth(),
                Role.USER,
                false
        );

        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok("Registered successfully");
    }

    // 🧩 Decode JWT payload from Auth0
    private String decodeAuth0Sub(String idToken) {
        String[] parts = idToken.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        return new JSONObject(payload).getString("sub");
    }
}
