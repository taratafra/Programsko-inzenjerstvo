package Pomna_Sedmica.Mindfulnes.domain.dto;

import Pomna_Sedmica.Mindfulnes.domain.enums.TimeOfDay;
import java.util.List;

public record MessageResponse(TimeOfDay timeOfDay, List<MessageItem> messages) {}
