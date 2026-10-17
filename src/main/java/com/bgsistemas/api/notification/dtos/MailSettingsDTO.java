package com.bgsistemas.api.notification.dtos;

import com.bgsistemas.api.notification.MailSettings;

// senderAccount is only shown in the panel (read only): it is changed in .env
public record MailSettingsDTO(String notifyEmail, String senderName, String senderAccount) {

    public static MailSettingsDTO from(MailSettings settings) {
        return new MailSettingsDTO(settings.notifyEmail(), settings.senderName(), settings.senderAccount());
    }
}
