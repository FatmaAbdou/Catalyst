package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.catalyst.service.AuthService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Checks credentials and stores a successful login in the application session. */
public final class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    private final SceneManager sceneManager;
    private final AuthService authService;

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    public LoginController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.authService = sceneManager.getAuthService();
    }

    @FXML
    private void logIn() {
        char[] password = passwordField.getText().toCharArray();
        try {
            var user = authService.logIn(emailField.getText(), password);
            if (user.isEmpty()) {
                showMessage("Email or password is incorrect, or this account is inactive.");
                return;
            }
            sceneManager.setCurrentUser(user.get());
            sceneManager.showWelcome();
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not check the login credentials.", exception);
            showMessage("We could not log you in right now. Please try again.");
        } catch (IllegalStateException exception) {
            LOGGER.log(Level.SEVERE, "The saved password hash could not be checked.", exception);
            showMessage("We could not check this account's password. Please contact support.");
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "The welcome page could not be opened.", exception);
            showMessage("You are signed in, but the welcome page could not be opened.");
        } finally {
            Arrays.fill(password, '\0');
            passwordField.clear();
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
    private void openSignUp() {
        try {
            sceneManager.showSignUp();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the sign-up page.", exception);
            showMessage("The sign-up page could not be opened.");
        }
    }

    private void showMessage(String message) {
        messageLabel.getStyleClass().remove("error-message");
        messageLabel.getStyleClass().add("error-message");
        messageLabel.setText(message);
    }
}
