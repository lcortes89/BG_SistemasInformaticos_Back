package com.bgsistemas.api.client;

import com.bgsistemas.api.client.dtos.AdminClientDTO;
import com.bgsistemas.api.client.dtos.ClientRequestDTO;
import com.bgsistemas.api.client.mappers.ClientMapper;
import com.bgsistemas.api.globals.PageResponseDTO;
import com.bgsistemas.api.security.RequiresConfirmation;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/clients")
public class AdminClientController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    public AdminClientController(ClientService clientService, ClientMapper clientMapper) {
        this.clientService = clientService;
        this.clientMapper = clientMapper;
    }

    // GET /api/admin/clients?page=0&size=10 -> all clients (published or not), by name
    @GetMapping
    public PageResponseDTO<AdminClientDTO> getClients(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by("name"));
        return PageResponseDTO.from(clientService.getClientsPage(pageRequest).map(clientMapper::toAdminDTO));
    }

    // POST /api/admin/clients -> 201 Created
    @PostMapping
    public ResponseEntity<AdminClientDTO> createClient(@Valid @RequestBody ClientRequestDTO request) {
        Client saved = clientService.createClient(clientMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(clientMapper.toAdminDTO(saved));
    }

    // PUT /api/admin/clients/{id} -> the whole client is replaced with the form data
    @PutMapping("/{id}")
    public AdminClientDTO updateClient(@PathVariable Long id, @Valid @RequestBody ClientRequestDTO request) {
        return clientMapper.toAdminDTO(clientService.updateClient(id, clientMapper.toEntity(request)));
    }

    // DELETE /api/admin/clients/{id} -> 204 No Content. Needs the password (BGSI-275)
    @RequiresConfirmation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
