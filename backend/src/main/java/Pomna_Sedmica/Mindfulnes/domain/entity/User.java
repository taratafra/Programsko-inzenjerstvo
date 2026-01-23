package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User{

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

    @Column(name = "onboarding_complete")
    private boolean onboardingComplete = false;

    @Column(name = "requires_password_reset")
    private boolean requiresPasswordReset = false;

    //@Column(name = "approved_trainer")
    //private boolean approvedTrainer = false;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_first_login")
    private boolean firstLogin = true; // jel ovo prvi puta da se korisnik prijavljuje -> kad postavi novu lozinku ide u 0

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "is_banned")
    private boolean isBanned;

    @Column(name = "bio", length = 1000)
    private String bio;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @ManyToMany(cascade = CascadeType.PERSIST)
    private Set<Trainer> trainers = new HashSet<>();

    public User(String email, String password, String auth0Id, String name, String surname, LocalDate dob, Role role, boolean isSocialLogin) {

        this.password = password;
        this.auth0Id = auth0Id;
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.dateOfBirth = dob;
        this.role = role;
        this.isSocialLogin = isSocialLogin;
        this.isBanned = false;
    }
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (lastLogin == null) lastLogin = LocalDateTime.now();
    }

    public void setLastUpdated(LocalDateTime now) {
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }

}
