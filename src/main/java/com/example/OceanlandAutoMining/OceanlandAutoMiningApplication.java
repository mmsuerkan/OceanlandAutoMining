package com.example.OceanlandAutoMining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OceanlandAutoMiningApplication {

	public static void main(String[] args) {
		SpringApplication.run(OceanlandAutoMiningApplication.class, args);
	}

}
