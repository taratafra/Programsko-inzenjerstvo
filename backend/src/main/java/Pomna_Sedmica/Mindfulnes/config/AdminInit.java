package Pomna_Sedmica.Mindfulnes.config;

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
                        "admin",
                        "admin",
                        null,
                        Role.ADMIN,//isto neka ostane user, a promijeni se kod onboardinga
                        false
                );
                userRepository.save(user);
            }
        };
    }
}
