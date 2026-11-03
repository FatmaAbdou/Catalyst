package org.catalyst.model.Event;

public final class WorkshopEvent extends Event {
    String instructor;
    String topic;
    String prerequisiteSkills;

    public WorkshopEvent(EventDetails details) {
        super(details);
        this.instructor = details.instructor();
        this.topic = details.topic();
        this.prerequisiteSkills = details.prerequisiteSkills();
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

