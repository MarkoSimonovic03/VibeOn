package com.metropolitan.VibeOn.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityConfigTest {

    private final RoleHierarchy roleHierarchy = SpringSecurityConfig.roleHierarchy();

    @Test
    void moderatorInheritsUserPermissions() {
        assertThat(reachableRoles("ROLE_MODERATOR"))
                .containsExactlyInAnyOrder("ROLE_MODERATOR", "ROLE_USER");
    }

    @Test
    void adminInheritsModeratorAndUserPermissions() {
        assertThat(reachableRoles("ROLE_ADMIN"))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MODERATOR", "ROLE_USER");
    }

    private List<String> reachableRoles(String role) {
        return roleHierarchy.getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority(role))
                ).stream()
                .map(authority -> authority.getAuthority())
                .toList();
    }
}
