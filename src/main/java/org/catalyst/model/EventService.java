package org.catalyst.model;

public class EventService {
  // call the event factory
    public Event createEvent(String type, EventDetails details){

    };
  public void editEvent(int eventId, EventDetails details){

  };
  public void linkCampaign(int eventId, int campaignId){

  }
  public void setStatus(int eventId, EventStatus status){

  }
  public void listUpcomingEvents(){

  };
  public void getParticipants(int eventId){

  };
  public void recordAttendance(int registrationId){

  };
  public void sendReminder(int eventId, String message){

  };
  public void sendUpdateNotice(int eventId, String message){

  };
  public void sendCancellationNotice(int eventId, String message){

  };
}
