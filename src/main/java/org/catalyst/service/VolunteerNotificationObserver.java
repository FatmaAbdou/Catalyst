package org.catalyst.service;

import org.catalyst.model.volunteer.VolunteerAssignment;
import org.catalyst.repo.VolunteerRepository;

import java.sql.SQLException;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Queues an in-app notification when a volunteer receives a new assignment. */
@SuppressWarnings("deprecation")
public final class VolunteerNotificationObserver implements Observer {
    private static final Logger LOGGER =
            Logger.getLogger(VolunteerNotificationObserver.class.getName());

    private final VolunteerRepository repository;

    public VolunteerNotificationObserver(VolunteerRepository repository) {
        this.repository = repository;
    }

    @Override
    public void update(Observable observable, Object event) {
        if (!(event instanceof VolunteerAssignment assignment)) {
            LOGGER.warning("Received an unexpected assignment notification.");
            return;
        }
        try {
            repository.queueAssignmentNotification(
                    assignment.userId(),
                    assignment.id(),
                    "You have been assigned to volunteer task: " + assignment.taskDescription());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE,
                    "Assignment was saved, but the volunteer notification could not be queued.",
                    exception);
        }
    }
}