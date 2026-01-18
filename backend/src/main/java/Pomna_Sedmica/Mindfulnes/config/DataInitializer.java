package Pomna_Sedmica.Mindfulnes.config;

import Pomna_Sedmica.Mindfulnes.domain.entity.*;
import Pomna_Sedmica.Mindfulnes.domain.enums.ContentType;
import Pomna_Sedmica.Mindfulnes.domain.enums.Goal; // <--- NOVI IMPORT
import Pomna_Sedmica.Mindfulnes.domain.enums.MeditationExperience; // <--- NOVI IMPORT
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
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final TrainerRepository trainerRepository;
    private final VideoRepository videoRepository;
    private final LabelRepository labelRepository;
    private final FirebaseStorageService firebaseService;

    private final Path contentBasePath = Paths.get("ContentBase");

    public DataInitializer(TrainerRepository trainerRepository,
                           VideoRepository videoRepository,
                           LabelRepository labelRepository,
                           FirebaseStorageService firebaseService) {
        this.trainerRepository = trainerRepository;
        this.videoRepository = videoRepository;
        this.labelRepository = labelRepository;
        this.firebaseService = firebaseService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("--- POKREĆEM DATA SEEDING (S LABELIRANJEM) ---");

        videoRepository.deleteAll();
        trainerRepository.deleteAll();
        labelRepository.deleteAll();

        List<Label> labels = createLabels();
        List<Trainer> trainers = createTrainers();

        processContent("videi", ContentType.VIDEO, trainers, labels);
        processContent("audio", ContentType.AUDIO, trainers, labels);
        processContent("blogovi", ContentType.BLOG, trainers, labels);

        System.out.println("--- GOTOVO: Sadržaj je sada filtrabilan! ---");
    }

    private List<Label> createLabels() {
        List<String> tags = Arrays.asList("Jutro", "Večer", "Vikend", "Ured", "Priroda", "Putovanje");
        List<Label> savedLabels = new ArrayList<>();
        for (String tag : tags) {
            savedLabels.add(labelRepository.save(new Label(tag)));
        }
        return savedLabels;
    }

    private List<Trainer> createTrainers() {
        List<Trainer> list = new ArrayList<>();

        Trainer t1 = new Trainer();
        t1.setName("Marko"); t1.setSurname("Marić"); t1.setEmail("marko@mindful.com");
        t1.setAuth0Id("auth0|1"); t1.setPassword("pass"); t1.setRole(Role.TRAINER); t1.setApproved(true);
        t1.setBio("Instruktor joge.");
        t1.setProfilePictureUrl("https://ui-avatars.com/api/?name=Marko+Maric&background=0D8ABC&color=fff");
        list.add(trainerRepository.save(t1));

        Trainer t2 = new Trainer();
        t2.setName("Ana"); t2.setSurname("Anić"); t2.setEmail("ana@mindful.com");
        t2.setAuth0Id("auth0|2"); t2.setPassword("pass"); t2.setRole(Role.TRAINER); t2.setApproved(true);
        t2.setBio("Psihologinja.");
        t2.setProfilePictureUrl("https://ui-avatars.com/api/?name=Ana+Anic&background=E91E63&color=fff");
        list.add(trainerRepository.save(t2));

        Trainer t3 = new Trainer();
        t3.setName("Ivan"); t3.setSurname("Ivić"); t3.setEmail("ivan@mindful.com");
        t3.setAuth0Id("auth0|3"); t3.setPassword("pass"); t3.setRole(Role.TRAINER); t3.setApproved(true);
        t3.setBio("Trener disanja.");
        t3.setProfilePictureUrl("https://ui-avatars.com/api/?name=Ivan+Ivic&background=4CAF50&color=fff");
        list.add(trainerRepository.save(t3));

        return list;
    }

    private void processContent(String folderName, ContentType type, List<Trainer> trainers, List<Label> labels) {
        Path folder = contentBasePath.resolve(folderName);

        if (!Files.exists(folder)) {
            System.out.println("Info: Folder " + folderName + " ne postoji.");
            return;
        }

        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String fileName = file.getFileName().toString();
                    String title = fileName.replaceAll("\\.[^.]+$", "").replace("_", " ").replace("-", " ");

                    String url;
                    String desc = "Sadržaj: " + title;

                    // Upload na Firebase
                    String remoteFolder = (type == ContentType.VIDEO) ? "videos" : (type == ContentType.AUDIO ? "audio" : "blogs");
                    url = firebaseService.uploadIfNotExists(file, remoteFolder);

                    if (type == ContentType.BLOG && fileName.endsWith(".txt")) {
                        try {
                            String fullText = Files.readString(file);
                            desc = fullText.length() > 200 ? fullText.substring(0, 200) + "..." : fullText;
                        } catch (Exception e) {}
                    }

                    Video content = new Video();
                    content.setTitle(title);
                    content.setType(type);
                    content.setUrl(url);
                    content.setDescription(desc);
                    content.setDuration(type == ContentType.BLOG ? 5 : 300);

                    // --- OVDJE JE PROMJENA ZA FILTRIRANJE ---

                    // 1. Postavi RANDOM GOAL (Cilj)
                    content.setGoal(getRandomGoal());

                    // 2. Postavi RANDOM LEVEL (Težina)
                    content.setLevel(getRandomLevel());

                    // ----------------------------------------

                    content.setTrainer(getRandomElement(trainers));
                    content.setLabels(getRandomLabels(labels));

                    videoRepository.save(content);
                    System.out.println("✅ Spremljeno (" + type + "): " + title + " [Goal: " + content.getGoal() + ", Level: " + content.getLevel() + "]");

                } catch (Exception e) {
                    System.err.println("❌ Greška: " + e.getMessage());
                }
            });
        } catch (IOException e) { e.printStackTrace(); }
    }

    // --- POMOĆNE METODE ---

    // Nova metoda za random Goal
    private Goal getRandomGoal() {
        Goal[] goals = Goal.values();
        // Izbjegavamo null vrijednosti ako ih ima, ali obično uzimamo bilo koji
        return goals[new Random().nextInt(goals.length)];
    }

    // Nova metoda za random Level
    private MeditationExperience getRandomLevel() {
        MeditationExperience[] levels = MeditationExperience.values();
        return levels[new Random().nextInt(levels.length)];
    }

    private <T> T getRandomElement(List<T> list) {
        if (list == null || list.isEmpty()) return null;
        return list.get(new Random().nextInt(list.size()));
    }

    private Set<Label> getRandomLabels(List<Label> allLabels) {
        Set<Label> subset = new HashSet<>();
        if (allLabels == null || allLabels.isEmpty()) return subset;
        Random rand = new Random();
        int count = rand.nextInt(3) + 1;
        for (int i = 0; i < count; i++) {
            subset.add(allLabels.get(rand.nextInt(allLabels.size())));
        }
        return subset;
    }
}