package com.kfpd.cloud.partner.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.kfpd.cloud.partner.dao.VipUserDao;
import com.kfpd.cloud.partner.pojo.UserProfile;
import com.kfpd.cloud.partner.pojo.VipUser;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final VipUserDao vipUserDao;

    public UserService(VipUserDao vipUserDao) {
        this.vipUserDao = vipUserDao;
    }

    public UserProfile currentUser(String username, String roles) {
        // Username comes from the gateway after token validation.
        VipUser vipUser = Optional.ofNullable(vipUserDao.findByUsername(username)).orElseGet(() -> fallbackUser(username));
        return new UserProfile(vipUser.getUsername(), vipUser.getDisplayName(), splitHeader(roles), LocalDateTime.now());
    }

    private VipUser fallbackUser(String username) {
        VipUser vipUser = new VipUser();
        vipUser.setUsername(username);
        vipUser.setDisplayName("demo-user");
        return vipUser;
    }

    private List<String> splitHeader(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
