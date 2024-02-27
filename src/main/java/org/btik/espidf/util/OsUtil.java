package org.btik.espidf.util;

import com.intellij.util.system.OS;

/**
 * @author lustre
 * @since 2024/2/15 18:33
 */
public class OsUtil {
    public interface Const {
        String WIN_CMD = "cmd.exe";
        String WIN_CMD_ARG = "/c";
        String WIN_IDF_EXE = "idf.py.exe";

        String UNIX_BASH = "/bin/bash";

        String UNIX_BASH_ARG = "-c";

        String UNIX_IDF_EXE = "idf.py";

        String POWER_SHELL_ENV_PREFIX = "$env:";
    }

    public final static boolean IS_WINDOWS = OS.CURRENT == OS.Windows;


    public static String getCmdEnv() {
        return IS_WINDOWS ? Const.WIN_CMD : Const.UNIX_BASH;
    }

    public static String getCmdArg() {
        return IS_WINDOWS ? Const.WIN_CMD_ARG : Const.UNIX_BASH_ARG;
    }

    public static String getIdfExe() {
        return IS_WINDOWS ? Const.WIN_IDF_EXE : Const.UNIX_IDF_EXE;
    }
}
