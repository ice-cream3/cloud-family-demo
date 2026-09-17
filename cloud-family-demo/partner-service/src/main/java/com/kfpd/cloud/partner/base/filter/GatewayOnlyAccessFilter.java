package com.kfpd.cloud.partner.base.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.common.web.GatewayHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class GatewayOnlyAccessFilter extends OncePerRequestFilter {

    @Value("${demo.gateway.internal-token:cloud-family-demo-gateway-internal-token}")
    private String internalToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/users/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestToken = request.getHeader(GatewayHeaders.INTERNAL_TOKEN);
        if (internalToken.equals(requestToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeForbidden(response);
    }

    private void writeForbidden(HttpServletResponse response) throws IOException {
        ErrorCode errorCode = ErrorCode.COMMON_FORBIDDEN;
        String body = "{\"code\":" + errorCode.getCode()
                + ",\"message\":\"Gateway access required\""
                + ",\"data\":null"
                + ",\"timestamp\":\"" + LocalDateTime.now()
                + "\"}";
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(body);
    }
}
