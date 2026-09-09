package com.kfpd.cloud.userapi.service;

import java.time.LocalDateTime;
import java.util.List;

import com.kfpd.cloud.userapi.pojo.UserProfile;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserProfile currentUser(String username) {
        // Username comes from the gateway after token validation.
        return new UserProfile(username, "demo-user", List.of("USER"), LocalDateTime.now());
    }
}
