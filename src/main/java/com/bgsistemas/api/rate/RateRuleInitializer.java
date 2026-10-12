package com.bgsistemas.api.rate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

import static com.bgsistemas.api.rate.AdjustmentType.ADDITION;
import static com.bgsistemas.api.rate.AdjustmentType.BASE;
import static com.bgsistemas.api.rate.AdjustmentType.FACTOR;
import static com.bgsistemas.api.rate.RateCategory.BUSINESS_TYPE;
import static com.bgsistemas.api.rate.RateCategory.NUM_LOCATIONS;
import static com.bgsistemas.api.rate.RateCategory.POS_COUNT;
import static com.bgsistemas.api.rate.RateCategory.SQUARE_METERS;
import static com.bgsistemas.api.rate.RateCategory.TERRACE;

// Runs when the application starts. The prices then live in the database
// and the admin changes them from the dashboard (BGSI-149), not in this code.
@Component
public class RateRuleInitializer implements CommandLineRunner {

    private final RateRuleRepository rateRuleRepository;

    public RateRuleInitializer(RateRuleRepository rateRuleRepository) {
        this.rateRuleRepository = rateRuleRepository;
    }

    @Override
    public void run(String... args) {
        if (rateRuleRepository.count() == 0) {
            rateRuleRepository.saveAll(defaultRules());
        }
    }

    static List<RateRule> defaultRules() {
        return List.of(
                rule(BUSINESS_TYPE, "RESTAURANT", "Restaurante", "49.00", BASE),
                rule(BUSINESS_TYPE, "BAR_CAFE", "Bar o Cafetería", "39.00", BASE),
                rule(BUSINESS_TYPE, "DARK_KITCHEN", "Dark Kitchen", "45.00", BASE),
                rule(BUSINESS_TYPE, "FOOD_TRUCK", "Food truck / Pop-up", "35.00", BASE),
                rule(BUSINESS_TYPE, "OTHER", "Otros (comercio, panadería...)", "45.00", BASE),

                rule(NUM_LOCATIONS, "ONE", "1 local", "1.00", FACTOR),
                rule(NUM_LOCATIONS, "TWO_THREE", "2 o 3 locales", "1.80", FACTOR),
                rule(NUM_LOCATIONS, "FOUR_PLUS", "4 o más locales", "2.50", FACTOR),

                rule(POS_COUNT, "ONE", "1 punto de venta", "0.00", ADDITION),
                rule(POS_COUNT, "TWO_THREE", "2 o 3 puntos de venta", "15.00", ADDITION),
                rule(POS_COUNT, "FOUR_PLUS", "4 o más puntos de venta", "30.00", ADDITION),

                rule(TERRACE, "INDOOR", "Solo comedor interior", "0.00", ADDITION),
                rule(TERRACE, "WITH_TERRACE", "Comedor + terraza", "10.00", ADDITION),

                rule(SQUARE_METERS, "SMALL", "Menos de 100 m²", "0.00", ADDITION),
                rule(SQUARE_METERS, "MEDIUM", "Entre 100 y 250 m²", "10.00", ADDITION),
                rule(SQUARE_METERS, "LARGE", "Más de 250 m²", "20.00", ADDITION)
        );
    }

    private static RateRule rule(RateCategory category, String optionCode, String label,
                                 String amount, AdjustmentType type) {
        return new RateRule(category, optionCode, label, new BigDecimal(amount), type);
    }
}
