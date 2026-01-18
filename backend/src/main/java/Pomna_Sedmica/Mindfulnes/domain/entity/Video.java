package Pomna_Sedmica.Mindfulnes.domain.entity;

import Pomna_Sedmica.Mindfulnes.domain.enums.ContentType;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal;
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set; // VAŽNO: Dodan import za Set

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor // Lombok kreira prazni konstruktor, ne treba ti ručno
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    private ContentType type;

    // IZMJENA: User -> Trainer
    // Budući da videe kreiraju treneri, bolje je ovdje vezati direktno na Trainer entitet
    @ManyToOne
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    // --- NOVO: Ovo je falilo za DataInitializer ---
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "video_labels",
            joinColumns = @JoinColumn(name = "video_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private Set<Label> labels;
    // ----------------------------------------------

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    private MeditationExperience level;

    private Integer duration; // in minutes

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}