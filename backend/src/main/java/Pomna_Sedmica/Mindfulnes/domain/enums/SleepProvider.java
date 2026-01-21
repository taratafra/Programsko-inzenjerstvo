package Pomna_Sedmica.Mindfulnes.domain.enums;

/**
 * Which external system produced the sleep data.
 *
 * NOTE: You can extend this later (e.g., GOOGLE_FIT, HEALTHKIT, OURA, FITBIT ...)
 * or keep these values as "aggregators" (e.g., TERRA/SHAHHA) depending on your approach.
 */
public enum SleepProvider {
    TERRA,
    SAHHA,
    MOCK
}
