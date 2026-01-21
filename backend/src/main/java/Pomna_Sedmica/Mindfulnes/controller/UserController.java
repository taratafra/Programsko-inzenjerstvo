package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Get current user's information from JWT token
     * GET /api/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDTOResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    /**
     * Update current user's email address
     * PUT /api/users/me/email
     */
    @PutMapping("/me/email")
    public ResponseEntity<UserDTOResponse> updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body
    ) {
        User user = userService.getOrCreateUserFromJwt(jwt);
        String newEmail = body.get("email");

        if (newEmail == null || newEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Basic email validation
        if (!newEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.badRequest().build();
        }

        user.setEmail(newEmail.trim());
        userRepository.save(user);

        return ResponseEntity.ok(UserMapper.toDTO(user));
    }

    /**
     * Get all users with TRAINER role
     * GET /api/users/trainers
     */
    @GetMapping("/trainers")
    public ResponseEntity<List<UserDTOResponse>> getAllTrainers() {
        List<UserDTOResponse> trainers = userService.getAllTrainers()
                .stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(trainers);
    }

    /**
     * Get user by ID
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDTOResponse> getUserById(@PathVariable Long userId) {
        return userService.getUserById(userId)
                .map(UserMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all users (admin endpoint)
     * GET /api/users
     */
    @GetMapping
    public ResponseEntity<List<UserDTOResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Complete onboarding for current user
     * POST /api/users/complete-onboarding
     */
    @PostMapping("/complete-onboarding")
    public ResponseEntity<UserDTOResponse> completeOnboarding(@AuthenticationPrincipal Jwt jwt) {
        User user = userService.getOrCreateUserFromJwt(jwt);

        return userService.completeOnboardingByAuth0Id(user.getAuth0Id())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Save or update user
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<UserDTOResponse> saveOrUpdateUser(@RequestBody SaveAuth0UserRequestDTO request) {
        UserDTOResponse savedUser = userService.saveOrUpdateUser(request);
        return ResponseEntity.ok(savedUser);
    }
}