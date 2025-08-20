package org.btik.espidf.toolwindow.settings.model;

public class CdcAcmVendorInfo {

    private int vendorId;
    private String vendorName;
    private int productId;
    private String defaultName;
    private String type;

    public CdcAcmVendorInfo(int vendorId, String vendorName, String defaultName) {
        this.vendorId = vendorId;
        this.vendorName = vendorName;
        this.defaultName = defaultName;
    }

    public CdcAcmVendorInfo() {
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

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }


    public String getDefaultName() {
        return defaultName;
    }

    public void setDefaultName(String defaultName) {
        this.defaultName = defaultName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProductName() {
        if (type != null) {
            return type;
        }
        if (defaultName != null) {
            return defaultName;
        }
        return vendorName + " [0x%04x]".formatted(productId);
    }

    @Override
    public String toString() {
        return "CdcAcmVendorInfo{" +
                "vendorId='0x%04x'".formatted(vendorId) +
                ", vendorName='" + vendorName + '\'' +
                ", productId='0x%04x'".formatted(productId) +
                ", defaultName='" + defaultName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
