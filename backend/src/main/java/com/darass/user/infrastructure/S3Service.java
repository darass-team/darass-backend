package com.darass.user.infrastructure;

import com.darass.exception.ExceptionWithMessageAndCode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class S3Service {

    private static final String FILE_NAME_FORMAT = "%s-%s-%s";

    private static final DateTimeFormatter NOW_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${name}")
    private String profileName;

    public String upload(MultipartFile multipartFile) {
        File uploadFile = convert(multipartFile);
        return uploadToS3(uploadFile);
    }

    public void delete(String profileImageUrl) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(extractKey(profileImageUrl))
            .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    private String extractKey(String profileImageUrl) {
        String[] split = profileImageUrl.split("/");
        int positionOfKey = split.length - 1;
        return URLDecoder.decode(split[positionOfKey], StandardCharsets.UTF_8);
    }

    private File convert(MultipartFile multipartFile) {
        File file = new File(multipartFile.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(multipartFile.getBytes());
        } catch (IOException e) {
            throw ExceptionWithMessageAndCode.IO_EXCEPTION.getException();
        }
        return file;
    }

    private String uploadToS3(File uploadFile) {
        String now = LocalDateTime.now().format(NOW_FORMATTER);
        String fileName = String.format(FILE_NAME_FORMAT, profileName, now, uploadFile.getName());
        PutObjectRequest objectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(fileName)
            .build();
        try {
            s3Client.putObject(objectRequest, RequestBody.fromFile(uploadFile));
        } catch (SdkClientException e) {
            throw e;
        } finally {
            uploadFile.delete();
        }
        return makeFilePath(fileName);
    }

    private String makeFilePath(String fileName) {
        GetUrlRequest getUrlRequest = GetUrlRequest.builder()
            .bucket(bucket)
            .key(fileName)
            .build();
        return s3Client.utilities().getUrl(getUrlRequest).toString();
    }
}
