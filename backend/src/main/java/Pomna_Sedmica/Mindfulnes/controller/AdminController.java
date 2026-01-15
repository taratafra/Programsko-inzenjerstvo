package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admins")
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class AdminController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDTOResponse> saveUser(@RequestBody SaveAuth0UserRequestDTO request) {
        UserDTOResponse savedUser = userService.saveOrUpdateUser(request);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping
    public ResponseEntity<List<UserDTOResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }


    @GetMapping("/me")
    public ResponseEntity<UserDTOResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getSubject();

        return userService.getUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }



    @PostMapping("/complete-onboarding")
    public ResponseEntity<UserDTOResponse> completeOnboarding(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            System.out.println("No JWT token found");
            return ResponseEntity.status(401).build();
        }

        String claim = extractEmailFromJwt(jwt);

        //provjeravamo jel email ako nije onda se user prijavia preko googla il necega
        boolean isEmail = claim != null && claim.matches("^[A-Za-z0-9+_.-]+@(.+)$");

        if (isEmail) {
            return userService.completeOnboarding(claim)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        return ResponseEntity.status(404).build();
                    });
        } else {
            return userService.completeOnboardingByAuth0Id(claim)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> {
                        return ResponseEntity.status(404).build();
                    });
        }
    }


    private String extractEmailFromJwt(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getSubject();
        }
        return email;
    }


}
