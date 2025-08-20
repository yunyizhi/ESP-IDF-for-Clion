package org.btik.espidf.toolwindow.settings;

import org.btik.espidf.toolwindow.settings.model.CdcAcmVendorInfo;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class SerialPortLoaderTest {
@Test
    public  void testLoadCdcAcmVendorInfo()
    {
        Map<Integer, Map<Integer, CdcAcmVendorInfo>> cdcAcmVendorInfos = SerialPortLoader.loadCdcAcmVendorInfo();
        assertNotNull(cdcAcmVendorInfos);

        CdcAcmVendorInfo cdcAcmVendorInfo = cdcAcmVendorInfos.get(0x303a).get(0x1001);
        assertNotNull(cdcAcmVendorInfo);
        assertEquals("Espressif Systems", cdcAcmVendorInfo.getVendorName());

        Map<Integer, CdcAcmVendorInfo> integerCdcAcmVendorInfoMap = cdcAcmVendorInfos.get(0x1a86);
        cdcAcmVendorInfo = integerCdcAcmVendorInfoMap.get(0x55d3);
        assertNotNull(cdcAcmVendorInfo);
        assertEquals("CH343", cdcAcmVendorInfo.getType());

        cdcAcmVendorInfo = integerCdcAcmVendorInfoMap.get(null);
        assertNotNull(cdcAcmVendorInfo);
        assertEquals("WCH Device", cdcAcmVendorInfo.getDefaultName());
    }
}