package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rates")
public class RateController {

    private final RateService rateService;

    public RateController(RateService rateService) {
        this.rateService = rateService;
    }

    // POST /api/rates/calculate -> public: used by the wizard of the website
    @PostMapping("/calculate")
    public RateQuoteDTO calculate(@Valid @RequestBody RateCalculationRequestDTO answers) {
        return rateService.calculate(answers);
    }
}
