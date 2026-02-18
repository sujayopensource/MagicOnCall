package com.magiconcall;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class MagicOnCallApplicationTest {

    @Test
    void contextLoads() {
        // Verifies the full application context wires up correctly
    }
}
