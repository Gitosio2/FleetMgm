package com.fleetmgm.config;

import com.fleetmgm.auth.domain.AppRole;
import com.fleetmgm.auth.domain.User;
import com.fleetmgm.auth.infrastructure.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) return;

        createUser("admin@fleetmgm.dev",   "admin123",   AppRole.ADMIN);
        createUser("manager@fleetmgm.dev", "manager123", AppRole.MANAGER);
        createUser("driver@fleetmgm.dev",  "driver123",  AppRole.DRIVER);
    }

    private void createUser(String email, String password, AppRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAppRole(role);
        userRepository.save(user);
    }
}
