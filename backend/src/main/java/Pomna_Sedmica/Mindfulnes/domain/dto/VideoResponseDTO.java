package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponseDTO {
    private Long id;
    private String title;
    private String description;
    private String url;
    private String trainerName;
    private LocalDateTime createdAt;
    private String type;
}

