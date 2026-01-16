package Pomna_Sedmica.Mindfulnes.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Map;

public record CompleteDailyFocusRequest(
        // ako null => today
        LocalDate date,

        // key=questionId, value=userAnswer (optional)
        Map<String, String> answers,

        // opcionalno: ako hoćeš da completion sadrži i moodScore
        Integer moodScore
) {}
