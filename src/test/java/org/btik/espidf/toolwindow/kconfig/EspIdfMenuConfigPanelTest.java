package org.btik.espidf.toolwindow.kconfig;


import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.ConfModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.mockito.Mockito.when;


public class EspIdfMenuConfigPanelTest {
    @Mock
    Project project;

    @Mock
    IdfProjectConfigService projectConfigService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        String path = Objects.requireNonNull(getClass().getResource("/project_root")).getPath();
        path = path.replace("%20", " ");
        File file = new File(path);
        System.out.println(file.getPath());
        assert file.exists();
        when(project.getBasePath()).thenReturn(file.getPath());
        when(project.getService(IdfProjectConfigService.class)).thenReturn(projectConfigService);
        when(projectConfigService.getCmakeBuildDir()).thenReturn("build");
    }

    private Path res2Path(String resPath){
        URL url = getClass().getResource(resPath);
        return new File(url.getPath().replace("%20", " ")).toPath();
    }

    @Test
    public void load() throws InterruptedException {
        List<ConfModel> confModel = KConfParser.parseKconfig(res2Path("/project_root/build/config/kconfig_menus.json"));
        assert !confModel.isEmpty();
        Map<String, Object> stringObjectMap = KConfParser.parseSdkConfig(res2Path("/project_root/build/config/sdkconfig.json"));
        assert !stringObjectMap.isEmpty();
    }
}