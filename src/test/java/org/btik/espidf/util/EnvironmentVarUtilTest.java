package org.btik.espidf.util;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EnvironmentVarUtilTest {

    Map<String, String> env;

    @Before
    public void setUp() {
        String path;
        if (OsUtil.IS_WINDOWS) {
            path = Objects.requireNonNull(getClass().getResource("/idf_fake_path/windows")).getPath();
        } else {
            path = Objects.requireNonNull(getClass().getResource("/idf_fake_path")).getPath();
        }
        path = path.replace("%20", " ");
        // PathEnvironmentVariableUtil 在非windows下需要判断可执行
        if (!OsUtil.IS_WINDOWS) {
            Path.of(path).resolve(OsUtil.Const.IDF_EXE).toFile().setExecutable(true);
        }
        File file = new File(path);
        env = new HashMap<>();
        env.put("PATH", file.getAbsolutePath());
    }

    @Test
    public void getIdf() {
        String idfFullPath = EnvironmentVarUtil.findIdfFullPath(env);
        System.out.println(idfFullPath);
        assert idfFullPath != null && !idfFullPath.isEmpty();
    }
}