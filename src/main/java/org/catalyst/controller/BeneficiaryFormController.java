package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.catalyst.model.beneficiary.Beneficiary;
import org.catalyst.service.BeneficiaryService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects create/edit details and saves them synchronously through the service. */
public final class BeneficiaryFormController {
    private static final Logger LOGGER =
            Logger.getLogger(BeneficiaryFormController.class.getName());

    private final SceneManager sceneManager;
    private final BeneficiaryService beneficiaryService;

    private int editingBeneficiaryID;
    private boolean saved;
    private Beneficiary initialBeneficiary;
    private Beneficiary savedBeneficiary;

    @FXML private Label officerLabel;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private TextField nameField;
    @FXML private TextField contactField;
    @FXML private TextField householdSizeField;
    @FXML private TextArea needsField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    public BeneficiaryFormController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.beneficiaryService = sceneManager.getBeneficiaryService();
    }

    @FXML
    private void initialize() {
        if (!sceneManager.canManageBeneficiaries()) {
            throw new SecurityException("A beneficiary officer account is required.");
        }

        officerLabel.setText("Signed in as " + sceneManager.getCurrentUser().getName());

        Beneficiary selected = sceneManager.getSelectedBeneficiary();
        initialBeneficiary = selected;
        if (selected != null) {
            editingBeneficiaryID = selected.getBeneficiaryID();
            titleLabel.setText("Edit beneficiary");
            saveButton.setText("Save changes");
            nameField.setText(selected.getName());
            contactField.setText(selected.getContactInfo());
            householdSizeField.setText(Integer.toString(selected.getHouseholdSize()));
            needsField.setText(selected.getNeeds());
        }
    }

    @FXML
    private void save() {
        if (saved) {
            return;
        }

        Beneficiary beneficiary;
        try {
            int householdSize = Integer.parseInt(householdSizeField.getText().trim());

            // Keep the selected model unchanged until the database save succeeds.
            beneficiary = new Beneficiary(
                    nameField.getText(), contactField.getText(), householdSize, needsField.getText());

            if (editingBeneficiaryID == 0) {
                beneficiaryService.createBeneficiary(sceneManager.getCurrentUser(), beneficiary);
            } else {
                beneficiary.setBeneficiaryID(editingBeneficiaryID);
                beneficiaryService.updateBeneficiary(sceneManager.getCurrentUser(), beneficiary);
            }
        } catch (NumberFormatException exception) {
            showMessage("Enter a whole number for household size.", true);
            return;
        } catch (IllegalArgumentException | SecurityException exception) {
            showMessage(exception.getMessage(), true);
            return;
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not save the beneficiary.", exception);
            showMessage("We couldn't save this beneficiary. Please try again.", true);
            return;
        }

        saved = true;
        savedBeneficiary = beneficiary;
        saveButton.setDisable(true);
        cancelButton.setText("Back");
        showMessage("Beneficiary saved successfully.", false);

        try {
            sceneManager.returnFromBeneficiaryForm(savedBeneficiary);
        } catch (IOException | SecurityException exception) {
            LOGGER.log(Level.WARNING, "Beneficiary saved, but the return page could not be opened.", exception);
            showMessage("Beneficiary saved. Use Back to return.", false);
        }
    }

    @FXML
    private void cancel() {
        try {
            sceneManager.returnFromBeneficiaryForm(saved ? savedBeneficiary : initialBeneficiary);
        } catch (IOException | SecurityException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the return page.", exception);
            showMessage("We couldn't open the previous page.", true);
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
