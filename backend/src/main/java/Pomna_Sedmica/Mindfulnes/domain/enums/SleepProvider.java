package Pomna_Sedmica.Mindfulnes.domain.enums;

/**
 * Which external system produced the sleep data.
 *
 * IMPORTANT:
 * Frontend currently uses provider=TERRA and /integrations/me/TERRA.
 * We keep TERRA as a legacy alias so Smartwatch.jsx stays untouched,
 * but internally this is now backed by Fitbit OAuth + Fitbit Sleep API.
 */
public enum SleepProvider {
    TERRA,   // legacy alias -> FITBIT in our implementation
    FITBIT,  // for future (if you later update frontend)
    SAHHA,
    MOCK
}
