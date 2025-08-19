package org.btik.espidf.toolwindow.settings.model;

public class SerialPortInfo {
    private String comPort;
    private String type;
    private String descriptivePortName;
    private String portDescription;
    private int vendorId = -1; // -1 表示未知
    private int productId = -1; // -1 表示未知

    private String productName;
    private String vendorName;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComPort() {
        return comPort;
    }

    public void setComPort(String comPort) {
        this.comPort = comPort;
    }

    public String getDescriptivePortName() {
        return descriptivePortName;
    }

    public void setDescriptivePortName(String descriptivePortName) {
        this.descriptivePortName = descriptivePortName;
    }

    public String getPortDescription() {
        return portDescription;
    }

    public void setPortDescription(String portDescription) {
        this.portDescription = portDescription;
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String dump() {
        return "SerialPortInfo{" +
                "comPort='" + comPort + '\'' +
                ", type='" + type + '\'' +
                ", descriptivePortName='" + descriptivePortName + '\'' +
                ", portDescription='" + portDescription + '\'' +
                ", vendorId=" + vendorId +
                ", productId=" + productId +
                ", productName='" + productName + '\'' +
                ", vendorName='" + vendorName + '\'' +
                '}';
    }
}
