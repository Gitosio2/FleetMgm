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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Test
    void search_narrowsByName_partialMatch_caseInsensitive_whenProvided() {
        Supplier match = supplierRepository.save(buildSupplier("Acme Parts", "B55555555"));
        buildAndSave("Ferretería Central", "B66666666");

        Page<Supplier> result = supplierRepository.search("acme", null, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Supplier::getId).containsExactly(match.getId());
    }

    @Test
    void search_narrowsByTaxId_partialMatch_caseInsensitive_whenProvided() {
        Supplier match = supplierRepository.save(buildSupplier("Acme Parts", "B77777777"));
        buildAndSave("Other Supplier", "B88888888");

        Page<Supplier> result = supplierRepository.search(null, "b7777", PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Supplier::getId).containsExactly(match.getId());
    }

    @Test
    void search_combinesNameAndTaxId_whenBothProvided() {
        Supplier match = supplierRepository.save(buildSupplier("Acme Parts", "B99999991"));
        buildAndSave("Acme Parts", "B99999992");
        buildAndSave("Other Supplier", "B99999993");

        Page<Supplier> result = supplierRepository.search("acme", "b99999991", PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Supplier::getId).containsExactly(match.getId());
    }

    @Test
    void search_returnsAll_whenNoFiltersProvided() {
        buildAndSave("Acme Parts", "C11111111");
        buildAndSave("Other Supplier", "C22222222");

        Page<Supplier> result = supplierRepository.search(null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(2);
    }

    private void buildAndSave(String name, String taxId) {
        supplierRepository.save(buildSupplier(name, taxId));
    }

    private Supplier buildSupplier(String taxId) {
        return buildSupplier("Test Supplier", taxId);
    }

    private Supplier buildSupplier(String name, String taxId) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setTaxId(taxId);
        return supplier;
    }
}
