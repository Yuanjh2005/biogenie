package com.agent.biogenie.utils;

import com.agent.biogenie.config.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioUtilTest {

    @Mock
    private MinioClient minioClient;

    private MinioUtil minioUtil;
    private MinioProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setEndpoint("http://127.0.0.1:9000");
        properties.setBucketName("biogenie");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setSignedUrlExpireSeconds(43200L);
        minioUtil = new MinioUtil(properties);
        minioUtil.setMinioClient(minioClient);
    }

    @Test
    void extractObjectKey_shouldStripQueryAndPathStylePrefix() {
        String key = minioUtil.extractObjectKey(
                "http://127.0.0.1:9000/biogenie/images/a.png?X-Amz-Signature=x");
        assertEquals("images/a.png", key);
    }

    @Test
    void toPermanentUrl_shouldStripSignature() {
        String permanent = minioUtil.toPermanentUrl(
                "http://127.0.0.1:9000/biogenie/images/a.png?X-Amz-Signature=x");
        assertEquals("http://127.0.0.1:9000/biogenie/images/a.png", permanent);
    }

    @Test
    void toPermanentUrl_external_shouldKeepHostStripQuery() {
        String permanent = minioUtil.toPermanentUrl("https://gitee.com/a.png?token=1");
        assertEquals("https://gitee.com/a.png", permanent);
    }

    @Test
    void toAccessUrl_bucketObject_shouldPresign() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://127.0.0.1:9000/biogenie/images/a.png?X-Amz-Signature=signed");

        String access = minioUtil.toAccessUrl("http://127.0.0.1:9000/biogenie/images/a.png");

        assertTrue(access.contains("X-Amz-Signature=signed"));
        ArgumentCaptor<GetPresignedObjectUrlArgs> captor =
                ArgumentCaptor.forClass(GetPresignedObjectUrlArgs.class);
        verify(minioClient).getPresignedObjectUrl(captor.capture());
    }

    @Test
    void toAccessUrl_external_shouldNotSign() throws Exception {
        String access = minioUtil.toAccessUrl("https://gitee.com/pic.png");
        assertEquals("https://gitee.com/pic.png", access);
        verify(minioClient, never()).getPresignedObjectUrl(any());
    }

    @Test
    void toAccessUrl_objectKey_shouldPresign() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://127.0.0.1:9000/biogenie/files/a.jpg?X-Amz-Signature=signed");

        String access = minioUtil.toAccessUrl("files/a.jpg");

        assertTrue(access.contains("X-Amz-Signature=signed"));
        verify(minioClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void resolveContentType_shouldPreferExtension() {
        assertEquals("image/jpeg", MinioUtil.resolveContentType("application/octet-stream", "a.jpg"));
        assertEquals("image/png", MinioUtil.resolveContentType("image/png", "a.png"));
    }

    @Test
    void stripQuery_nullSafe() {
        assertNull(MinioUtil.stripQuery(null));
        assertEquals("a", MinioUtil.stripQuery("a"));
    }

    @Test
    void buildPermanentUrl_shouldUsePathStyle() {
        assertEquals("http://127.0.0.1:9000/biogenie/files/a.png",
                minioUtil.buildPermanentUrl("files/a.png"));
    }

    @Test
    void publicEndpoint_shouldRecognizeLegacyInternalUrlAndUsePublicPermanentUrl() {
        properties.setPublicEndpoint("https://files.example.com");
        String legacy = "http://127.0.0.1:9000/biogenie/files/a.txt?X-Amz-Signature=old";
        assertEquals("files/a.txt", minioUtil.extractObjectKey(legacy));
        assertEquals("https://files.example.com/biogenie/files/a.txt", minioUtil.toPermanentUrl(legacy));
        assertEquals("files/a.txt", minioUtil.extractObjectKey(
                "https://files.example.com/biogenie/files/a.txt?X-Amz-Signature=new"));
    }
}
