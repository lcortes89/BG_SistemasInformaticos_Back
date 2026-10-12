package com.bgsistemas.api.service;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByBrandId(Long brandId);
}