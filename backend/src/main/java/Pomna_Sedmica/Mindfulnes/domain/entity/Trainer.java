package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Trainer extends User{
    private boolean approved = false;

    @ManyToMany(mappedBy = "trainers")
    private Set<User> subscribers = new HashSet<>();

    @OneToMany
    private Set<Video> videoContent;
    public Trainer(String email, String password, String auth0Id, String name, String surname, LocalDate dob, Role role, boolean isSocialLogin) {
        super(email, password, auth0Id, name, surname, dob, role, isSocialLogin);
    }

public Trainer(User user) {
    super(
            user.getEmail(),
            user.getPassword(),
            user.getAuth0Id(),
            user.getName(),
            user.getSurname(),
            user.getDateOfBirth(),
            user.getRole(),
            user.isSocialLogin()
    );

    // Preserve identity so DTO mapping works and subscriptions match correctly
    this.setId(user.getId());

    // Preserve audit/common persisted fields
    this.setCreatedAt(user.getCreatedAt());
    this.setLastLogin(user.getLastLogin());
    this.setLastModifiedAt(user.getLastModifiedAt());
    this.setOnboardingComplete(user.isOnboardingComplete());

    // ✅ Hardcode these so the frontend doesn't force the password reset screen
    this.setFirstLogin(false);
    this.setRequiresPasswordReset(false);

    // Keep optional profile fields
    this.setBio(user.getBio());
    this.setProfilePictureUrl(user.getProfilePictureUrl());

    // If it was already a Trainer instance, preserve trainer-specific state
    if (user instanceof Trainer t) {
        this.approved = t.isApproved();
        this.subscribers = t.getSubscribers();
        this.videoContent = t.getVideoContent();
    }
}
}
