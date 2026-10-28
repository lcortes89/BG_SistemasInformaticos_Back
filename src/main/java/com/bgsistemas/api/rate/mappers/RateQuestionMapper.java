package com.bgsistemas.api.rate.mappers;

import com.bgsistemas.api.rate.RateOption;
import com.bgsistemas.api.rate.RateQuestion;
import com.bgsistemas.api.rate.dtos.AdminRateOptionDTO;
import com.bgsistemas.api.rate.dtos.AdminRateQuestionDTO;
import com.bgsistemas.api.rate.dtos.RateOptionRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuestionRequestDTO;
import com.bgsistemas.api.rate.dtos.RateStepDTO;
import com.bgsistemas.api.rate.dtos.RateStepOptionDTO;
import org.springframework.stereotype.Component;

@Component
public class RateQuestionMapper {

    // Entity -> public wizard (no prices)
    public RateStepDTO toStepDTO(RateQuestion question) {
        return new RateStepDTO(
                question.getId(),
                question.getTitle(),
                question.getQuestion(),
                question.getHint(),
                question.getOptions().stream()
                        .map(option -> new RateStepOptionDTO(option.getId(), option.getIcon(),
                                option.getLabel(), option.getDescription()))
                        .toList()
        );
    }

    // Entity -> admin panel (with prices)
    public AdminRateQuestionDTO toAdminDTO(RateQuestion question) {
        return new AdminRateQuestionDTO(
                question.getId(),
                question.getTitle(),
                question.getQuestion(),
                question.getHint(),
                question.getPosition(),
                question.isActive(),
                question.getOptions().stream().map(this::toAdminOptionDTO).toList()
        );
    }

    public AdminRateOptionDTO toAdminOptionDTO(RateOption option) {
        return new AdminRateOptionDTO(
                option.getId(),
                option.getIcon(),
                option.getLabel(),
                option.getDescription(),
                option.getAmount(),
                option.getAdjustmentType(),
                option.getPosition()
        );
    }

    // Admin form -> entity
    public RateQuestion toEntity(RateQuestionRequestDTO request) {
        RateQuestion question = new RateQuestion(request.title().trim(), request.question().trim(),
                blankToNull(request.hint()), 0);
        question.setActive(request.active() == null || request.active());
        return question;
    }

    public RateOption toEntity(RateOptionRequestDTO request) {
        return new RateOption(blankToNull(request.icon()), request.label().trim(),
                blankToNull(request.description()), request.amount(), request.adjustmentType());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
