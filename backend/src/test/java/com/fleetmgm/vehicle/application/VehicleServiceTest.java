package com.fleetmgm.vehicle.application;

import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.ConflictException;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.vehicle.domain.DriverVehicleAssignment;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.dto.CreateVehicleRequest;
import com.fleetmgm.vehicle.dto.UpdateVehicleRequest;
import com.fleetmgm.vehicle.dto.VehicleMapper;
import com.fleetmgm.vehicle.dto.VehicleResponse;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.infrastructure.AssignmentRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock VehicleRepository vehicleRepository;
    @Mock AssignmentRepository assignmentRepository;
    @Mock VehicleMapper vehicleMapper;
    @InjectMocks VehicleService vehicleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // --- create ---

    @Test
    void create_persistsAndReturnsDto_whenValid() {
        // licensePlate is null (machinery) → no duplicate check
        CreateVehicleRequest request = new CreateVehicleRequest(
                VehicleCategory.HEAVY_MACHINERY, UsageMeasure.HOURS,
                "Caterpillar", "330", 2021,
                null, null, null, null, null, null, null, null, null, null, null, null);
        Vehicle entity = new Vehicle();
        VehicleResponse expected = sampleResponse(UUID.randomUUID());

        when(vehicleMapper.toEntity(request)).thenReturn(entity);
        when(vehicleRepository.save(entity)).thenReturn(entity);
        when(vehicleMapper.toResponse(entity)).thenReturn(expected);

        VehicleResponse result = vehicleService.create(request);

        assertThat(result).isEqualTo(expected);
        verify(vehicleRepository).save(entity);
        verify(vehicleRepository, never()).existsByLicensePlate(any());
    }

    @Test
    void create_throwsConflict_whenLicensePlateDuplicated() {
        CreateVehicleRequest request = new CreateVehicleRequest(
                VehicleCategory.LIGHT_VEHICLE, UsageMeasure.KILOMETERS,
                "Toyota", "Corolla", 2020,
                "1234ABC", null, null, null, null, null, null, null, null, null, null, null);

        when(vehicleRepository.existsByLicensePlate("1234ABC")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.create(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("VEHICLE_LICENSE_PLATE_CONFLICT"));

        verify(vehicleRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_returnsDto_whenFound() {
        UUID id = UUID.randomUUID();
        Vehicle entity = new Vehicle();
        VehicleResponse expected = sampleResponse(id);

        when(vehicleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(vehicleMapper.toResponse(entity)).thenReturn(expected);

        assertThat(vehicleService.getById(id)).isEqualTo(expected);
    }

    @Test
    void getById_throwsNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(vehicleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("VEHICLE_NOT_FOUND"));
    }

    // --- update ---

    @Test
    void update_throwsConflict_whenLicensePlateUsedByOtherVehicle() {
        UUID id = UUID.randomUUID();
        Vehicle entity = new Vehicle();
        UpdateVehicleRequest request = new UpdateVehicleRequest(
                VehicleCategory.LIGHT_VEHICLE, UsageMeasure.KILOMETERS,
                "Honda", "Civic", 2019,
                "XXXX99", null, null, null, null, null, null, null, null, null);

        when(vehicleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(vehicleRepository.existsByLicensePlateAndIdNot("XXXX99", id)).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.update(id, request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode())
                        .isEqualTo("VEHICLE_LICENSE_PLATE_CONFLICT"));

        verify(vehicleRepository, never()).save(any());
    }

    // --- delete ---

    @Test
    void delete_softDeletes_whenExists() {
        UUID id = UUID.randomUUID();
        Vehicle entity = new Vehicle();

        when(vehicleRepository.findById(id)).thenReturn(Optional.of(entity));
        when(vehicleRepository.save(entity)).thenReturn(entity);

        vehicleService.delete(id);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    // --- list as DRIVER ---

    @Test
    void list_asDriver_returnsOnlyAssignedVehicle() {
        String email = "driver@example.com";
        UUID vehicleId = UUID.randomUUID();

        // Set up SecurityContext as DRIVER using concrete token (avoids mocking interfaces)
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        // Set up assignment
        Vehicle vehicle = new Vehicle();
        DriverVehicleAssignment assignment = mock(DriverVehicleAssignment.class);
        when(assignment.getVehicle()).thenReturn(vehicle);
        VehicleResponse expectedResponse = sampleResponse(vehicleId);

        when(assignmentRepository.findActiveByDriverEmail(email)).thenReturn(Optional.of(assignment));
        when(vehicleMapper.toResponse(vehicle)).thenReturn(expectedResponse);

        PageResponse<VehicleResponse> result = vehicleService.list(PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0)).isEqualTo(expectedResponse);
        assertThat(result.totalElements()).isEqualTo(1L);
        verify(assignmentRepository).findActiveByDriverEmail(email);
        verify(vehicleRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    // --- helpers ---

    private VehicleResponse sampleResponse(UUID id) {
        return new VehicleResponse(id,
                VehicleCategory.LIGHT_VEHICLE, UsageMeasure.KILOMETERS,
                "Toyota", "Corolla", 2020,
                "1234ABC", null, null, null,
                com.fleetmgm.vehicle.domain.VehicleStatus.ACTIVE,
                null, null, null, null, null, null, null, null,
                Instant.now());
    }
}
