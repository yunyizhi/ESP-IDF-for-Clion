package org.btik.espidf.toolwindow.settings;


import com.fazecast.jSerialComm.SerialPort;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class SerialCom {
    public static List<SerialPortInfo> getSerialPorts() {
        List<SerialPortInfo> portList = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts(); // 获取所有串口

        for (SerialPort port : ports) {
            SerialPortInfo info = new SerialPortInfo();
            info.setComPort(port.getSystemPortPath()); // 如 COM1, /dev/ttyUSB0
            info.setDescriptivePortName(port.getDescriptivePortName()); // 如 "USB Serial Port"
            info.setPortDescription(port.getPortDescription()); // 更详细的描述
            info.setVendorId(port.getVendorID()); // VID (如果可用)
            info.setProductId(port.getProductID()); // PID (如果可用)

            portList.add(info);
        }
        return portList;
    }

    @Test
    public void testGetPort(){
        List<SerialPortInfo> serialPorts = getSerialPorts();
        for (SerialPortInfo serialPort : serialPorts) {
            System.out.println(serialPort.dump());
        }
    }
}
