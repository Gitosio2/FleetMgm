package com.fleetmgm.client.application;

import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.dto.ClientMapper;
import com.fleetmgm.client.dto.ClientResponse;
import com.fleetmgm.client.dto.CreateClientRequest;
import com.fleetmgm.client.dto.UpdateClientRequest;
import com.fleetmgm.client.infrastructure.ClientRepository;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock ClientRepository clientRepository;
    @Mock ClientMapper clientMapper;
    @InjectMocks ClientService clientService;

    // --- list ---

    @Test
    void list_passesNameAndTaxIdFilters_toRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Client entity = new Client();
        ClientResponse expected = new ClientResponse(UUID.randomUUID(), "Acme", "B123", null, null, null, Instant.now());

        when(clientRepository.search("Acme", "B123", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(clientMapper.toResponse(entity)).thenReturn(expected);

        var result = clientService.list("Acme", "B123", pageable);

        assertThat(result.content()).containsExactly(expected);
        verify(clientRepository).search("Acme", "B123", pageable);
    }

    // --- create ---

    @Test
    void create_persistsAndReturnsDto_whenValid() {
        CreateClientRequest request = new CreateClientRequest("Acme", "B12345678", null, null, null);
        Client entity = new Client();
        ClientResponse expected = new ClientResponse(UUID.randomUUID(), "Acme", "B12345678", null, null, null, Instant.now());

        when(clientRepository.existsByTaxId("B12345678")).thenReturn(false);
        when(clientMapper.toEntity(request)).thenReturn(entity);
        when(clientRepository.save(entity)).thenReturn(entity);
        when(clientMapper.toResponse(entity)).thenReturn(expected);

        ClientResponse result = clientService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(clientRepository).save(entity);
    }

    @Test
    void create_throwsConflict_whenTaxIdDuplicated() {
        CreateClientRequest request = new CreateClientRequest("Acme", "B12345678", null, null, null);
        when(clientRepository.existsByTaxId("B12345678")).thenReturn(true);

        assertThatThrownBy(() -> clientService.create(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("CLIENT_TAX_ID_CONFLICT"));

        verify(clientRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_returnsDto_whenFound() {
        UUID id = UUID.randomUUID();
        Client entity = new Client();
        ClientResponse expected = new ClientResponse(id, "Acme", "B12345678", null, null, null, Instant.now());

        when(clientRepository.findById(id)).thenReturn(Optional.of(entity));
        when(clientMapper.toResponse(entity)).thenReturn(expected);

        assertThat(clientService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("CLIENT_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_throwsConflict_whenTaxIdUsedByOtherClient() {
        UUID id = UUID.randomUUID();
        Client entity = new Client();
        UpdateClientRequest request = new UpdateClientRequest("Acme", "B99999999", null, null, null);

        when(clientRepository.findById(id)).thenReturn(Optional.of(entity));
        when(clientRepository.existsByTaxIdAndIdNot("B99999999", id)).thenReturn(true);

        assertThatThrownBy(() -> clientService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("CLIENT_TAX_ID_CONFLICT"));

        verify(clientRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_softDeletes_whenExists() {
        UUID id = UUID.randomUUID();
        Client entity = new Client();

        when(clientRepository.findById(id)).thenReturn(Optional.of(entity));
        when(clientRepository.save(entity)).thenReturn(entity);

        clientService.delete(id);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }
}
