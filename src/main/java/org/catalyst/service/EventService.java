package org.catalyst.service;

import org.catalyst.model.Event;
import org.catalyst.model.EventDetails;
import org.catalyst.model.EventFactory;
import org.catalyst.repo.EventRepository;
import org.catalyst.model.EventSummary;
import java.util.List;

import java.sql.SQLException;

public class EventService {
  private final EventRepository eventRepository;

  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  public int createEvent(EventFactory factory, EventDetails details)
          throws SQLException {
    Event event = factory.prepareEvent(details);
    return eventRepository.save(event);
  }

  public void editEvent(int eventId, EventDetails details)
          throws SQLException {
    boolean updated = eventRepository.update(eventId, details);

    if (!updated) {
      throw new IllegalArgumentException("Event not found: " + eventId);
    }
  }
  public List<EventSummary> listUpcomingEvents() throws SQLException {
    return eventRepository.findUpcoming();
  }
}