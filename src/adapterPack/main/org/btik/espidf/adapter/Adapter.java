package org.btik.espidf.adapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.intellij.execution.ExecutionException;
import com.jetbrains.cidr.system.LocalHost;
import org.bitk.espidf.conf.IdfToolConf;

/**
 * @author lustre
 * @since 2024/2/15 21:09
 */
public class Adapter {
    public static Map<String, String> readEnvironment(IdfToolConf idfToolConf) throws IOException, ExecutionException {
        return idfToolConf.getToolchain().getToolSet().readEnvironment(idfToolConf.getEnvFileName(), LocalHost.INSTANCE,
                new HashMap<>());
    }
}
