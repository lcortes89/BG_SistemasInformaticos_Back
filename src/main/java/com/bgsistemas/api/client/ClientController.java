package com.bgsistemas.api.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    public ClientController(ClientService clientService, ClientMapper clientMapper) {
        this.clientService = clientService;
        this.clientMapper = clientMapper;
    }

    // GET /api/clients → only published clients (public page)
    @GetMapping
    public List<ClientDTO> getPublishedClients() {
        return clientService.getPublishedClients().stream()
                .map(clientMapper::toDTO)
                .toList();
    }
}