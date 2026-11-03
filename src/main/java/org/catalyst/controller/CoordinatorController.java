package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.catalyst.model.Certificate;
import org.catalyst.model.ServiceHour;
import org.catalyst.model.Skill;
import org.catalyst.model.user.VolunteerCoordinator;
import org.catalyst.model.volunteer.Volunteer;
import org.catalyst.model.volunteer.VolunteerAssignment;
import org.catalyst.model.volunteer.VolunteerTask;
import org.catalyst.service.AssignmentService;
import org.catalyst.service.VolunteerService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Coordinator controls for creating tasks and matching volunteers. */
@SuppressWarnings("deprecation")
public final class CoordinatorController implements Observer {
    private static final Logger LOGGER = Logger.getLogger(CoordinatorController.class.getName());

    private final SceneManager sceneManager;
    private final VolunteerService volunteerService;
    private final AssignmentService assignmentService;
    private final Set<Skill> selectedRequiredSkills = new LinkedHashSet<>();
    @FXML private Label greetingLabel;
    @FXML private Label messageLabel;
    @FXML private ComboBox<Map.Entry<Integer, String>> eventBox;
    @FXML private TextArea taskDescriptionField;
    @FXML private DatePicker taskDatePicker;
    @FXML private ComboBox<LocalTime> taskStartBox;
    @FXML private ComboBox<LocalTime> taskEndBox;
    @FXML private VBox requiredSkillsBox;
    @FXML private TextField volunteerLimitField;
    @FXML private ListView<VolunteerTask> taskList;
    @FXML private ComboBox<VolunteerTask> scheduleTaskFilterBox;
    @FXML private ComboBox<String> scheduleVolunteerFilterBox;
    @FXML private TableView<VolunteerTask> scheduleTable;
    @FXML private TableColumn<VolunteerTask, String> scheduleTaskColumn;
    @FXML private TableColumn<VolunteerTask, String> scheduleVolunteerColumn;
    @FXML private TableColumn<VolunteerTask, String> scheduleDateColumn;
    @FXML private TableColumn<VolunteerTask, String> scheduleStartColumn;
    @FXML private TableColumn<VolunteerTask, String> scheduleEndColumn;
    @FXML private TextField searchField;
    @FXML private ListView<Volunteer> volunteerList;
    @FXML private Button assignButton;
    @FXML private ComboBox<String> skillFilterBox;
    @FXML private ComboBox<String> availabilityFilterBox;
    @FXML private Label selectedVolunteerInfoLabel;
    @FXML private ListView<VolunteerAssignment> assignmentList;
    @FXML private ListView<ServiceHour> pendingHoursList;
    @FXML private ComboBox<String> certificateTypeBox;
    @FXML private ComboBox<VolunteerTask> certificateTaskBox;
    @FXML private TextArea certificateMessageField;
    private List<Volunteer> searchResults = List.of();
    private List<VolunteerAssignment> allScheduleAssignments = List.of();
    private List<VolunteerTask> allTasks = List.of();

