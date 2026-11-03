package org.catalyst.model.Event;

public abstract class EventFactory {
    public abstract Event createEvent(EventDetails details);

    public Event prepareEvent(EventDetails details){
        return createEvent(details);
    };
};
