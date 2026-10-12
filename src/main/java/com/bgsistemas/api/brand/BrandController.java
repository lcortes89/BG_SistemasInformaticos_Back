package com.bgsistemas.api.brand;

import com.bgsistemas.api.service.ServiceDTO;
import com.bgsistemas.api.service.ServiceMapper;
import com.bgsistemas.api.service.ServiceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;
    private final BrandMapper brandMapper;
    private final ServiceService serviceService;   // NEW
    private final ServiceMapper serviceMapper;     // NEW

    public BrandController(BrandService brandService, BrandMapper brandMapper,
                           ServiceService serviceService, ServiceMapper serviceMapper) {
        this.brandService = brandService;
        this.brandMapper = brandMapper;
        this.serviceService = serviceService;
        this.serviceMapper = serviceMapper;
    }

    @GetMapping
    public List<BrandDTO> getAllBrands() {
        return brandService.getAllBrands().stream()
                .map(brandMapper::toDTO)
                .collect(Collectors.toList());
    }

    // NEW: GET /api/brands/{id}/services
    @GetMapping("/{id}/services")
    public List<ServiceDTO> getServicesByBrand(@PathVariable Long id) {
        return serviceService.getServicesByBrandId(id).stream()
                .map(serviceMapper::toDTO)
                .collect(Collectors.toList());
    }
}