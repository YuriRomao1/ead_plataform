package com.yuriromao.ead.authuser;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Boots the auth-user-service Spring application and enables scheduled infrastructure jobs. */
@EnableScheduling
@OpenAPIDefinition(
    info =
        @Info(
            title = "EAD Auth/User Service API",
            version = "0.0.1",
            description = "HTTP API for user registration in the Auth/User bounded context."))
@SpringBootApplication
public class AuthUserServiceApplication {

  /** Starts the service when the module is executed as a Spring Boot application. */
  public static void main(String[] args) {
    SpringApplication.run(AuthUserServiceApplication.class, args);
  }
}
