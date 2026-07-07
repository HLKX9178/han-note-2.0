package com.hanserwei.framework.rpc.interceptor;

import com.hanserwei.framework.biz.context.holder.LoginUserContextHolder;
import com.hanserwei.framework.common.constant.GlobalConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserIdRelayInterceptorTest {

    private final UserIdRelayInterceptor interceptor = new UserIdRelayInterceptor();

    @AfterEach
    void tearDown() {
        LoginUserContextHolder.remove();
    }

    @Test
    void addsUserIdHeader_whenContextHasUserId() throws IOException {
        LoginUserContextHolder.setUserId(123L);
        HttpRequest request = new MockClientHttpRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(GlobalConstants.USER_ID)).isEqualTo("123");
    }

    @Test
    void skipsHeader_whenContextEmpty() throws IOException {
        HttpRequest request = new MockClientHttpRequest();
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertThat(request.getHeaders().getFirst(GlobalConstants.USER_ID)).isNull();
    }
}
