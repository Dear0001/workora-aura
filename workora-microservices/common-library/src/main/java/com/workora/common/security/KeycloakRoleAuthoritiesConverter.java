package com.workora.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class KeycloakRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addRoles(authorities, jwt.getClaims().get("roles"));

        Object realmAccessClaim = jwt.getClaims().get("realm_access");
        if (realmAccessClaim instanceof Map<?, ?> realmAccess) {
            addRoles(authorities, realmAccess.get("roles"));
        }

        return authorities;
    }

    private void addRoles(Set<GrantedAuthority> authorities, Object claim) {
        if (!(claim instanceof Collection<?> roles)) {
            return;
        }

        for (Object role : roles) {
            if (role instanceof String name && !name.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
            }
        }
    }
}
