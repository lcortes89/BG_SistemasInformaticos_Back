package com.bgsistemas.api.service;

import java.util.List;

public interface ServiceService {

    List<Service> getAllServices();

    List<Service> getServicesByBrandId(Long brandId);  // NEW
}