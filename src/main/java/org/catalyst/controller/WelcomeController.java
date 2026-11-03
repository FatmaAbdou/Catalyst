package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.catalyst.model.Volunteer;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Handles navigation from the welcome page. */
public final class WelcomeController {
    private static final Logger LOGGER = Logger.getLogger(WelcomeController.class.getName());

    private final SceneManager sceneManager;

    @FXML private Label welcomeMessage;
    @FXML private Button signUpButton;
    @FXML private Button topSignUpButton;
    @FXML private Button logInButton;
    @FXML private Button beneficiariesButton;
    @FXML private Button logOutButton;
    @FXML private Button volunteerButton;

    public WelcomeController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    private void initialize() {
        var user = sceneManager.getCurrentUser();
        boolean signedIn = user != null;
        welcomeMessage.setText(signedIn
                ? "Welcome back, " + user.getName() + ". Your kindness makes a difference."
                : "Small acts of kindness can change a community.");
        setShown(signUpButton, !signedIn);
        setShown(topSignUpButton, !signedIn);
        setShown(logInButton, !signedIn);
        setShown(logOutButton, signedIn);
        setShown(beneficiariesButton, sceneManager.canManageBeneficiaries());
        setShown(volunteerButton, user instanceof Volunteer);
    }

    @FXML
    private void openSignUp() {
        navigate(sceneManager::showSignUp);
    }

    @FXML
    private void openLogin() {
        navigate(sceneManager::showLogin);
    }

    @FXML
    private void openBeneficiaries() {
        navigate(sceneManager::showBeneficiaryList);
    }

    @FXML
    private void logOut() {
        sceneManager.setCurrentUser(null);
        navigate(sceneManager::showWelcome);
    }

    @FXML
    private void openVolunteerDashboard() {
        navigate(sceneManager::showVolunteerDashboard);
    }

    private void navigate(ViewAction action) {
        try {
            action.run();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the requested page.", exception);
            welcomeMessage.setText("We couldn't open that page. Please try again.");
        }
    }

    private void setShown(Button button, boolean shown) {
        button.setVisible(shown);
        button.setManaged(shown);
    }

    @FunctionalInterface
    private interface ViewAction {
        void run() throws IOException;
    }
}
