package org.catalyst.model.Event;

public class FundraiserCreator extends EventFactory{
    @Override
    public Event createEvent(EventDetails details){
         FundraisingEvent event = new FundraisingEvent( details);
        return event;
    };

    @Override
    public Event prepareEvent(EventDetails details) {
        return super.prepareEvent(details);
    }
}
