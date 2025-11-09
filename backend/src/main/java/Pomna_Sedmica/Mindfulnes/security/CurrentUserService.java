package Pomna_Sedmica.Mindfulnes.security;

import Pomna_Sedmica.Mindfulnes.domain.entity.User;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Servis koji iz Auth0 JWT-a izvlaci identitet (sub/email) i vraca User entitet.
 * Ako korisnik ne postoji, kreira ga minimalno (lazy provisioning).
 *
 * Napomena: radi SAMO za user-token (Authorization Code / SPA).
 * Ako je token M2M (client credentials), nedostaje 'sub' i metoda baca 401.
 */
@Service
public class CurrentUserService {

    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public User getOrCreate(Jwt jwt) {

        String sub = jwt.getClaimAsString("sub");
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT user identity (sub) is required");
        }


        Optional<User> byAuth0 = users.findByAuth0Id(sub);
        if (byAuth0.isPresent()) {
            User u = byAuth0.get();
            u.setLastLogin(LocalDateTime.now());
            return users.save(u);
        }


        String email = jwt.getClaimAsString("email");
        if (email != null && !email.isBlank()) {
            Optional<User> byEmail = users.findByEmail(email);
            if (byEmail.isPresent()) {
                User u = byEmail.get();
                u.setAuth0Id(sub);
                u.setLastLogin(LocalDateTime.now());
                return users.save(u);
            }
        }


        User u = new User(
                email != null ? email : (sub + "@placeholder.local"),
                sub,
                "Auth0User",
                "",
                LocalDate.now(),
                Role.USER,
                true
        );
        u.setCreatedAt(LocalDateTime.now());
        u.setLastLogin(LocalDateTime.now());
        return users.save(u);
    }
}
