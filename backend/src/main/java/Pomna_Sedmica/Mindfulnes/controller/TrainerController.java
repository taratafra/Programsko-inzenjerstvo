package Pomna_Sedmica.Mindfulnes.controller;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.SubscribeDTORequest;
import Pomna_Sedmica.Mindfulnes.domain.dto.TrainerDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.dto.UserDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import Pomna_Sedmica.Mindfulnes.service.TrainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/trainers")
@CrossOrigin(origins = "${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
public class TrainerController {

    private final TrainerService trainerService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<TrainerDTOResponse> saveTrainer(@RequestBody SaveAuth0UserRequestDTO request) {
        TrainerDTOResponse savedUser = trainerService.saveOrUpdateTrainer(request);
        return ResponseEntity.ok(savedUser);
    }

    @GetMapping
    public ResponseEntity<List<TrainerDTOResponse>> getAllTrainers() {
        return ResponseEntity.ok(trainerService.getAllTrainers());
    }

    @GetMapping("/me")
    public ResponseEntity<TrainerDTOResponse> getCurrentTrainer(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();

        String email = jwt.getClaimAsString("email");
        if (email == null) email = jwt.getSubject();

        return trainerService.getTrainerByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @PostMapping("/subscribe")
    public void subscribe(SubscribeDTORequest request) {
        User user = userRepository.findByEmail(request.emailUser()).orElse(null);
        if (user == null) return;

        Trainer trainer = new Trainer(userRepository.findByEmail(request.emailTrainer()).orElse(null));
        if (trainer == null) return;

        trainer.getSubscribers().add(user);
    }

    @PostMapping("/unsubscribe")
    public void unsubscribe(SubscribeDTORequest request) {
        User user = userRepository.findByEmail(request.emailUser()).orElse(null);
        if (user == null) return;

        Trainer trainer = new Trainer(userRepository.findByEmail(request.emailTrainer()).orElse(null));
        if (trainer == null) return;

        trainer.getSubscribers().remove(user);
    }

    @PostMapping("/complete-onboarding")
    public ResponseEntity<TrainerDTOResponse> completeTrainerOnboarding(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();

        String claim = jwt.getSubject();
        boolean isEmail = claim != null && claim.matches("^[A-Za-z0-9+_.-]+@(.+)$");

        if (isEmail) {
            return trainerService.completeOnboarding(claim)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).build());
        } else {
            return trainerService.completeOnboardingByAuth0Id(claim)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(404).build());
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
