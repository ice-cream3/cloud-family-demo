package com.kfpd.cloud.auth.service;

import com.kfpd.cloud.auth.pojo.dto.LoginResponse;
import com.kfpd.cloud.auth.pojo.dto.LoginRequestContext;
import com.kfpd.cloud.auth.pojo.dto.TokenValidation;
import com.kfpd.cloud.auth.pojo.vo.KickOutVO;
import com.kfpd.cloud.auth.pojo.vo.LoginVO;
import com.kfpd.cloud.auth.pojo.vo.RefreshTokenVO;

public interface AuthService {

    LoginResponse apiLogin(LoginVO request, LoginRequestContext context);

    LoginResponse managerLogin(LoginVO request, LoginRequestContext context);

    LoginResponse refreshAccessToken(RefreshTokenVO request);

    TokenValidation validate(String authorization);

    void logout(String authorization);

    int kickOut(KickOutVO request, String operatorAuthorization);
}
