package org.catalyst.controller;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.catalyst.model.Beneficiary;
import org.catalyst.service.BeneficiaryService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads and searches beneficiaries synchronously through the service. */
public final class BeneficiaryListController {
    private static final Logger LOGGER =
            Logger.getLogger(BeneficiaryListController.class.getName());

    private final SceneManager sceneManager;
    private final BeneficiaryService beneficiaryService;

    @FXML private Label officerLabel;
    @FXML private Label messageLabel;
    @FXML private TextField searchField;
    @FXML private Button editButton;
    @FXML private Button detailsButton;
    @FXML private TableView<Beneficiary> beneficiariesTable;
    @FXML private TableColumn<Beneficiary, String> nameColumn;
    @FXML private TableColumn<Beneficiary, String> contactColumn;
    @FXML private TableColumn<Beneficiary, Number> householdColumn;
    @FXML private TableColumn<Beneficiary, String> needsColumn;

    public BeneficiaryListController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.beneficiaryService = sceneManager.getBeneficiaryService();
    }

    @FXML
    private void initialize() {
        if (!sceneManager.canManageBeneficiaries()) {
            throw new SecurityException("A beneficiary officer account is required.");
        }

        officerLabel.setText("Signed in as " + sceneManager.getCurrentUser().getName());

        nameColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getName()));
        contactColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getContactInfo()));
        householdColumn.setCellValueFactory(data ->
                new ReadOnlyIntegerWrapper(data.getValue().getHouseholdSize()));
        needsColumn.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(data.getValue().getNeeds()));

        beneficiariesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        editButton.disableProperty().bind(
                beneficiariesTable.getSelectionModel().selectedItemProperty().isNull());
        detailsButton.disableProperty().bind(
                beneficiariesTable.getSelectionModel().selectedItemProperty().isNull());

        loadBeneficiaries("");
    }

    @FXML
    private void search() {
        loadBeneficiaries(searchField.getText());
    }

    @FXML
    private void refresh() {
        // Refresh keeps the current search filter.
        loadBeneficiaries(searchField.getText());
    }

    private void loadBeneficiaries(String keyword) {
        try {
            List<Beneficiary> results = beneficiaryService.searchBeneficiaries(
                    sceneManager.getCurrentUser(), keyword);

            beneficiariesTable.getItems().setAll(results);
            beneficiariesTable.setPlaceholder(new Label("No beneficiaries found."));
            messageLabel.getStyleClass().remove("error-message");
            messageLabel.setText(results.size() == 1
                    ? "1 beneficiary found."
                    : results.size() + " beneficiaries found.");
        } catch (SecurityException exception) {
            beneficiariesTable.getItems().clear();
            beneficiariesTable.setPlaceholder(new Label("Access unavailable."));
            showError("A beneficiary officer account is required.");
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not load beneficiaries.", exception);
            beneficiariesTable.getItems().clear();
            beneficiariesTable.setPlaceholder(new Label("Could not load beneficiaries."));
            showError("We couldn't load beneficiaries. Please try again.");
        }
    }

    private void showError(String message) {
        messageLabel.getStyleClass().remove("error-message");
        messageLabel.getStyleClass().add("error-message");
        messageLabel.setText(message);
    }

    @FXML
    private void createBeneficiary() {
        try {
            sceneManager.showCreateBeneficiary();
        } catch (IOException | SecurityException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the beneficiary form.", exception);
            showError("We couldn't open the beneficiary form.");
        }
    }

    @FXML
    private void editBeneficiary() {
        Beneficiary selected = beneficiariesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a beneficiary to edit.");
            return;
        }

        try {
            sceneManager.showEditBeneficiary(selected);
        } catch (IOException | SecurityException | IllegalArgumentException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the beneficiary form.", exception);
            showError("We couldn't open the beneficiary form.");
        }
    }

    @FXML
    private void viewDetails() {
        Beneficiary selected = beneficiariesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a beneficiary to view.");
            return;
        }
        try {
            sceneManager.showBeneficiaryDetails(selected);
        } catch (IOException | SecurityException | IllegalArgumentException exception) {
            LOGGER.log(Level.SEVERE, "Could not open beneficiary details.", exception);
            showError("We couldn't open the beneficiary details.");
        }
    }

    @FXML
    private void openHome() {
        try {
            sceneManager.showWelcome();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the welcome page.", exception);
            showError("We couldn't open the home page.");
        }
    }
}
