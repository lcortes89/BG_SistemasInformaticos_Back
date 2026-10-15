package com.bgsistemas.api.rate;

import java.util.List;

public interface RateQuestionService {

    List<RateQuestion> getAllQuestions();

    // Visible questions that have at least one option (public wizard)
    List<RateQuestion> getActiveSteps();

    RateQuestion createQuestion(RateQuestion question);

    RateQuestion updateQuestion(Long id, RateQuestion changes);

    void deleteQuestion(Long id);

    List<RateQuestion> reorderQuestions(List<Long> orderedIds);

    RateOption addOption(Long questionId, RateOption option);

    RateOption updateOption(Long optionId, RateOption changes);

    void deleteOption(Long optionId);
}
