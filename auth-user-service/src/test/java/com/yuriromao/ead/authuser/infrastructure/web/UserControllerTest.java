package com.yuriromao.ead.authuser.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yuriromao.ead.authuser.application.exception.UserEmailAlreadyExistsException;
import com.yuriromao.ead.authuser.application.exception.UserRoleNotAllowedForPublicRegistrationException;
import com.yuriromao.ead.authuser.application.usecase.CreateUserCommand;
import com.yuriromao.ead.authuser.application.usecase.CreateUserResult;
import com.yuriromao.ead.authuser.application.usecase.CreateUserUseCase;
import com.yuriromao.ead.authuser.domain.model.UserRole;
import com.yuriromao.ead.authuser.domain.model.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(UserController.class)
@Import(CorrelationIdFilter.class)
class UserControllerTest {

  private static final UUID USER_ID = UUID.fromString("6fbe1f59-aace-4bb9-8ff6-9da5e1183f17");
  private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateUserUseCase createUserUseCase;

  @Test
  void shouldCreateUser() throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(createdUserResult());

    mockMvc
        .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/users/" + USER_ID)))
        .andExpect(header().exists(CorrelationId.HEADER_NAME))
        .andExpect(jsonPath("$.id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.name").value("User Name"))
        .andExpect(jsonPath("$.email").value("user@email.com"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
        .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist())
        .andExpect(MockMvcResultMatchers.content().string(not(containsString("plainPassword"))));

    ArgumentCaptor<CreateUserCommand> commandCaptor =
        ArgumentCaptor.forClass(CreateUserCommand.class);
    verify(createUserUseCase).execute(commandCaptor.capture());
    CreateUserCommand command = commandCaptor.getValue();

    org.junit.jupiter.api.Assertions.assertAll(
        () -> org.junit.jupiter.api.Assertions.assertEquals("User Name", command.name()),
        () -> org.junit.jupiter.api.Assertions.assertEquals("user@email.com", command.email()),
        () -> org.junit.jupiter.api.Assertions.assertEquals("plainPassword", command.password()),
        () ->
            org.junit.jupiter.api.Assertions.assertEquals(
                Set.of(UserRole.STUDENT), command.roles()));
  }

  @Test
  void shouldGenerateCorrelationIdWhenRequestHeaderIsMissing() throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(createdUserResult());

    MvcResult result =
        mockMvc
            .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
            .andExpect(status().isCreated())
            .andExpect(header().exists(CorrelationId.HEADER_NAME))
            .andReturn();

    String correlationId = result.getResponse().getHeader(CorrelationId.HEADER_NAME);

