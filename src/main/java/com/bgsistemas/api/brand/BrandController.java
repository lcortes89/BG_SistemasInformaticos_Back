package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.dtos.BrandDTO;
import com.bgsistemas.api.brand.mappers.BrandMapper;
import com.bgsistemas.api.globals.PageResponseDTO;
import com.bgsistemas.api.service.ServiceService;
import com.bgsistemas.api.service.dtos.ServiceDTO;
import com.bgsistemas.api.service.mappers.ServiceMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private static final int MAX_PAGE_SIZE = 50;

    private final BrandService brandService;
    private final BrandMapper brandMapper;
    private final ServiceService serviceService;
    private final ServiceMapper serviceMapper;

    public BrandController(BrandService brandService, BrandMapper brandMapper,
                           ServiceService serviceService, ServiceMapper serviceMapper) {
        this.brandService = brandService;
        this.brandMapper = brandMapper;
        this.serviceService = serviceService;
        this.serviceMapper = serviceMapper;
    }

    // GET /api/brands -> only active brands (the admin can hide a brand without deleting it)
    @GetMapping
    public List<BrandDTO> getActiveBrands() {
        return brandService.getActiveBrands().stream()
                .map(brandMapper::toDTO)
                .toList();
    }

    // GET /api/brands/{id}/services?page=0&size=4 -> one page of products (BGSI-246), 404 if the brand does not exist
    @GetMapping("/{id}/services")
    public PageResponseDTO<ServiceDTO> getServicesByBrand(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "4") int size) {
        brandService.getBrandById(id);
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by("id"));
        return PageResponseDTO.from(serviceService.getServicesPage(id, pageRequest).map(serviceMapper::toDTO));
    }
}
