package com.bgsistemas.api.export;

import com.bgsistemas.api.brand.Brand;
import com.bgsistemas.api.brand.BrandService;
import com.bgsistemas.api.client.Client;
import com.bgsistemas.api.client.ClientService;
import com.bgsistemas.api.service.Service;
import com.bgsistemas.api.service.ServiceService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Only ADMIN (SecurityConfig protects /api/admin/**). Exports EVERY record, not only one page (BGSI-253)
@RestController
@RequestMapping("/api/admin")
public class AdminExportController {

    private static final Pageable ALL_BY_NAME = Pageable.unpaged(Sort.by("name"));

    private final ClientService clientService;
    private final BrandService brandService;
    private final ServiceService serviceService;
    private final ExportService exportService;

    public AdminExportController(ClientService clientService, BrandService brandService,
                                 ServiceService serviceService, ExportService exportService) {
        this.clientService = clientService;
        this.brandService = brandService;
        this.serviceService = serviceService;
        this.exportService = exportService;
    }

    // GET /api/admin/clients/export?format=pdf|xlsx
    @GetMapping("/clients/export")
    public ResponseEntity<byte[]> exportClients(@RequestParam String format) {
        ExportFormat exportFormat = ExportFormat.from(format); // first: a wrong format is a 400
        List<List<String>> rows = clientService.getClientsPage(ALL_BY_NAME).stream()
                .map(this::clientRow)
                .toList();
        ExportTable table = new ExportTable("Clientes",
                List.of("Nombre", "Tipo de negocio", "Valoración", "Testimonio", "Estado"), rows);
        return exportService.download(table, exportFormat, "clientes");
    }

    // GET /api/admin/brands/export?format=pdf|xlsx
    @GetMapping("/brands/export")
    public ResponseEntity<byte[]> exportBrands(@RequestParam String format) {
        ExportFormat exportFormat = ExportFormat.from(format); // first: a wrong format is a 400
        List<List<String>> rows = brandService.getBrandsPage(ALL_BY_NAME).stream()
                .map(this::brandRow)
                .toList();
        ExportTable table = new ExportTable("Marcas",
                List.of("Nombre", "Categoría", "Descripción", "Nº de productos", "Estado"), rows);
        return exportService.download(table, exportFormat, "marcas");
    }

    // GET /api/admin/services/export?format=pdf|xlsx&brandId=1 -> the same filter as the table
    @GetMapping("/services/export")
    public ResponseEntity<byte[]> exportServices(@RequestParam String format,
                                                 @RequestParam(required = false) Long brandId) {
        ExportFormat exportFormat = ExportFormat.from(format); // first: a wrong format is a 400
        List<List<String>> rows = serviceService.getServicesPage(brandId, ALL_BY_NAME).stream()
                .map(this::serviceRow)
                .toList();
        ExportTable table = new ExportTable("Productos",
                List.of("Nombre", "Marca", "Descripción", "Destacado", "Estado"), rows);
        return exportService.download(table, exportFormat, "productos");
    }

    private List<String> clientRow(Client client) {
        return List.of(
                text(client.getName()),
                text(client.getBusinessType()),
                client.getRating() == null ? "" : client.getRating() + "/5",
                text(client.getTestimonial()),
                client.isPublished() ? "Publicado" : "Oculto");
    }

    private List<String> brandRow(Brand brand) {
        return List.of(
                text(brand.getName()),
                text(brand.getCategory()),
                text(brand.getDescription()),
                String.valueOf(brandService.countServices(brand.getId())),
                brand.isActive() ? "Activa" : "Oculta");
    }

    private List<String> serviceRow(Service service) {
        return List.of(
                text(service.getName()),
                service.getBrand() != null ? text(service.getBrand().getName()) : "",
                text(service.getDescription()),
                service.isFeatured() ? "Sí" : "No",
                service.isActive() ? "Activo" : "Inactivo");
    }

    // List.of does not accept null values
    private String text(String value) {
        return value == null ? "" : value;
    }
}
