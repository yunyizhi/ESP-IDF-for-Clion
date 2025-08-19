package org.btik.espidf.toolwindow.settings;


import com.fazecast.jSerialComm.SerialPort;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SerialCom {
    public static List<SerialPortInfo> getSerialPorts() {
        List<SerialPortInfo> portList = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts(); // 获取所有串口

        for (SerialPort port : ports) {
            SerialPortInfo info = new SerialPortInfo();
            info.setComPort(port.getSystemPortName()); // 如 COM1, /dev/ttyUSB0
            info.setDescriptivePortName(port.getDescriptivePortName()); // 如 "USB Serial Port"
            info.setPortDescription(port.getPortDescription()); // 更详细的描述
            info.setVendorId(port.getVendorID()); // VID (如果可用)
            info.setProductId(port.getProductID()); // PID (如果可用)

            // 尝试打开端口以检查是否被占用（可选）
            if (port.openPort()) {
                info.setOpened(true);
                info.setBaudRate(port.getBaudRate()); // 当前波特率
                port.closePort(); // 立即关闭
            } else {
                info.setOpened(false);
            }

            portList.add(info);
        }
        return portList;
    }

    // 串口信息数据类
    public static class SerialPortInfo {
        private String comPort;
        private String descriptivePortName;
        private String portDescription;
        private int vendorId = -1; // -1 表示未知
        private int productId = -1; // -1 表示未知
        private boolean opened;
        private int baudRate;

        // Getters and Setters
        public String getComPort() { return comPort; }
        public void setComPort(String comPort) { this.comPort = comPort; }

        public String getDescriptivePortName() { return descriptivePortName; }
        public void setDescriptivePortName(String descriptivePortName) { this.descriptivePortName = descriptivePortName; }

        public String getPortDescription() { return portDescription; }
        public void setPortDescription(String portDescription) { this.portDescription = portDescription; }

        public int getVendorId() { return vendorId; }
        public void setVendorId(int vendorId) { this.vendorId = vendorId; }

        public int getProductId() { return productId; }
        public void setProductId(int productId) { this.productId = productId; }

        public boolean isOpened() { return opened; }
        public void setOpened(boolean opened) { this.opened = opened; }

        public int getBaudRate() { return baudRate; }
        public void setBaudRate(int baudRate) { this.baudRate = baudRate; }

        @Override
        public String toString() {
            return "SerialPortInfo{" +
                    "comPort='" + comPort + '\'' +
                    ", descriptivePortName='" + descriptivePortName + '\'' +
                    ", portDescription='" + portDescription + '\'' +
                    ", vendorId=" + (vendorId != -1 ? String.format("0x%04X", vendorId) : "N/A") +
                    ", productId=" + (productId != -1 ? String.format("0x%04X", productId) : "N/A") +
                    ", opened=" + opened +
                    ", baudRate=" + baudRate +
                    '}';
        }
    }

    @Test
    public void testGetPort(){
        List<SerialPortInfo> serialPorts = getSerialPorts();
        for (SerialPortInfo serialPort : serialPorts) {
            System.out.println(serialPort);
        }
    }
}
