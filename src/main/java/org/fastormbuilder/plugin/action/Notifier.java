package org.fastormbuilder.plugin.action;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BuildNumber;

public class Notifier {
    private static final String GROUP_ID = "FastORMBuilder.Notifications";
    private static Notifier instance;
    private NotificationGroup group;

    private Notifier() {
        try {
            if (BuildNumber.fromString("203.3645.34").compareTo(ApplicationInfo.getInstance().getBuild()) <= 0) {
                Class<?> mgr = Class.forName("com.intellij.notification.NotificationGroupManager");
                Object manager = mgr.getMethod("getInstance").invoke(null);
                this.group = (NotificationGroup) mgr.getMethod("getNotificationGroup", String.class).invoke(manager, GROUP_ID);
            } else {
                this.group = (NotificationGroup) NotificationGroup.class.getMethod("balloonGroup", String.class).invoke(null, GROUP_ID);
            }
        } catch (Exception ignored) {
        }
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
        if (group != null) Notifications.Bus.notify(group.createNotification(msg, type), project);
    }
}
