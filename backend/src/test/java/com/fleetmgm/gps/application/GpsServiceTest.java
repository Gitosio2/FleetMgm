package com.fleetmgm.gps.application;

import com.fleetmgm.gps.domain.GpsPosition;
import com.fleetmgm.gps.dto.GpsMapper;
import com.fleetmgm.gps.dto.GpsPositionResponse;
import com.fleetmgm.gps.infrastructure.GpsRepository;
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
        GpsPositionResponse responseA = sampleResponse();
        GpsPositionResponse responseB = sampleResponse();
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of(positionA, positionB));
        when(gpsMapper.toResponse(positionA)).thenReturn(responseA);
        when(gpsMapper.toResponse(positionB)).thenReturn(responseB);

        List<GpsPositionResponse> result = gpsService.findLatest();

        assertThat(result).containsExactly(responseA, responseB);
    }

    @Test
    void findLatest_returnsEmptyList_whenNoActiveVehicleHasAPosition() {
        when(gpsRepository.findLatestForAllActiveVehicles()).thenReturn(List.of());

        List<GpsPositionResponse> result = gpsService.findLatest();

        assertThat(result).isEmpty();
    }

    private static GpsPositionResponse sampleResponse() {
        return new GpsPositionResponse(UUID.randomUUID(), UUID.randomUUID(), "1234ABC",
                com.fleetmgm.vehicle.domain.VehicleCategory.LIGHT_VEHICLE,
                40.0, -3.0, 90.0, 50.0, Instant.now(), com.fleetmgm.gps.domain.GpsSource.MOCK);
    }
}
