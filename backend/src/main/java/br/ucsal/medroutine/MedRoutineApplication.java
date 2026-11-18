package br.ucsal.medroutine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedRoutineApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedRoutineApplication.class, args);
    }
}
