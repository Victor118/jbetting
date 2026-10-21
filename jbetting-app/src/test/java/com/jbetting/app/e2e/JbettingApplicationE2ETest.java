package com.jbetting.app.e2e;

import com.jbetting.app.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JbettingApplicationE2ETest {

    @Test
    void devrait_demarrer() {
    }
}
