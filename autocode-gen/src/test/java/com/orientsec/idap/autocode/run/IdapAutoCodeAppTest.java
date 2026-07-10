package com.orientsec.idap.autocode.run;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class IdapAutoCodeAppTest {

    @Test
    void delegatesTheIdapUserTableToTheIntranetGenerator() throws Exception {
        String[] arguments = {"--force"};

        IdapAutoCodeApp.run(FakeAutoCodeBase.class.getName(), arguments);

        assertEquals("idap", FakeAutoCodeBase.project);
        assertEquals(1, FakeAutoCodeBase.tables.size());
        assertEquals("idap_user_info", FakeAutoCodeBase.tables.get(0));
        assertArrayEquals(new String[] {"--force"}, FakeAutoCodeBase.arguments);
    }

    public static class FakeAutoCodeBase {
        private static String project;
        private static List<String> tables;
        private static String[] arguments;

        public static void doStart(String project, List<String> tables, String[] arguments) {
            FakeAutoCodeBase.project = project;
            FakeAutoCodeBase.tables = tables;
            FakeAutoCodeBase.arguments = arguments;
        }
    }
}
