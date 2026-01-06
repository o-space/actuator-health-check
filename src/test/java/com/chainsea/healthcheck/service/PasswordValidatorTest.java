package com.chainsea.healthcheck.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PasswordValidatorTest {

    @Test
    void shouldReturnTrueGivenAPasswordWhichLengthGreaterThan12WhenUserValidatesTheSecret() {
        PasswordValidator passwordValidator = new PasswordValidator();
        assertThat(passwordValidator.validate("1111111111111")).isTrue();
    }

    @Test
    void shouldReturnFalseGivenAPasswordWhichLengthEquals12WhenUserValidatesTheSecret() {
        PasswordValidator passwordValidator = new PasswordValidator();
        assertThat(passwordValidator.validate("111111111111")).isFalse();
    }

    @Test
    void shouldReturnFalseGivenAPasswordWhichLengthLessThan12WhenUserValidatesTheSecret() {
        PasswordValidator passwordValidator = new PasswordValidator();
        assertThat(passwordValidator.validate("11111111111")).isFalse();
    }
}
