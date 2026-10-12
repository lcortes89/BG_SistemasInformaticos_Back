package com.bgsistemas.api.client;

import com.bgsistemas.api.client.exceptions.ClientNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    public ClientServiceImpl(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    public Page<Client> getPublishedClients(Pageable pageable) {
        return clientRepository.findByPublishedTrue(pageable);
    }

    @Override
    public Page<Client> getClientsPage(Pageable pageable) {
        return clientRepository.findAll(pageable);
    }

    @Override
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    @Override
    public Client createClient(Client client) {
        client.setId(null); // always a new row
        return clientRepository.save(client);
    }

    @Override
    public Client updateClient(Long id, Client changes) {
        Client client = getClientById(id);
        client.setName(changes.getName());
        client.setBusinessType(changes.getBusinessType());
        client.setTestimonial(changes.getTestimonial());
        client.setLogoUrl(changes.getLogoUrl());
        client.setRating(changes.getRating());
        client.setPublished(changes.isPublished());
        return clientRepository.save(client);
    }

    @Override
    public void deleteClient(Long id) {
        clientRepository.delete(getClientById(id));
    }
}
