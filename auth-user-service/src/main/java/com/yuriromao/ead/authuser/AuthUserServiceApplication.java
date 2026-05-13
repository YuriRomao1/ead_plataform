package com.yuriromao.ead.authuser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Boots the auth-user-service Spring application and enables scheduled infrastructure jobs. */
@EnableScheduling
@SpringBootApplication
public class AuthUserServiceApplication {

  /** Starts the service when the module is executed as a Spring Boot application. */
  public static void main(String[] args) {
    SpringApplication.run(AuthUserServiceApplication.class, args);
  }
}
