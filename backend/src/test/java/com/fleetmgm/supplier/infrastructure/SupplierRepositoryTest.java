package com.fleetmgm.supplier.infrastructure;

import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
import com.fleetmgm.supplier.domain.Supplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class SupplierRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    SupplierRepository supplierRepository;

    @Test
    void findAll_excludesSoftDeletedSuppliers() {
        Supplier active = buildSupplier("B11111111");
        Supplier deleted = buildSupplier("B22222222");
        deleted.setDeletedAt(Instant.now());

        supplierRepository.saveAll(List.of(active, deleted));

        List<Supplier> results = supplierRepository.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTaxId()).isEqualTo("B11111111");
    }

    @Test
    void existsByTaxId_returnsTrueWhenExists() {
        supplierRepository.save(buildSupplier("B33333333"));

        assertThat(supplierRepository.existsByTaxId("B33333333")).isTrue();
        assertThat(supplierRepository.existsByTaxId("B99999999")).isFalse();
    }

    @Test
    void existsByTaxIdAndIdNot_returnsFalseForSameEntity() {
        Supplier supplier = supplierRepository.save(buildSupplier("B44444444"));

        // same entity → should not conflict with itself
        assertThat(supplierRepository.existsByTaxIdAndIdNot("B44444444", supplier.getId())).isFalse();
    }

    @Test
    void save_allowsMultipleSuppliers_withNullTaxId() {
        Supplier first = new Supplier();
        first.setName("Backfilled Supplier A");
        Supplier second = new Supplier();
        second.setName("Backfilled Supplier B");

        assertThatCode(() -> supplierRepository.saveAll(List.of(first, second)))
                .doesNotThrowAnyException();
    }

    private Supplier buildSupplier(String taxId) {
        Supplier supplier = new Supplier();
        supplier.setName("Test Supplier");
        supplier.setTaxId(taxId);
        return supplier;
    }
}
