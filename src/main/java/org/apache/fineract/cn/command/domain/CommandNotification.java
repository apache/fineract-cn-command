package org.apache.fineract.cn.command.domain;

public final class CommandNotification {

    private String commandName;
    private Object identifierMariaDb;
    private Object command;
    private NotificationEvent event;

    public CommandNotification(String commandName, Object identifierMariaDb, Object command) {
        this.commandName = commandName;
        this.identifierMariaDb = identifierMariaDb;
        this.command = command;
    }

    public CommandNotification() {

    }

}
