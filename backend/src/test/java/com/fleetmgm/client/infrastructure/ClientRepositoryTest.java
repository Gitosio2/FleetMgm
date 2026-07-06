package com.fleetmgm.client.infrastructure;

import com.fleetmgm.client.domain.Client;
import com.fleetmgm.config.AuditorAwareImpl;
import com.fleetmgm.config.JpaAuditingConfig;
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
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import({JpaAuditingConfig.class, AuditorAwareImpl.class})
class ClientRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ClientRepository clientRepository;

    @Test
    void findAll_excludesSoftDeletedClients() {
        Client active = buildClient("B11111111");
        Client deleted = buildClient("B22222222");
        deleted.setDeletedAt(Instant.now());

        clientRepository.saveAll(List.of(active, deleted));

        List<Client> results = clientRepository.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTaxId()).isEqualTo("B11111111");
    }

    @Test
    void existsByTaxId_returnsTrueWhenExists() {
        clientRepository.save(buildClient("B33333333"));

        assertThat(clientRepository.existsByTaxId("B33333333")).isTrue();
        assertThat(clientRepository.existsByTaxId("B99999999")).isFalse();
    }

    @Test
    void existsByTaxIdAndIdNot_returnsFalseForSameEntity() {
        Client client = clientRepository.save(buildClient("B44444444"));

        // same entity → should not conflict with itself
        assertThat(clientRepository.existsByTaxIdAndIdNot("B44444444", client.getId())).isFalse();
    }

    private Client buildClient(String taxId) {
        Client client = new Client();
        client.setName("Test Client");
        client.setTaxId(taxId);
        return client;
    }
}
