package Pomna_Sedmica.Mindfulnes.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RatingResponseDTO {
    private Long videoId;
    private Double averageRating;
    private Long totalRatings;
    private Integer userRating; // null if user hasn't rated

    public Double averageRating() {
        return averageRating;
    }

    public Long totalRatings() {
        return totalRatings;
    }

    public Integer userRating() {
        return userRating;
    }
}