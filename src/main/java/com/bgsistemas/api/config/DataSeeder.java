package com.bgsistemas.api.config;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// Loads the real catalog for development and demos. Only runs with the "dev" profile
// and only when the database is empty, so restarting the app never duplicates data.
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final BrandRepository brandRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;

    public DataSeeder(BrandRepository brandRepository, ServiceRepository serviceRepository,
                      ClientRepository clientRepository) {
        this.brandRepository = brandRepository;
        this.serviceRepository = serviceRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public void run(String... args) {
        if (brandRepository.count() == 0) {
            seedCatalog();
        }
        if (clientRepository.count() == 0) {
            seedSampleClients();
        }
    }

    private void seedCatalog() {
        Brand bdp = brand("BDP", "Software TPV para bares, restaurantes y cafeterías.",
                "Software para hostelería", "/images/brands/bdp.png");
        service(bdp, "BDP .Net", "La versión clásica de BDP para Windows.", false);
        service(bdp, "BDP One", "La nueva versión multiplataforma, para Windows y Android.", true);

        Brand ahora = brand("AHORA One", "Software de gestión (ERP) modular para tu negocio.",
                "Software de gestión (ERP)", "/images/brands/ahora-one.png");
        for (String module : new String[]{"Almacén", "Ventas", "Contratos", "Compras", "Cartera", "TPV"}) {
            service(ahora, "AHORA One " + module, "Módulo de " + module.toLowerCase() + " de AHORA One.", false);
        }

        Brand cashGuard = brand("CashGuard", "Máquinas de cobro automático, fabricadas por StrongPoint.",
                "Hardware: máquinas de cobro", "/images/brands/cashguard.png");
        service(cashGuard, "Terminal de cobro CashGuard",
                "Cobro en efectivo automático y seguro, sin errores de caja.", true);

        Brand pudu = brand("PUDU", "Robots de servicio para hostelería.",
                "Hardware: robots de servicio", "/images/brands/pudu.png");
        service(pudu, "Robot de servicio PUDU", "Robot que lleva los platos a las mesas.", true);
    }

    private void seedSampleClients() {
        clientRepository.save(new Client(null, "Cliente de ejemplo 1", "Restaurante",
                "Testimonio de ejemplo (datos de demostración).", "/images/clients/sample-1.png", 5, true));
        clientRepository.save(new Client(null, "Cliente de ejemplo 2", "Cafetería",
                "Testimonio de ejemplo (datos de demostración).", "/images/clients/sample-2.png", 4, true));
    }

    private Brand brand(String name, String description, String category, String logoUrl) {
        return brandRepository.save(new Brand(null, name, description, category, logoUrl, true));
    }

    private void service(Brand brand, String name, String description, boolean featured) {
        Service service = new Service(null, name, description, "/images/services/default.png");
        service.setBrand(brand);
        service.setFeatured(featured);
        serviceRepository.save(service);
    }
}
