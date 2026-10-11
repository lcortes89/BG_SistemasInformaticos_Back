package com.bgsistemas.api.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
public class ServiceController {

    private final ServiceService serviceService;
    private final ServiceMapper serviceMapper;

    public ServiceController(ServiceService serviceService, ServiceMapper serviceMapper) {
        this.serviceService = serviceService;
        this.serviceMapper = serviceMapper;
    }

    @GetMapping
    public List<ServiceDTO> getAllServices() {
        return serviceService.getAllServices()
                .stream()
                .map(serviceMapper::toDTO)
                .toList();
    }
}