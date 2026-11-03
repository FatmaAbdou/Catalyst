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

    public void validateDetails(){

    };
    public void validateParticipant(User user){

    };
}
