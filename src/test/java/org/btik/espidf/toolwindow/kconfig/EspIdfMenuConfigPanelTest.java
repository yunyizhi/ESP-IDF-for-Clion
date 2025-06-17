package org.btik.espidf.toolwindow.kconfig;


import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfProjectConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.btik.espidf.toolwindow.kconfig.KConfParser.treeEach;
import static org.mockito.Mockito.when;


public class EspIdfMenuConfigPanelTest {
    @Mock
    Project project;

    @Mock
    IdfProjectConfigService projectConfigService;

    @Before
    public void setUp() throws URISyntaxException {
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

    @Test
    public void load() throws InterruptedException {
        URL url = getClass().getResource("/project_root/build/config/kconfig_menus.json");
        List<ConfModel> confModel = KConfParser.parseKconfig(new File(url.getPath().replace("%20", " ")).toPath());
        assert !confModel.isEmpty();
        for (ConfModel model : confModel) {
            DefaultMutableTreeNode treeNode = KConfParser.buildTree(model);
            System.out.println(treeNode.toString());
        }
        url = getClass().getResource("/project_root/build/config/sdkconfig.json");
        Map<String, Object> stringObjectMap = KConfParser.parseSdkConfig(new File(url.getPath().replace("%20", " ")).toPath());
        stringObjectMap.forEach((k,v)-> System.out.println(k+":"+v));
        for (ConfModel model : confModel) {
            treeEach(model, (node) ->{


            });
        }
    }
}