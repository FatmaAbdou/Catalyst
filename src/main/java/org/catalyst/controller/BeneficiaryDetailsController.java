package org.catalyst.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.catalyst.model.AssistanceRecord;
import org.catalyst.model.Beneficiary;
import org.catalyst.model.MonetaryAssistance;
import org.catalyst.service.BeneficiaryService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Displays fresh beneficiary details, assistance history, and follow-up notes. */
public final class BeneficiaryDetailsController {
    private static final Logger LOGGER = Logger.getLogger(BeneficiaryDetailsController.class.getName());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SceneManager sceneManager;
    private final BeneficiaryService beneficiaryService;
    private int beneficiaryID;
    private Beneficiary beneficiary;

    @FXML private Label officerLabel;
    @FXML private Label nameLabel;
    @FXML private Label contactLabel;
    @FXML private Label householdSizeLabel;
    @FXML private Label needsLabel;
    @FXML private Label messageLabel;
    @FXML private Label historyCountLabel;
    @FXML private Label notesTargetLabel;
    @FXML private Label followUpHintLabel;
    @FXML private Button editButton;
    @FXML private Button recordButton;
    @FXML private Button addNoteButton;
    @FXML private TableView<AssistanceRecord> historyTable;
    @FXML private TableColumn<AssistanceRecord, LocalDate> dateColumn;
    @FXML private TableColumn<AssistanceRecord, String> typeColumn;
    @FXML private TableColumn<AssistanceRecord, String> categoryColumn;
    @FXML private TableColumn<AssistanceRecord, String> summaryColumn;
    @FXML private TextArea recordNotes;
    @FXML private TextArea followUpField;

