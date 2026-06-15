package com.fleetmgm.client.infrastructure;

import com.fleetmgm.client.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {}
