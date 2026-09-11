package com.kfpd.cloud.auth.service;

import com.kfpd.cloud.auth.pojo.LoginResponse;
import com.kfpd.cloud.auth.pojo.TokenValidation;
import com.kfpd.cloud.auth.pojo.vo.LoginVO;
import com.kfpd.cloud.auth.pojo.vo.RefreshTokenVO;

public interface AuthService {

    LoginResponse apiLogin(LoginVO request, String clientIp);

    LoginResponse managerLogin(LoginVO request, String clientIp);

    LoginResponse refreshAccessToken(RefreshTokenVO request);

    TokenValidation validate(String authorization);
}
