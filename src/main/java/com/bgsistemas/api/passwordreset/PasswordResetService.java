package com.bgsistemas.api.passwordreset;

// BGSI-273: "¿Olvidaste tu contraseña?"
public interface PasswordResetService {

    // Sends a link if the email belongs to an admin. It never says if the email exists
    void requestReset(String email);

    // Sets the new password with the token of the link (one use, 30 minutes)
    void resetPassword(String token, String newPassword);
}
