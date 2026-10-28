package com.bgsistemas.api.notification;

import org.springframework.util.StringUtils;

// BGSI-260: who receives the emails, the name they show and the Gmail account that sends them
public record MailSettings(String notifyEmail, String senderName, String senderAccount) {

    // Without a recipient or a sender account no email can be sent
    public boolean canSend() {
        return StringUtils.hasText(notifyEmail) && StringUtils.hasText(senderAccount);
    }
}
