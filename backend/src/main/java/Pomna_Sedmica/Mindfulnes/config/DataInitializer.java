package Pomna_Sedmica.Mindfulnes.config;

import Pomna_Sedmica.Mindfulnes.domain.entity.*;
import Pomna_Sedmica.Mindfulnes.domain.enums.Role;
import Pomna_Sedmica.Mindfulnes.repository.*;
import Pomna_Sedmica.Mindfulnes.service.FirebaseStorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Component
@Profile("!prod") // Ne pokreće se na produkciji
public class DataInitializer implements CommandLineRunner {

    private final TrainerRepository trainerRepository;
    private final VideoRepository videoRepository;
    private final AudioRepository audioRepository;
    private final BlogPostRepository blogPostRepository;
    private final LabelRepository labelRepository;
    private final FirebaseStorageService firebaseService;

    // Putanja do ContentBase foldera (u rootu projekta)
    private final Path contentBasePath = Paths.get("ContentBase");

    public DataInitializer(TrainerRepository trainerRepository,
                           VideoRepository videoRepository,
                           AudioRepository audioRepository,
                           BlogPostRepository blogPostRepository,
                           LabelRepository labelRepository,
                           FirebaseStorageService firebaseService) {
        this.trainerRepository = trainerRepository;
        this.videoRepository = videoRepository;
        this.audioRepository = audioRepository;
        this.blogPostRepository = blogPostRepository;
        this.labelRepository = labelRepository;
        this.firebaseService = firebaseService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("--- POKREĆEM INICIJALIZACIJU PODATAKA ---");

        // 1. Očisti bazu (poredak je bitan zbog stranih ključeva)
        videoRepository.deleteAll();
        audioRepository.deleteAll();
        blogPostRepository.deleteAll();
        labelRepository.deleteAll();
        trainerRepository.deleteAll();

        // 2. Kreiraj Labele
        List<Label> labels = createLabels();

        // 3. Kreiraj Trenere
        List<Trainer> trainers = createTrainers();

        // 4. Obradi sadržaj (Video, Audio, Blog)
        processVideos(trainers, labels);
        processAudio(trainers, labels);
        processBlogs(trainers, labels);

        System.out.println("--- INICIJALIZACIJA ZAVRŠENA ---");
    }

    private List<Label> createLabels() {
        List<String> tags = Arrays.asList("Stres", "San", "Fokus", "Jutro", "Anksioznost", "Početnici", "Disanje");
        List<Label> savedLabels = new ArrayList<>();
        for (String tag : tags) {
            savedLabels.add(labelRepository.save(new Label(tag)));
        }
        return savedLabels;
    }

    private List<Trainer> createTrainers() {
        // Prilagodi polja ako se razlikuju u tvojoj klasi Trainer
        Trainer t1 = new Trainer("marko@mindful.com", "pass1", "auth0|1", "Marko", "Marić", LocalDate.of(1990, 5, 12), Role.TRAINER, false);
        t1.setApproved(true);

        Trainer t2 = new Trainer("ana@mindful.com", "pass2", "auth0|2", "Ana", "Anić", LocalDate.of(1988, 3, 15), Role.TRAINER, false);
        t2.setApproved(true);

        Trainer t3 = new Trainer("ivan@mindful.com", "pass3", "auth0|3", "Ivan", "Ivić", LocalDate.of(1992, 7, 20), Role.TRAINER, false);
        t3.setApproved(true);

        return trainerRepository.saveAll(Arrays.asList(t1, t2, t3));
    }

    private void processVideos(List<Trainer> trainers, List<Label> labels) {
        Path folder = contentBasePath.resolve("videi");
        if (!Files.exists(folder)) {
            System.out.println("Folder 'videi' ne postoji.");
            return;
        }

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    // Upload na Firebase (u folder "videos")
                    String url = firebaseService.uploadIfNotExists(file, "videos");

                    Video video = new Video();
                    video.setTitle(formatTitle(file));
                    video.setUrl(url); // Provjeri zove li se setter setUrl ili setVideoUrl
                    video.setDuration(300); // Dummy duration (5 min)
                    video.setTrainer(getRandomElement(trainers));
                    video.setLabels(getRandomLabels(labels));

                    videoRepository.save(video);
                    System.out.println("Video dodan: " + video.getTitle());
                } catch (Exception e) {
                    System.err.println("Greška kod videa " + file.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void processAudio(List<Trainer> trainers, List<Label> labels) {
        Path folder = contentBasePath.resolve("audio");
        if (!Files.exists(folder)) return;

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    // Upload na Firebase (u folder "audio")
                    String url = firebaseService.uploadIfNotExists(file, "audio");

                    Audio audio = new Audio();
                    audio.setTitle(formatTitle(file));
                    audio.setAudioUrl(url);
                    audio.setDurationSeconds(180); // Dummy duration (3 min)
                    audio.setTrainer(getRandomElement(trainers));
                    audio.setLabels(getRandomLabels(labels));

                    audioRepository.save(audio);
                    System.out.println("Audio dodan: " + audio.getTitle());
                } catch (Exception e) {
                    System.err.println("Greška kod audia " + file.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void processBlogs(List<Trainer> trainers, List<Label> labels) {
        Path folder = contentBasePath.resolve("blogovi");
        if (!Files.exists(folder)) return;

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    // Blogove NE uploadamo na Firebase, već čitamo tekst
                    String content = Files.readString(file);

                    BlogPost blog = new BlogPost();
                    blog.setTitle(formatTitle(file));
                    blog.setContent(content);
                    blog.setTrainer(getRandomElement(trainers));
                    blog.setLabels(getRandomLabels(labels));

                    blogPostRepository.save(blog);
                    System.out.println("Blog dodan: " + blog.getTitle());
                } catch (Exception e) {
                    System.err.println("Greška kod bloga " + file.getFileName() + ": " + e.getMessage());
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- POMOĆNE METODE ---

    private String formatTitle(Path file) {
        String name = file.getFileName().toString();
        // Ukloni ekstenziju (.mp4, .txt, .mp3)
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) name = name.substring(0, lastDot);
        // Zamijeni donje crte razmacima
        return name.replace('_', ' ').replace('-', ' ');
    }

    private <T> T getRandomElement(List<T> list) {
        return list.get(new Random().nextInt(list.size()));
    }

    private Set<Label> getRandomLabels(List<Label> allLabels) {
        Set<Label> subset = new HashSet<>();
        Random rand = new Random();
        int count = rand.nextInt(3) + 1; // Uzmi 1 do 3 labela
        for (int i = 0; i < count; i++) {
            subset.add(allLabels.get(rand.nextInt(allLabels.size())));
        }
        return subset;
    }
}