package br.com.officyna_execution_production.infrastructure.auth;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private final AuthService service = new AuthService();

    @Test
    void createsAuthoritiesAndMechanicFromDirectClaims() {
        var claims = Jwts.claims(Map.of("roles", "MECHANIC, ADMIN", "mechanicId", "m-1", "mechanicName", "Ana"));

        var user = service.loadUserByUsername("ana@example.com", claims);

        assertEquals("ana@example.com", user.getUsername());
        assertTrue(user.getAuthorities().stream().map(a -> a.getAuthority()).toList()
                .containsAll(List.of("ROLE_MECHANIC", "ROLE_ADMIN")));
        assertEquals("m-1", ((br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails) user).getMechanicId());
        assertEquals("Ana", ((br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails) user).getMechanicName());
    }

    @Test
    void supportsRoleClaimsAsMapsAndUsesUsernameAsNameFallback() {
        var claims = Jwts.claims(Map.of("roles", List.of(Map.of("authority", "MECHANIC", "mechanicId", "m-2"))));

        UserDetails user = service.loadUserByUsername("ana", claims);

        assertEquals(List.of("ROLE_MECHANIC"), user.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        var authenticated = (br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails) user;
        assertEquals("m-2", authenticated.getMechanicId());
        assertEquals("ana", authenticated.getMechanicName());
    }

    @Test
    void acceptsSingleMapRoleAndLeavesUnknownClaimsEmpty() {
        var claims = Jwts.claims(Map.of("roles", Map.of("role", "ROLE_MANAGER")));

        var user = service.loadUserByUsername("manager", claims);

        assertEquals(List.of("ROLE_MANAGER"), user.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        var authenticated = (br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails) user;
        assertNull(authenticated.getMechanicId());
        assertEquals("manager", authenticated.getMechanicName());
    }
}
