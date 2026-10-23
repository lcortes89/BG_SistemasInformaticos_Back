package com.bgsistemas.api.brand;

import com.bgsistemas.api.brand.dtos.AdminBrandDTO;
import com.bgsistemas.api.brand.dtos.BrandRequestDTO;
import com.bgsistemas.api.brand.mappers.BrandMapper;
import com.bgsistemas.api.globals.ActiveRequestDTO;
import com.bgsistemas.api.globals.PageResponseDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
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
@RequestMapping("/api/admin/brands")
public class AdminBrandController {

    private static final int MAX_PAGE_SIZE = 50;

    private final BrandService brandService;
    private final BrandMapper brandMapper;

    public AdminBrandController(BrandService brandService, BrandMapper brandMapper) {
        this.brandService = brandService;
        this.brandMapper = brandMapper;
    }

    // GET /api/admin/brands?page=0&size=10 -> all brands (active or not), by name
    @GetMapping
    public PageResponseDTO<AdminBrandDTO> getBrands(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("name"));
        return PageResponseDTO.from(brandService.getBrandsPage(pageRequest).map(this::toAdminDTO));
    }

    // POST /api/admin/brands -> 201 Created
    @PostMapping
    public ResponseEntity<AdminBrandDTO> createBrand(@Valid @RequestBody BrandRequestDTO request) {
        Brand saved = brandService.createBrand(brandMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toAdminDTO(saved));
    }

    // PUT /api/admin/brands/{id} -> the whole brand is replaced with the form data
    @PutMapping("/{id}")
    public AdminBrandDTO updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequestDTO request) {
        return toAdminDTO(brandService.updateBrand(id, brandMapper.toEntity(request)));
    }

    // PATCH /api/admin/brands/{id}/active { "active": false } -> the brand and its products are hidden (BGSI-309)
    @PatchMapping("/{id}/active")
    public AdminBrandDTO setActive(@PathVariable Long id, @Valid @RequestBody ActiveRequestDTO request) {
        return toAdminDTO(brandService.setBrandActive(id, request.active()));
    }

    // DELETE /api/admin/brands/{id} -> 204 No Content (409 if it still has services). Needs the password (BGSI-275)
    @RequiresConfirmation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }

    private AdminBrandDTO toAdminDTO(Brand brand) {
        return brandMapper.toAdminDTO(brand, brandService.countServices(brand.getId()));
    }
}
