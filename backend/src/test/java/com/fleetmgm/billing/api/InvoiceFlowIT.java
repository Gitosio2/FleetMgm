package com.fleetmgm.billing.api;

import com.fleetmgm.auth.domain.AppRole;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.dto.AuthResponse;
import com.fleetmgm.auth.dto.LoginRequest;
import com.fleetmgm.auth.infrastructure.UserRepository;
import com.fleetmgm.billing.domain.InvoiceStatus;
import com.fleetmgm.billing.dto.CreateInvoiceRequest;
import com.fleetmgm.billing.dto.InvoiceResponse;
import com.fleetmgm.billing.dto.LineItemRequest;
import com.fleetmgm.billing.dto.LineItemResponse;
import com.fleetmgm.client.domain.Client;
import com.fleetmgm.client.infrastructure.ClientRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Full-flow critical path (CLAUDE.md: "login → job → invoice"): DRAFT -> line item -> issue ->
// pay -> PDF download, exercising InvoiceService's totals computation and PdfExportService
// end-to-end through the real HTTP server.
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class InvoiceFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final String PASSWORD = "correct-horse-battery-1";

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ClientRepository clientRepository;

    @Test
    void draftToPaid_computesTotals_andExportsPdf() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(loginAsAdmin());
        Client client = persistClient();

        ResponseEntity<InvoiceResponse> createResponse = restTemplate.exchange(
                "/api/v1/invoices", HttpMethod.POST,
                new HttpEntity<>(new CreateInvoiceRequest(client.getId(), null, null, null), headers),
                InvoiceResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID invoiceId = createResponse.getBody().id();
        assertThat(createResponse.getBody().status()).isEqualTo(InvoiceStatus.DRAFT);

        ResponseEntity<LineItemResponse> lineItemResponse = restTemplate.exchange(
                "/api/v1/invoices/{id}/line-items", HttpMethod.POST,
                new HttpEntity<>(
                        new LineItemRequest("Transporte", new BigDecimal("2"), new BigDecimal("100.00"), null),
                        headers),
                LineItemResponse.class, invoiceId);
        assertThat(lineItemResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<InvoiceResponse> issueResponse = restTemplate.exchange(
                "/api/v1/invoices/{id}/issue", HttpMethod.PATCH, new HttpEntity<>(headers),
                InvoiceResponse.class, invoiceId);
        assertThat(issueResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(issueResponse.getBody().status()).isEqualTo(InvoiceStatus.ISSUED);
        // Default tax rate (billing.default-tax-rate = 0.2100, application.yml): 200.00 * 0.21 = 42.00.
        assertThat(issueResponse.getBody().subtotal()).isEqualByComparingTo("200.00");
        assertThat(issueResponse.getBody().taxAmount()).isEqualByComparingTo("42.00");
        assertThat(issueResponse.getBody().total()).isEqualByComparingTo("242.00");

        ResponseEntity<InvoiceResponse> payResponse = restTemplate.exchange(
                "/api/v1/invoices/{id}/pay", HttpMethod.PATCH, new HttpEntity<>(headers),
                InvoiceResponse.class, invoiceId);
        assertThat(payResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payResponse.getBody().status()).isEqualTo(InvoiceStatus.PAID);

        ResponseEntity<byte[]> pdfResponse = restTemplate.exchange(
                "/api/v1/invoices/{id}/pdf", HttpMethod.GET, new HttpEntity<>(headers),
                byte[].class, invoiceId);
        assertThat(pdfResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pdfResponse.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(pdfResponse.getBody()).isNotEmpty();
        assertThat(new String(pdfResponse.getBody(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    private String loginAsAdmin() {
        String email = "invoice-flow-" + UUID.randomUUID() + "@fleetmgm.test";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setAppRole(AppRole.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, PASSWORD), AuthResponse.class);
        return loginResponse.getBody().accessToken();
    }

    private Client persistClient() {
        Client client = new Client();
        client.setName("Test Client");
        client.setTaxId("IT-" + UUID.randomUUID().toString().substring(0, 8));
        return clientRepository.save(client);
    }
}
