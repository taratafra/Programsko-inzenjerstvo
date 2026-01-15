//package Pomna_Sedmica.Mindfulnes.controller;
//
//import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
//import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
//import Pomna_Sedmica.Mindfulnes.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.jwt.Jwt;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RequiredArgsConstructor
//@RestController
//@RequestMapping("/api/users")
//@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
//public class UserController {
//
//    private final UserService userService;
//
//    @PostMapping
//    public ResponseEntity<UserDTOResponse> saveUser(@RequestBody SaveAuth0UserRequestDTO request) {
//        UserDTOResponse savedUser = userService.saveOrUpdateUser(request);
//        return ResponseEntity.ok(savedUser);
//    }
//
//    @GetMapping
//    public ResponseEntity<List<UserDTOResponse>> getAllUsers() {
//        return ResponseEntity.ok(userService.getAllUsers());
//    }
//
//
//    @GetMapping("/me")
//    public ResponseEntity<UserDTOResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
//        if (jwt == null) return ResponseEntity.status(401).build();
//
//        String email = jwt.getClaimAsString("email");
//        if (email == null) email = jwt.getSubject();
//
//        return userService.getUserByEmail(email)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.status(404).build());
//    }
//
//
//
//    @PostMapping("/complete-onboarding")
//    public ResponseEntity<UserDTOResponse> completeOnboarding(@AuthenticationPrincipal Jwt jwt) {
//
//        if (jwt == null) {
//            System.out.println("No JWT token found");
//            return ResponseEntity.status(401).build();
//        }
//
//        String claim = extractEmailFromJwt(jwt);
//
//        //provjeravamo jel email ako nije onda se user prijavia preko googla il necega
//        boolean isEmail = claim != null && claim.matches("^[A-Za-z0-9+_.-]+@(.+)$");
//
//        if (isEmail) {
//            return userService.completeOnboarding(claim)
//                    .map(ResponseEntity::ok)
//                    .orElseGet(() -> {
//                        return ResponseEntity.status(404).build();
//                    });
//        } else {
//            return userService.completeOnboardingByAuth0Id(claim)
//                    .map(ResponseEntity::ok)
//                    .orElseGet(() -> {
//                        return ResponseEntity.status(404).build();
//                    });
//        }
//    }
//
//
//    private String extractEmailFromJwt(Jwt jwt) {
//        String email = jwt.getClaimAsString("email");
//        if (email == null) {
//            email = jwt.getSubject();
//        }
//        return email;
//    }
//
//
//}
package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.mapper.UserMapper;
import Pomna_Sedmica.Mindfulnes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

    @PostMapping
    public ResponseEntity<UserDTOResponse> saveOrUpdateUser(@RequestBody SaveAuth0UserRequestDTO request) {
        UserDTOResponse savedUser = userService.saveOrUpdateUser(request);
        return ResponseEntity.ok(savedUser);
    }
}