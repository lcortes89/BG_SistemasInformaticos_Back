package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.exceptions.InvalidRateAmountException;
import com.bgsistemas.api.rate.exceptions.RateOptionNotFoundException;
import com.bgsistemas.api.rate.exceptions.RateQuestionNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Transactional
public class RateQuestionServiceImpl implements RateQuestionService {

    private final RateQuestionRepository questionRepository;
    private final RateOptionRepository optionRepository;

    public RateQuestionServiceImpl(RateQuestionRepository questionRepository, RateOptionRepository optionRepository) {
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateQuestion> getAllQuestions() {
        return questionRepository.findAllByOrderByPositionAscIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateQuestion> getActiveSteps() {
        return questionRepository.findByActiveTrueOrderByPositionAscIdAsc().stream()
                .filter(question -> !question.getOptions().isEmpty())
                .toList();
    }

    // A new question always goes at the end of the wizard
    @Override
    public RateQuestion createQuestion(RateQuestion question) {
        question.setId(null);
        question.setPosition((int) questionRepository.count());
        return questionRepository.save(question);
    }

    @Override
    public RateQuestion updateQuestion(Long id, RateQuestion changes) {
        RateQuestion question = findQuestion(id);
        question.setTitle(changes.getTitle());
        question.setQuestion(changes.getQuestion());
        question.setHint(changes.getHint());
        question.setActive(changes.isActive());
        question.getOptions().size(); // loads the options before the transaction ends (for the response)
        return question;
    }

    @Override
    public void deleteQuestion(Long id) {
        questionRepository.delete(findQuestion(id));
    }

    // The admin sends every question id in the new order: [3, 1, 2...]
    @Override
    public List<RateQuestion> reorderQuestions(List<Long> orderedIds) {
        List<RateQuestion> questions = questionRepository.findAllByOrderByPositionAscIdAsc();
        Map<Long, RateQuestion> byId = questions.stream()
                .collect(Collectors.toMap(RateQuestion::getId, Function.identity()));

        if (orderedIds.size() != questions.size() || !new HashSet<>(orderedIds).equals(byId.keySet())) {
            throw new IllegalArgumentException("The new order must contain every question exactly once");
        }

        for (int position = 0; position < orderedIds.size(); position++) {
            byId.get(orderedIds.get(position)).setPosition(position);
        }
        return orderedIds.stream().map(byId::get).toList();
    }

    @Override
    public RateOption addOption(Long questionId, RateOption option) {
        checkAmount(option);
        RateQuestion question = findQuestion(questionId);
        option.setId(null);
        question.addOption(option);
        return optionRepository.save(option);
    }

    @Override
    public RateOption updateOption(Long optionId, RateOption changes) {
        checkAmount(changes);
        RateOption option = findOption(optionId);
        option.setIcon(changes.getIcon());
        option.setLabel(changes.getLabel());
        option.setDescription(changes.getDescription());
        option.setAmount(changes.getAmount());
        option.setAdjustmentType(changes.getAdjustmentType());
        return option;
    }

    @Override
    public void deleteOption(Long optionId) {
        RateOption option = findOption(optionId);
        option.getQuestion().removeOption(option);
        optionRepository.delete(option);
    }

    // A multiplier of 0 would make every quote 0 €
    private void checkAmount(RateOption option) {
        if (option.getAdjustmentType() == AdjustmentType.FACTOR && option.getAmount().signum() <= 0) {
            throw new InvalidRateAmountException("A factor must be greater than 0");
        }
    }

    private RateQuestion findQuestion(Long id) {
        return questionRepository.findById(id).orElseThrow(() -> new RateQuestionNotFoundException(id));
    }

    private RateOption findOption(Long id) {
        return optionRepository.findById(id).orElseThrow(() -> new RateOptionNotFoundException(id));
    }
}