    public BeneficiaryDetailsController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.beneficiaryService = sceneManager.getBeneficiaryService();
    }

    @FXML
    private void initialize() {
        if (!sceneManager.canManageBeneficiaries()) {
            throw new SecurityException("A beneficiary officer account is required.");
        }
        Beneficiary selected = sceneManager.getSelectedBeneficiary();
        if (selected == null || selected.getBeneficiaryID() <= 0) {
            throw new IllegalArgumentException("Select a saved beneficiary.");
        }
        beneficiaryID = selected.getBeneficiaryID();
        officerLabel.setText("Signed in as " + sceneManager.getCurrentUser().getName());

        dateColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getDate()));
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : DATE_FORMAT.format(date));
            }
        });
        typeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue() instanceof MonetaryAssistance ? "Monetary" : "In-kind"));
        categoryColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getCategory()));
        summaryColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getSummary()));
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        historyTable.getSelectionModel().selectedItemProperty().addListener((observable, oldRecord, newRecord) ->
                updateNoteSelection(newRecord));
        addNoteButton.disableProperty().bind(
                historyTable.getSelectionModel().selectedItemProperty().isNull().or(
                        Bindings.createBooleanBinding(() -> followUpField.getText().isBlank(), followUpField.textProperty())));

        loadDetails(null, true);
    }

    @FXML
    private void refresh() {
        AssistanceRecord selected = historyTable.getSelectionModel().getSelectedItem();
        loadDetails(selected == null ? null : selected.getRecordID(), false);
    }

    private boolean loadDetails(Integer selectedRecordID, boolean selectNewest) {
        try {
            Beneficiary fresh = beneficiaryService.getBeneficiaryById(sceneManager.getCurrentUser(), beneficiaryID);
            List<AssistanceRecord> history = beneficiaryService.viewAssistanceHistory(sceneManager.getCurrentUser(), fresh);
            beneficiary = fresh;
            nameLabel.setText(fresh.getName());
            contactLabel.setText(fresh.getContactInfo().isBlank() ? "Not provided" : fresh.getContactInfo());
            householdSizeLabel.setText(Integer.toString(fresh.getHouseholdSize()));
            needsLabel.setText(fresh.getNeeds().isBlank() ? "No needs recorded." : fresh.getNeeds());
            editButton.setDisable(false);
            recordButton.setDisable(false);
            historyTable.getSelectionModel().clearSelection();
            historyTable.getItems().setAll(history);
            historyTable.setPlaceholder(new Label("No assistance recorded yet."));
            historyCountLabel.setText(history.size() == 1 ? "1 record" : history.size() + " records");
            recordNotes.clear();
            if (selectedRecordID != null) {
                history.stream().filter(record -> record.getRecordID() == selectedRecordID).findFirst()
                        .ifPresent(record -> historyTable.getSelectionModel().select(record));
            } else if (selectNewest && !history.isEmpty()) {
                historyTable.getSelectionModel().selectFirst();
            }
            updateNoteSelection(historyTable.getSelectionModel().getSelectedItem());
            showMessage("", false);
            return true;
        } catch (IllegalArgumentException | SecurityException exception) {
            showUnavailable(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load beneficiary details or history.", exception);
            showUnavailable("We couldn't load this beneficiary and their history. Please try Refresh.");
        }
        return false;
    }

    private void showUnavailable(String message) {
        beneficiary = null;
        nameLabel.setText("Unavailable");
        contactLabel.setText("-");
        householdSizeLabel.setText("-");
        needsLabel.setText("-");
        editButton.setDisable(true);
        recordButton.setDisable(true);
        followUpField.setDisable(true);
        historyTable.getItems().clear();
        historyTable.setPlaceholder(new Label("History unavailable."));
        historyCountLabel.setText("");
        recordNotes.clear();
        updateNoteSelection(null);
        showMessage(message, true);
    }

    private void updateNoteSelection(AssistanceRecord selected) {
        boolean hasSelection = selected != null && beneficiary != null;
        followUpField.setDisable(!hasSelection);
        if (hasSelection) {
            recordNotes.setText(selected.getNotes());
            recordNotes.setPromptText("No notes have been recorded for this assistance yet.");
            notesTargetLabel.setText("Selected: " + DATE_FORMAT.format(selected.getDate())
                    + " | " + selected.getCategory() + " | " + selected.getSummary());
            followUpHintLabel.setText("Type your new note below, then click Save follow-up note.");
            followUpField.setPromptText("Type a new follow-up note here");
        } else {
            recordNotes.clear();
            recordNotes.setPromptText("Saved notes for the selected assistance record appear here.");
            if (beneficiary == null) {
                notesTargetLabel.setText("Assistance history is unavailable.");
                followUpHintLabel.setText("Refresh the beneficiary details before adding a note.");
            } else if (historyTable.getItems().isEmpty()) {
                notesTargetLabel.setText("No assistance has been recorded for this beneficiary yet.");
                followUpHintLabel.setText("Click Record assistance first. Follow-up notes belong to an assistance record.");
            } else {
                notesTargetLabel.setText("Select an assistance record in the history table above.");
                followUpHintLabel.setText("Select an assistance record above before typing a follow-up note.");
            }
            followUpField.setPromptText("Select an assistance record first");
        }
    }

    @FXML
    private void addNote() {
        AssistanceRecord selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null || beneficiary == null) {
            showMessage("Select an assistance record first.", true);
            return;
        }
        try {
            beneficiaryService.addNote(sceneManager.getCurrentUser(), selected, followUpField.getText());
        } catch (IllegalArgumentException | SecurityException exception) {
            showMessage(exception.getMessage(), true);
            return;
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not add the follow-up note.", exception);
            showMessage("We couldn't save the note. Your text is still available to retry.", true);
            return;
        }

        followUpField.clear();
        if (loadDetails(selected.getRecordID(), false)) {
            showMessage("Follow-up note added.", false);
        } else {
            showMessage("The note was saved, but history could not be reloaded. Please try Refresh.", true);
        }
    }

    @FXML
    private void editBeneficiary() {
        if (beneficiary == null) {
            return;
        }
        try {
            sceneManager.showEditBeneficiaryFromDetails(beneficiary);
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the beneficiary editor.", exception);
            showMessage("We couldn't open the beneficiary editor.", true);
        }
    }

    @FXML
    private void recordAssistance() {
        if (beneficiary == null) {
            return;
        }
        try {
            sceneManager.showRecordAssistance(beneficiary);
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the assistance form.", exception);
            showMessage("We couldn't open the assistance form.", true);
        }
    }

    @FXML
    private void backToList() {
        try {
            sceneManager.showBeneficiaryList();
        } catch (IOException | SecurityException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the beneficiary list.", exception);
            showMessage("We couldn't open the beneficiary list.", true);
        }
    }

    private void showMessage(String message, boolean error) {
        messageLabel.getStyleClass().remove("error-message");
        if (error) {
            messageLabel.getStyleClass().add("error-message");
        }
        messageLabel.setText(message);
    }
}
