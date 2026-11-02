package com.kfpd.cloud.partner.controller;

import com.kfpd.cloud.common.exception.BusinessException;
import com.kfpd.cloud.partner.dao.cloud.ShowcaseDao;
import com.kfpd.cloud.partner.pojo.vo.ShowcaseQueryVO;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShowcaseControllerTests {
    private final ShowcaseDao dao = mock(ShowcaseDao.class, withSettings().mockMaker("mock-maker-subclass"));
    private final ShowcaseController controller = new ShowcaseController(dao);

    @Test void capsPaginationAndTrimsFilters() {
        controller.page(new ShowcaseQueryVO(-1, 1000, " 精选 ", " 关键词 "));
        verify(dao).page("精选", "关键词", 0, 100);
    }
    @Test void missingOrOfflineDetailsReturnNotFound() {
        when(dao.detail(99L)).thenReturn(null);
        assertThatThrownBy(() -> controller.detail(99L)).isInstanceOf(BusinessException.class)
            .hasMessage("精品内容不存在或已下架");
    }
    @Test void rejectsOverlongFilter() {
        assertThatThrownBy(() -> controller.page(new ShowcaseQueryVO(1, 12, null, "x".repeat(101))))
            .isInstanceOf(BusinessException.class);
        verifyNoInteractions(dao);
    }
    @Test void usesDefaultPageSizeWithoutQuery() {
        controller.page(null);
        verify(dao).page(null, null, 0, 12);
    }
}
