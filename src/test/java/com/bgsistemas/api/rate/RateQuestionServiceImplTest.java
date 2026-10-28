package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.exceptions.InvalidRateAmountException;
import com.bgsistemas.api.rate.exceptions.RateQuestionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-233: the admin manages the questions and options of the wizard
@ExtendWith(MockitoExtension.class)
class RateQuestionServiceImplTest {

    @Mock
    private RateQuestionRepository questionRepository;

    @Mock
    private RateOptionRepository optionRepository;

    @InjectMocks
    private RateQuestionServiceImpl service;

    private RateQuestion question(Long id, int position) {
        RateQuestion question = new RateQuestion("Title " + id, "Question " + id + "?", null, position);
        question.setId(id);
        return question;
    }

    @Test
    void shouldPutANewQuestionAtTheEnd() {
        when(questionRepository.count()).thenReturn(5L);
        when(questionRepository.save(any(RateQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RateQuestion saved = service.createQuestion(new RateQuestion("Carta", "¿Tienes carta digital?", null, 0));

        assertThat(saved.getPosition()).isEqualTo(5);
    }

    @Test
    void shouldOnlyShowVisibleQuestionsWithOptions() {
        RateQuestion withOptions = question(1L, 0);
        withOptions.addOption(new RateOption(null, "Yes", null, BigDecimal.TEN, AdjustmentType.ADDITION));
        RateQuestion empty = question(2L, 1);
        when(questionRepository.findByActiveTrueOrderByPositionAscIdAsc()).thenReturn(List.of(withOptions, empty));

        assertThat(service.getActiveSteps()).containsExactly(withOptions);
    }

    @Test
    void shouldReorderTheQuestions() {
        RateQuestion first = question(1L, 0);
        RateQuestion second = question(2L, 1);
        RateQuestion third = question(3L, 2);
        when(questionRepository.findAllByOrderByPositionAscIdAsc()).thenReturn(List.of(first, second, third));

        List<RateQuestion> result = service.reorderQuestions(List.of(3L, 1L, 2L));

        assertThat(result).containsExactly(third, first, second);
        assertThat(third.getPosition()).isZero();
        assertThat(first.getPosition()).isEqualTo(1);
        assertThat(second.getPosition()).isEqualTo(2);
    }

    @Test
    void shouldRejectAnOrderWithMissingOrRepeatedQuestions() {
        when(questionRepository.findAllByOrderByPositionAscIdAsc())
                .thenReturn(List.of(question(1L, 0), question(2L, 1)));

        assertThatThrownBy(() -> service.reorderQuestions(List.of(1L, 1L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.reorderQuestions(List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAddAnOptionAtTheEndOfItsQuestion() {
        RateQuestion question = question(1L, 0);
        question.addOption(new RateOption(null, "First", null, BigDecimal.ONE, AdjustmentType.ADDITION));
        when(questionRepository.findById(1L)).thenReturn(Optional.of(question));
        when(optionRepository.save(any(RateOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RateOption saved = service.addOption(1L,
                new RateOption("💳", "Card", null, new BigDecimal("5.00"), AdjustmentType.ADDITION));

        assertThat(saved.getQuestion()).isSameAs(question);
        assertThat(saved.getPosition()).isEqualTo(1);
        assertThat(question.getOptions()).hasSize(2);
    }

    @Test
    void shouldNotAllowAFactorOfZero() {
        RateOption zeroFactor = new RateOption(null, "Zero", null, BigDecimal.ZERO, AdjustmentType.FACTOR);

        assertThatThrownBy(() -> service.addOption(1L, zeroFactor))
                .isInstanceOf(InvalidRateAmountException.class)
                .hasMessageContaining("greater than 0");
        verify(optionRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenTheQuestionDoesNotExist() {
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteQuestion(99L))
                .isInstanceOf(RateQuestionNotFoundException.class)
                .hasMessage("Rate question with id 99 not found");
    }
}