    public CoordinatorController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.volunteerService = sceneManager.getVolunteerService();
        this.assignmentService = sceneManager.getAssignmentService();
    }

    @FXML
    private void initialize() {
        if (!(sceneManager.getCurrentUser() instanceof VolunteerCoordinator coordinator)) {
            goHome();
            return;
        }

        assignmentService.addObserver(this);
        greetingLabel.setText("Hello, " + coordinator.getName());
        taskDatePicker.setValue(LocalDate.now());
        volunteerLimitField.setText("1");
        certificateTypeBox.getItems().setAll("Participation", "Hours milestone", "Appreciation");
        taskStartBox.getItems().setAll(getTimes());
        taskEndBox.getItems().setAll(getTimes());
        configureScheduleTable();
        assignButton.setDisable(true);
        availabilityFilterBox.getItems().setAll("All volunteers", "Available", "Not available");
        availabilityFilterBox.setValue("All volunteers");
        certificateTaskBox.setDisable(true);
        eventBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Map.Entry<Integer, String> event) {
                return event == null ? "" : event.getValue();
            }

            @Override
            public Map.Entry<Integer, String> fromString(String value) {
                return eventBox.getItems().stream()
                        .filter(event -> event.getValue().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        volunteerList.setCellFactory(list -> createVolunteerCell());
        taskList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldTask, task) -> {
                    loadAssignments(task);
                    applyVolunteerFilters();
                    updateAssignButton();
                });
        scheduleTaskFilterBox.valueProperty().addListener(
                (observable, oldTask, task) -> applyScheduleFilters());
        scheduleVolunteerFilterBox.valueProperty().addListener(
                (observable, oldName, name) -> applyScheduleFilters());
        volunteerList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldVolunteer, volunteer) -> {
                    updateVolunteerInfo(volunteer);
                    loadCertificateTasks(volunteer);
                    updateAssignButton();
                });
        skillFilterBox.valueProperty().addListener(
                (observable, oldSkill, skill) -> applyVolunteerFilters());
        availabilityFilterBox.valueProperty().addListener(
                (observable, oldFilter, filter) -> applyVolunteerFilters());
        certificateTypeBox.valueProperty().addListener(
                (observable, oldType, type) -> certificateTaskBox.setDisable(
                        !"Participation".equals(type)));

        try {
            Map<Integer, String> events = volunteerService.getEvents();
            eventBox.getItems().setAll(events.entrySet());
            populateRequiredSkills(volunteerService.getSkills());
            skillFilterBox.getItems().setAll("All skills");
            skillFilterBox.getItems().addAll(volunteerService.getSkills().stream()
                    .map(Skill::name).toList());
            skillFilterBox.setValue("All skills");
            loadTasks();
            searchVolunteers();
            loadPendingHours();
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load coordinator data.", exception);
            showMessage("We could not load coordinator data. Please try again.");
        }
    }

    @FXML
    private void createTask() {
        Map.Entry<Integer, String> event = eventBox.getValue();
        LocalDate date = taskDatePicker.getValue();
        LocalTime starts = taskStartBox.getValue();
        LocalTime ends = taskEndBox.getValue();
        if (date == null || starts == null || ends == null) {
            showMessage("Choose a date, start time, and end time.");
            return;
        }
        try {
            int capacity = Integer.parseInt(volunteerLimitField.getText().trim());
            volunteerService.createTask(
                    event == null ? null : event.getKey(),
                    taskDescriptionField.getText(),
                    LocalDateTime.of(date, starts),
                    LocalDateTime.of(date, ends),
                    Set.copyOf(selectedRequiredSkills),
                    capacity);
            taskDescriptionField.clear();
            selectedRequiredSkills.clear();
            requiredSkillsBox.getChildren().stream()
                    .filter(CheckBox.class::isInstance)
                    .map(CheckBox.class::cast)
                    .forEach(checkBox -> checkBox.setSelected(false));
            showMessage("Volunteer task created.");
            loadTasks();
        } catch (NumberFormatException exception) {
            showMessage("Enter a whole number for the volunteer limit.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not create the volunteer task.", exception);
            showMessage("The task could not be saved. Please try again.");
        }
    }

    @FXML
    private void searchVolunteers() {
        try {
            searchResults = volunteerService.searchVolunteers(searchField.getText());
            applyVolunteerFilters();
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not search volunteers.", exception);
            showMessage("Volunteer search failed. Please try again.");
        }
    }

    @FXML
    private void assignVolunteer() {
        Volunteer volunteer = volunteerList.getSelectionModel().getSelectedItem();
        VolunteerTask task = taskList.getSelectionModel().getSelectedItem();
        if (volunteer == null || task == null) {
            showMessage("Select a volunteer and a task before assigning.");
            return;
        }
        if (!volunteerService.isAvailableForTask(volunteer, task)) {
            showMessage("This volunteer is not available during the selected task.");
            return;
        }
        try {
            assignmentService.assign(volunteer, task);
            loadAssignments(task);
            showMessage("Volunteer assigned to the task.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not assign volunteer to task.", exception);
            loadAssignments(task);
            showMessage(exception.getMessage() != null
                    && exception.getMessage().startsWith("Assignment was saved")
                    ? exception.getMessage()
                    : "The volunteer could not be assigned. Please try again.");
        }
    }

    @FXML
    private void unassignVolunteer() {
        VolunteerAssignment assignment = assignmentList.getSelectionModel().getSelectedItem();
        VolunteerTask task = taskList.getSelectionModel().getSelectedItem();
        if (assignment == null) {
            showMessage("Select an assignment to remove.");
            return;
        }
        try {
            assignmentService.unassign(assignment);
            loadAssignments(task);
            showMessage("Volunteer unassigned from the task.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not unassign volunteer.", exception);
            showMessage("The volunteer could not be unassigned. Please try again.");
        }
    }

    @FXML
    private void deleteAssignment() {
        VolunteerAssignment assignment = assignmentList.getSelectionModel().getSelectedItem();
        VolunteerTask task = taskList.getSelectionModel().getSelectedItem();
        if (assignment == null) {
            showMessage("Select an assignment to delete.");
            return;
        }
        if (!confirm("Delete assignment", "Delete " + assignment.volunteerName()
                + "'s assignment to this task?")) {
            return;
        }
        try {
            assignmentService.delete(assignment);
            loadAssignments(task);
            showMessage("Assignment deleted.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not delete the volunteer assignment.", exception);
            showMessage("The assignment could not be deleted. Please try again.");
        }
    }

    @FXML
    private void deleteTask() {
        VolunteerTask task = taskList.getSelectionModel().getSelectedItem();
        if (task == null) {
            showMessage("Select a task to delete.");
            return;
        }
        if (!confirm("Delete task", "Delete \"" + task.description()
                + "\" and its assignments? Tasks with submitted service hours are kept for history.")) {
            return;
        }
        try {
            volunteerService.deleteTask(task);
            loadTasks();
            showMessage("Task deleted.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not delete the volunteer task.", exception);
            showMessage("The task could not be deleted. Please try again.");
        }
    }

    @FXML
    private void approveHours() {
        ServiceHour hours = pendingHoursList.getSelectionModel().getSelectedItem();
        try {
            volunteerService.approveServiceHour(hours);
            loadPendingHours();
            Volunteer selectedVolunteer = volunteerList.getSelectionModel().getSelectedItem();
            if (selectedVolunteer != null) {
                loadCertificateTasks(selectedVolunteer);
            }
            showMessage("Service hours approved.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not approve service hours.", exception);
            showMessage("The service-hour entry could not be approved. Please try again.");
        }
    }

    @FXML
    private void generateCertificate() {
        Volunteer volunteer = volunteerList.getSelectionModel().getSelectedItem();
        VolunteerTask task = certificateTaskBox.getValue();
        try {
            Certificate certificate = volunteerService.createCertificate(
                    certificateTypeBox.getValue(),
                    volunteer,
                    task,
                    certificateMessageField.getText());
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save volunteer certificate");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("HTML documents", "*.html"));
            chooser.setInitialFileName("Catalyst-Certificate.html");
            File file = chooser.showSaveDialog(sceneManager.getStage());
            if (file == null) {
                return;
            }
            Path output = file.toPath();
            if (!file.getName().toLowerCase().endsWith(".html")) {
                output = Path.of(output + ".html");
            }
            Files.writeString(output, certificate.generate(), StandardCharsets.UTF_8);
            showMessage("Certificate saved. Open the HTML file to print or share it.");
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException | IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not generate the volunteer certificate.", exception);
            showMessage("The certificate could not be saved. Please try again.");
        }
    }

    private void loadPendingHours() throws SQLException {
        pendingHoursList.getItems().setAll(volunteerService.getPendingServiceHours());
        pendingHoursList.setPlaceholder(new Label("No hours are waiting for approval."));
    }

    private void loadCertificateTasks(Volunteer volunteer) {
        if (volunteer == null) {
            certificateTaskBox.getItems().clear();
            return;
        }
        try {
            certificateTaskBox.getItems().setAll(volunteerService.getCertificateTasks(volunteer));
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load eligible certificate tasks.", exception);
            showMessage("Eligible participation tasks could not be loaded.");
        }
    }

    private void loadTasks() throws SQLException {
        allTasks = volunteerService.getTasks();
        taskList.getItems().setAll(allTasks);
        scheduleTaskFilterBox.getItems().setAll(allTasks);
        allScheduleAssignments = volunteerService.getScheduleAssignments();
        scheduleVolunteerFilterBox.getItems().setAll(allScheduleAssignments.stream()
                .map(VolunteerAssignment::volunteerName).distinct().sorted().toList());
        applyScheduleFilters();
        if (!taskList.getItems().isEmpty()) {
            taskList.getSelectionModel().selectFirst();
        } else {
            assignmentList.getItems().clear();
        }
        applyVolunteerFilters();
    }

    private void configureScheduleTable() {
        scheduleTaskColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().description()));
        scheduleVolunteerColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(allScheduleAssignments.stream()
                        .filter(assignment -> assignment.taskId() == cell.getValue().id())
                        .map(VolunteerAssignment::volunteerName)
                        .distinct()
                        .reduce((first, second) -> first + ", " + second)
                        .orElse("No volunteers assigned")));
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
            protected void updateItem(VolunteerTask task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setTooltip(null);
                } else {
                    String volunteers = allScheduleAssignments.stream()
                            .filter(assignment -> assignment.taskId() == task.id())
                            .map(VolunteerAssignment::volunteerName)
                            .distinct()
                            .reduce((first, second) -> first + ", " + second)
                            .orElse("No volunteers assigned");
                    setTooltip(new Tooltip(task.description() + "\n"
                                    + "Volunteers: " + volunteers + "\n"
                                    + task.startsAt().format(DateTimeFormatter.ofPattern(
                                            "EEEE, MMM d, yyyy · h:mm a"))
                                    + " – " + task.endsAt().format(
                                            DateTimeFormatter.ofPattern("h:mm a"))
                                    + "\nEvent: "
                                    + (task.eventName() == null ? "None" : task.eventName())
                                    + "\nRequired skills: "
                                    + (task.requiredSkills().isEmpty() ? "None"
                                    : task.requiredSkills().stream().map(Skill::name)
                                            .collect(java.util.stream.Collectors.joining(", ")))));
                }
            }
        });
    }

    private void applyScheduleFilters() {
        VolunteerTask task = scheduleTaskFilterBox.getValue();
        String volunteerName = scheduleVolunteerFilterBox.getValue();
        scheduleTable.getItems().setAll(allTasks.stream()
                .filter(scheduledTask -> task == null || scheduledTask.id() == task.id())
                .filter(scheduledTask -> volunteerName == null
                        || allScheduleAssignments.stream().anyMatch(assignment ->
                                assignment.taskId() == scheduledTask.id()
                                        && assignment.volunteerName().equals(volunteerName)))
                .toList());
        scheduleTable.setPlaceholder(new Label("No tasks match these filters."));
    }

    @FXML
    private void clearScheduleFilters() {
        scheduleTaskFilterBox.setValue(null);
        scheduleVolunteerFilterBox.setValue(null);
    }

    private void populateRequiredSkills(List<Skill> skills) {
        requiredSkillsBox.getChildren().clear();
        for (Skill skill : skills) {
            CheckBox checkBox = new CheckBox(skill.name());
            checkBox.setSelected(selectedRequiredSkills.contains(skill));
            checkBox.setOnAction(event -> {
                if (checkBox.isSelected()) {
                    selectedRequiredSkills.add(skill);
                } else {
                    selectedRequiredSkills.remove(skill);
                }
            });
            requiredSkillsBox.getChildren().add(checkBox);
        }
    }

    private void applyVolunteerFilters() {
        String skill = skillFilterBox.getValue();
        String availability = availabilityFilterBox.getValue();
        VolunteerTask selectedTask = taskList.getSelectionModel().getSelectedItem();
        List<Volunteer> filtered = searchResults.stream()
                .filter(volunteer -> skill == null || "All skills".equals(skill)
                        || volunteer.getSkills().stream().anyMatch(
                                        volunteerSkill -> volunteerSkill.name().equalsIgnoreCase(skill)))
                .filter(volunteer -> {
                    if ("Available".equals(availability)) {
                        return volunteerService.isAvailableForTask(volunteer, selectedTask);
                    }
                    if ("Not available".equals(availability)) {
                        return selectedTask != null
                                && !volunteerService.isAvailableForTask(volunteer, selectedTask);
                    }
                    return true;
                })
                .toList();
        volunteerList.getItems().setAll(filtered);
        volunteerList.setPlaceholder(new Label(selectedTask == null
                ? "Select a task to check volunteer availability."
                : "No volunteers match these filters."));
        updateVolunteerInfo(volunteerList.getSelectionModel().getSelectedItem());
        updateAssignButton();
    }

    private void updateAssignButton() {
        Volunteer volunteer = volunteerList.getSelectionModel().getSelectedItem();
        VolunteerTask task = taskList.getSelectionModel().getSelectedItem();
        assignButton.setDisable(!volunteerService.isAvailableForTask(volunteer, task));
    }

    private void updateVolunteerInfo(Volunteer volunteer) {
        if (volunteer == null) {
            selectedVolunteerInfoLabel.setText("Select a volunteer to view preferences.");
            return;
        }
        VolunteerTask task = taskList.getSelectionModel().getSelectedItem();
        String availability = task == null ? "Choose a task to check availability."
                : volunteerService.isAvailableForTask(volunteer, task)
                ? "Available for this task." : "Not available for this task.";
        String preferences = volunteer.getPreferences().isBlank()
                ? "No preferences provided." : volunteer.getPreferences();
        selectedVolunteerInfoLabel.setText("Preferences: " + preferences + "\n" + availability);
    }

    private ListCell<Volunteer> createVolunteerCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Volunteer volunteer, boolean empty) {
                super.updateItem(volunteer, empty);
                if (empty || volunteer == null) {
                    setGraphic(null);
                    setText(null);
                    getStyleClass().remove("volunteer-result-cell");
                    return;
                }

                Label avatar = new Label(initials(volunteer.getName()));
                avatar.getStyleClass().add("volunteer-avatar");
                avatar.setMinSize(42, 42);
                avatar.setAlignment(Pos.CENTER);

                Label name = new Label(volunteer.getName());
                name.getStyleClass().add("volunteer-result-name");
                Label email = new Label(volunteer.getEmail());
                email.getStyleClass().add("volunteer-result-email");
                Label skills = new Label(volunteer.getSkills().isEmpty()
                        ? "Skills not added"
                        : volunteer.getSkills().stream()
                                .map(Skill::toString)
                                .collect(java.util.stream.Collectors.joining("  ·  ")));
                skills.getStyleClass().add("volunteer-result-skills");
                skills.setWrapText(true);
                VolunteerTask selectedTask = taskList.getSelectionModel().getSelectedItem();
                boolean available = volunteerService.isAvailableForTask(volunteer, selectedTask);
                Label availability = new Label(selectedTask == null
                        ? "Choose a task"
                        : available ? "AVAILABLE" : "NOT AVAILABLE");
                availability.getStyleClass().add(available
                        ? "volunteer-availability-available"
                        : "volunteer-availability-unavailable");
                VBox details = new VBox(4, name, email, skills, availability);
                HBox card = new HBox(12, avatar, details);
                card.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(details, Priority.ALWAYS);
                setText(null);
                setGraphic(card);
                setOpacity(selectedTask != null && !available ? 0.58 : 1);
                if (!getStyleClass().contains("volunteer-result-cell")) {
                    getStyleClass().add("volunteer-result-cell");
                }
            }
        };
    }

    private String initials(String name) {
        return java.util.Arrays.stream(name.trim().split("\\s+"))
                .filter(part -> !part.isEmpty())
                .limit(2)
                .map(part -> part.substring(0, 1).toUpperCase())
                .reduce("", String::concat);
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void loadAssignments(VolunteerTask task) {
        if (task == null) {
            assignmentList.getItems().clear();
            return;
        }
        try {
            assignmentList.getItems().setAll(volunteerService.getAssignments(task));
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load task assignments.", exception);
            showMessage("The task assignments could not be loaded.");
        }
    }

    @Override
    public void update(Observable observable, Object event) {
        if (!(event instanceof VolunteerAssignment assignment)) {
            LOGGER.warning("Received an unexpected assignment schedule update.");
            return;
        }
        try {
            loadTasks();
            taskList.getItems().stream()
                    .filter(task -> task.id() == assignment.taskId())
                    .findFirst()
                    .ifPresent(task -> taskList.getSelectionModel().select(task));
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not refresh the volunteer task schedule.", exception);
            showMessage("The assignment was saved, but the task schedule could not refresh.");
        }
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
    private void goHome() {
        assignmentService.deleteObserver(this);
        try {
            sceneManager.showWelcome();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the welcome page.", exception);
            showMessage("The welcome page could not be opened.");
        }
    }

    @FXML
    private void logOut() {
        assignmentService.deleteObserver(this);
        sceneManager.setCurrentUser(null);
        goHome();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
