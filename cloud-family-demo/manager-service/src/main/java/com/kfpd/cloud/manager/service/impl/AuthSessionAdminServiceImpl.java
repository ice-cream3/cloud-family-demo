package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Map;

import com.kfpd.cloud.common.web.ApiResponse;
import com.kfpd.cloud.manager.pojo.vo.AuthSessionStatusVO;
import com.kfpd.cloud.manager.service.AuthSessionAdminService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AuthSessionAdminServiceImpl implements AuthSessionAdminService {

    private static final ParameterizedTypeReference<ApiResponse<List<AuthSessionStatusVO>>> STATUS_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<Integer>> KICK_OUT_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient authRestClient;

    public AuthSessionAdminServiceImpl(@Value("${demo.auth-service-url:http://localhost:38081}") String authServiceUrl) {
        this.authRestClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    @Override
    public List<AuthSessionStatusVO> apiSessionStatuses(List<String> usernames, String authorization) {
        if (usernames == null || usernames.isEmpty()) {
            return List.of();
        }
        ApiResponse<List<AuthSessionStatusVO>> response = authRestClient.post()
                .uri("/auth/manager/api-session-status")
                .header(HttpHeaders.AUTHORIZATION, authorization == null ? "" : authorization)
                .body(Map.of("usernames", usernames))
                .retrieve()
                .body(STATUS_RESPONSE_TYPE);
        return response == null || response.data() == null ? List.of() : response.data();
    }

    @Override
    public int kickOutApiUser(String username, String authorization) {
        ApiResponse<Integer> response = authRestClient.post()
                .uri("/auth/manager/api-kick-out")
                .header(HttpHeaders.AUTHORIZATION, authorization == null ? "" : authorization)
                .body(Map.of("username", username))
                .retrieve()
                .body(KICK_OUT_RESPONSE_TYPE);
        return response == null || response.data() == null ? 0 : response.data();
    }
}
