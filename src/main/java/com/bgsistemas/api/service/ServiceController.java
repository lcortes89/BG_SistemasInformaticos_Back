package com.bgsistemas.api.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final ServiceMapper serviceMapper;

    public ServiceController(ServiceService serviceService, ServiceMapper serviceMapper) {
        this.serviceService = serviceService;
        this.serviceMapper = serviceMapper;
    }

    // GET /api/services            → all services
    // GET /api/services?featured=true → only featured services
    @GetMapping
    public List<ServiceDTO> getServices(@RequestParam(required = false) Boolean featured) {
        List<Service> services = Boolean.TRUE.equals(featured)
                ? serviceService.getFeaturedServices()
                : serviceService.getAllServices();

        return services.stream()
                .map(serviceMapper::toDTO)
                .toList();
    }
}