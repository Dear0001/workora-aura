package com.workora.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

@Configuration
public class MinioStorageConfiguration {

    @Bean
    MinioClient minioClient(
            @Value("${storage.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${storage.minio.access-key:${STORAGE_MINIO_ACCESS_KEY:}}") String accessKey,
            @Value("${storage.minio.secret-key:${STORAGE_MINIO_SECRET_KEY:}}") String secretKey,
            @Value("${storage.minio.region:${STORAGE_MINIO_REGION:us-east-1}}") String region) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }

    @Bean
    MinioClient minioPresignClient(
            @Value("${storage.minio.presign-endpoint:http://localhost:9000}") String endpoint,
            @Value("${storage.minio.access-key:}") String accessKey,
            @Value("${storage.minio.secret-key:}") String secretKey,
            @Value("${storage.minio.region:us-east-1}") String region) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .region(region)
                .build();
    }
}
