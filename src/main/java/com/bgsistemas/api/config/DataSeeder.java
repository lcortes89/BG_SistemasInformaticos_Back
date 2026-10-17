package com.bgsistemas.api.config;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandRepository;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientRepository;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceRepository;
import com.bgsistemas.api.setting.SiteSetting;
import com.bgsistemas.api.setting.SiteSettingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// Loads the real catalog for development and demos. Only runs with the "dev" profile
// and only when the database is empty, so restarting the app never duplicates data.
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    // BGSI-208: real texts, images and official pages of each product (short on purpose:
    // the "Ver ficha oficial" button takes the visitor to the maker's page for the details)
    record ProductContent(String description, List<String> features, List<String> sectors,
                          String imageUrl, String officialUrl) {
    }

    // Saved in site_setting once the real content is applied, so it is applied only once
    // and never overwrites what the admin changes later in the panel
    static final String REAL_CATALOG_KEY = "catalog.real-content";
    static final String REAL_CATALOG_VERSION = "BGSI-208";

    // The PUDU product had a provisional name before BGSI-208
    private static final Map<String, String> OLD_NAMES = Map.of("Robot de servicio PUDU", "Robot de reparto PuduBot 2");

    private static final String AHORA_IMAGE = "https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/154034724283/original/ApyZAMo3m_TDJUzb4Xw42FEZBCwJooDiRw.png?1728291722";
    private static final String AHORA_URL = "https://www.ahora.es/";
    private static final List<String> SMES = List.of("Pymes", "Comercios", "Hostelería");

    static final Map<String, ProductContent> CATALOG = Map.ofEntries(
            Map.entry("BDP One", new ProductContent(
                    "TPV multiplataforma para Android y Windows, en la nube o en local, que se gestiona desde cualquier lugar.",
                    List.of("Funciona en TPV, tablet, móvil o PC", "Sigue trabajando aunque se caiga la red", "Cumple VeriFactu y TicketBAI"),
                    List.of("Bares", "Restaurantes", "Cafeterías", "Comercios"),
                    "https://www.bdpcenter.com/wp-content/uploads/2025/01/BDP-ONE_.png",
                    "https://www.bdpcenter.com/bdp-one/")),
            Map.entry("BDP .Net", new ProductContent(
                    "El TPV clásico y consolidado de BDP, para un local o para varios gestionados desde una oficina central.",
                    List.of("Botonera personalizable", "Control de stock y mermas", "Varios terminales, con o sin conexión"),
                    List.of("Restaurantes", "Franquicias", "Comercios"),
                    "https://www.bdpcenter.com/wp-content/uploads/2025/02/Group-1261153492-1-1536x978.png",
                    "https://www.bdpcenter.com/bdp-net/")),
            Map.entry("Terminal de cobro CashGuard", new ProductContent(
                    "Cajón de cobro inteligente que cuenta el efectivo y devuelve el cambio exacto, sin descuadres de caja.",
                    List.of("Cambio exacto automático", "Efectivo protegido en un cofre de acero", "Se integra con el TPV"),
                    List.of("Restaurantes", "Panaderías", "Tiendas de alimentación"),
                    "https://www.strongpoint.com/app/uploads/2022/10/cashguard-premium-featured-image_v4-1024x1024.png",
                    "https://www.strongpoint.com/es/soluciones/cajas-de-pago-autoservicio/cashguard-cash-management/")),
            Map.entry("Robot de reparto PuduBot 2", new ProductContent(
                    "Robot que lleva los platos de la cocina a las mesas para que tu equipo dedique más tiempo a los clientes.",
                    List.of("Reparte platos y recoge la vajilla", "Esquiva obstáculos solo", "Recarga completa en unas 3 horas"),
                    List.of("Restaurantes", "Hoteles", "Buffets"),
                    "https://cdn.pudutech.com/img_entertainment_827f906627.webp",
                    "https://www.pudurobotics.com/es/products/pudubot2")),
            Map.entry("AHORA One Almacén", new ProductContent(
                    "Controla tu stock y los movimientos de almacén.",
                    List.of("Entradas y salidas", "Recuentos de inventario", "Lotes y números de serie"),
                    SMES, AHORA_IMAGE, AHORA_URL)),
            Map.entry("AHORA One Ventas", new ProductContent(
                    "Del presupuesto a la factura, con tus clientes en un solo sitio.",
                    List.of("Presupuestos, albaranes y facturas", "Factura electrónica", "TicketBAI y VeriFactu"),
                    SMES, AHORA_IMAGE, AHORA_URL)),
            Map.entry("AHORA One Contratos", new ProductContent(
                    "Automatiza tus cuotas recurrentes.",
                    List.of("Periodicidad y fechas de inicio y fin", "Cuotas automáticas", "Planes de contratos"),
                    List.of("Pymes", "Autónomos"), AHORA_IMAGE, AHORA_URL)),
            Map.entry("AHORA One Compras", new ProductContent(
                    "Proveedores, pedidos y gastos bajo control.",
                    List.of("Pedidos de compra", "Albaranes y facturas", "Documentos adjuntos"),
                    SMES, AHORA_IMAGE, AHORA_URL)),
            Map.entry("AHORA One Cartera", new ProductContent(
                    "Controla cobros y pagos y envía remesas al banco.",
                    List.of("Cobros y pagos", "Remesas SEPA CORE", "Envío a la banca electrónica"),
                    List.of("Pymes", "Autónomos"), AHORA_IMAGE, AHORA_URL)),
            Map.entry("AHORA One TPV", new ProductContent(
                    "TPV para comercios conectado con el ERP.",
                    List.of("Tickets y devoluciones", "Cobros pendientes", "Cierres y arqueos de caja"),
                    List.of("Comercios", "Hostelería"), AHORA_IMAGE, AHORA_URL))
    );

    private final BrandRepository brandRepository;
    private final ServiceRepository serviceRepository;
    private final ClientRepository clientRepository;
    private final SiteSettingRepository siteSettingRepository;

    public DataSeeder(BrandRepository brandRepository, ServiceRepository serviceRepository,
                      ClientRepository clientRepository, SiteSettingRepository siteSettingRepository) {
        this.brandRepository = brandRepository;
        this.serviceRepository = serviceRepository;
        this.clientRepository = clientRepository;
        this.siteSettingRepository = siteSettingRepository;
    }

    @Override
    public void run(String... args) {
        if (brandRepository.count() == 0) {
            seedCatalog();
        }
        if (clientRepository.count() == 0) {
            seedSampleClients();
        }
        if (!siteSettingRepository.existsById(REAL_CATALOG_KEY)) {
            applyRealCatalog();
        }
    }

    private void seedCatalog() {
        Brand bdp = brand("BDP", "Software TPV para bares, restaurantes y cafeterías.",
                "Software para hostelería", "/images/brands/bdp.png");
        service(bdp, "BDP .Net", false);
        service(bdp, "BDP One", true);

        Brand ahora = brand("AHORA One", "Software de gestión (ERP) modular para tu negocio.",
                "Software de gestión (ERP)", "/images/brands/ahora-one.png");
        for (String module : new String[]{"Almacén", "Ventas", "Contratos", "Compras", "Cartera", "TPV"}) {
            service(ahora, "AHORA One " + module, false);
        }

        Brand cashGuard = brand("CashGuard", "Máquinas de cobro automático, fabricadas por StrongPoint.",
                "Hardware: máquinas de cobro", "/images/brands/cashguard.png");
        service(cashGuard, "Terminal de cobro CashGuard", true);

        Brand pudu = brand("PUDU", "Robots de servicio para hostelería.",
                "Hardware: robots de servicio", "/images/brands/pudu.png");
        service(pudu, "Robot de reparto PuduBot 2", true);
    }

    // BGSI-208: real content, once (also on databases created with the provisional texts)
    private void applyRealCatalog() {
        for (Service service : serviceRepository.findAll()) {
            String name = OLD_NAMES.getOrDefault(service.getName(), service.getName());
            Optional.ofNullable(CATALOG.get(name)).ifPresent(content -> {
                service.setName(name);
                service.setDescription(content.description());
                service.setLongDescription(null); // short sheet: the card shows the description
                service.setFeatures(content.features());
                service.setSectors(content.sectors());
                service.setImageUrl(content.imageUrl());
                service.setOfficialUrl(content.officialUrl());
                serviceRepository.save(service);
            });
        }
        siteSettingRepository.save(new SiteSetting(REAL_CATALOG_KEY, REAL_CATALOG_VERSION));
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

    private void service(Brand brand, String name, boolean featured) {
        Service service = new Service(null, name, name, null);
        service.setBrand(brand);
        service.setFeatured(featured);
        serviceRepository.save(service);
    }
}
