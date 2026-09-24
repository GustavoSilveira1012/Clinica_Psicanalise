package com.psicogest.psicogest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PsicogestApplication {

	public static void main(String[] args) {
		SpringApplication.run(PsicogestApplication.class, args);
	}

}
