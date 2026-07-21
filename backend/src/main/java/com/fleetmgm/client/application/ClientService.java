package com.fleetmgm.client.application;

import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.dto.ClientMapper;
import com.fleetmgm.client.dto.ClientResponse;
import com.fleetmgm.client.dto.CreateClientRequest;
import com.fleetmgm.client.dto.UpdateClientRequest;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.domain.AuditAction;
import com.fleetmgm.shared.domain.AuditLogHelper;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ClientService {

    private static final String ENTITY_TYPE = "Client";

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;
    private final AuditLogHelper auditLogHelper;

    public ClientService(ClientRepository clientRepository, ClientMapper clientMapper, AuditLogHelper auditLogHelper) {
        this.clientRepository = clientRepository;
        this.clientMapper = clientMapper;
        this.auditLogHelper = auditLogHelper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public PageResponse<ClientResponse> list(String name, String taxId, Pageable pageable) {
        return PageResponse.from(clientRepository.search(name, taxId, pageable).map(clientMapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public ClientResponse create(CreateClientRequest request) {
        if (clientRepository.existsByTaxId(request.taxId())) {
            throw new ConflictException("CLIENT_TAX_ID_CONFLICT",
                    "A client with taxId " + request.taxId() + " already exists");
        }
        Client client = clientMapper.toEntity(request);
        var saved = clientMapper.toResponse(clientRepository.save(client));
        auditLogHelper.log(ENTITY_TYPE, saved.id().toString(), AuditAction.CREATE);
        return saved;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public ClientResponse getById(UUID id) {
        return clientRepository.findById(id)
                .map(clientMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " not found"));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public ClientResponse update(UUID id, UpdateClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " not found"));
        if (clientRepository.existsByTaxIdAndIdNot(request.taxId(), id)) {
            throw new ConflictException("CLIENT_TAX_ID_CONFLICT",
                    "A client with taxId " + request.taxId() + " already exists");
        }
        clientMapper.updateEntity(request, client);
        var saved = clientMapper.toResponse(clientRepository.save(client));
        auditLogHelper.log(ENTITY_TYPE, saved.id().toString(), AuditAction.UPDATE);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ADMINISTRATIVE')")
    public void delete(UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CLIENT_NOT_FOUND", "Client " + id + " not found"));
        client.setDeletedAt(Instant.now());
        clientRepository.save(client);
        auditLogHelper.log(ENTITY_TYPE, id.toString(), AuditAction.DELETE);
    }
}
