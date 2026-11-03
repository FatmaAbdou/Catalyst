package org.catalyst.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.catalyst.model.AvailabilitySlot;
import org.catalyst.model.AssignmentNotification;
import org.catalyst.model.Certificate;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Volunteer;
import org.catalyst.model.VolunteerAssignment;
import org.catalyst.model.VolunteerTask;
import org.catalyst.service.VolunteerService;
import org.catalyst.view.SceneManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
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
    private int approvedHours;

    @FXML private Label greetingLabel;
    @FXML private Label messageLabel;
    @FXML private Label hoursLimitLabel;
    @FXML private Label approvedHoursLabel;
    @FXML private Label certificateInfoLabel;
    @FXML private Label notificationCountLabel;
    @FXML private Label notificationDetailsLabel;
    @FXML private Button downloadCertificateButton;
    @FXML private MenuButton skillsMenuButton;
    @FXML private ListView<String> selectedSkillsList;
    @FXML private ComboBox<DayOfWeek> dayBox;
    @FXML private ComboBox<LocalTime> startTimeBox;
    @FXML private ComboBox<LocalTime> endTimeBox;
    @FXML private ListView<AvailabilitySlot> availabilityList;
    @FXML private TextArea preferencesField;
    @FXML private ComboBox<String> channelBox;
    @FXML private Button logHoursButton;
    @FXML private ListView<VolunteerAssignment> assignmentsList;
    @FXML private ListView<ServiceHour> hoursList;
    @FXML private ListView<AssignmentNotification> notificationsList;
    @FXML private TableView<VolunteerAssignment> scheduleTable;
    @FXML private TableColumn<VolunteerAssignment, String> scheduleTaskColumn;
    @FXML private TableColumn<VolunteerAssignment, String> scheduleDateColumn;
    @FXML private TableColumn<VolunteerAssignment, String> scheduleStartColumn;
    @FXML private TableColumn<VolunteerAssignment, String> scheduleEndColumn;
    @FXML private ListView<VolunteerTask> certificateTasksList;
    @FXML private ComboBox<String> certificateTypeBox;
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
        hoursDatePicker.setValue(LocalDate.now());
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
        certificateTypeBox.getItems().setAll("Participation", "Hours milestone");
        configureScheduleTable();
        notificationsList.setCellFactory(list -> createNotificationCell());
        assignmentsList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldAssignment, assignment) -> updateHoursLimit(assignment));
        notificationsList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldNotification, notification) -> showNotificationDetails(notification));
        certificateTasksList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTask, task) -> updateCertificateInfo(task));
        certificateTypeBox.valueProperty().addListener(
                (observable, oldType, type) -> updateCertificateInfo(
                        certificateTasksList.getSelectionModel().getSelectedItem()));
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
            loadCertificates(volunteer);
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
        scheduleTable.getItems().setAll(volunteerService.getScheduleAssignments(volunteer));
        notificationCountLabel.setText(Integer.toString(
                volunteerService.getUnreadAssignmentNotificationCount(volunteer)));
        assignmentsList.setPlaceholder(new Label("No assignments yet."));
        hoursList.setPlaceholder(new Label("No service hours have been submitted yet."));
        notificationsList.setPlaceholder(new Label("No assignment notifications."));
        updateHoursLimit(assignmentsList.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void markNotificationRead() {
        if (!(sceneManager.getCurrentUser() instanceof Volunteer volunteer)) {
            goHome();
            return;
        }
        AssignmentNotification notification =
                notificationsList.getSelectionModel().getSelectedItem();
        try {
            volunteerService.markAssignmentNotificationRead(volunteer, notification);
            refreshActivity(volunteer);
            if (notification != null) {
                notificationsList.getSelectionModel().select(
                        notificationsList.getItems().stream()
                                .filter(item -> item.id() == notification.id())
                                .findFirst().orElse(null));
            }
            showMessage("Notification marked as read.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not mark the assignment notification as read.", exception);
            showMessage("The notification could not be marked as read. Please try again.");
        }
    }

    private void showNotificationDetails(AssignmentNotification notification) {
        if (notification == null) {
            notificationDetailsLabel.setText("Select a notification to see its task details.");
            return;
        }
        VolunteerTask task = notification.task();
        String event = task.eventName() == null ? "Not linked to an event" : task.eventName();
        String skills = task.requiredSkills().isEmpty()
                ? "No specific skills required" : String.join(", ", task.requiredSkills());
        notificationDetailsLabel.setText(
                task.description() + "\n"
                        + "Event: " + event + "\n"
                        + "When: " + task.startsAt().format(DateTimeFormatter.ofPattern(
                                "EEE, MMM d, yyyy 'at' h:mm a"))
                        + " – " + task.endsAt().format(DateTimeFormatter.ofPattern("h:mm a")) + "\n"
                        + "Required skills: " + skills);
    }

    private ListCell<AssignmentNotification> createNotificationCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(AssignmentNotification notification, boolean empty) {
                super.updateItem(notification, empty);
                setText(empty || notification == null ? null : notification.toString());
                if (!empty && notification != null && !notification.read()) {
                    if (!getStyleClass().contains("unread-notification")) {
                        getStyleClass().add("unread-notification");
                    }
                } else {
                    getStyleClass().remove("unread-notification");
                }
            }
        };
    }

    private void configureScheduleTable() {
        scheduleTaskColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().taskDescription()));
        scheduleDateColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().startsAt()
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))));
        scheduleStartColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().startsAt()
                        .format(DateTimeFormatter.ofPattern("h:mm a"))));
        scheduleEndColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().endsAt()
                        .format(DateTimeFormatter.ofPattern("h:mm a"))));
        scheduleTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(VolunteerAssignment assignment, boolean empty) {
                super.updateItem(assignment, empty);
                if (empty || assignment == null) {
                    setTooltip(null);
                } else {
                    Tooltip tooltip = new Tooltip(
                            assignment.taskDescription() + "\n"
                                    + assignment.startsAt().format(DateTimeFormatter.ofPattern(
                                            "EEEE, MMM d, yyyy · h:mm a"))
                                    + " – " + assignment.endsAt().format(
                                            DateTimeFormatter.ofPattern("h:mm a"))
                                    + "\nStatus: " + assignment.status());
                    setTooltip(tooltip);
                }
            }
        });
    }

    private void updateHoursLimit(VolunteerAssignment assignment) {
        if (assignment == null) {
            hoursLimitLabel.setText("Select an assignment to see its hour limit.");
            logHoursButton.setDisable(true);
            return;
        }
        Duration duration = Duration.between(assignment.startsAt(), assignment.endsAt());
        java.math.BigDecimal maximum = java.math.BigDecimal.valueOf(duration.toSeconds())
                .divide(java.math.BigDecimal.valueOf(3600), 2, java.math.RoundingMode.DOWN);
        java.math.BigDecimal logged = hoursList.getItems().stream()
                .filter(hour -> hour.assignmentId() == assignment.id())
                .map(ServiceHour::hours)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal remaining = maximum.subtract(logged).max(java.math.BigDecimal.ZERO);
        logHoursButton.setDisable(remaining.signum() <= 0);
        hoursDatePicker.setValue(assignment.startsAt().toLocalDate());
        hoursLimitLabel.setText("Scheduled duration: "
                + maximum.stripTrailingZeros().toPlainString() + " hours · "
                + remaining.stripTrailingZeros().toPlainString() + " hours remaining.");
    }

    private void loadCertificates(Volunteer volunteer) throws SQLException {
        certificateTasksList.getItems().setAll(volunteerService.getCertificateTasks(volunteer));
        certificateTasksList.setPlaceholder(
                new Label("No tasks have approved service hours yet."));
        approvedHours = volunteerService.getApprovedHours(volunteer);
        approvedHoursLabel.setText("Approved hours: " + approvedHours);
        if (!certificateTasksList.getItems().isEmpty()) {
            certificateTasksList.getSelectionModel().selectFirst();
        }
        updateCertificateInfo(certificateTasksList.getSelectionModel().getSelectedItem());
    }

    private void updateCertificateInfo(VolunteerTask task) {
        String type = certificateTypeBox.getValue();
        if ("Hours milestone".equals(type)) {
            downloadCertificateButton.setDisable(approvedHours < 25);
            certificateInfoLabel.setText(approvedHours < 25
                    ? "At least 25 approved hours are needed for a milestone certificate. "
                            + "You currently have " + approvedHours + "."
                    : "Your approved hours qualify for a "
                            + (approvedHours / 25 * 25) + "-hour milestone certificate.");
            return;
        }
        if (!"Participation".equals(type)) {
            downloadCertificateButton.setDisable(true);
            certificateInfoLabel.setText("Choose a certificate type to continue.");
            return;
        }
        if (task == null) {
            downloadCertificateButton.setDisable(true);
            certificateInfoLabel.setText("Choose a task for a participation certificate. "
                    + "An hours milestone certificate uses your total approved hours.");
            return;
        }
        downloadCertificateButton.setDisable(false);
        certificateInfoLabel.setText("Approved participation: " + task.description()
                + " · " + task.startsAt().toLocalDate());
    }

    @FXML
    private void downloadCertificate() {
        if (!(sceneManager.getCurrentUser() instanceof Volunteer volunteer)) {
            goHome();
            return;
        }
        String type = certificateTypeBox.getValue();
        VolunteerTask task = "Participation".equals(type)
                ? certificateTasksList.getSelectionModel().getSelectedItem() : null;
        try {
            Certificate certificate = volunteerService.createCertificate(
                    type, volunteer, task, "");
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Download volunteer certificate");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("HTML documents", "*.html"));
            chooser.setInitialFileName("Catalyst-Certificate.html");
            File file = chooser.showSaveDialog(sceneManager.getStage());
            if (file == null) {
                return;
            }
            Path output = file.toPath();
            if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".html")) {
                output = Path.of(output + ".html");
            }
            Files.writeString(output, certificate.generate(), StandardCharsets.UTF_8);
            showMessage("Certificate downloaded. Open the HTML file to print or share it.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException | IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not download the volunteer certificate.", exception);
            showMessage("The certificate could not be downloaded. Please try again.");
        }
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
