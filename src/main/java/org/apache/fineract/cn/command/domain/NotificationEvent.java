package org.apache.fineract.cn.command.domain;

import java.sql.Blob;
import java.util.UUID;

public final class NotificationEvent {

    private String tag1;
    private String timebucket;
    private UUID timestamp;
    private String presistence_id;
    private Double partition_nr;
    private Double sequence_nr;
    private Blob event;
    private String event_manifest;
    private Blob message;
    private Integer ser_id;
    private String ser_manifest;
    private UUID writer_uuid;

    public NotificationEvent() {
    }


}

