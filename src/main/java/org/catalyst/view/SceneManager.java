package org.catalyst.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.catalyst.model.User;
import org.catalyst.model.Volunteer;
import org.catalyst.repo.UserRepository;
import org.catalyst.repo.VolunteerRepository;
import org.catalyst.service.AuthService;
import org.catalyst.service.PasswordHasher;
import org.catalyst.service.VolunteerService;

import java.io.IOException;
import java.util.Objects;

/** Loads the application's FXML views and shares the current login session. */
public final class SceneManager {
    private final Stage stage;
    private Scene scene;
    private final AuthService authService = new AuthService(new UserRepository(), new PasswordHasher());
    private final VolunteerService volunteerService = new VolunteerService(new VolunteerRepository());
    private User currentUser;

    public SceneManager(Stage stage) {
        this.stage = stage;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public VolunteerService getVolunteerService() {
        return volunteerService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void showWelcome() throws IOException {
        show("welcome.fxml");
    }

    public void showSignUp() throws IOException {
        show("signup.fxml");
    }

    public void showLogin() throws IOException {
        show("login.fxml");
    }

    public void showVolunteerDashboard() throws IOException {
        show("volunteer.fxml");
    }

    private void show(String fileName) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/org/catalyst/view/" + fileName)));
        loader.setControllerFactory(controllerType -> {
            try {
                return controllerType.getDeclaredConstructor(SceneManager.class).newInstance(this);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Could not create view controller.", exception);
            }
        });

        Parent root = loader.load();
        if (scene == null) {
            scene = new Scene(root, 1100, 760);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/org/catalyst/view/theme.css")).toExternalForm());
            stage.setTitle("Catalyst | Community support");
            stage.setMinWidth(700);
            stage.setMinHeight(560);
            stage.setScene(scene);
            stage.show();
            stage.setMaximized(true);
        } else {
            scene.setRoot(root);
        }
    }
}
