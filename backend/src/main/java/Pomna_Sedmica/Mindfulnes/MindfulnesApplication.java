package Pomna_Sedmica.Mindfulnes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MindfulnesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MindfulnesApplication.class, args);
		System.out.println("Current Working Directory: " + System.getProperty("user.dir"));
	}

}
