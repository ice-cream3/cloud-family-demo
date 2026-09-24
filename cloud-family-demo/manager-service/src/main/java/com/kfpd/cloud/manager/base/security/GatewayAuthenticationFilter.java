package com.kfpd.cloud.manager.base.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.kfpd.cloud.common.web.GatewayHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String username = request.getHeader(GatewayHeaders.USER_NAME);
        if (hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
            List<SimpleGrantedAuthority> authorities = parseAuthorities(request.getHeader(GatewayHeaders.USER_PERMISSIONS));
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseAuthorities(String permissionsHeader) {
        if (!hasText(permissionsHeader)) {
            return List.of();
        }
        return Arrays.stream(permissionsHeader.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
