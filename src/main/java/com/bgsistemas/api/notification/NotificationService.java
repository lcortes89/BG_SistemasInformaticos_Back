package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;

// BGSI-53: tells the business that something happened on the website
public interface NotificationService {

    // In the background: a failure is only logged, it never breaks the contact form
    void notifyNewContact(Contact contact);

    // BGSI-260: the admin checks the settings; throws EmailNotSentException if it fails
    void sendTest();
}
