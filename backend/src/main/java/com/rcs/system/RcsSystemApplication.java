package com.rcs.system;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RcsSystemApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(RcsSystemApplication.class, args);
	}

	@Override
	public void run(String... args) {
		System.out.println("RCS Frozen Sorting backend started. Dashboard websocket available at /ws/dashboard");
	}
}
