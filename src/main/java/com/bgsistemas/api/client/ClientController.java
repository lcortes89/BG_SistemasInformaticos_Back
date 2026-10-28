package com.bgsistemas.api.client;

import com.bgsistemas.api.client.dtos.ClientDTO;
import com.bgsistemas.api.client.mappers.ClientMapper;
import com.bgsistemas.api.globals.PageResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    public ClientController(ClientService clientService, ClientMapper clientMapper) {
        this.clientService = clientService;
        this.clientMapper = clientMapper;
    }

    // GET /api/clients?page=0&size=6 -> published clients, one page at a time
    @GetMapping
    public PageResponseDTO<ClientDTO> getPublishedClients(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "6") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("id"));
        return PageResponseDTO.from(
                clientService.getPublishedClients(pageRequest).map(clientMapper::toDTO));
    }
}
