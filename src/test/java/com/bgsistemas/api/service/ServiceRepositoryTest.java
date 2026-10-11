package com.bgsistemas.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ServiceRepositoryTest {

    @Autowired
    private ServiceRepository serviceRepository;

    @Test
    void shouldSaveAndFindService() {
        Service service = new Service(null, "Web Design", "Custom website design", "design.jpg");

        Service saved = serviceRepository.save(service);

        assertThat(saved.getId()).isNotNull();
        assertThat(serviceRepository.findById(saved.getId())).isPresent();
    }
}