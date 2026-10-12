package com.bgsistemas.api.rate;

import com.bgsistemas.api.rate.dtos.RateCalculationRequestDTO;
import com.bgsistemas.api.rate.dtos.RateQuoteDTO;

public interface RateService {

    RateQuoteDTO calculate(RateCalculationRequestDTO answers);
}
