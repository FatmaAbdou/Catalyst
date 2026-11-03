package org.catalyst.service;

import org.catalyst.model.Event;
import org.catalyst.model.EventDetails;
import org.catalyst.model.EventFactory;
import org.catalyst.repo.EventRepository;

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
}