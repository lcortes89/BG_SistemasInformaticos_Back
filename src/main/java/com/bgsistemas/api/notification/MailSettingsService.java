package com.bgsistemas.api.notification;

public interface MailSettingsService {

    // Values saved in the admin panel, or the ones of .env when nothing is saved yet
    MailSettings getSettings();

    MailSettings updateSettings(String notifyEmail, String senderName);
}
