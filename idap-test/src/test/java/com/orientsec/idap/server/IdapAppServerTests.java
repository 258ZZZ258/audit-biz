package com.orientsec.idap.server;

import com.orientsec.idap.core.base.IdapTestServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** A0 冒烟：Spring 上下文能装载（应用可启动）。 */
@SpringBootTest(classes = IdapTestServer.class)
class IdapAppServerTests {

    @Test
    void contextLoads() {}
}
