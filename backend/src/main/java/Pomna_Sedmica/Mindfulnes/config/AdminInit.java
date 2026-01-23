package Pomna_Sedmica.Mindfulnes.config;

import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.controller.TrainerController;
import Pomna_Sedmica.Mindfulnes.domain.dto.SubscribeDTORequest;
import Pomna_Sedmica.Mindfulnes.domain.entity.Trainer;
import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class AdminInit {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initAdmin() {
        return args -> {
            if (userRepository.findByEmail("admin@admin").isEmpty()) {
                User user = new User(
                        "admin@admin",
                        passwordEncoder.encode("admin"), //moze se prebacit u env
                        null,
                        "admin",
                        "adminkovic",
                        null,
                        Role.ADMIN,//isto neka ostane user, a promijeni se kod onboardinga
                        false
                );
                userRepository.save(user);
            }
        };
    }

    @Bean
    CommandLineRunner initTestUsers() {
        return args -> {
            if (userRepository.findByEmail("user@user").isEmpty()) {
                User user = new User(
                        "user@user",
                        passwordEncoder.encode("admin"), //moze se prebacit u env
                        null,
                        "Pero",
                        "Peric",
                        null,
                        Role.USER,//isto neka ostane user, a promijeni se kod onboardinga
                        false
                );
                userRepository.save(user);
                user = new User(
                        "trainer@trainer",
                        passwordEncoder.encode("admin"), //moze se prebacit u env
                        null,
                        "Zvonko",
                        "Zvonic",
                        null,
                        Role.TRAINER,//isto neka ostane user, a promijeni se kod onboardinga
                        false
                );
                userRepository.save(user);
                user.setOnboardingComplete(true);
                user.setRequiresPasswordReset(false);
                user = userRepository.findByEmail("trainer@trainer").orElse(null);
                userRepository.delete(user);
                userRepository.save(new Trainer(user));
                //Trainer trainer = new Trainer()
                Trainer trainer = new Trainer(
                        "trainer2@trainer",
                        passwordEncoder.encode("admin"),//moze se prebacit u env
                        null,
                        "Petar",
                        "Grasak",
                        null,
                        Role.TRAINER,//isto neka ostane user, a promijeni se kod onboardinga
                        false
                );
                userRepository.save(trainer);
                user.setOnboardingComplete(true);
                user.setRequiresPasswordReset(false);
            }
        };
    }
}
