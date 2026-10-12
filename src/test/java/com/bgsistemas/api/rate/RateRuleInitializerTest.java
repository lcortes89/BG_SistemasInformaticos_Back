package com.bgsistemas.api.rate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateRuleInitializerTest {

    @Mock
    private RateRuleRepository rateRuleRepository;

    @InjectMocks
    private RateRuleInitializer initializer;

    @Test
    void shouldLoadTheDefaultRulesWhenTheTableIsEmpty() {
        when(rateRuleRepository.count()).thenReturn(0L);

        initializer.run();

        verify(rateRuleRepository).saveAll(anyList());
    }

    @Test
    void shouldNotTouchTheRulesWhenTheyAlreadyExist() {
        when(rateRuleRepository.count()).thenReturn(16L);

        initializer.run();

        verify(rateRuleRepository, never()).saveAll(anyList());
    }
}
