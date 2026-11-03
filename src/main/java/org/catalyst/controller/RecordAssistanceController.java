package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.catalyst.model.AssistanceFactory;
import org.catalyst.model.AssistanceRecord;
import org.catalyst.model.beneficiary.Beneficiary;
import org.catalyst.model.InKindAssistance;
import org.catalyst.model.MonetaryAssistance;
import org.catalyst.service.BeneficiaryService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects a completed assistance draft and records it with no background workers. */
public final class RecordAssistanceController {
    private static final Logger LOGGER = Logger.getLogger(RecordAssistanceController.class.getName());
    private final SceneManager sceneManager;
    private final BeneficiaryService beneficiaryService;
    private final AssistanceFactory factory = new AssistanceFactory();
    private Beneficiary beneficiary;
    private boolean saved;

    @FXML private Label officerLabel;
    @FXML private Label beneficiaryLabel;
    @FXML private Label messageLabel;
    @FXML private DatePicker deliveryDatePicker;
    @FXML private ComboBox<String> categoryBox;
    @FXML private ComboBox<String> typeBox;
    @FXML private VBox monetaryFields;
    @FXML private VBox inKindFields;
    @FXML private TextField amountField;
    @FXML private TextField descriptionField;
    @FXML private TextField quantityField;
    @FXML private TextField unitField;
    @FXML private TextArea notesField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    public RecordAssistanceController(SceneManager sceneManager) {
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
        officerLabel.setText("Signed in as " + sceneManager.getCurrentUser().getName());
        typeBox.getItems().setAll("Monetary", "In-kind");
        typeBox.setValue("Monetary");
        typeBox.valueProperty().addListener((observable, oldType, newType) -> updateTypeFields());
        updateTypeFields();
        deliveryDatePicker.setValue(LocalDate.now());
        deliveryDatePicker.setEditable(false);
        deliveryDatePicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date == null || date.isAfter(LocalDate.now()));
            }
        });

        try {
            beneficiary = beneficiaryService.getBeneficiaryById(
                    sceneManager.getCurrentUser(), selected.getBeneficiaryID());
            beneficiaryLabel.setText("For " + beneficiary.getName());
            reloadCategories();
        } catch (IllegalArgumentException | SecurityException exception) {
            beneficiaryLabel.setText("Beneficiary unavailable");
            saveButton.setDisable(true);
            showMessage(exception.getMessage(), true);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load the assistance recipient.", exception);
            beneficiaryLabel.setText("Beneficiary unavailable");
            saveButton.setDisable(true);
            showMessage("We couldn't load this beneficiary. Return to details and try again.", true);
        }
    }

    private void updateTypeFields() {
        boolean monetary = "Monetary".equals(typeBox.getValue());
        monetaryFields.setVisible(monetary);
        monetaryFields.setManaged(monetary);
        inKindFields.setVisible(!monetary);
        inKindFields.setManaged(!monetary);
    }

    @FXML
    private void reloadCategories() {
        if (beneficiary == null) {
            return;
        }
        try {
            String selectedCategory = categoryBox.getValue();
            List<String> categories = beneficiaryService.getAssistanceCategories(sceneManager.getCurrentUser());
            categoryBox.getItems().setAll(categories);
            if (selectedCategory != null && categories.contains(selectedCategory)) {
                categoryBox.setValue(selectedCategory);
            } else {
                categoryBox.setValue(categories.size() == 1 ? categories.getFirst() : null);
            }
            categoryBox.setDisable(categories.isEmpty());
            saveButton.setDisable(saved || categories.isEmpty());
            if (categories.isEmpty()) {
                showMessage("No assistance categories are available. Contact your project administrator.", true);
            } else {
                showMessage(saved ? "Assistance already recorded. Use Back to details to return." : "", false);
            }
        } catch (SecurityException exception) {
            categoryBox.getItems().clear();
            categoryBox.setValue(null);
            categoryBox.setDisable(true);
            saveButton.setDisable(true);
            showMessage(exception.getMessage(), true);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load assistance categories.", exception);
            categoryBox.getItems().clear();
            categoryBox.setValue(null);
            categoryBox.setDisable(true);
            saveButton.setDisable(true);
            showMessage("We couldn't load categories. Please try Reload categories.", true);
        }
    }

    @FXML
    private void save() {
        if (saved || beneficiary == null) {
            return;
        }
        try {
            String type = switch (typeBox.getValue() == null ? "" : typeBox.getValue()) {
                case "Monetary" -> "MONETARY";
                case "In-kind" -> "IN_KIND";
                default -> throw new IllegalArgumentException("Choose an assistance type.");
            };
            AssistanceRecord draft = factory.createAssistance(type);
            draft.setDate(deliveryDatePicker.getValue());
            draft.setCategory(categoryBox.getValue());
            draft.setNotes(notesField.getText());
            if (draft instanceof MonetaryAssistance monetary) {
                monetary.setAmount(new BigDecimal(amountField.getText().trim()));
            } else {
                ((InKindAssistance) draft).setItemDetails(descriptionField.getText(),
                        Integer.parseInt(quantityField.getText().trim()), unitField.getText());
            }
            beneficiaryService.recordAssistance(sceneManager.getCurrentUser(), beneficiary, draft);
        } catch (NumberFormatException exception) {
            showMessage("Monetary".equals(typeBox.getValue())
                    ? "Enter a valid decimal amount." : "Enter a whole number for quantity.", true);
            return;
        } catch (IllegalArgumentException | SecurityException exception) {
            showMessage(exception.getMessage(), true);
            return;
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not record assistance.", exception);
            showMessage("We couldn't record assistance. Your entries are still available to retry.", true);
            return;
        }

        saved = true;
        saveButton.setDisable(true);
        cancelButton.setText("Back to details");
        showMessage("Assistance recorded successfully.", false);
        try {
            sceneManager.showBeneficiaryDetails(beneficiary);
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            LOGGER.log(Level.WARNING, "Assistance was saved, but details could not be opened.", exception);
            showMessage("Assistance was recorded. Use Back to details to return.", false);
        }
    }

    @FXML
    private void cancel() {
        Beneficiary target = beneficiary != null ? beneficiary : sceneManager.getSelectedBeneficiary();
        try {
            sceneManager.showBeneficiaryDetails(target);
        } catch (IOException | IllegalArgumentException | SecurityException exception) {
            LOGGER.log(Level.SEVERE, "Could not return to beneficiary details.", exception);
            showMessage("We couldn't open beneficiary details. Please try again.", true);
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
