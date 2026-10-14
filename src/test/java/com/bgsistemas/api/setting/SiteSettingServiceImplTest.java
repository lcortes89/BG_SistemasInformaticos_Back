package com.bgsistemas.api.setting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteSettingServiceImplTest {

    private static final String URL = "http://localhost:8080/uploads/hero.jpg";

    @Mock
    private SiteSettingRepository siteSettingRepository;

    @InjectMocks
    private SiteSettingServiceImpl siteSettingService;

    @Test
    void shouldReturnTheHeroImageWhenItIsSaved() {
        when(siteSettingRepository.findById("hero.image.url"))
                .thenReturn(Optional.of(new SiteSetting("hero.image.url", URL)));

        assertThat(siteSettingService.getHeroImageUrl()).contains(URL);
    }

    @Test
    void shouldReturnEmptyWhenNoHeroImageWasChosen() {
        when(siteSettingRepository.findById("hero.image.url")).thenReturn(Optional.empty());

        assertThat(siteSettingService.getHeroImageUrl()).isEmpty();
    }

    @Test
    void shouldSaveTheTrimmedHeroImageUrl() {
        when(siteSettingRepository.save(any(SiteSetting.class))).thenAnswer(call -> call.getArgument(0));

        String saved = siteSettingService.updateHeroImageUrl("  " + URL + " ");

        ArgumentCaptor<SiteSetting> captor = ArgumentCaptor.forClass(SiteSetting.class);
        verify(siteSettingRepository).save(captor.capture());
        assertThat(captor.getValue().getKey()).isEqualTo("hero.image.url");
        assertThat(saved).isEqualTo(URL);
    }

    @Test
    void shouldDeleteTheSettingToGoBackToTheDefaultImage() {
        siteSettingService.resetHeroImage();

        verify(siteSettingRepository).deleteById("hero.image.url");
    }
}
