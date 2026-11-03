package org.catalyst.service;

import org.catalyst.model.volunteer.Volunteer;
import org.catalyst.model.volunteer.VolunteerAssignment;
import org.catalyst.model.volunteer.VolunteerTask;
import org.catalyst.repo.VolunteerRepository;

import java.sql.SQLException;
import java.util.Observable;

/** Assigns and unassigns volunteers from tasks. */
@SuppressWarnings("deprecation")
public final class AssignmentService extends Observable {
    private final VolunteerRepository repository;

    public AssignmentService(VolunteerRepository repository) {
        this.repository = repository;
    }

    public void assign(Volunteer volunteer, VolunteerTask task) throws SQLException {
        if (volunteer == null || task == null) {
            throw new IllegalArgumentException("Select a volunteer and a task before assigning.");
        }
        VolunteerAssignment assignment = repository.assign(volunteer.getId(), task.id());
        setChanged();
        notifyObservers(assignment);
    }

    public void unassign(VolunteerAssignment assignment) throws SQLException {
        repository.unassign(assignment.id());
    }

    public void delete(VolunteerAssignment assignment) throws SQLException {
        if (assignment == null) {
            throw new IllegalArgumentException("Select an assignment to delete.");
        }
        repository.deleteAssignment(assignment.id());
    }
}
