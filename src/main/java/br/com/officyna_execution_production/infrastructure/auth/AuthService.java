package br.com.officyna_execution_production.infrastructure.auth;

import br.com.officyna_execution_production.infrastructure.security.AuthenticatedUserDetails;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Temporary stand-in for the separate auth service. Keep the filter dependent on
 * this user-loading boundary so its implementation can later call the real service.
 */
@Service
public class AuthService {

    public UserDetails loadUserByUsername(String username, Claims claims) {
        Object roles = claims.get("roles");
        List<SimpleGrantedAuthority> authorities = authorityNames(roles).stream()
                .filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .toList();

        return new AuthenticatedUserDetails(username, authorities,
                claim(claims.get("mechanicId"), roleClaim(roles, "mechanicId")),
                first(claims.get("mechanicName"), roleClaim(roles, "mechanicName"), username));
    }

    private List<String> authorityNames(Object roles) {
        if (roles instanceof String text) return Arrays.stream(text.split(",")).map(String::trim).toList();
        if (roles instanceof Collection<?> values) return values.stream().map(this::roleName).filter(Objects::nonNull).toList();
        if (roles instanceof Map<?, ?> values) {
            Object role = values.get("role");
            if (role == null) role = values.get("authority");
            return role == null ? List.of() : List.of(role.toString());
        }
        return List.of();
    }

    private String roleName(Object value) {
        if (value instanceof Map<?, ?> claims) {
            Object role = claims.get("role");
            if (role == null) role = claims.get("authority");
            return role == null ? null : role.toString();
        }
        return value == null ? null : value.toString();
    }

    private Object roleClaim(Object roles, String key) {
        if (roles instanceof Map<?, ?> roleClaims) return roleClaims.get(key);
        if (roles instanceof Collection<?> entries) {
            for (Object entry : entries) {
                if (entry instanceof Map<?, ?> roleClaims && roleClaims.containsKey(key)) return roleClaims.get(key);
            }
        }
        return null;
    }

    private String claim(Object direct, Object nested) {
        Object value = direct == null ? nested : direct;
        return value == null ? null : value.toString();
    }

    private String first(Object direct, Object nested, String fallback) {
        String value = claim(direct, nested);
        return value == null || value.isBlank() ? fallback : value;
    }
}
