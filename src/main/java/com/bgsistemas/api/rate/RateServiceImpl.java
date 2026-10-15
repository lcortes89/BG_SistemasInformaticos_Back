package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.exceptions.IncompleteRateAnswersException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RateServiceImpl implements RateService {

    private final RateQuestionRepository questionRepository;
    private final RateOptionRepository optionRepository;
    private final RateCalculator rateCalculator;

    public RateServiceImpl(RateQuestionRepository questionRepository, RateOptionRepository optionRepository,
                           RateCalculator rateCalculator) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.rateCalculator = rateCalculator;
    }

    // BGSI-233: the wizard sends the id of the option chosen in each visible question
    @Override
    @Transactional(readOnly = true)
    public RateQuoteDTO calculate(RateCalculationRequestDTO answers) {
        Set<Long> visibleQuestionIds = questionRepository.findByActiveTrueOrderByPositionAscIdAsc().stream()
                .filter(question -> !question.getOptions().isEmpty())
                .map(RateQuestion::getId)
                .collect(Collectors.toSet());

        // Answers of hidden questions are left out
        List<RateOption> chosen = optionRepository.findByIdIn(answers.optionIds().stream().distinct().toList())
                .stream()
                .filter(option -> visibleQuestionIds.contains(option.getQuestion().getId()))
                .toList();

        // Exactly one answer per visible question
        Map<Long, Long> answersPerQuestion = chosen.stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId(), Collectors.counting()));
        boolean oneAnswerEach = answersPerQuestion.keySet().equals(visibleQuestionIds)
                && answersPerQuestion.values().stream().allMatch(count -> count == 1);
        if (!oneAnswerEach) {
            throw new IncompleteRateAnswersException("Choose one answer for every question");
        }

        return rateCalculator.calculate(chosen);
    }
}