    assertDoesNotThrow(() -> UUID.fromString(correlationId));
    assertNull(MDC.get(CorrelationId.MDC_KEY));
  }

  @Test
  void shouldGenerateCorrelationIdWhenRequestHeaderIsBlank() throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(createdUserResult());

    MvcResult result =
        mockMvc
            .perform(
                post("/users")
                    .header(CorrelationId.HEADER_NAME, " ")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest()))
            .andExpect(status().isCreated())
            .andExpect(header().exists(CorrelationId.HEADER_NAME))
            .andReturn();

    String correlationId = result.getResponse().getHeader(CorrelationId.HEADER_NAME);

    assertDoesNotThrow(() -> UUID.fromString(correlationId));
    assertNull(MDC.get(CorrelationId.MDC_KEY));
  }

  @Test
  void shouldReuseCorrelationIdFromRequestHeader() throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class))).thenReturn(createdUserResult());

    mockMvc
        .perform(
            post("/users")
                .header(CorrelationId.HEADER_NAME, "request-correlation-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isCreated())
        .andExpect(header().string(CorrelationId.HEADER_NAME, "request-correlation-id"));

    assertNull(MDC.get(CorrelationId.MDC_KEY));
  }

  @Test
  void shouldRejectInvalidName() throws Exception {
    mockMvc
        .perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "name": "",
								  "email": "user@email.com",
								  "password": "plainPassword",
								  "roles": ["STUDENT"]
								}
								"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_NAME_REQUIRED"))
        .andExpect(jsonPath("$.details").isArray());

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  void shouldRejectInvalidEmail() throws Exception {
    mockMvc
        .perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "name": "User Name",
								  "email": "invalid-email",
								  "password": "plainPassword",
								  "roles": ["STUDENT"]
								}
								"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_EMAIL_INVALID"));

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  void shouldRejectInvalidPassword() throws Exception {
    mockMvc
        .perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "name": "User Name",
								  "email": "user@email.com",
								  "password": "",
								  "roles": ["STUDENT"]
								}
								"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_PASSWORD_REQUIRED"));

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  void shouldRejectMissingRoles() throws Exception {
    mockMvc
        .perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "name": "User Name",
								  "email": "user@email.com",
								  "password": "plainPassword",
								  "roles": []
								}
								"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_ROLE_REQUIRED"));

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  void shouldRejectInvalidRole() throws Exception {
    mockMvc
        .perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "name": "User Name",
								  "email": "user@email.com",
								  "password": "plainPassword",
								  "roles": ["INVALID"]
								}
								"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_ROLE_INVALID"));

    verifyNoInteractions(createUserUseCase);
  }

  @Test
  void shouldRejectTeacherRoleForPublicRegistration() throws Exception {
    assertRoleNotAllowedForPublicRegistration(
        """
				["TEACHER"]
				""");
  }

  @Test
  void shouldRejectAdminRoleForPublicRegistration() throws Exception {
    assertRoleNotAllowedForPublicRegistration(
        """
				["ADMIN"]
				""");
  }

  @Test
  void shouldRejectMultipleRolesWhenAnyRoleIsNotAllowedForPublicRegistration() throws Exception {
    assertRoleNotAllowedForPublicRegistration(
        """
				["STUDENT", "TEACHER"]
				""");
    assertRoleNotAllowedForPublicRegistration(
        """
				["STUDENT", "ADMIN"]
				""");
  }

  @Test
  void shouldRejectDuplicateEmail() throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class)))
        .thenThrow(new UserEmailAlreadyExistsException("user@email.com"));

    mockMvc
        .perform(
            post("/users")
                .header(CorrelationId.HEADER_NAME, "duplicate-email-correlation-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest()))
        .andExpect(status().isConflict())
        .andExpect(header().string(CorrelationId.HEADER_NAME, "duplicate-email-correlation-id"))
        .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").value("Email already exists."))
        .andExpect(jsonPath("$.correlationId").value("duplicate-email-correlation-id"));
  }

  @Test
  void shouldReturnInternalErrorForUnexpectedFailures() throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class)))
        .thenThrow(new IllegalStateException("unexpected failure"));

    mockMvc
        .perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("Internal error."));
  }

  private String validRequest() {
    return """
				{
				  "name": "User Name",
				  "email": "user@email.com",
				  "password": "plainPassword",
				  "roles": ["STUDENT"]
				}
				""";
  }

  private void assertRoleNotAllowedForPublicRegistration(String rolesJson) throws Exception {
    when(createUserUseCase.execute(any(CreateUserCommand.class)))
        .thenThrow(new UserRoleNotAllowedForPublicRegistrationException());

    mockMvc
        .perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
								{
								  "name": "User Name",
								  "email": "user@email.com",
								  "password": "plainPassword",
								  "roles": %s
								}
								"""
                        .formatted(rolesJson)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER_ROLE_NOT_ALLOWED_FOR_PUBLIC_REGISTRATION"))
        .andExpect(jsonPath("$.message").value("Role is not allowed for public registration."));

    verify(createUserUseCase, times(1)).execute(any(CreateUserCommand.class));
    clearInvocations(createUserUseCase);
  }

  private CreateUserResult createdUserResult() {
    return new CreateUserResult(
        USER_ID,
        "User Name",
        "user@email.com",
        UserStatus.ACTIVE,
        Set.of(UserRole.STUDENT),
        CREATED_AT);
  }
}
