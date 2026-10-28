package com.bgsistemas.api.rate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateQuestionInitializerTest {

    @Mock
    private RateQuestionRepository questionRepository;

    @InjectMocks
    private RateQuestionInitializer initializer;

    @Test
    void shouldCreateTheFiveDefaultQuestionsWhenThereAreNone() {
        when(questionRepository.count()).thenReturn(0L);

        initializer.run();

        verify(questionRepository).saveAll(anyList());
    }

    @Test
    void shouldNotTouchTheQuestionsTheAdminAlreadyHas() {
        when(questionRepository.count()).thenReturn(3L);

        initializer.run();

        verify(questionRepository, never()).saveAll(anyList());
    }

    @Test
    void defaultQuestionsKeepTheOrderAndTheOptionsOfTheWizard() {
        List<RateQuestion> questions = RateQuestionInitializer.defaultQuestions();

        assertThat(questions).extracting(RateQuestion::getTitle)
                .containsExactly("Negocio", "Locales", "Puntos de venta", "Espacio", "Superficie");
        assertThat(questions).extracting(question -> question.getOptions().size()).containsExactly(5, 3, 3, 2, 3);
        assertThat(questions.get(0).getOptions()).allMatch(option -> option.getAdjustmentType() == AdjustmentType.BASE);
        assertThat(questions.get(0).getOptions().get(0).getQuestion()).isSameAs(questions.get(0));
    }
}
