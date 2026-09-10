package com.kfpd.cloud.manager.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.kfpd.cloud.manager.pojo.vo.IdListVO;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

final class SystemManagementSupport {

    private SystemManagementSupport() {
    }

    static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(NOT_FOUND, message);
    }

    static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, message);
        }
    }

    static List<Long> ids(IdListVO request) {
        if (request == null || request.ids() == null) {
            return List.of();
        }
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long id : request.ids()) {
            if (id != null) {
                distinct.add(id);
            }
        }
        return new ArrayList<>(distinct);
    }

    static String defaultStatus(String status) {
        return status == null || status.isBlank() ? "ENABLED" : status;
    }
}
