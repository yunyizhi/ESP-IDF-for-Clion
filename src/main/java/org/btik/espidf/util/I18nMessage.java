package org.btik.espidf.util;

import com.intellij.DynamicBundle;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import org.jetbrains.annotations.NotNull;

/**
 * @author lustre
 * @since 2024/2/8 23:39
 */
public class I18nMessage extends DynamicBundle {
    private static final I18nMessage INSTANCE = new I18nMessage("messages.org_btik_espidf");
    public static NotificationGroup NOTIFICATION_GROUP =
            NotificationGroupManager.getInstance().getNotificationGroup("org.btiik.espidf");

    public I18nMessage(@NotNull String pathToBundle) {
        super(pathToBundle);
    }

    public static String getMsg(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            return INSTANCE.getResourceBundle().getString(key);
        } catch (Exception e) {
            NOTIFICATION_GROUP.createNotification(getMsg("idf.get.msg.failed"),
                    e.getMessage(), NotificationType.ERROR).notify(null);
            return key;
        }
    }

    public static String $i18n(String key) {
        return getMsg(key);
    }

    public static String getMsgF(String key, Object... o) {
        return String.format(getMsg(key), o);
    }
}
