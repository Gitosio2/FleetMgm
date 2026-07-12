package com.fleetmgm.gps.application;

import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.dto.GpsMapper;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.domain.DriverVehicleAssignment;
import com.fleetmgm.vehicle.infrastructure.AssignmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsServiceTest {

    @Mock GpsRepository gpsRepository;
    @Mock AssignmentRepository assignmentRepository;
    @Mock GpsMapper gpsMapper;
    @InjectMocks GpsService gpsService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findLatest_asAdmin_returnsAllActiveVehiclePositions() {
        GpsPosition positionA = new GpsPosition();
        GpsPosition positionB = new GpsPosition();
        GpsPositionResponse responseA = sampleResponse();
        GpsPositionResponse responseB = sampleResponse();
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(positionA, positionB));
        when(gpsMapper.toResponse(positionA)).thenReturn(responseA);
        when(gpsMapper.toResponse(positionB)).thenReturn(responseB);

        List<GpsPositionResponse> result = gpsService.findLatest();

        assertThat(result).containsExactly(responseA, responseB);
        verify(assignmentRepository, never()).findActiveByDriverEmail(any());
    }

    @Test
    void findLatest_asDriver_returnsOnlyOwnVehiclePosition() {
        String email = "driver@example.com";
        setAuthentication(email, "ROLE_DRIVER");
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = vehicleWithId(vehicleId);
        DriverVehicleAssignment assignment = mock(DriverVehicleAssignment.class);
        when(assignment.getVehicle()).thenReturn(vehicle);
        GpsPosition position = new GpsPosition();
        GpsPositionResponse expected = sampleResponse();
        when(assignmentRepository.findActiveByDriverEmail(email)).thenReturn(Optional.of(assignment));
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicleId)).thenReturn(Optional.of(position));
        when(gpsMapper.toResponse(position)).thenReturn(expected);

        List<GpsPositionResponse> result = gpsService.findLatest();

        assertThat(result).containsExactly(expected);
        verify(gpsRepository, never()).findLatestForAllActiveVehicles();
    }

    @Test
    void findLatest_asDriver_returnsEmptyList_whenNoActiveAssignment() {
        String email = "driver@example.com";
        setAuthentication(email, "ROLE_DRIVER");
        when(assignmentRepository.findActiveByDriverEmail(email)).thenReturn(Optional.empty());

        List<GpsPositionResponse> result = gpsService.findLatest();

        assertThat(result).isEmpty();
        verify(gpsRepository, never()).findFirstByVehicleIdOrderByRecordedAtDesc(any());
    }

    @Test
    void findLatest_asDriver_returnsEmptyList_whenAssignedVehicleHasNoRecordedPosition() {
        String email = "driver@example.com";
        setAuthentication(email, "ROLE_DRIVER");
        UUID vehicleId = UUID.randomUUID();
        Vehicle vehicle = vehicleWithId(vehicleId);
        DriverVehicleAssignment assignment = mock(DriverVehicleAssignment.class);
        when(assignment.getVehicle()).thenReturn(vehicle);
        when(assignmentRepository.findActiveByDriverEmail(email)).thenReturn(Optional.of(assignment));
        when(gpsRepository.findFirstByVehicleIdOrderByRecordedAtDesc(vehicleId)).thenReturn(Optional.empty());

        List<GpsPositionResponse> result = gpsService.findLatest();

        assertThat(result).isEmpty();
    }

    private static void setAuthentication(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private static Vehicle vehicleWithId(UUID id) {
        Vehicle vehicle = new Vehicle();
        try {
            var field = Vehicle.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(vehicle, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return vehicle;
    }

    private static GpsPositionResponse sampleResponse() {
        return new GpsPositionResponse(UUID.randomUUID(), UUID.randomUUID(), "1234ABC",
                com.fleetmgm.vehicle.domain.VehicleCategory.LIGHT_VEHICLE,
                40.0, -3.0, 90.0, 50.0, Instant.now(), com.fleetmgm.gps.domain.GpsSource.MOCK);
    }
}
