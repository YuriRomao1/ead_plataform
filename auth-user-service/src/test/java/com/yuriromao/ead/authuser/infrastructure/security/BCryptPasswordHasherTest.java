package com.yuriromao.ead.authuser.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yuriromao.ead.authuser.application.port.PasswordHasher;
import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

  private static final String RAW_PASSWORD = "plainPassword";
  private static final String WRONG_PASSWORD = "wrongPassword";
  private static final int TEST_STRENGTH = 10;

  private final PasswordHasher passwordHasher = new BCryptPasswordHasher(TEST_STRENGTH);

  @Test
  void shouldGenerateBCryptHash() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertTrue(passwordHash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$"));
  }

  @Test
  void shouldGenerateHashDifferentFromRawPassword() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertNotEquals(RAW_PASSWORD, passwordHash);
  }

  @Test
  void shouldMatchCorrectPasswordAgainstHash() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertTrue(passwordHasher.matches(RAW_PASSWORD, passwordHash));
  }

  @Test
  void shouldRejectIncorrectPasswordAgainstHash() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertFalse(passwordHasher.matches(WRONG_PASSWORD, passwordHash));
  }

  @Test
  void shouldGenerateDifferentHashesForSamePassword() {
    String firstPasswordHash = passwordHasher.hash(RAW_PASSWORD);
    String secondPasswordHash = passwordHasher.hash(RAW_PASSWORD);

    assertAll(
        () -> assertNotEquals(firstPasswordHash, secondPasswordHash),
        () -> assertTrue(passwordHasher.matches(RAW_PASSWORD, firstPasswordHash)),
        () -> assertTrue(passwordHasher.matches(RAW_PASSWORD, secondPasswordHash)));
  }

  @Test
  void shouldUseConfiguredStrength() {
    PasswordHasher configuredPasswordHasher = new BCryptPasswordHasher(4);

    String passwordHash = configuredPasswordHasher.hash(RAW_PASSWORD);

    assertTrue(passwordHash.matches("^\\$2[aby]\\$04\\$[./A-Za-z0-9]{53}$"));
  }

  @Test
  void shouldRejectNullPasswordWhenHashing() {
    assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(null));
  }

  @Test
  void shouldRejectEmptyPasswordWhenHashing() {
    assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash(""));
  }

  @Test
  void shouldRejectBlankPasswordWhenHashing() {
    assertThrows(IllegalArgumentException.class, () -> passwordHasher.hash("   "));
  }

  @Test
  void shouldRejectNullPasswordWhenMatching() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertThrows(IllegalArgumentException.class, () -> passwordHasher.matches(null, passwordHash));
  }

  @Test
  void shouldRejectEmptyPasswordWhenMatching() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertThrows(IllegalArgumentException.class, () -> passwordHasher.matches("", passwordHash));
  }

  @Test
  void shouldRejectBlankPasswordWhenMatching() {
    String passwordHash = passwordHasher.hash(RAW_PASSWORD);

    assertThrows(IllegalArgumentException.class, () -> passwordHasher.matches("   ", passwordHash));
  }

  @Test
  void shouldRejectNullHashWhenMatching() {
    assertThrows(IllegalArgumentException.class, () -> passwordHasher.matches(RAW_PASSWORD, null));
  }

  @Test
  void shouldRejectEmptyHashWhenMatching() {
    assertThrows(IllegalArgumentException.class, () -> passwordHasher.matches(RAW_PASSWORD, ""));
  }

  @Test
  void shouldRejectBlankHashWhenMatching() {
    assertThrows(IllegalArgumentException.class, () -> passwordHasher.matches(RAW_PASSWORD, "   "));
  }
}
