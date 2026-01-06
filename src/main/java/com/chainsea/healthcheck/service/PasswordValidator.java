package com.chainsea.healthcheck.service;

public class PasswordValidator {
    public boolean validate(String secret) {
        return secret.length() > 12;
    }
}
