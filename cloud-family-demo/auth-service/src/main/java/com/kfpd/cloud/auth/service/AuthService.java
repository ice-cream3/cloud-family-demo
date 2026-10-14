package com.kfpd.cloud.auth.service;

import com.kfpd.cloud.auth.pojo.dto.LoginResponse;
import com.kfpd.cloud.auth.pojo.dto.LoginRequestContext;
import com.kfpd.cloud.auth.pojo.dto.SessionStatusVO;
import com.kfpd.cloud.auth.pojo.dto.TokenValidation;
import com.kfpd.cloud.auth.pojo.vo.AntiAbuseChallengeVO;
import com.kfpd.cloud.auth.pojo.vo.KickOutVO;
import com.kfpd.cloud.auth.pojo.vo.LoginVO;
import com.kfpd.cloud.auth.pojo.vo.RefreshTokenVO;
import com.kfpd.cloud.auth.pojo.vo.RegisterVO;

import java.util.List;

public interface AuthService {

    LoginResponse apiLogin(LoginVO request, LoginRequestContext context);

    LoginResponse apiRegister(RegisterVO request, LoginRequestContext context);

    AntiAbuseChallengeVO createLoginChallenge(String purpose, LoginRequestContext context);

    LoginResponse managerLogin(LoginVO request, LoginRequestContext context);

    LoginResponse refreshAccessToken(RefreshTokenVO request);

    TokenValidation validate(String authorization);

    void logout(String authorization);

    int kickOut(KickOutVO request, String operatorAuthorization);

    List<SessionStatusVO> apiSessionStatuses(List<String> usernames, String operatorAuthorization);

    int kickOutApiUser(KickOutVO request, String operatorAuthorization);
}
