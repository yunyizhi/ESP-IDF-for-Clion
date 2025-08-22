package org.btik.espidf.toolwindow.tasks.line.marker;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import org.btik.espidf.toolwindow.tasks.TreeXmlMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EspCustomRunLineMarkerContributor extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(@NotNull PsiElement element) {

        return null;
    }

    @Override
    public Info getSlowInfo(@NotNull PsiElement element) {
        if (!(element instanceof XmlTag xmlTag)) {
            return null;
        }
        String fileName = xmlTag.getContainingFile().getName();
        if (!TreeXmlMeta.ESP_CUSTOM_TASKS_XML.equals(fileName)) {
            return null;
        }
        String tagName = xmlTag.getName();
        switch (tagName) {
            case TreeXmlMeta.CONSOLE_COMMAND:
            case TreeXmlMeta.COMMAND_TAG:
            case TreeXmlMeta.LOCAL_EXEC:
                System.out.println(xmlTag.getText());
                break;
            default:
                return null;
        }
        return null;
    }
}
