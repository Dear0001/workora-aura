package com.workora.bug;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import com.workora.common.storage.MinioStorageConfiguration;
import com.workora.common.storage.StorageObjectController;
import com.workora.common.storage.StorageObjectService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@Import({MinioStorageConfiguration.class, StorageObjectService.class, StorageObjectController.class})
@OpenAPIDefinition(servers = @Server(url = "/api/bug", description = "API Gateway"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste a Keycloak access token. Do not include the Bearer prefix."
)
public class BugServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BugServiceApplication.class, args);
    }
}
