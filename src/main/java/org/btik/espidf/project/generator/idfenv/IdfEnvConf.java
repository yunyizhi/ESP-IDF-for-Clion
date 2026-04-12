package org.btik.espidf.project.generator.idfenv;

import java.util.List;

public class IdfEnvConf {
    public static final String IDF_ENV_JSON = "idf-env.json";
    private String path;
    private String version;
    private List<String> targets;
    private String idfToolsPath;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public String getIdfToolsPath() {
        return idfToolsPath;
    }

    public void setIdfToolsPath(String idfToolsPath) {
        this.idfToolsPath = idfToolsPath;
    }

    @Override
    public String toString() {
        return "ESPIDF" + version;
    }
}
