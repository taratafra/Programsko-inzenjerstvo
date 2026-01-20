package Pomna_Sedmica.Mindfulnes.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Simple response so the frontend can open Terra's auth widget. */
public record TerraWidgetSessionResponse(
        @JsonProperty("url") String url
) {}
