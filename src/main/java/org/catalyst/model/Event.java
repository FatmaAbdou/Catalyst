package org.catalyst.model;

import java.sql.Time;
import java.util.Date;

public abstract class Event {
    int id;
    String name;
    String description;
    String location;
    Date date;
    Time startTime;
    Time endTime;
    int capacity;
    float ticketPrice;
    EventStatus status;

    protected Event(EventDetails details) {
        this.name = details.name();
        this.description = details.description();
        this.location = details.location();
        this.date = details.date();
        this.startTime = details.startTime();
        this.endTime = details.endTime();
        this.capacity = details.capacity();
        this.ticketPrice = details.ticketPrice();
        this.status = EventStatus.UPCOMING;
    }

    public Date getDate() {
        return date;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Time getStartTime() {
        return startTime;
    }
    public Time getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public float getTicketPrice() {
        return ticketPrice;
    }


    //    public void validateDetails(){
//
//    };
//    public void validateParticipant(User user){
//
//    };
}
