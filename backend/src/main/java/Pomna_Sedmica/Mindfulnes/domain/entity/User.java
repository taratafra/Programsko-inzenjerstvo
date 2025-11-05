package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auth0_id")
    private String auth0Id; // bit ce null vrijednost ako je local login

    @Column(unique = true)
    private String email;

    private String password; // samo za ovaj local login

    private String name;
    private String surname;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "is_social_login")
    private boolean isSocialLogin; // ema ovo je za tebe znas sta triba

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    public User(String email, String passwordOrAuth0Id, String name, String surname, LocalDate dob, Role role, boolean isSocialLogin) {
        if(isSocialLogin) {
            this.auth0Id = passwordOrAuth0Id; // za korisnika koji se ulogirao preko auth0
            this.password = null;
        } else {
            this.password = passwordOrAuth0Id; // za korisnika koji se lokalno ulogira
            this.auth0Id = null;
        }
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dob;
        this.role = role;
        this.isSocialLogin = isSocialLogin;
        this.createdAt = LocalDateTime.now();
        this.lastLogin = LocalDateTime.now();
    }
}