package com.bgsistemas.api.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    // SELECT * FROM client WHERE published = true
    List<Client> findByPublishedTrue();
}