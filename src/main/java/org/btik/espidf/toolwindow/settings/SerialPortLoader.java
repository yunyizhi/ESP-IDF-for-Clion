package org.btik.espidf.toolwindow.settings;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import org.btik.espidf.service.IdfSysConfService;
import org.btik.espidf.toolwindow.settings.model.CdcAcmVendorInfo;
import org.btik.espidf.toolwindow.settings.model.SerialPortInfo;
import org.btik.espidf.toolwindow.tasks.EspIdfTaskTreeFactory;
import org.btik.espidf.util.DomUtil;
import org.btik.espidf.util.OsUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.btik.espidf.toolwindow.settings.model.CdcAcmVendorsMeta.*;
import static org.btik.espidf.util.I18nMessage.*;

public class SerialPortLoader {

    public static List<SerialPortInfo> getSerialPortInfo() {
        IdfSysConfService sysConfService = ApplicationManager.getApplication().getService(IdfSysConfService.class);
        List<SerialPortInfo> portList = new ArrayList<>();
        SerialPort[] ports = SerialPort.getCommPorts(); // 获取所有串口

        for (SerialPort port : ports) {
            SerialPortInfo info = new SerialPortInfo();
            portList.add(info);

            if (OsUtil.IS_WINDOWS) {
                info.setComPort(port.getSystemPortName());
            }else {
                info.setComPort(port.getSystemPortPath());
            }

            info.setDescriptivePortName(port.getDescriptivePortName());
            info.setPortDescription(port.getPortDescription());
            int vendorID = port.getVendorID();
            info.setVendorId(vendorID);
            int productID = port.getProductID();
            info.setProductId(productID);
            if (vendorID == -1) {
                continue;
            }
            CdcAcmVendorInfo cdcAcmVendorInfo = sysConfService.getCdcAcmVendorInfo(vendorID, productID);
            if (cdcAcmVendorInfo != null) {
                info.setVendorName(cdcAcmVendorInfo.getVendorName());
                info.setProductName(cdcAcmVendorInfo.getProductName());
            }

        }
        portList.sort(null);
        return portList;
    }

    public static Map<Integer, Map<Integer, CdcAcmVendorInfo>> loadCdcAcmVendorInfo() {
        Element documentElement;
        try {
            Document cdcAcmVendorsDom = DomUtil.parse(EspIdfTaskTreeFactory.class
                    .getResourceAsStream("/org-btik-esp-idf/conf/cdc_acm_vendors.xml"));
            documentElement = cdcAcmVendorsDom.getDocumentElement();

        } catch (Exception e) {
            NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                    e.getMessage(), NotificationType.ERROR).notify(null);
            return null;
        }
        if (!VENDORS.equals(documentElement.getTagName())) {
            NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                    $i18nF("idf.xml.load.failed", VENDORS), NotificationType.ERROR).notify(null);
            return null;
        }
        return parseVendors(documentElement);

    }

    private static Map<Integer, Map<Integer, CdcAcmVendorInfo>> parseVendors(Element vendors) {
        Map<Integer, Map<Integer, CdcAcmVendorInfo>> cdcAcmVendorInfoMap = new HashMap<>();
        DomUtil.eachByTagName(vendors, VENDOR, (vendorElement) -> {
            int vendorId = Integer.parseInt(vendorElement.getAttribute(VENDOR_ID).trim(), 16);
            String vendorName = vendorElement.getAttribute(VENDOR_NAME).trim();
            String defaultName = vendorElement.getAttribute(DEV_VENDOR_DEFAULT_NAME).trim();

            HashMap<Integer, CdcAcmVendorInfo> vendorInfoMap = new HashMap<>();
            cdcAcmVendorInfoMap.put(vendorId, vendorInfoMap);
            vendorInfoMap.put(null, new CdcAcmVendorInfo(vendorId, vendorName, defaultName));

            DomUtil.eachByTagName(vendorElement, PRODUCT, (productElement) -> {
                CdcAcmVendorInfo cdcAcmVendorInfo = new CdcAcmVendorInfo(vendorId, vendorName, defaultName);

                int productId = Integer.parseInt(productElement.getAttribute(PRODUCT_ID).trim(), 16);
                cdcAcmVendorInfo.setProductId(productId);
                String type = productElement.getAttribute(PRODUCT_TYPE).trim();
                cdcAcmVendorInfo.setType(type);
                vendorInfoMap.put(productId, cdcAcmVendorInfo);
            });
        });
        return cdcAcmVendorInfoMap;
    }

}
