package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.exceptions.IncompleteRateAnswersException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;

// BGSI-233: the wizard sends one option id per visible question
@ExtendWith(MockitoExtension.class)
class RateServiceImplTest {

    @Mock
    private RateQuestionRepository questionRepository;

    @Mock
    private RateOptionRepository optionRepository;

    private List<RateQuestion> questions;

    // The 5 default questions with fake ids: questions 1..5, options 1..16
    @BeforeEach
    void useDefaultQuestions() {
        questions = RateQuestionInitializer.defaultQuestions();
        AtomicLong questionId = new AtomicLong(1);
        AtomicLong optionId = new AtomicLong(1);
        questions.forEach(question -> {
            question.setId(questionId.getAndIncrement());
            question.getOptions().forEach(option -> option.setId(optionId.getAndIncrement()));
        });

        lenient().when(questionRepository.findByActiveTrueOrderByPositionAscIdAsc())
                .thenAnswer(invocation -> questions.stream().filter(RateQuestion::isActive).toList());
        lenient().when(optionRepository.findByIdIn(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return questions.stream().flatMap(question -> question.getOptions().stream())
                    .filter(option -> ids.contains(option.getId()))
                    .toList();
        });
    }

    private RateServiceImpl rateService() {
        return new RateServiceImpl(questionRepository, optionRepository, new RateCalculator());
    }

    private RateQuoteDTO calculate(Long... optionIds) {
        return rateService().calculate(new RateCalculationRequestDTO(List.of(optionIds)));
    }

    @Test
    void shouldCalculateTheExampleOfTheDocumentation() {
        // Restaurante (1), 2 o 3 locales (7), 2 o 3 TPV (10), terraza (13), 100-250 m² (15)
        RateQuoteDTO quote = calculate(1L, 7L, 10L, 13L, 15L);

        // 49 x 1.8 + 15 + 10 + 10
        assertThat(quote.monthlyPrice()).isEqualByComparingTo("123.20");
    }

    @Test
    void shouldFailWhenAQuestionHasNoAnswer() {
        assertThatThrownBy(() -> calculate(1L, 7L, 10L, 13L))
                .isInstanceOf(IncompleteRateAnswersException.class)
                .hasMessageContaining("every question");
    }

    @Test
    void shouldFailWhenAQuestionHasTwoAnswers() {
        assertThatThrownBy(() -> calculate(1L, 2L, 7L, 10L, 13L, 15L))
                .isInstanceOf(IncompleteRateAnswersException.class);
    }

    @Test
    void shouldIgnoreTheAnswerOfAHiddenQuestion() {
        questions.get(4).setActive(false); // "Superficie" hidden by the admin

        // The answer to the hidden question (15) does not count: 49 x 1.8 + 15 + 10
        assertThat(calculate(1L, 7L, 10L, 13L, 15L).monthlyPrice()).isEqualByComparingTo("113.20");
        assertThat(calculate(1L, 7L, 10L, 13L).monthlyPrice()).isEqualByComparingTo("113.20");
    }
}
