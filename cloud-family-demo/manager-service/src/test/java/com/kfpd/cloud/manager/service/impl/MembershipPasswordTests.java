package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import com.kfpd.cloud.manager.controller.MembershipPasswordController;
import com.kfpd.cloud.manager.dao.cloud.UserFeatureAdminDao;
import com.kfpd.cloud.manager.pojo.vo.PasswordResetRequestVO;
import com.kfpd.cloud.manager.service.VipUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipPasswordTests {
    @Configuration @EnableMethodSecurity
    static class Config {
        @Bean UserFeatureAdminDao dao() { return mock(UserFeatureAdminDao.class,withSettings().mockMaker("mock-maker-subclass")); }
        @Bean VipUserService users() { return mock(VipUserService.class,withSettings().mockMaker("mock-maker-subclass")); }
        @Bean MembershipPasswordController controller(UserFeatureAdminDao dao,VipUserService users) {
            return new MembershipPasswordController(dao,users);
        }
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private void login(String permission) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin","",List.of(new SimpleGrantedAuthority(permission))));
    }
    @Test void requiresDedicatedPermission() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            login("partner:feature:edit");
            assertThatThrownBy(()->context.getBean(MembershipPasswordController.class).resetPassword(7L,new PasswordResetRequestVO(null,"new-password"))).isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(context.getBean(VipUserService.class),context.getBean(UserFeatureAdminDao.class));
        }
    }
    @Test void resolvesAccountIdInsteadOfResettingMembershipId() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            login("partner:membership:reset-password");
            var dao=context.getBean(UserFeatureAdminDao.class);var users=context.getBean(VipUserService.class);
            when(dao.findMembershipUserId(7L)).thenReturn(91L);
            var request=new PasswordResetRequestVO(null,"new-password");
            context.getBean(MembershipPasswordController.class).resetPassword(7L,request);
            verify(users).resetVipUserPassword(91L,request);
            verify(users,never()).resetVipUserPassword(eq(7L),any());
        }
    }
    @Test void missingAccountDoesNotResetAnyPassword() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            login("partner:membership:reset-password");
            when(context.getBean(UserFeatureAdminDao.class).findMembershipUserId(7L)).thenReturn(null);
            assertThatThrownBy(()->context.getBean(MembershipPasswordController.class).resetPassword(7L,new PasswordResetRequestVO(null,"new-password"))).hasMessageContaining("关联账号不存在");
            verifyNoInteractions(context.getBean(VipUserService.class));
        }
    }
}
