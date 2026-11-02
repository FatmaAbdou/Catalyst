package org.catalyst;

import javafx.application.Application;
import javafx.stage.Stage;
import org.catalyst.repo.DatabaseConnection;
import org.catalyst.view.SceneManager;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Starts the JavaFX application and opens the welcome page. */
public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage stage) throws Exception {
        new SceneManager(stage).showWelcome();
    }

    @Override
    public void stop() {
        try {
            DatabaseConnection.getInstance().close();
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not close the database connection.", exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
