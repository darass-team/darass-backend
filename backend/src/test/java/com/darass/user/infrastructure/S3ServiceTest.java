package com.darass.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMapOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.darass.SpringContainerTest;
import com.darass.exception.httpbasicexception.BadRequestException;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@DisplayName("S3Uploader 클래스")
class S3ServiceTest extends SpringContainerTest {

    @SpyBean
    private S3Service s3Service;

    @MockBean
    private S3Utilities s3Utilities;

    @MockBean
    private S3Client s3Client;

    @DisplayName("upload() 메서드는 S3에 이미지를 업로드 했을 때, 정상적으로 이미지 URL을 Return한다.")
    @Test
    void upload() throws IOException {
        given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .willReturn(null);
        given(s3Client.utilities()).willReturn(s3Utilities);
        given(s3Utilities.getUrl(any(GetUrlRequest.class))).willReturn(new URL("http://testUrl.com"));

        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("fileName");
        byte[] byteArray = {0};
        when(multipartFile.getBytes()).thenReturn(byteArray);

        assertThat(s3Service.upload(multipartFile)).isEqualTo("http://testUrl.com");
    }

    @DisplayName("upload() 메서드는 IOException이 발생했을 때, BadRequestException 발생시킨다.")
    @Test
    void upload_throwIOException() throws IOException {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("fileName");
        when(multipartFile.getBytes()).thenThrow(new IOException());

        assertThatThrownBy(() -> s3Service.upload(multipartFile))
            .isInstanceOf(BadRequestException.class);
    }

    @DisplayName("upload() 메서드는 SdkClientException이 발생했을 때, InternalServerException을 발생시킨다.")
    @Test
    void upload_throwSdkClientException() throws IOException {
        // given
        S3Client s3 = mock(S3Client.class);
        SdkClientException sdkClientException = mock(SdkClientException.class);
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(sdkClientException);
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("fileName");
        byte[] byteArray = {};
        when(multipartFile.getBytes()).thenReturn(byteArray);
        S3Service s3Service = new S3Service(s3);
        // when, then
        assertThatThrownBy(() -> s3Service.upload(multipartFile))
            .isInstanceOf(SdkClientException.class);
    }

    @DisplayName("delete() 메서드는 S3 버킷의 이미지 파일을 제거한다.")
    @Test
    void delete() {
        given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .willReturn(null);
        assertThatCode(() -> s3Service.delete("test"))
            .doesNotThrowAnyException();
    }
}
