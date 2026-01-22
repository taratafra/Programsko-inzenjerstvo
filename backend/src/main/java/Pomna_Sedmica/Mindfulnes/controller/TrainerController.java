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
import java.util.Set;

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

    // Add this method to TrainerController.java

    @GetMapping("/{id}")
    public ResponseEntity<TrainerDTOResponse> getTrainerById(@PathVariable Long id) {
        return trainerService.getTrainerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @GetMapping("/me")
    public ResponseEntity<TrainerDTOResponse> getCurrentTrainer(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();

        String auth0Id = jwt.getSubject();
        if (auth0Id == null) return ResponseEntity.status(401).build();

        return trainerService.getTrainerByAuth0IdWithId(auth0Id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @PostMapping("/subscribe")
    public void subscribe(@RequestBody SubscribeDTORequest request) {
        System.out.println("hello, trying to map subscribers");
        System.out.println(request);
        User user = userRepository.findByEmail(request.emailUser()).orElse(null);
        if (user == null) return;

        Trainer trainer = new Trainer(userRepository.findByEmail(request.emailTrainer()).orElse(null));
        if (trainer == null) return;
        /*System.out.println("checkpoint");
        Set<User> subscribers = trainer.getSubscribers();
        subscribers.add(user);
        System.out.println("printing subscribers");
        System.out.println(subscribers);
        trainer.setSubscribers(subscribers);*/
        user.getTrainers().add(trainer);
        userRepository.save(user);
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
