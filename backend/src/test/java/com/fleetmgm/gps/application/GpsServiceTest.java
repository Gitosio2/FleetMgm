package com.fleetmgm.gps.application;

import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.dto.GpsMapper;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.infrastructure.GpsRepository;
import com.fleetmgm.vehicle.domain.VehicleCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GpsServiceTest {

    @Mock GpsRepository gpsRepository;
    @Mock GpsMapper gpsMapper;
    @InjectMocks GpsService gpsService;

    @Test
    void findLatest_returnsAllActiveVehiclePositions_mapped() {
        GpsPosition positionA = new GpsPosition();
        GpsPosition positionB = new GpsPosition();
        GpsPositionResponse responseA = sampleResponse(UUID.randomUUID(), VehicleCategory.LIGHT_VEHICLE);
        GpsPositionResponse responseB = sampleResponse(UUID.randomUUID(), VehicleCategory.HEAVY_MACHINERY);
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(positionA, positionB));
        when(gpsMapper.toResponse(positionA)).thenReturn(responseA);
        when(gpsMapper.toResponse(positionB)).thenReturn(responseB);

        List<GpsPositionResponse> result = gpsService.findLatest(null, null);

        assertThat(result).containsExactly(responseA, responseB);
    }

    @Test
    void findLatest_returnsEmptyList_whenNoActiveVehicleHasAPosition() {
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of());

        List<GpsPositionResponse> result = gpsService.findLatest(null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void findLatest_filtersByCategory_whenProvided() {
        GpsPosition positionA = new GpsPosition();
        GpsPosition positionB = new GpsPosition();
        GpsPositionResponse lightVehicle = sampleResponse(UUID.randomUUID(), VehicleCategory.LIGHT_VEHICLE);
        GpsPositionResponse heavyMachinery = sampleResponse(UUID.randomUUID(), VehicleCategory.HEAVY_MACHINERY);
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(positionA, positionB));
        when(gpsMapper.toResponse(positionA)).thenReturn(lightVehicle);
        when(gpsMapper.toResponse(positionB)).thenReturn(heavyMachinery);

        List<GpsPositionResponse> result = gpsService.findLatest(VehicleCategory.HEAVY_MACHINERY, null);

        assertThat(result).containsExactly(heavyMachinery);
    }

    @Test
    void findLatest_filtersByVehicleId_whenProvided() {
        UUID targetVehicleId = UUID.randomUUID();
        GpsPosition positionA = new GpsPosition();
        GpsPosition positionB = new GpsPosition();
        GpsPositionResponse target = sampleResponse(targetVehicleId, VehicleCategory.LIGHT_VEHICLE);
        GpsPositionResponse other = sampleResponse(UUID.randomUUID(), VehicleCategory.LIGHT_VEHICLE);
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(positionA, positionB));
        when(gpsMapper.toResponse(positionA)).thenReturn(target);
        when(gpsMapper.toResponse(positionB)).thenReturn(other);

        List<GpsPositionResponse> result = gpsService.findLatest(null, targetVehicleId);

        assertThat(result).containsExactly(target);
    }

    @Test
    void findLatest_combinesCategoryAndVehicleIdFilters() {
        UUID targetVehicleId = UUID.randomUUID();
        GpsPosition positionA = new GpsPosition();
        GpsPosition positionB = new GpsPosition();
        // Same vehicleId as the target but wrong category — must NOT match; the two filters are AND-combined.
        GpsPositionResponse wrongCategory = sampleResponse(targetVehicleId, VehicleCategory.HEAVY_MACHINERY);
        GpsPositionResponse match = sampleResponse(targetVehicleId, VehicleCategory.LIGHT_VEHICLE);
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(positionA, positionB));
        when(gpsMapper.toResponse(positionA)).thenReturn(wrongCategory);
        when(gpsMapper.toResponse(positionB)).thenReturn(match);

        List<GpsPositionResponse> result =
                gpsService.findLatest(VehicleCategory.LIGHT_VEHICLE, targetVehicleId);

        assertThat(result).containsExactly(match);
    }

    private static GpsPositionResponse sampleResponse(UUID vehicleId, VehicleCategory category) {
        return new GpsPositionResponse(UUID.randomUUID(), vehicleId, "1234ABC", "Toyota", "Hilux", category,
                40.0, -3.0, 90.0, 50.0, Instant.now(), com.fleetmgm.gps.domain.GpsSource.MOCK);
    }
}
