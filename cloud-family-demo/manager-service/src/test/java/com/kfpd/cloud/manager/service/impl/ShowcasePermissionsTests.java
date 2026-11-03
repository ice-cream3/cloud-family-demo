package com.kfpd.cloud.manager.service.impl;

import java.util.List;
import java.util.Map;
import com.kfpd.cloud.manager.controller.UserFeatureAdminController;
import com.kfpd.cloud.manager.service.UserFeatureAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShowcasePermissionsTests {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean UserFeatureAdminService service() {
            return mock(UserFeatureAdminService.class, withSettings().mockMaker("mock-maker-subclass"));
        }
        @Bean UserFeatureAdminController controller(UserFeatureAdminService service) {
            return new UserFeatureAdminController(service);
        }
    }

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    private void login(String permission) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "test-manager", "", List.of(new SimpleGrantedAuthority(permission))));
    }

    @Test void queryPermissionDoesNotAllowWritingOrDeleting() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(UserFeatureAdminController.class);
            var service = context.getBean(UserFeatureAdminService.class);
            login("partner:showcase:view");
            controller.page("showcase", null);
            controller.page("commerce-shops", null);
            controller.page("commerce-products", null);
            assertThatThrownBy(() -> controller.update("commerce-products", 1L, Map.of())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.delete("commerce-shops", 1L)).isInstanceOf(AccessDeniedException.class);

            verify(service).findPage("showcase", null);
            assertThatThrownBy(() -> controller.create("showcase", Map.of())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.update("showcase", 1L, Map.of())).isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> controller.delete("showcase", 1L)).isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test void merchantTypesUseIndependentPermissions() {
        try(var context=new AnnotationConfigApplicationContext(Config.class)) {
            var controller=context.getBean(UserFeatureAdminController.class);
            login("partner:showcase:view");
            assertThatThrownBy(()->controller.page("commerce-types",null)).isInstanceOf(AccessDeniedException.class);
            login("partner:shop-type:view");controller.page("commerce-types",null);
            assertThatThrownBy(()->controller.create("commerce-types",Map.of())).isInstanceOf(AccessDeniedException.class);
            login("partner:shop-type:add");controller.create("commerce-types",Map.of());
        }
    }

    @Test void correspondingPermissionsAllowEachAction() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(UserFeatureAdminController.class);
            var service = context.getBean(UserFeatureAdminService.class);
            login("partner:showcase:add"); controller.create("showcase", Map.of());
            login("partner:showcase:edit"); controller.update("showcase", 1L, Map.of());
            login("partner:showcase:delete"); controller.delete("showcase", 1L);
            verify(service).create("showcase", Map.of());
            verify(service).update("showcase", 1L, Map.of());
            verify(service).delete("showcase", 1L);
        }
    }
}
