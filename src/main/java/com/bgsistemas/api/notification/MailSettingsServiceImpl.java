package com.bgsistemas.api.notification;

import com.bgsistemas.api.setting.SiteSetting;
import com.bgsistemas.api.setting.SiteSettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

// BGSI-260: the email settings live in the site_setting table (key -> value).
// The Gmail account and its password stay in .env: secrets never go to the database.
@Component
public class MailSettingsServiceImpl implements MailSettingsService {

    static final String NOTIFY_EMAIL_KEY = "mail.notify.email";
    static final String SENDER_NAME_KEY = "mail.sender.name";
    static final String DEFAULT_SENDER_NAME = "BG Sistemas Web";

    private final SiteSettingRepository siteSettingRepository;
    private final String defaultNotifyEmail;
    private final String senderAccount;

    public MailSettingsServiceImpl(SiteSettingRepository siteSettingRepository,
                                   @Value("${app.mail.to:}") String defaultNotifyEmail,
                                   @Value("${app.mail.from:}") String senderAccount) {
        this.siteSettingRepository = siteSettingRepository;
        this.defaultNotifyEmail = defaultNotifyEmail;
        this.senderAccount = senderAccount;
    }

    @Override
    public MailSettings getSettings() {
        return new MailSettings(
                savedValue(NOTIFY_EMAIL_KEY).orElse(defaultNotifyEmail),
                savedValue(SENDER_NAME_KEY).orElse(DEFAULT_SENDER_NAME),
                senderAccount);
    }

    @Override
    @Transactional
    public MailSettings updateSettings(String notifyEmail, String senderName) {
        siteSettingRepository.save(new SiteSetting(NOTIFY_EMAIL_KEY, notifyEmail.trim()));
        if (StringUtils.hasText(senderName)) {
            siteSettingRepository.save(new SiteSetting(SENDER_NAME_KEY, senderName.trim()));
        } else {
            // An empty name goes back to the default one
            siteSettingRepository.deleteById(SENDER_NAME_KEY);
        }
        return getSettings();
    }

    private Optional<String> savedValue(String key) {
        return siteSettingRepository.findById(key)
                .map(SiteSetting::getValue)
                .filter(StringUtils::hasText);
    }
}
