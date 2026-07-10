package com.orientsec.idap.autocode.run;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Entry point that delegates generation to the intranet AutoCodeBase implementation. */
public final class IdapAutoCodeApp {

    private static final String DEFAULT_AUTO_CODE_BASE_CLASS =
            "com.orientsec.idap.autocode.AutoCodeBase";

    private IdapAutoCodeApp() {}

    public static void main(String[] args) throws Exception {
        run(System.getProperty("idap.autocode.base-class", DEFAULT_AUTO_CODE_BASE_CLASS), args);
    }

    static void run(String autoCodeBaseClass, String[] args) throws Exception {
        List<String> tables = new ArrayList<String>();
        tables.add("idap_user_info");

        Class<?> generator = Class.forName(autoCodeBaseClass);
        Method doStart = generator.getMethod("doStart", String.class, List.class, String[].class);
        doStart.invoke(null, new Object[] {"idap", tables, args});
    }
}
