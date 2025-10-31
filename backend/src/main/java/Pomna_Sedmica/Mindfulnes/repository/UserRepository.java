package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
