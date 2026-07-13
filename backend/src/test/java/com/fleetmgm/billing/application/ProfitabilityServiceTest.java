package com.fleetmgm.billing.application;

import com.fleetmgm.billing.dto.MonthlyFinancialResponse;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.infrastructure.MonthlyFinancialProjection;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.VehicleProfitabilityProjection;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void getByVehicleId_mapsProjection_toResponse() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        UUID vehicleId = UUID.randomUUID();
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "9999III", "Renault", "Kangoo", new BigDecimal("800.00"), new BigDecimal("300.00"));
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId))
                .thenReturn(Optional.of(projection));

        ProfitabilityResponse response = profitabilityService.getByVehicleId(vehicleId);

        assertThat(response.vehicleId()).isEqualTo(vehicleId);
        assertThat(response.vehicleLicensePlate()).isEqualTo("9999III");
        assertThat(response.vehicleMake()).isEqualTo("Renault");
        assertThat(response.vehicleModel()).isEqualTo("Kangoo");
        assertThat(response.revenue()).isEqualByComparingTo("800.00");
        assertThat(response.costs()).isEqualByComparingTo("300.00");
        assertThat(response.margin()).isEqualByComparingTo("500.00");
    }

    @Test
    void getByVehicleId_throwsNotFoundException_whenVehicleUnknown() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        UUID vehicleId = UUID.randomUUID();
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profitabilityService.getByVehicleId(vehicleId))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getCode())
                .isEqualTo("VEHICLE_NOT_FOUND");
    }

    @Test
    void getFinancialTrend_mapsProjections_toResponses() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        MonthlyFinancialProjection projection =
                buildMonthlyProjection("2026-06", new BigDecimal("1000.00"), new BigDecimal("400.00"));
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any()))
                .thenReturn(List.of(projection));

        List<MonthlyFinancialResponse> result = profitabilityService.getFinancialTrend(6);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).month()).isEqualTo("2026-06");
        assertThat(result.get(0).revenue()).isEqualByComparingTo("1000.00");
        assertThat(result.get(0).costs()).isEqualByComparingTo("400.00");
    }

    @Test
    void getFinancialTrend_computesFromAndTo_forGivenMonthsWindow() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        profitabilityService.getFinancialTrend(6);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(profitabilityRepository).findMonthlyFinancialTrend(fromCaptor.capture(), toCaptor.capture());

        YearMonth currentMonth = YearMonth.now();
        assertThat(toCaptor.getValue()).isEqualTo(currentMonth.atDay(1));
        assertThat(fromCaptor.getValue()).isEqualTo(currentMonth.minusMonths(5).atDay(1));
    }

    @Test
    void getFinancialTrend_clampsMonths_whenBelowMinimum() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        profitabilityService.getFinancialTrend(0);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(profitabilityRepository).findMonthlyFinancialTrend(fromCaptor.capture(), any());
        YearMonth currentMonth = YearMonth.now();
        assertThat(fromCaptor.getValue()).isEqualTo(currentMonth.atDay(1));
    }

    @Test
    void getFinancialTrend_clampsMonths_whenAboveMaximum() {
        profitabilityService = new ProfitabilityService(profitabilityRepository);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        profitabilityService.getFinancialTrend(50);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(profitabilityRepository).findMonthlyFinancialTrend(fromCaptor.capture(), any());
        YearMonth currentMonth = YearMonth.now();
        assertThat(fromCaptor.getValue()).isEqualTo(currentMonth.minusMonths(11).atDay(1));
    }

    private MonthlyFinancialProjection buildMonthlyProjection(String month, BigDecimal revenue, BigDecimal costs) {
        return new MonthlyFinancialProjection() {
            @Override public String getMonth() { return month; }
            @Override public BigDecimal getRevenue() { return revenue; }
            @Override public BigDecimal getCosts() { return costs; }
        };
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
