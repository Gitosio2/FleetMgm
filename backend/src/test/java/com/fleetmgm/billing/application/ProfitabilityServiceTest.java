package com.fleetmgm.billing.application;

import com.fleetmgm.billing.domain.Invoice;
import com.fleetmgm.billing.domain.InvoiceLineItem;
import com.fleetmgm.billing.domain.SupplierInvoice;
import com.fleetmgm.billing.domain.SupplierInvoiceLineItem;
import com.fleetmgm.billing.dto.MonthlyFinancialResponse;
import com.fleetmgm.billing.dto.ProfitabilityResponse;
import com.fleetmgm.billing.dto.VehicleExpenseResponse;
import com.fleetmgm.billing.dto.VehicleRevenueLineItemResponse;
import com.fleetmgm.billing.infrastructure.LineItemRepository;
import com.fleetmgm.billing.infrastructure.MonthlyFinancialProjection;
import com.fleetmgm.billing.infrastructure.ProfitabilityRepository;
import com.fleetmgm.billing.infrastructure.SupplierInvoiceLineItemRepository;
import com.fleetmgm.billing.infrastructure.SupplierInvoiceRepository;
import com.fleetmgm.billing.infrastructure.VehicleProfitabilityProjection;
import com.fleetmgm.shared.PageResponse;
import com.fleetmgm.shared.exception.NotFoundException;
import com.fleetmgm.supplier.domain.Supplier;
import com.fleetmgm.vehicle.domain.UsageMeasure;
import com.fleetmgm.vehicle.domain.Vehicle;
import com.fleetmgm.vehicle.infrastructure.UsageLogRepository;
import com.fleetmgm.vehicle.infrastructure.VehicleRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfitabilityServiceTest {

    @Mock ProfitabilityRepository profitabilityRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock LineItemRepository lineItemRepository;
    @Mock UsageLogRepository usageLogRepository;
    @Mock SupplierInvoiceRepository supplierInvoiceRepository;
    @Mock SupplierInvoiceLineItemRepository supplierInvoiceLineItemRepository;
    ProfitabilityService profitabilityService;

    @Test
    void list_computesMargin_asRevenueMinusCosts() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
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
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
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
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
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
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "9999III", "Renault", "Kangoo", new BigDecimal("800.00"), new BigDecimal("300.00"));
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId, null, null))
                .thenReturn(Optional.of(projection));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(buildVehicle(vehicleId, UsageMeasure.KILOMETERS)));

        ProfitabilityResponse response = profitabilityService.getByVehicleId(vehicleId, null, null);

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
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId, null, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profitabilityService.getByVehicleId(vehicleId, null, null))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getCode())
                .isEqualTo("VEHICLE_NOT_FOUND");
    }

    @Test
    void getByVehicleId_forwardsFromAndToFilters_toRepository() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "1010JJJ", "Fiat", "Ducato", new BigDecimal("400.00"), new BigDecimal("100.00"));
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId, from, to))
                .thenReturn(Optional.of(projection));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(buildVehicle(vehicleId, UsageMeasure.KILOMETERS)));

        ProfitabilityResponse response = profitabilityService.getByVehicleId(vehicleId, from, to);

        assertThat(response.revenue()).isEqualByComparingTo("400.00");
        verify(profitabilityRepository).findProfitabilityByVehicleId(vehicleId, from, to);
    }

    // --- usage-scoped costPerUsageUnit/profitPerUsageUnit (getByVehicleId only) ---

    @Test
    void getByVehicleId_computesCostAndProfitPerUsageUnit_whenUsageDataAvailable() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "1010JJJ", "Fiat", "Ducato", new BigDecimal("800.00"), new BigDecimal("300.00"));
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId, from, to))
                .thenReturn(Optional.of(projection));
        Vehicle vehicle = buildVehicle(vehicleId, UsageMeasure.KILOMETERS);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(usageLogRepository.findLatestValueBeforeDate(vehicleId, from)).thenReturn(Optional.of(15000L));
        when(usageLogRepository.findLatestValueUpToDate(vehicleId, to)).thenReturn(Optional.of(15300L));

        ProfitabilityResponse response = profitabilityService.getByVehicleId(vehicleId, from, to);

        // usageInRange = 15300 - 15000 = 300 km; costs 300.00 / 300 = 1.00; margin 500.00 / 300 = 1.666... -> 1.67
        assertThat(response.costPerUsageUnit()).isEqualByComparingTo("1.00");
        assertThat(response.profitPerUsageUnit()).isEqualByComparingTo("1.67");
        assertThat(response.usageMeasure()).isEqualTo(UsageMeasure.KILOMETERS);
    }

    @Test
    void getByVehicleId_returnsNullUsageMetrics_whenNoBaselineBeforeFrom() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "2020KKK", "Fiat", "Ducato", new BigDecimal("800.00"), new BigDecimal("300.00"));
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId, from, null))
                .thenReturn(Optional.of(projection));
        // `to` is unset, so the end value falls back to the vehicle's current cumulative reading
        // (must be non-null here, otherwise computeUsageInRange would short-circuit on the end
        // value alone and never even reach the missing-baseline check this test targets).
        Vehicle vehicle = buildVehicle(vehicleId, UsageMeasure.KILOMETERS);
        vehicle.setCurrentUsageValue(15300L);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        // No usage log recorded before `from` — the period baseline can't be established, so this
        // must NOT fabricate a "0 km before" assumption (see ProfitabilityService's convention).
        when(usageLogRepository.findLatestValueBeforeDate(vehicleId, from)).thenReturn(Optional.empty());

        ProfitabilityResponse response = profitabilityService.getByVehicleId(vehicleId, from, null);

        assertThat(response.costPerUsageUnit()).isNull();
        assertThat(response.profitPerUsageUnit()).isNull();
    }

    @Test
    void getByVehicleId_returnsNullUsageMetrics_whenUsageInRangeIsZero() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "3030LLL", "Fiat", "Ducato", new BigDecimal("800.00"), new BigDecimal("300.00"));
        when(profitabilityRepository.findProfitabilityByVehicleId(vehicleId, from, to))
                .thenReturn(Optional.of(projection));
        Vehicle vehicle = buildVehicle(vehicleId, UsageMeasure.KILOMETERS);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        // Vehicle recorded no movement in the period — start and end readings are identical.
        when(usageLogRepository.findLatestValueBeforeDate(vehicleId, from)).thenReturn(Optional.of(15000L));
        when(usageLogRepository.findLatestValueUpToDate(vehicleId, to)).thenReturn(Optional.of(15000L));

        ProfitabilityResponse response = profitabilityService.getByVehicleId(vehicleId, from, to);

        assertThat(response.costPerUsageUnit()).isNull();
        assertThat(response.profitPerUsageUnit()).isNull();
    }

    @Test
    void list_neverComputesUsageMetrics_andLeavesThemNull() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        VehicleProfitabilityProjection projection = buildProjection(
                vehicleId, "4040MMM", "Fiat", "Ducato", new BigDecimal("800.00"), new BigDecimal("300.00"));
        Pageable pageable = PageRequest.of(0, 20);
        when(profitabilityRepository.findProfitabilityByVehicle(pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));

        PageResponse<ProfitabilityResponse> result = profitabilityService.list(pageable);

        ProfitabilityResponse response = result.content().get(0);
        assertThat(response.costPerUsageUnit()).isNull();
        assertThat(response.profitPerUsageUnit()).isNull();
        assertThat(response.usageMeasure()).isNull();
        // list() is the fleet-wide paged table — computing a per-vehicle usage-log query for every
        // row would be an N+1 query pattern, so it must never touch usageLogRepository at all.
        verifyNoInteractions(usageLogRepository);
    }

    @Test
    void getFinancialTrend_mapsProjections_toResponses() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
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
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
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
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        profitabilityService.getFinancialTrend(0);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(profitabilityRepository).findMonthlyFinancialTrend(fromCaptor.capture(), any());
        YearMonth currentMonth = YearMonth.now();
        assertThat(fromCaptor.getValue()).isEqualTo(currentMonth.atDay(1));
    }

    @Test
    void getFinancialTrend_clampsMonths_whenAboveMaximum() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        when(profitabilityRepository.findMonthlyFinancialTrend(any(), any())).thenReturn(List.of());

        profitabilityService.getFinancialTrend(50);

        ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(profitabilityRepository).findMonthlyFinancialTrend(fromCaptor.capture(), any());
        YearMonth currentMonth = YearMonth.now();
        assertThat(fromCaptor.getValue()).isEqualTo(currentMonth.minusMonths(11).atDay(1));
    }

    @Test
    void getRevenueByVehicle_mapsLineItems_toResponses() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        InvoiceLineItem lineItem = buildLineItem("INV-2026-00001", LocalDate.of(2026, 7, 5), "Transport",
                new BigDecimal("2"), new BigDecimal("50.00"), new BigDecimal("100.00"));
        when(lineItemRepository.findAllByVehicleIdAndPeriod(vehicleId, from, to))
                .thenReturn(List.of(lineItem));

        List<VehicleRevenueLineItemResponse> result = profitabilityService.getRevenueByVehicle(vehicleId, from, to);

        assertThat(result).hasSize(1);
        VehicleRevenueLineItemResponse response = result.get(0);
        assertThat(response.invoiceNumber()).isEqualTo("INV-2026-00001");
        assertThat(response.issueDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(response.description()).isEqualTo("Transport");
        assertThat(response.quantity()).isEqualByComparingTo("2");
        assertThat(response.unitPrice()).isEqualByComparingTo("50.00");
        assertThat(response.subtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void getRevenueByVehicle_throwsNotFoundException_whenVehicleUnknown() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profitabilityService.getRevenueByVehicle(vehicleId, null, null))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getCode())
                .isEqualTo("VEHICLE_NOT_FOUND");
    }

    // --- getExpensesByVehicle (Hito 45 — merged "Historial de gastos" list) ---

    @Test
    void getExpensesByVehicle_mergesAndSortsInvoiceAndLineItemSources_byDateDescending() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        SupplierInvoice invoice = buildSupplierInvoice(
                "Taller Central", "F-2026-0456", LocalDate.of(2026, 7, 1), new BigDecimal("121.00"));
        when(supplierInvoiceRepository.findAllByVehicleIdAndPeriod(vehicleId, null, null))
                .thenReturn(List.of(invoice));
        SupplierInvoiceLineItem lineItem = buildSupplierInvoiceLineItem(
                "Gasolinera Norte", "Gasoil - Toyota Hilux", LocalDate.of(2026, 7, 8), new BigDecimal("60.00"));
        when(supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicleId, null, null))
                .thenReturn(List.of(lineItem));

        List<VehicleExpenseResponse> result = profitabilityService.getExpensesByVehicle(vehicleId, null, null);

        // Line item is dated 2026-07-08 (newer) so it sorts before the 2026-07-01 invoice.
        assertThat(result).hasSize(2);
        assertThat(result.get(0).description()).isEqualTo("Gasolinera Norte: Gasoil - Toyota Hilux");
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 7, 8));
        assertThat(result.get(0).amount()).isEqualByComparingTo("60.00");
        assertThat(result.get(1).description()).isEqualTo("Taller Central – F-2026-0456");
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.get(1).amount()).isEqualByComparingTo("121.00");
    }

    @Test
    void getExpensesByVehicle_omitsInvoiceNumber_whenNull() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        SupplierInvoice invoice = buildSupplierInvoice(
                "Gasolinera Norte", null, LocalDate.of(2026, 7, 5), new BigDecimal("60.50"));
        when(supplierInvoiceRepository.findAllByVehicleIdAndPeriod(vehicleId, null, null))
                .thenReturn(List.of(invoice));
        when(supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicleId, null, null))
                .thenReturn(List.of());

        List<VehicleExpenseResponse> result = profitabilityService.getExpensesByVehicle(vehicleId, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).description()).isEqualTo("Gasolinera Norte");
    }

    @Test
    void getExpensesByVehicle_returnsEmptyList_whenVehicleHasNoExpenses() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        when(supplierInvoiceRepository.findAllByVehicleIdAndPeriod(vehicleId, null, null))
                .thenReturn(List.of());
        when(supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicleId, null, null))
                .thenReturn(List.of());

        List<VehicleExpenseResponse> result = profitabilityService.getExpensesByVehicle(vehicleId, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getExpensesByVehicle_forwardsFromAndToFilters_toBothRepositories() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(new Vehicle()));
        when(supplierInvoiceRepository.findAllByVehicleIdAndPeriod(vehicleId, from, to))
                .thenReturn(List.of());
        when(supplierInvoiceLineItemRepository.findAllByVehicleIdAndPeriod(vehicleId, from, to))
                .thenReturn(List.of());

        profitabilityService.getExpensesByVehicle(vehicleId, from, to);

        verify(supplierInvoiceRepository).findAllByVehicleIdAndPeriod(vehicleId, from, to);
        verify(supplierInvoiceLineItemRepository).findAllByVehicleIdAndPeriod(vehicleId, from, to);
    }

    @Test
    void getExpensesByVehicle_throwsNotFoundException_andNeverQueriesEitherRepository_whenVehicleUnknown() {
        profitabilityService = new ProfitabilityService(
                profitabilityRepository, vehicleRepository, lineItemRepository, usageLogRepository,
                supplierInvoiceRepository, supplierInvoiceLineItemRepository);
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profitabilityService.getExpensesByVehicle(vehicleId, null, null))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getCode())
                .isEqualTo("VEHICLE_NOT_FOUND");
        verifyNoInteractions(supplierInvoiceRepository, supplierInvoiceLineItemRepository);
    }

    private SupplierInvoice buildSupplierInvoice(String supplierName, String supplierInvoiceNumber,
                                                  LocalDate invoiceDate, BigDecimal total) {
        Supplier supplier = new Supplier();
        supplier.setName(supplierName);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);
        invoice.setSupplierInvoiceNumber(supplierInvoiceNumber);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setTotal(total);
        return invoice;
    }

    private SupplierInvoiceLineItem buildSupplierInvoiceLineItem(String supplierName, String description,
                                                                   LocalDate invoiceDate, BigDecimal subtotal) {
        Supplier supplier = new Supplier();
        supplier.setName(supplierName);
        SupplierInvoice invoice = new SupplierInvoice();
        invoice.setSupplier(supplier);
        invoice.setInvoiceDate(invoiceDate);
        SupplierInvoiceLineItem lineItem = new SupplierInvoiceLineItem();
        lineItem.setInvoice(invoice);
        lineItem.setDescription(description);
        lineItem.setSubtotal(subtotal);
        return lineItem;
    }

    // The actual CANCELLED-invoice exclusion is proven at the SQL level in
    // LineItemRepositoryTest.findAllByVehicleIdAndPeriod_excludesLineItems_fromCancelledInvoice —
    // mirroring ProfitabilityRepositoryTest.findProfitabilityByVehicle_excludesRevenue_fromCancelledInvoice's
    // pattern — since this service method only delegates to the repository query; a mocked
    // repository here cannot exercise the real status-allowlist filter.

    private InvoiceLineItem buildLineItem(String invoiceNumber, LocalDate issueDate, String description,
                                           BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(issueDate);
        InvoiceLineItem lineItem = new InvoiceLineItem();
        lineItem.setInvoice(invoice);
        lineItem.setDescription(description);
        lineItem.setQuantity(quantity);
        lineItem.setUnitPrice(unitPrice);
        lineItem.setSubtotal(subtotal);
        return lineItem;
    }

    private MonthlyFinancialProjection buildMonthlyProjection(String month, BigDecimal revenue, BigDecimal costs) {
        return new MonthlyFinancialProjection() {
            @Override public String getMonth() { return month; }
            @Override public BigDecimal getRevenue() { return revenue; }
            @Override public BigDecimal getCosts() { return costs; }
        };
    }

    private Vehicle buildVehicle(UUID vehicleId, UsageMeasure usageMeasure) {
        Vehicle vehicle = new Vehicle() {
            @Override public UUID getId() { return vehicleId; }
        };
        vehicle.setUsageMeasure(usageMeasure);
        return vehicle;
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
