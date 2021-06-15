
package com.ververica.statefun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaHttpApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaHttpApplication.class, args);
	}
}
