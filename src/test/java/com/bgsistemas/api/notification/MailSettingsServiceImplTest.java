package com.bgsistemas.api.notification;

import com.bgsistemas.api.setting.SiteSetting;
import com.bgsistemas.api.setting.SiteSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSettingsServiceImplTest {

    @Mock
    private SiteSettingRepository siteSettingRepository;

    private MailSettingsServiceImpl service;

    @BeforeEach
    void setUp() {
        // The two last values are the ones of .env (MAIL_TO and MAIL_USERNAME)
        service = new MailSettingsServiceImpl(siteSettingRepository, "env@bg.es", "web@gmail.com");
    }

    private void saved(String key, String value) {
        when(siteSettingRepository.findById(key)).thenReturn(Optional.of(new SiteSetting(key, value)));
    }

    private void notSaved(String key) {
        when(siteSettingRepository.findById(key)).thenReturn(Optional.empty());
    }

    // SiteSetting has no equals(): compare its key and value
    private boolean isSetting(SiteSetting setting, String key, String value) {
        return setting != null && key.equals(setting.getKey()) && value.equals(setting.getValue());
    }

    @Test
    void usesTheValuesOfEnvWhenNothingIsSaved() {
        notSaved(MailSettingsServiceImpl.NOTIFY_EMAIL_KEY);
        notSaved(MailSettingsServiceImpl.SENDER_NAME_KEY);

        MailSettings settings = service.getSettings();

        assertThat(settings.notifyEmail()).isEqualTo("env@bg.es");
        assertThat(settings.senderName()).isEqualTo("BG Sistemas Web");
        assertThat(settings.senderAccount()).isEqualTo("web@gmail.com");
        assertThat(settings.canSend()).isTrue();
    }

    @Test
    void usesTheValuesSavedInThePanel() {
        saved(MailSettingsServiceImpl.NOTIFY_EMAIL_KEY, "duenio@bg.es");
        saved(MailSettingsServiceImpl.SENDER_NAME_KEY, "Web de BG");

        MailSettings settings = service.getSettings();

        assertThat(settings.notifyEmail()).isEqualTo("duenio@bg.es");
        assertThat(settings.senderName()).isEqualTo("Web de BG");
    }

    @Test
    void savesTheTrimmedValues() {
        saved(MailSettingsServiceImpl.NOTIFY_EMAIL_KEY, "duenio@bg.es");
        saved(MailSettingsServiceImpl.SENDER_NAME_KEY, "Web de BG");

        MailSettings settings = service.updateSettings("  duenio@bg.es ", " Web de BG ");

        verify(siteSettingRepository).save(argThat(setting -> isSetting(setting, MailSettingsServiceImpl.NOTIFY_EMAIL_KEY, "duenio@bg.es")));
        verify(siteSettingRepository).save(argThat(setting -> isSetting(setting, MailSettingsServiceImpl.SENDER_NAME_KEY, "Web de BG")));
        assertThat(settings.notifyEmail()).isEqualTo("duenio@bg.es");
    }

    @Test
    void anEmptySenderNameGoesBackToTheDefaultOne() {
        saved(MailSettingsServiceImpl.NOTIFY_EMAIL_KEY, "duenio@bg.es");
        notSaved(MailSettingsServiceImpl.SENDER_NAME_KEY);

        MailSettings settings = service.updateSettings("duenio@bg.es", "  ");

        verify(siteSettingRepository).deleteById(MailSettingsServiceImpl.SENDER_NAME_KEY);
        assertThat(settings.senderName()).isEqualTo("BG Sistemas Web");
    }

    @Test
    void cannotSendWithoutARecipient() {
        assertThat(new MailSettings("", "BG Sistemas Web", "web@gmail.com").canSend()).isFalse();
        assertThat(new MailSettings("duenio@bg.es", "BG Sistemas Web", "").canSend()).isFalse();
    }
}
