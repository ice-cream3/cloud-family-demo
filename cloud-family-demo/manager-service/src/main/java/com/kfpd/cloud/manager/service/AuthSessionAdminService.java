package com.kfpd.cloud.manager.service;

import java.util.List;

import com.kfpd.cloud.manager.pojo.vo.AuthSessionStatusVO;

public interface AuthSessionAdminService {

    List<AuthSessionStatusVO> apiSessionStatuses(List<String> usernames, String authorization);

    int kickOutApiUser(String username, String authorization);
}
