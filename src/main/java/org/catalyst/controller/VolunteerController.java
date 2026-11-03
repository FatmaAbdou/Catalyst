package org.catalyst.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.util.StringConverter;
import org.catalyst.model.AvailabilitySlot;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerAssignment;
import org.catalyst.service.VolunteerService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Lets a volunteer update their profile and review their activity. */
public final class VolunteerController {
    private static final Logger LOGGER = Logger.getLogger(VolunteerController.class.getName());

    private final SceneManager sceneManager;
    private final VolunteerService volunteerService;
    private final Set<String> selectedSkills = new LinkedHashSet<>();
    private final ObservableList<AvailabilitySlot> availabilitySlots =
            FXCollections.observableArrayList();
    private List<String> currentSkills = List.of();

    @FXML private Label greetingLabel;
    @FXML private Label messageLabel;
    @FXML private MenuButton skillsMenuButton;
    @FXML private ListView<String> selectedSkillsList;
    @FXML private ComboBox<DayOfWeek> dayBox;
    @FXML private ComboBox<LocalTime> startTimeBox;
    @FXML private ComboBox<LocalTime> endTimeBox;
    @FXML private ListView<AvailabilitySlot> availabilityList;
    @FXML private TextArea preferencesField;
    @FXML private ComboBox<String> channelBox;
    @FXML private ListView<VolunteerAssignment> assignmentsList;
    @FXML private ListView<ServiceHour> hoursList;
    @FXML private ListView<String> notificationsList;
    @FXML private DatePicker hoursDatePicker;
    @FXML private TextField hoursAmountField;

