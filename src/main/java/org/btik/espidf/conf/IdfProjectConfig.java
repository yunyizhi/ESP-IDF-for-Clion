package org.btik.espidf.conf;

import java.util.Objects;

/**
 * 项目级配置
 *
 * @author lustre
 * @since 2024/8/25 15:53
 */

public class IdfProjectConfig {
    private String port;
    private String monitorBaud;
    private String uploadBaud;

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getMonitorBaud() {
        return monitorBaud;
    }

    public void setMonitorBaud(String monitorBaud) {
        this.monitorBaud = monitorBaud;
    }

    public String getUploadBaud() {
        return uploadBaud;
    }

    public void setUploadBaud(String uploadBaud) {
        this.uploadBaud = uploadBaud;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdfProjectConfig that)) return false;
        return Objects.equals(getPort(), that.getPort()) && Objects.equals(getMonitorBaud(), that.getMonitorBaud()) && Objects.equals(getUploadBaud(), that.getUploadBaud());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPort(), getMonitorBaud(), getUploadBaud());
    }

    public boolean isEmpty() {
        if (getPort() == null || getMonitorBaud() == null || getUploadBaud() == null) {
            return true;
        }
        return getPort().isEmpty() && getMonitorBaud().isEmpty() && getUploadBaud().isEmpty();
    }
}
