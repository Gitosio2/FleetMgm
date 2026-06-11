package com.fleetmgm.auth.infrastructure;

import com.fleetmgm.auth.domain.RefreshToken;
import com.fleetmgm.auth.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
