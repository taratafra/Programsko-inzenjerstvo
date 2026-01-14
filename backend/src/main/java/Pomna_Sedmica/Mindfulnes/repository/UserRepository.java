package Pomna_Sedmica.Mindfulnes.repository;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByAuth0Id(String auth0Id);

    List<User> findAllByRole(Role role);

    Optional<User> findByEmailAndRole(String email, Role role);

    Optional<User> findByAuth0IdAndRole(String auth0Id, Role role);

    @Modifying
    @Query(value = "INSERT INTO trainer (id, approved) VALUES (:id, :approved)", nativeQuery = true)
    void promoteToTrainer(@Param("id") Long id, @Param("approved") boolean approved);
}
