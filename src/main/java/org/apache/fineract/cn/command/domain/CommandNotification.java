package org.apache.fineract.cn.command.domain;

public final class CommandNotification {

    private String action;
    private Object command;
    private Object identifierMariaDb;

    public CommandNotification(String action, Object command, Object identifierMariaDb) {
        this.action = action;
        this.command = command;
        this.identifierMariaDb = identifierMariaDb;
    }
}
