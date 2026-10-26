package com.bgsistemas.api.service;

import com.bgsistemas.api.globals.ActiveRequestDTO;
import com.bgsistemas.api.globals.PageResponseDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
import com.bgsistemas.api.service.dtos.AdminServiceDTO;
import com.bgsistemas.api.service.dtos.ServiceRequestDTO;
import com.bgsistemas.api.service.mappers.ServiceMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/services")
public class AdminServiceController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ServiceService serviceService;
    private final ServiceMapper serviceMapper;

    public AdminServiceController(ServiceService serviceService, ServiceMapper serviceMapper) {
        this.serviceService = serviceService;
        this.serviceMapper = serviceMapper;
    }

    // GET /api/admin/services?brandId=1&q=bdp&active=true&page=0&size=10 -> by name (q and active: BGSI-302)
    @GetMapping
    public PageResponseDTO<AdminServiceDTO> getServices(@RequestParam(required = false) Long brandId,
                                                        @RequestParam(required = false) String q,
                                                        @RequestParam(required = false) Boolean active,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("name"));
        return PageResponseDTO.from(
                serviceService.getServicesPage(brandId, q, active, pageRequest).map(serviceMapper::toAdminDTO));
    }

    // POST /api/admin/services -> 201 Created
    @PostMapping
    public ResponseEntity<AdminServiceDTO> createService(@Valid @RequestBody ServiceRequestDTO request) {
        Service saved = serviceService.createService(serviceMapper.toEntity(request), request.brandId());
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceMapper.toAdminDTO(saved));
    }

    // PUT /api/admin/services/{id} -> the whole service is replaced with the form data
    @PutMapping("/{id}")
    public AdminServiceDTO updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequestDTO request) {
        Service updated = serviceService.updateService(id, serviceMapper.toEntity(request), request.brandId());
        return serviceMapper.toAdminDTO(updated);
    }

    // PATCH /api/admin/services/{id}/active { "active": false } -> hidden from the website (BGSI-309)
    @PatchMapping("/{id}/active")
    public AdminServiceDTO setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequestDTO request) {
        return serviceMapper.toAdminDTO(serviceService.setServiceActive(id, request.active()));
    }

    // DELETE /api/admin/services/{id} -> 204 No Content. Needs the password (BGSI-275)
    @RequiresConfirmation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
