package com.workora.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestJwtConfig.class)
class ServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}
