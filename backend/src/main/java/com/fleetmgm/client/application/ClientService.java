package com.fleetmgm.client.application;

import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.dto.ClientMapper;
import com.fleetmgm.client.dto.ClientResponse;
import com.fleetmgm.client.dto.CreateClientRequest;
import com.fleetmgm.client.dto.UpdateClientRequest;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> list(Pageable pageable) {
        return PageResponse.from(clientRepository.findAll(pageable).map(clientMapper::toResponse));
    }

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        Client client = clientMapper.toEntity(request);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional(readOnly = true)
    public ClientResponse getById(UUID id) {
        return clientRepository.findById(id)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " not found"));
    }

    @Transactional
    public ClientResponse update(UUID id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " not found"));
        clientMapper.updateEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public void delete(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " not found"));
        client.setDeletedAt(Instant.now());
        clientRepository.save(client);
    }
}
