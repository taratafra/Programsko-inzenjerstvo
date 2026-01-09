package Pomna_Sedmica.Mindfulnes.domain.enums;

public enum ReminderStatus {
    PENDING,  // log je kreiran i čeka slanje
    SENT,     // uspješno poslano
    FAILED,   // pokušano, ali je bacilo grešku
    SKIPPED   // preskočeno (npr. nije due, user nema email, disabled, itd.)
}
