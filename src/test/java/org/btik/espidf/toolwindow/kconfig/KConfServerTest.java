package org.btik.espidf.toolwindow.kconfig;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputType;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class KConfServerTest {
    private KConfServer kConfServer;
    private final AtomicReference<KconfigStatus> lastStatus = new AtomicReference<>();
    private final AtomicInteger messageCount = new AtomicInteger(0);
    ProcessOutputType outputType = ProcessOutputType.STDOUT;
    @Before
    public void setUp() {
        kConfServer = new KConfServer(null, status -> {
            lastStatus.set(status);
            messageCount.incrementAndGet();
        });
    }

    @Test
    public void onTextAvailable_SingleJsonMessage() {
        // 完整JSON数据
        String json = "{\"version\": 2, \"ranges\": {\"TEST\": [0,10]}, \"visible\": {}, \"values\": {}}";
        kConfServer.onTextAvailable(newProcessEvent(json), outputType);

        assertNotNull("callback ok", lastStatus.get());
        assertEquals("version ok", "2", lastStatus.get().getVersion());
        assertEquals(1, messageCount.get());
    }

    @Test
    public void onTextAvailable_ChunkedJsonMessage() {
        // 分三次发送分片数据
        kConfServer.onTextAvailable(newProcessEvent("{"), outputType);
        kConfServer.onTextAvailable(newProcessEvent("\"version\":2,"), outputType);
        kConfServer.onTextAvailable(newProcessEvent("\"visible\":{}}"), outputType);

        assertNotNull("json ok", lastStatus.get());
        assertEquals("version ok", "2", lastStatus.get().getVersion());
        assertEquals(1, messageCount.get());
    }

    @Test
    public void onTextAvailable_MultipleJsonMessages() {
        // 两个连续JSON对象
        String doubleJson = "{\"version\":1}{ \"version\":2, \"ranges\": {}}";
        kConfServer.onTextAvailable(newProcessEvent(doubleJson), outputType);

        assertEquals("count 2", 2, messageCount.get());
        assertEquals("version ok", "2", lastStatus.get().getVersion());
    }

    @Test
    public void onTextAvailable_InvalidJsonIgnored() {
        // 非法JSON + 合法JSON
        String mixed = "INVALID {\"version\":3}";
        kConfServer.onTextAvailable(newProcessEvent(mixed), outputType);

        assertNotNull("json ok", lastStatus.get());
        assertEquals("version ok", "3", lastStatus.get().getVersion());
        assertEquals(1, messageCount.get());
    }

    @Test
    public void onTextAvailable_ComplexNestedJson() {
        // 复杂嵌套结构
        String complexJson = "{\"version\":4, \"ranges\": {\"A\":[1,2]}, \"visible\": {\"C\": false}}";
        kConfServer.onTextAvailable(newProcessEvent(complexJson), outputType);

        KconfigStatus status = lastStatus.get();
        assertNotNull("ComplexNestedJson", status);
        assertEquals("4", status.getVersion());
        assertNotNull(status.getRanges());
    }

    private ProcessEvent newProcessEvent(String json) {
        return new ProcessEvent(new ProcessHandler() {
            @Override
            protected void destroyProcessImpl() {

            }

            @Override
            protected void detachProcessImpl() {

            }

            @Override
            public boolean detachIsDefault() {
                return false;
            }

            @Override
            public @Nullable OutputStream getProcessInput() {
                return null;
            }
        }, json);
    }

}