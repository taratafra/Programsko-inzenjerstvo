package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String surname;
    private LocalDate dateOfBirth;
}

