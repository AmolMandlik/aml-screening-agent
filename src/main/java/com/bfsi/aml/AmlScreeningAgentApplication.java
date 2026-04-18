package com.bfsi.aml;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@AllArgsConstructor
public class AmlScreeningAgentApplication {

	private final Environment environment;

	public static void main(String[] args) {
		SpringApplication.run(AmlScreeningAgentApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void loadOnStartup() {
				log.info("AML Screening Agent Application is ready to serve requests!");
				log.info("Application '{}' is running at http://{}:{} with profile(s) {}",
				environment.getProperty("spring.application.name"),
				environment.getProperty("server.address", "localhost"),
				environment.getProperty("server.port", "8080"),
				environment.getActiveProfiles().length == 0 ? "default" : String.join(", ", environment.getActiveProfiles()));

	}

}
