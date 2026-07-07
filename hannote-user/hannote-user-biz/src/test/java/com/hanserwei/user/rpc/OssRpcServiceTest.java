package com.hanserwei.user.rpc;

import com.hanserwei.framework.common.response.Response;
import com.hanserwei.oss.api.FileHttpApi;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OssRpcServiceTest {

    @Test
    void uploadFile_returnsUrl_onSuccess() {
        FileHttpApi api = mock(FileHttpApi.class);
        when(api.uploadFile(any(Resource.class)))
                .thenReturn(Response.success("https://cdn/x.png"));
        OssRpcService service = new OssRpcService(api);

        MultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[]{1, 2, 3});

        assertThat(service.uploadFile(file)).isEqualTo("https://cdn/x.png");
    }

    @Test
    void uploadFile_returnsNull_onFailure() {
        FileHttpApi api = mock(FileHttpApi.class);
        Response<String> failed = new Response<>();
        failed.setSuccess(false);
        when(api.uploadFile(any(Resource.class))).thenReturn(failed);
        OssRpcService service = new OssRpcService(api);

        MultipartFile file = new MockMultipartFile(
                "file", "x.png", "image/png", new byte[]{1, 2, 3});

        assertThat(service.uploadFile(file)).isNull();
    }
}
