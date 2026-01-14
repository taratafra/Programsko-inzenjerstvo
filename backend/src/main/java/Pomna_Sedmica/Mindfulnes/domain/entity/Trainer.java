package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Trainer extends User{
    private boolean approved = false;
    public Trainer(String email, String passwordOrAuth0Id, String name, String surname, LocalDate dob, Role role, boolean isSocialLogin) {
        super(email, passwordOrAuth0Id, name, surname, dob, role, isSocialLogin);
    }
}
