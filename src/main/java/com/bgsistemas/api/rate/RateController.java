package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import com.bgsistemas.api.rate.dtos.RateStepDTO;
import com.bgsistemas.api.rate.mappers.RateQuestionMapper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
public class RateController {

    private final RateService rateService;
    private final RateQuestionService rateQuestionService;
    private final RateQuestionMapper rateQuestionMapper;

    public RateController(RateService rateService, RateQuestionService rateQuestionService,
                          RateQuestionMapper rateQuestionMapper) {
        this.rateService = rateService;
        this.rateQuestionService = rateQuestionService;
        this.rateQuestionMapper = rateQuestionMapper;
    }

    // GET /api/rates/steps -> public: the questions of the wizard, without prices (BGSI-233)
    @GetMapping("/steps")
    public List<RateStepDTO> getSteps() {
        return rateQuestionService.getActiveSteps().stream()
                .map(rateQuestionMapper::toStepDTO)
                .toList();
    }

    // POST /api/rates/calculate  { "optionIds": [1, 7, 10, 13, 15] } -> public: used by the wizard
    @PostMapping("/calculate")
    public RateQuoteDTO calculate(@Valid @RequestBody RateCalculationRequestDTO answers) {
        return rateService.calculate(answers);
    }
}
