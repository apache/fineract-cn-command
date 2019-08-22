package org.apache.fineract.cn.command.domain;

public final class NotificationMs {

    private String commandName;
    private Object identifierMariaDb;
    private Object command;

    public NotificationMs(String commandName, Object identifierMariaDb, Object command) {
        this.commandName = commandName;
        this.identifierMariaDb = identifierMariaDb;
        this.command = command;
    }

    public NotificationMs() {

    }

}
