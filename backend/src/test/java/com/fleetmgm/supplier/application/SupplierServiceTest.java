package com.fleetmgm.supplier.application;

import com.fleetmgm.shared.domain.AuditLogHelper;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.supplier.domain.Supplier;
import com.fleetmgm.supplier.dto.CreateSupplierRequest;
import com.fleetmgm.supplier.dto.SupplierMapper;
import com.fleetmgm.supplier.dto.SupplierResponse;
import com.fleetmgm.supplier.dto.UpdateSupplierRequest;
import com.fleetmgm.supplier.infrastructure.SupplierRepository;
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
class SupplierServiceTest {

    @Mock SupplierRepository supplierRepository;
    @Mock SupplierMapper supplierMapper;
    @Mock AuditLogHelper auditLogHelper;
    @InjectMocks SupplierService supplierService;

    // --- list ---

    @Test
    void list_passesNameAndTaxIdFilters_toRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Supplier entity = new Supplier();
        SupplierResponse expected = new SupplierResponse(UUID.randomUUID(), "Acme Parts", "B12345678", null, null, null, Instant.now());

        when(supplierRepository.search("Acme", "B123", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(supplierMapper.toResponse(entity)).thenReturn(expected);

        var result = supplierService.list("Acme", "B123", pageable);

        assertThat(result.content()).containsExactly(expected);
        verify(supplierRepository).search("Acme", "B123", pageable);
    }

    // --- create ---

    @Test
    void create_persistsAndReturnsDto_whenValid() {
        CreateSupplierRequest request = new CreateSupplierRequest("Acme Parts", "B12345678", null, null, null);
        Supplier entity = new Supplier();
        SupplierResponse expected = new SupplierResponse(UUID.randomUUID(), "Acme Parts", "B12345678", null, null, null, Instant.now());

        when(supplierRepository.existsByTaxId("B12345678")).thenReturn(false);
        when(supplierMapper.toEntity(request)).thenReturn(entity);
        when(supplierRepository.save(entity)).thenReturn(entity);
        when(supplierMapper.toResponse(entity)).thenReturn(expected);

        SupplierResponse result = supplierService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(supplierRepository).save(entity);
    }

    @Test
    void create_persistsAndReturnsDto_whenTaxIdOmitted() {
        CreateSupplierRequest request = new CreateSupplierRequest("Acme Parts", null, null, null, null);
        Supplier entity = new Supplier();
        SupplierResponse expected = new SupplierResponse(UUID.randomUUID(), "Acme Parts", null, null, null, null, Instant.now());

        when(supplierMapper.toEntity(request)).thenReturn(entity);
        when(supplierRepository.save(entity)).thenReturn(entity);
        when(supplierMapper.toResponse(entity)).thenReturn(expected);

        SupplierResponse result = supplierService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(supplierRepository, never()).existsByTaxId(any());
    }

    @Test
    void create_throwsConflict_whenTaxIdDuplicated() {
        CreateSupplierRequest request = new CreateSupplierRequest("Acme Parts", "B12345678", null, null, null);
        when(supplierRepository.existsByTaxId("B12345678")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("SUPPLIER_TAX_ID_CONFLICT"));

        verify(supplierRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_returnsDto_whenFound() {
        UUID id = UUID.randomUUID();
        Supplier entity = new Supplier();
        SupplierResponse expected = new SupplierResponse(id, "Acme Parts", "B12345678", null, null, null, Instant.now());

        when(supplierRepository.findById(id)).thenReturn(Optional.of(entity));
        when(supplierMapper.toResponse(entity)).thenReturn(expected);

        assertThat(supplierService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(supplierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_throwsConflict_whenTaxIdUsedByOtherSupplier() {
        UUID id = UUID.randomUUID();
        Supplier entity = new Supplier();
        UpdateSupplierRequest request = new UpdateSupplierRequest("Acme Parts", "B99999999", null, null, null);

        when(supplierRepository.findById(id)).thenReturn(Optional.of(entity));
        when(supplierRepository.existsByTaxIdAndIdNot("B99999999", id)).thenReturn(true);

        assertThatThrownBy(() -> supplierService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("SUPPLIER_TAX_ID_CONFLICT"));

        verify(supplierRepository, never()).save(any());
    }

    @Test
    void update_skipsConflictCheck_whenTaxIdOmitted() {
        UUID id = UUID.randomUUID();
        Supplier entity = new Supplier();
        UpdateSupplierRequest request = new UpdateSupplierRequest("Acme Parts", null, null, null, null);
        SupplierResponse expected = new SupplierResponse(id, "Acme Parts", null, null, null, null, Instant.now());

        when(supplierRepository.findById(id)).thenReturn(Optional.of(entity));
        when(supplierRepository.save(entity)).thenReturn(entity);
        when(supplierMapper.toResponse(entity)).thenReturn(expected);

        SupplierResponse result = supplierService.update(id, request);

        assertThat(result).isEqualTo(expected);
        verify(supplierRepository, never()).existsByTaxIdAndIdNot(any(), any());
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        UpdateSupplierRequest request = new UpdateSupplierRequest("Acme Parts", "B12345678", null, null, null);
        when(supplierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.update(id, request))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_NOT_FOUND"));
    }

    // --- delete ---

    @Test
    void delete_softDeletes_whenExists() {
        UUID id = UUID.randomUUID();
        Supplier entity = new Supplier();

        when(supplierRepository.findById(id)).thenReturn(Optional.of(entity));
        when(supplierRepository.save(entity)).thenReturn(entity);

        supplierService.delete(id);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(supplierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.delete(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("SUPPLIER_NOT_FOUND"));
    }
}
