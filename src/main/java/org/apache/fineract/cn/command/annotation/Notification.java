package org.apache.fineract.cn.command.annotation;

public enum Notification {

    NOTIFY("notify"),
    NOT_NOTIFY("not-notify");

    private final String notification;

    Notification(String notification) {
        this.notification = notification;
    }

    public String getNotification() {
        return notification;
    }
}
