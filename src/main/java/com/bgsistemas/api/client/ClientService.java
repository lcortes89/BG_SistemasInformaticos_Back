package com.bgsistemas.api.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClientService {

    Page<Client> getPublishedClients(Pageable pageable);
}
