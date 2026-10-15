package com.bgsistemas.api.rate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

import static com.bgsistemas.api.rate.AdjustmentType.ADDITION;
import static com.bgsistemas.api.rate.AdjustmentType.BASE;
import static com.bgsistemas.api.rate.AdjustmentType.FACTOR;

// Runs when the application starts. If there are no questions yet, it creates the 5 questions
// the wizard had until now (BGSI-233). From then on the admin manages them from the dashboard.
@Component
public class RateQuestionInitializer implements CommandLineRunner {

    private final RateQuestionRepository questionRepository;

    public RateQuestionInitializer(RateQuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() == 0) {
            questionRepository.saveAll(defaultQuestions());
        }
    }

    static List<RateQuestion> defaultQuestions() {
        RateQuestion business = question(0, "Negocio", "¿Qué tipo de negocio tienes?",
                "Elige la opción que mejor describa tu actividad.");
        business.addOption(option("🍽️", "Restaurante", "Comedor con servicio de mesa.", "49.00", BASE));
        business.addOption(option("☕", "Bar o Cafetería", "Barra, terraza y servicio rápido.", "39.00", BASE));
        business.addOption(option("🛵", "Dark Kitchen", "Solo cocina para delivery.", "45.00", BASE));
        business.addOption(option("🚚", "Food truck / Pop-up", "Negocio móvil o temporal.", "35.00", BASE));
        business.addOption(option("🏪", "Otros", "Comercio, panadería, heladería…", "45.00", BASE));

        RateQuestion locations = question(1, "Locales", "¿Cuántos locales tienes?",
                "El precio se ajusta según el número de establecimientos.");
        locations.addOption(option("🏠", "1 local", "Un único establecimiento.", "1.00", FACTOR));
        locations.addOption(option("🏘️", "2 o 3 locales", "Una pequeña cadena.", "1.80", FACTOR));
        locations.addOption(option("🏙️", "4 o más", "Grupo de restauración.", "2.50", FACTOR));

        RateQuestion pos = question(2, "Puntos de venta", "¿Cuántos puntos de venta (TPV) necesitas por local?",
                "Cada caja o terminal donde se cobra cuenta como un punto de venta.");
        pos.addOption(option("🖥️", "1 TPV", "Una sola caja.", "0.00", ADDITION));
        pos.addOption(option("💻", "2 o 3 TPV", "Barra y comedor, por ejemplo.", "15.00", ADDITION));
        pos.addOption(option("🧾", "4 o más", "Varias zonas de cobro.", "30.00", ADDITION));

        RateQuestion space = question(3, "Espacio", "¿Cómo es tu espacio?",
                "La terraza suele necesitar comanderas o terminales extra.");
        space.addOption(option("🪑", "Solo interior", "Comedor o barra interior.", "0.00", ADDITION));
        space.addOption(option("⛱️", "Interior + terraza", "También sirves en terraza.", "10.00", ADDITION));

        RateQuestion surface = question(4, "Superficie", "¿Cuál es la superficie aproximada?",
                "Nos ayuda a calcular la cobertura de red y los equipos.");
        surface.addOption(option("📐", "Menos de 100 m²", "Local pequeño.", "0.00", ADDITION));
        surface.addOption(option("📏", "Entre 100 y 250 m²", "Local mediano.", "10.00", ADDITION));
        surface.addOption(option("🏢", "Más de 250 m²", "Local grande.", "20.00", ADDITION));

        return List.of(business, locations, pos, space, surface);
    }

    private static RateQuestion question(int position, String title, String text, String hint) {
        return new RateQuestion(title, text, hint, position);
    }

    private static RateOption option(String icon, String label, String description, String amount,
                                     AdjustmentType type) {
        return new RateOption(icon, label, description, new BigDecimal(amount), type);
    }
}
