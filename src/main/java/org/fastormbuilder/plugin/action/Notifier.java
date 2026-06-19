package org.fastormbuilder.plugin.action;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;

public class Notifier {
    private static final String GROUP_ID = "FastORMBuilder.Notifications";
    private static Notifier instance;

    private Notifier() {
    }

    public static Notifier getInstance() {
        if (instance == null) {
            synchronized (GROUP_ID) {
                if (instance == null) instance = new Notifier();
            }
        }
        return instance;
    }

    public void info(String msg, Project project) {
        notify(msg, NotificationType.INFORMATION, project);
    }

    public void warn(String msg, Project project) {
        notify(msg, NotificationType.WARNING, project);
    }

    public void error(String msg, Project project) {
        notify(msg, NotificationType.ERROR, project);
    }

    private void notify(String msg, NotificationType type, Project project) {
        Notifications.Bus.notify(
                NotificationGroupManager.getInstance()
                        .getNotificationGroup(GROUP_ID)
                        .createNotification(msg, type),
                project);
    }
}
