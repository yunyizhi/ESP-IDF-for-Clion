package org.btik.espidf.toolwindow.kconfig;


import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfProjectConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;

import static org.mockito.Mockito.when;


public class EspIdfMenuConfigPanelTest {
    @Mock
    Project project;

    @Mock
    IdfProjectConfigService  projectConfigService;
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
        EspIdfMenuConfigPanel panel = new EspIdfMenuConfigPanel(project);

    }
}