    public VolunteerController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.volunteerService = sceneManager.getVolunteerService();
    }

    @FXML
    private void initialize() {
        if (!(sceneManager.getCurrentUser() instanceof Volunteer volunteer)) {
            goHome();
            return;
        }

        greetingLabel.setText("Hello, " + volunteer.getName());
        hoursDatePicker.setValue(java.time.LocalDate.now());
        selectedSkillsList.getItems().setAll(selectedSkills);
        availabilityList.setItems(availabilitySlots);
        dayBox.getItems().setAll(DayOfWeek.values());
        dayBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek day) {
                return day == null ? "" : day.getDisplayName(TextStyle.FULL, Locale.getDefault());
            }

            @Override
            public DayOfWeek fromString(String value) {
                return DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
            }
        });
        startTimeBox.getItems().setAll(getTimes());
        endTimeBox.getItems().setAll(getTimes());
        channelBox.getItems().setAll("EMAIL", "SMS");
        try {
            volunteerService.load(volunteer);
            selectedSkills.addAll(volunteer.getSkills());
            selectedSkillsList.getItems().setAll(selectedSkills);
            availabilitySlots.setAll(volunteer.getAvailability());
            sortAvailability();
            preferencesField.setText(volunteer.getPreferences());
            channelBox.setValue(volunteer.getNotificationChannel());
            loadSkillMenu(volunteerService.getSkills());
            refreshActivity(volunteer);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load the volunteer profile.", exception);
            showMessage("We could not load your volunteering details. Please try again.");
        }
    }

    @FXML
    private void addAvailability() {
        if (dayBox.getValue() == null || startTimeBox.getValue() == null
                || endTimeBox.getValue() == null) {
            showMessage("Choose a day, start time, and end time.");
            return;
        }
        try {
            AvailabilitySlot slot = new AvailabilitySlot(
                    dayBox.getValue(), startTimeBox.getValue(), endTimeBox.getValue());
            if (availabilitySlots.contains(slot)) {
                showMessage("That time slot is already in your schedule.");
                return;
            }
            availabilitySlots.add(slot);
            sortAvailability();
            showMessage("");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        }
    }

    @FXML
    private void removeAvailability() {
        AvailabilitySlot selected = availabilityList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            availabilitySlots.remove(selected);
        }
    }

    @FXML
    private void removeSkill() {
        String skill = selectedSkillsList.getSelectionModel().getSelectedItem();
        if (skill != null) {
            selectedSkills.remove(skill);
            selectedSkillsList.getItems().remove(skill);
            loadSkillMenu(currentSkills);
        }
    }

    private void loadSkillMenu(List<String> skills) {
        currentSkills = List.copyOf(skills);
        skillsMenuButton.getItems().clear();
        for (String skill : currentSkills) {
            CheckMenuItem item = new CheckMenuItem(skill);
            item.setSelected(selectedSkills.contains(skill));
            item.setOnAction(event -> {
                if (item.isSelected()) {
                    selectedSkills.add(skill);
                } else {
                    selectedSkills.remove(skill);
                }
                selectedSkillsList.getItems().setAll(selectedSkills);
                updateSkillButtonText();
            });
            skillsMenuButton.getItems().add(item);
        }

        if (!skillsMenuButton.getItems().isEmpty()) {
            skillsMenuButton.getItems().add(new SeparatorMenuItem());
        }
        MenuItem otherItem = new MenuItem("＋  Other...");
        otherItem.setOnAction(event -> addOtherSkill());
        skillsMenuButton.getItems().add(otherItem);
        updateSkillButtonText();
    }

    private void addOtherSkill() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add a skill");
        dialog.setHeaderText("Add a new skill to the shared list");
        dialog.setContentText("Skill name:");
        dialog.showAndWait().ifPresent(name -> {
            try {
                String savedSkill = volunteerService.addSkill(name);
                selectedSkills.add(savedSkill);
                selectedSkillsList.getItems().setAll(selectedSkills);
                loadSkillMenu(volunteerService.getSkills());
                showMessage("Skill added. Save your profile to keep your selections.");
            } catch (IllegalArgumentException exception) {
                showMessage(exception.getMessage());
            } catch (SQLException exception) {
                LOGGER.log(Level.SEVERE, "Could not add the new skill.", exception);
                showMessage("The new skill could not be saved.");
            }
        });
    }

    private void updateSkillButtonText() {
        skillsMenuButton.setText(selectedSkills.isEmpty()
                ? "Choose skills" : "Choose skills (" + selectedSkills.size() + " selected)");
    }

    private void sortAvailability() {
        availabilitySlots.sort((first, second) -> {
            int dayComparison = first.day().compareTo(second.day());
            return dayComparison != 0 ? dayComparison : first.start().compareTo(second.start());
        });
    }

    private List<LocalTime> getTimes() {
        List<LocalTime> times = new ArrayList<>();
        for (int hour = 6; hour <= 22; hour++) {
            times.add(LocalTime.of(hour, 0));
            if (hour < 22) {
                times.add(LocalTime.of(hour, 30));
            }
        }
        return times;
    }

    @FXML
    private void saveProfile() {
        if (!(sceneManager.getCurrentUser() instanceof Volunteer volunteer)) {
            goHome();
            return;
        }
        try {
            volunteerService.save(
                    volunteer,
                    selectedSkills,
                    List.copyOf(availabilitySlots),
                    preferencesField.getText(),
                    channelBox.getValue());
            showMessage("Your volunteering details have been saved.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not save the volunteer profile.", exception);
            showMessage("We could not save your details. Please try again.");
        }
    }

    @FXML
    private void logServiceHours() {
        if (!(sceneManager.getCurrentUser() instanceof Volunteer volunteer)) {
            goHome();
            return;
        }
        try {
            volunteerService.recordHours(
                    volunteer,
                    assignmentsList.getSelectionModel().getSelectedItem(),
                    hoursDatePicker.getValue(),
                    new java.math.BigDecimal(hoursAmountField.getText().trim()));
            hoursAmountField.clear();
            refreshActivity(volunteer);
            showMessage("Service hours submitted for coordinator approval.");
        } catch (NumberFormatException exception) {
            showMessage("Enter a valid number of hours.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not submit service hours.", exception);
            showMessage("Service hours could not be submitted. Please try again.");
        }
    }

    private void refreshActivity(Volunteer volunteer) throws SQLException {
        assignmentsList.getItems().setAll(volunteerService.getAssignments(volunteer));
        hoursList.getItems().setAll(volunteerService.getServiceHours(volunteer));
        notificationsList.getItems().setAll(
                volunteerService.getAssignmentNotifications(volunteer));
        assignmentsList.setPlaceholder(new Label("No assignments yet."));
        hoursList.setPlaceholder(new Label("No service hours have been submitted yet."));
        notificationsList.setPlaceholder(new Label("No new assignment notifications."));
    }

    @FXML
    private void goHome() {
        try {
            sceneManager.showWelcome();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the welcome page.", exception);
            showMessage("The welcome page could not be opened.");
        }
    }

    @FXML
    private void logOut() {
        sceneManager.setCurrentUser(null);
        goHome();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
