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

import java.util.List;
import java.util.Map;

// Loads the real catalog for development and demos. Only runs with the "dev" profile
// and only when the database is empty, so restarting the app never duplicates data.
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    // BGSI-205: PROVISIONAL product sheets until the owner sends the real texts (BGSI-208).
    // They are written only on products that still have no long description, so they
    // never overwrite what the admin edits in the panel.
    private record Sheet(String longDescription, List<String> features, List<String> sectors) {
    }

    private static final List<String> HOSPITALITY = List.of("Bares", "Restaurantes", "Cafeterías");
    private static final List<String> BUSINESS = List.of("Hostelería", "Comercio", "Pymes");

    private static final Map<String, Sheet> SHEETS = Map.of(
            "BDP .Net", new Sheet(
                    "La versión clásica de BDP, instalada en el ordenador del local. Gestiona mesas, comandas y cobros sin depender de internet.",
                    List.of("Gestión de mesas y salones", "Comandas a cocina", "Cierre de caja", "Funciona sin conexión"),
                    HOSPITALITY),
            "BDP One", new Sheet(
                    "La nueva versión multiplataforma de BDP, para Windows y Android. Toma comandas, cobra y consulta las ventas desde cualquier dispositivo.",
                    List.of("Comandas en mesa desde el móvil", "Envío a cocina e impresoras", "Cierre de caja y arqueo", "Informes de ventas"),
                    HOSPITALITY),
            "Terminal de cobro CashGuard", new Sheet(
                    "Cobro en efectivo automático: el cliente introduce el dinero y la máquina da el cambio exacto, sin que el personal toque el efectivo.",
                    List.of("Cambio exacto automático", "Sin descuadres de caja", "Más higiene en el cobro", "Se conecta con el TPV"),
                    List.of("Hostelería", "Comercio", "Panaderías")),
            "Robot de servicio PUDU", new Sheet(
                    "Robot que lleva los platos de la cocina a las mesas y recoge la vajilla, para que el personal dedique más tiempo a los clientes.",
                    List.of("Lleva platos a las mesas", "Recoge vajilla", "Evita obstáculos", "Varias bandejas por viaje"),
                    List.of("Restaurantes", "Hoteles", "Buffets"))
    );

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
        completeProductSheets();
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

    // BGSI-205: fills the empty product sheets (also on databases created before this change)
    private void completeProductSheets() {
        for (Service service : serviceRepository.findAll()) {
            if (service.getLongDescription() != null) {
                continue; // already written (by the seeder or by the admin)
            }
            Sheet sheet = sheetFor(service.getName());
            if (sheet != null) {
                service.setLongDescription(sheet.longDescription());
                service.setFeatures(sheet.features());
                service.setSectors(sheet.sectors());
                serviceRepository.save(service);
            }
        }
    }

    private Sheet sheetFor(String name) {
        if (SHEETS.containsKey(name)) {
            return SHEETS.get(name);
        }
        if (name.startsWith("AHORA One ")) {
            String module = name.substring("AHORA One ".length()).toLowerCase();
            return new Sheet(
                    "Módulo de " + module + " del ERP AHORA One. Se combina con el resto de módulos para gestionar todo el negocio desde un solo programa.",
                    List.of("Datos compartidos con los demás módulos", "Informes y listados", "Acceso por usuarios y permisos"),
                    BUSINESS);
        }
        return null;
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
