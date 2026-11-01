package br.com.officyna_execution_production.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/** UserDetails returned by the temporary auth-service adapter. */
public class AuthenticatedUserDetails extends User {
    private final String mechanicId;
    private final String mechanicName;

    public AuthenticatedUserDetails(String username, Collection<? extends GrantedAuthority> authorities,
                                    String mechanicId, String mechanicName) {
        super(username, "", authorities);
        this.mechanicId = mechanicId;
        this.mechanicName = mechanicName;
    }

    public String getMechanicId() {
        return mechanicId;
    }

    public String getMechanicName() {
        return mechanicName;
    }
}
