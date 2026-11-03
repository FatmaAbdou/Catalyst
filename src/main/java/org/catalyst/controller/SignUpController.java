package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.catalyst.model.DonorFactory;
import org.catalyst.model.UserFactory;
import org.catalyst.model.UserType;
import org.catalyst.model.VolunteerFactory;
import org.catalyst.model.VolunteerCoordinatorFactory;
import org.catalyst.service.AuthService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects sign-up details and passes account creation to the authentication service. */
public final class SignUpController {
    private static final Logger LOGGER = Logger.getLogger(SignUpController.class.getName());

    private final SceneManager sceneManager;
    private final AuthService authService;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField contactField;
    @FXML private ComboBox<UserType> userTypeBox;
    @FXML private VBox organizationSection;
    @FXML private TextField organizationField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    public SignUpController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.authService = sceneManager.getAuthService();
    }

    @FXML
    private void initialize() {
        userTypeBox.getItems().setAll(
                UserType.DONOR, UserType.VOLUNTEER, UserType.VOLUNTEER_COORDINATOR);
        userTypeBox.setValue(UserType.VOLUNTEER);
        updateOrganizationVisibility();
        userTypeBox.valueProperty().addListener((observable, oldValue, newValue) ->
                updateOrganizationVisibility());
    }

    @FXML
    private void createAccount() {
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showMessage("The passwords do not match.");
            return;
        }

        char[] password = passwordField.getText().toCharArray();
        try {
            UserFactory userFactory = switch (userTypeBox.getValue()) {
                case DONOR -> new DonorFactory();
                case VOLUNTEER -> new VolunteerFactory();
                case VOLUNTEER_COORDINATOR -> new VolunteerCoordinatorFactory();
                case null -> throw new IllegalArgumentException("Choose an account type.");
                default -> throw new IllegalArgumentException("Choose a public account type.");
            };
            authService.signUp(
                    nameField.getText(),
                    emailField.getText(),
                    contactField.getText(),
                    userFactory,
                    organizationField.getText(),
                    password);
            passwordField.clear();
            confirmPasswordField.clear();
            messageLabel.getStyleClass().remove("error-message");
            messageLabel.setText("Your account is ready. Please log in.");
            sceneManager.showLogin();
        } catch (IllegalArgumentException exception) {
            showMessage(exception.getMessage());
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not create the user account.", exception);
            showMessage("We could not create your account right now. Please try again.");
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "The login page could not be opened.", exception);
            showMessage("Your account was created, but the login page could not be opened.");
        } finally {
            Arrays.fill(password, '\0');
            passwordField.clear();
            confirmPasswordField.clear();
        }
    }

    @FXML
    private void openWelcome() {
        try {
            sceneManager.showWelcome();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the welcome page.", exception);
            showMessage("The welcome page could not be opened.");
        }
    }

    @FXML
    private void openLogin() {
        try {
            sceneManager.showLogin();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the login page.", exception);
            showMessage("The login page could not be opened.");
        }
    }

    private void updateOrganizationVisibility() {
        boolean isDonor = userTypeBox.getValue() == UserType.DONOR;
        organizationSection.setVisible(isDonor);
        organizationSection.setManaged(isDonor);
    }

    private void showMessage(String message) {
        messageLabel.getStyleClass().remove("error-message");
        messageLabel.getStyleClass().add("error-message");
        messageLabel.setText(message);
    }
}
