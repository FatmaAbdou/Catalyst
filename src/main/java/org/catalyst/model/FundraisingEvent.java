package org.catalyst.model;

public final class FundraisingEvent extends Event {
    float fundraisingGoal;

    public FundraisingEvent(EventDetails details) {
        super(details);
        this.fundraisingGoal = details.fundraisingGoal();
    }

//    @Override
//    public void validateDetails() {
//        // TODO: Implement this event type's detail validation.
//    }
//
//    @Override
//    public void validateParticipant(User user) {
//        // TODO: Implement this event type's participation rules.
//    }
}