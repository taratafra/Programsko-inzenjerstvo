package Pomna_Sedmica.Mindfulnes.service;

import Pomna_Sedmica.Mindfulnes.domain.dto.SaveAuth0UserRequestDTO;
import Pomna_Sedmica.Mindfulnes.domain.dto.TrainerDTOResponse;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.mapper.TrainerMapper;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class TrainerService {

    private final UserRepository userRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${auth0.domain}")
    private String auth0Domain;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public TrainerDTOResponse saveOrUpdateTrainer(SaveAuth0UserRequestDTO dto) {
        Optional<User> existingUser = Optional.empty();

        if (dto.auth0Id() != null && !dto.auth0Id().isEmpty()) {
            existingUser = userRepository.findByAuth0Id(dto.auth0Id()).stream().findFirst();
        }

        if (existingUser.isEmpty() && dto.email() != null && !dto.email().isEmpty()) {
            existingUser = userRepository.findByEmail(dto.email());
        }

        Trainer user = existingUser.map(existing -> {
            return TrainerMapper.updateExisting(new Trainer(existing), dto);
        }).orElseGet(() -> {
            return TrainerMapper.toNewEntity(dto);
        });

        Trainer savedUser = userRepository.save(user);
        return TrainerMapper.toDTO(savedUser);
    }

    public List<TrainerDTOResponse> getAllTrainers() {
        return userRepository.findAllByRole(Role.TRAINER)
                .stream()
                .map(Trainer::new)
                .map(TrainerMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<TrainerDTOResponse> getTrainerByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(user -> user.getRole() == Role.TRAINER)
                .map(Trainer::new)
                .map(TrainerMapper::toDTO);
    }


    public Optional<TrainerDTOResponse> getTrainerByAuth0IdWithId(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .filter(user -> user.getRole() == Role.TRAINER)
                .map(Trainer::new)
                .map(TrainerMapper::toDTO);
    }

    @Transactional
    public Optional<TrainerDTOResponse> completeOnboarding(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    System.out.println("DEBUG completeOnboarding: Found user with email: " + email);
                    return convertToTrainerAndComplete(user);
                });
    }

    @Transactional
    public Optional<TrainerDTOResponse> completeOnboardingByAuth0Id(String auth0Id) {
        return userRepository.findByAuth0Id(auth0Id)
                .stream().findFirst()
                .map(user -> {
                    System.out.println("DEBUG completeOnboardingByAuth0Id: Found user with auth0Id: " + auth0Id);
                    System.out.println("DEBUG: User ID: " + user.getId());
                    System.out.println("DEBUG: User email: " + user.getEmail());
                    System.out.println("DEBUG: User instanceof Trainer: " + (user instanceof Trainer));

                    return convertToTrainerAndComplete(user);
                });
    }


    public Optional<TrainerDTOResponse> getTrainerById(Long id) {
        return userRepository.findById(id)
                .filter(user -> user.getRole() == Role.TRAINER)
                .map(Trainer::new)
                .map(TrainerMapper::toDTO);
    }

    private TrainerDTOResponse convertToTrainerAndComplete(User user) {
        // Update user properties
        user.setOnboardingComplete(true);
        user.setRequiresPasswordReset(false);
        user.setFirstLogin(false);
        user.setRole(Role.TRAINER);

        // If already a Trainer, just save and return
        if (user instanceof Trainer) {
            System.out.println("DEBUG: User is already a Trainer");
            Trainer savedTrainer = userRepository.save((Trainer) user);
            return TrainerMapper.toDTO(savedTrainer);
        }

        System.out.println("DEBUG: Converting User to Trainer");

        // Save the updated User first to ensure role is set
        User savedUser = userRepository.save(user);
        entityManager.flush();

        System.out.println("DEBUG: Saved user with ID: " + savedUser.getId());

        // Create a Trainer entry in the trainer table
        try {
            // Check if trainer entry already exists
            Long count = (Long) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM trainer WHERE id = ?")
                    .setParameter(1, savedUser.getId())
                    .getSingleResult();

            if (count == 0) {
                entityManager.createNativeQuery(
                                "INSERT INTO trainer (id, approved) VALUES (?, ?)")
                        .setParameter(1, savedUser.getId())
                        .setParameter(2, false)
                        .executeUpdate();

                System.out.println("DEBUG: Created trainer table entry");
            } else {
                System.out.println("DEBUG: Trainer entry already exists");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to create/check trainer entry: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create trainer record", e);
        }

        entityManager.flush();
        entityManager.clear();

        // Now fetch it back as a Trainer
        Trainer trainer = (Trainer) userRepository.findById(savedUser.getId())
                .orElseThrow(() -> new RuntimeException("Could not find trainer after conversion"));

        System.out.println("DEBUG: Successfully fetched as Trainer, approved: " + trainer.isApproved());

        return TrainerMapper.toDTO(trainer);
    }

    @Transactional
    public User getOrCreateTrainerFromJwt(Jwt jwt) {
        String sub = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");

        System.out.println("DEBUG: getOrCreateTrainerFromJwt - sub: " + sub + ", email: " + email);

        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT 'sub' claim is required");
        }

        if ((email == null || email.isBlank()) && sub.contains("@")) {
            email = sub;
        }

        // Try to fetch email from UserInfo endpoint if missing
        if (email == null || email.isBlank()) {
            try {
                String domain = auth0Domain.startsWith("http") ? auth0Domain : "https://" + auth0Domain;
                WebClient webClient = webClientBuilder.baseUrl(domain).build();
                Map userInfo = webClient.get()
                        .uri("/userinfo")
                        .header("Authorization", "Bearer " + jwt.getTokenValue())
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                if (userInfo != null && userInfo.get("email") != null) {
                    email = (String) userInfo.get("email");
                }
            } catch (Exception e) {
                System.out.println("Failed to fetch userinfo in TrainerService: " + e.getMessage());
            }
        }

        Optional<User> byAuth0Id = userRepository.findByAuth0Id(sub).stream().findFirst();
        if (byAuth0Id.isPresent()) {
            User user = byAuth0Id.get();

            if (user.isBanned()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been banned");
            }

            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }

        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User user = byEmail.get();

                if (user.isBanned()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account has been banned");
                }

                if (user.getAuth0Id() == null || user.getAuth0Id().isEmpty()) {
                    user.setAuth0Id(sub);
                }
                user.setLastLogin(LocalDateTime.now());
                return userRepository.save(user);
            }
        }

        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String fullName = jwt.getClaimAsString("name");

        String firstName;
        String lastName;

        if (givenName != null && !givenName.isBlank() && familyName != null && !familyName.isBlank()) {
            firstName = givenName;
            lastName = familyName;
        } else if (givenName != null && !givenName.isBlank()) {
            firstName = givenName;
            lastName = familyName != null ? familyName : "";
        } else if (fullName != null && !fullName.isBlank()) {
            if (fullName.contains(" ")) {
                String[] nameParts = fullName.trim().split("\\s+", 2);
                firstName = nameParts[0];
                lastName = nameParts.length > 1 ? nameParts[1] : "";
            } else {
                firstName = fullName;
                lastName = "";
            }
        } else {
            firstName = "User";
            lastName = "";
        }

        User newUser = new User();
        newUser.setAuth0Id(sub);
        newUser.setEmail(email != null ? email : sub + "@placeholder.local");
        newUser.setName(firstName);
        newUser.setSurname(lastName);
        newUser.setRole(Role.TRAINER);
        newUser.setSocialLogin(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setLastLogin(LocalDateTime.now());
        newUser.setFirstLogin(true);
        newUser.setOnboardingComplete(false);
        newUser.setRequiresPasswordReset(false);

        return userRepository.save(newUser);
    }
}