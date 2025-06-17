package com.example.travel_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TravelProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelProjectApplication.class, args);
	}

}
