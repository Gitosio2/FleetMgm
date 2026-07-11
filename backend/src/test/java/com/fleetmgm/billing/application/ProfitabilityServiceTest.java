package com.fleetmgm.billing.application;

import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.VehicleProfitabilityProjection;
import com.fleetmgm.shared.PageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitabilityServiceTest {

    @Mock ProfitabilityRepository profitabilityRepository;
    ProfitabilityService profitabilityService;

    @Test
    void list_computesMargin_asRevenueMinusCosts() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        UUID vehicleId = UUID.randomUUID();
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "1111AAA", "Toyota", "Hilux", new BigDecimal("1000.00"), new BigDecimal("400.00"));
        Pageable pageable = PageRequest.of(0, 20);
        when(profitabilityRepository.findProfitabilityByVehicle(pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        PageResponse<ProfitabilityResponse> result = profitabilityService.list(pageable);

        assertThat(result.content()).hasSize(1);
        ProfitabilityResponse response = result.content().get(0);
        assertThat(response.vehicleId()).isEqualTo(vehicleId);
        assertThat(response.vehicleLicensePlate()).isEqualTo("1111AAA");
        assertThat(response.vehicleMake()).isEqualTo("Toyota");
        assertThat(response.vehicleModel()).isEqualTo("Hilux");
        assertThat(response.revenue()).isEqualByComparingTo("1000.00");
        assertThat(response.costs()).isEqualByComparingTo("400.00");
        assertThat(response.margin()).isEqualByComparingTo("600.00");
    }

    @Test
    void list_allowsNegativeMargin_whenCostsExceedRevenue() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        UUID vehicleId = UUID.randomUUID();
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "2222BBB", "Ford", "Transit", new BigDecimal("100.00"), new BigDecimal("350.00"));
        Pageable pageable = PageRequest.of(0, 20);
        when(profitabilityRepository.findProfitabilityByVehicle(pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        PageResponse<ProfitabilityResponse> result = profitabilityService.list(pageable);

        ProfitabilityResponse response = result.content().get(0);
        assertThat(response.margin()).isEqualByComparingTo("-250.00");
    }

    @Test
    void list_passesPageableThrough_toRepository() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        Pageable pageable = PageRequest.of(2, 10);
        when(profitabilityRepository.findProfitabilityByVehicle(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        profitabilityService.list(pageable);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(profitabilityRepository).findProfitabilityByVehicle(captor.capture());
        assertThat(captor.getValue()).isEqualTo(pageable);
    }

    private VehicleProfitabilityProjection buildProjection(UUID vehicleId, String licensePlate, String make,
                                                             String model, BigDecimal revenue, BigDecimal costs) {
        return new VehicleProfitabilityProjection() {
            @Override public UUID getVehicleId() { return vehicleId; }
            @Override public String getVehicleLicensePlate() { return licensePlate; }
            @Override public String getVehicleMake() { return make; }
            @Override public String getVehicleModel() { return model; }
            @Override public BigDecimal getRevenue() { return revenue; }
            @Override public BigDecimal getCosts() { return costs; }
        };
    }
}
