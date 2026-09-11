package com.kfpd.cloud.manager.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.common.exception.ErrorCode;
import com.kfpd.cloud.manager.pojo.vo.IdListVO;
import com.kfpd.cloud.manager.pojo.vo.PageQueryVO;
import com.kfpd.cloud.manager.pojo.vo.PageVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SystemManagementSupport {

    private static final Logger log = LoggerFactory.getLogger(SystemManagementSupport.class);
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private SystemManagementSupport() {
    }

    static BusinessException notFound(String message) {
        log.warn("Manager service exception: code={}, message={}", ErrorCode.MANAGER_RESOURCE_NOT_FOUND.getCode(), message);
        return new BusinessException(ErrorCode.MANAGER_RESOURCE_NOT_FOUND, message);
    }

    static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            log.warn("Manager service exception: code={}, message={}", ErrorCode.COMMON_BAD_REQUEST.getCode(), message);
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, message);
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

    static int pageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    static int pageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    static int pageNum(PageQueryVO query) {
        return pageNum(query == null ? null : query.pageNum());
    }

    static int pageSize(PageQueryVO query) {
        return pageSize(query == null ? null : query.pageSize());
    }

    static <T> PageVO<T> pageVO(Page<T> page, int pageNum, int pageSize) {
        return new PageVO<>(page.getTotal(), pageNum, pageSize, page.getRecords());
    }
}
