package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVideoRequestDTO {
    private String title;
    private String description;
    private String url;
}

