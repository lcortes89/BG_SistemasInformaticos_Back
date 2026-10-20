package com.bgsistemas.api.globals;

// BGSI-273: one place for the password rules of the admins (used by several request DTOs)
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 72; // the most that BCrypt can use
    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$";

    public static final String SIZE_MESSAGE = "New password must have between 10 and 72 characters";
    public static final String PATTERN_MESSAGE = "New password needs an uppercase letter, a lowercase letter and a number";

    private PasswordPolicy() {
    }
}
