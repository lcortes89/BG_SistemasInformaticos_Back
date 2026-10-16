package com.bgsistemas.api.notification;

import com.bgsistemas.api.contact.Contact;

// BGSI-53: tells the business that something happened on the website
public interface NotificationService {

    void notifyNewContact(Contact contact);
}
