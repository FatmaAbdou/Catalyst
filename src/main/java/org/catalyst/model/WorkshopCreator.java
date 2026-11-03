package org.catalyst.model;

public class WorkshopCreator extends EventFactory{
    @Override
    public Event createEvent(EventDetails details){
        WorkshopEvent event = new WorkshopEvent(details);
        return event;
    };

    @Override
    public Event prepareEvent(EventDetails details) {
        return super.prepareEvent(details);
    }
}
