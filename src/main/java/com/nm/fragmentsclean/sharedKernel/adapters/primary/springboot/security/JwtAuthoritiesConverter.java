package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        addAuthorities(authorities, jwt.getClaimAsStringList("roles"), "ROLE_");
        addAuthorities(authorities, jwt.getClaimAsStringList("scopes"), "SCOPE_");
        return authorities;
    }

    private void addAuthorities(List<GrantedAuthority> authorities, List<String> values, String prefix) {
        if (values != null) {
            values.forEach(value -> authorities.add(new SimpleGrantedAuthority(prefix + value)));
        }
    }
}
