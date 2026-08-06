package com.metropolitan.VibeOn.config;

import com.metropolitan.VibeOn.entity.Role;
import com.metropolitan.VibeOn.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String USER_ROLE = "ROLE_USER";

    @Bean
    CommandLineRunner initData(RoleRepository roleRepository) {
        return args -> {
            createRoleIfMissing(roleRepository, ADMIN_ROLE);
            createRoleIfMissing(roleRepository, USER_ROLE);
        };
    }

    private void createRoleIfMissing(RoleRepository roleRepository, String roleName) {
        roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
    }
}
