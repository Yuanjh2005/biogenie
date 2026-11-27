package com.agent.biogenie.config;

import com.agent.biogenie.common.auth.AuthConstants;
import com.agent.biogenie.module.admin.entity.ApiBehaviorLog;
import com.agent.biogenie.module.admin.service.ApiBehaviorLogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class LoggingInterceptorTest {
    @Test
    void authenticatedApiCallShouldCreateBehaviorLog() {
        ApiBehaviorLogService service = mock(ApiBehaviorLogService.class);
        LoggingInterceptor interceptor = new LoggingInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/profile");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());
        request.setAttribute(AuthConstants.ATTR_USER_ID, 7L);
        request.setAttribute(AuthConstants.ATTR_ROLE, "ADMIN");

        interceptor.afterCompletion(request, response, new Object(), null);

        ArgumentCaptor<ApiBehaviorLog> captor = ArgumentCaptor.forClass(ApiBehaviorLog.class);
        verify(service).record(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals("ADMIN", captor.getValue().getUserRole());
        assertEquals("GET", captor.getValue().getRequestMethod());
        assertEquals("/api/user/profile", captor.getValue().getRequestPath());
    }

    @Test
    void logManagementApiShouldNotCreateBehaviorLogForItself() {
        ApiBehaviorLogService service = mock(ApiBehaviorLogService.class);
        LoggingInterceptor interceptor = new LoggingInterceptor(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/behavior-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, new Object());
        request.setAttribute(AuthConstants.ATTR_USER_ID, 7L);
        request.setAttribute(AuthConstants.ATTR_ROLE, "SUPER_ADMIN");

        interceptor.afterCompletion(request, response, new Object(), null);

        verify(service, never()).record(org.mockito.ArgumentMatchers.any());
    }
}
