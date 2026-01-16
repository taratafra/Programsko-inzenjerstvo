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
        super(user.getEmail(), user.getPassword(), user.getAuth0Id(), user.getName(), user.getSurname(), user.getDateOfBirth(), user.getRole(), user.isSocialLogin());
    }
}
