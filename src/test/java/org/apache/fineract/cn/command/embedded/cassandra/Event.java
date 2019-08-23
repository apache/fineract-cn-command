package org.apache.fineract.cn.command.embedded.cassandra;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import javax.validation.constraints.NotNull;

@Table(value = "events")
public final class Event {

    @PrimaryKey
    @NotNull
    private Integer event_id;
    @NotNull
    private String event_name;
    @NotNull
    private String event_city;
    @NotNull
    private Double event_sal;
    @NotNull
    private Double event_phone;

    public Event(Integer event_id, String event_name, String event_city, Double event_sal, Double event_phone) {
        this.event_id = event_id;
        this.event_name = event_name;
        this.event_city = event_city;
        this.event_sal = event_sal;
        this.event_phone = event_phone;
    }


    public Integer getEvent_id() {
        return event_id;
    }

    public void setEvent_id(Integer event_id) {
        this.event_id = event_id;
    }

    public String getEvent_name() {
        return event_name;
    }

    public void setEvent_name(String event_name) {
        this.event_name = event_name;
    }

    public String getEvent_city() {
        return event_city;
    }

    public void setEvent_city(String event_city) {
        this.event_city = event_city;
    }

    public Double getEvent_sal() {
        return event_sal;
    }

    public void setEvent_sal(Double event_sal) {
        this.event_sal = event_sal;
    }

    public Double getEvent_phone() {
        return event_phone;
    }

    public void setEvent_phone(Double event_phone) {
        this.event_phone = event_phone;
    }
}
