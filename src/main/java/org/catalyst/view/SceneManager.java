package org.catalyst.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.catalyst.model.Beneficiary;
import org.catalyst.model.User;
import org.catalyst.model.UserType;
import org.catalyst.model.Volunteer;
import org.catalyst.repo.UserRepository;
import org.catalyst.repo.VolunteerRepository;
import org.catalyst.service.AssignmentService;
import org.catalyst.service.AuthService;
import org.catalyst.service.BeneficiaryService;
import org.catalyst.service.PasswordHasher;
import org.catalyst.service.VolunteerService;
import org.catalyst.service.VolunteerNotificationObserver;

import java.io.IOException;
import java.util.Objects;

/** Loads the application's FXML views and shares the current login session. */
public final class SceneManager {
    private final Stage stage;
    private Scene scene;
    private final AuthService authService = new AuthService(new UserRepository(), new PasswordHasher());
    private final BeneficiaryService beneficiaryService = new BeneficiaryService();
    private final VolunteerRepository volunteerRepository = new VolunteerRepository();
    private final VolunteerService volunteerService = new VolunteerService(volunteerRepository);
    private final AssignmentService assignmentService = new AssignmentService(volunteerRepository);
    private User currentUser;
    private Beneficiary selectedBeneficiary;
    private boolean beneficiaryFormReturnsToDetails;

    public SceneManager(Stage stage) {
        this.stage = stage;
        assignmentService.addObserver(new VolunteerNotificationObserver(volunteerRepository));
    }

    public AuthService getAuthService() {
        return authService;
    }

    public BeneficiaryService getBeneficiaryService() {
        return beneficiaryService;
    }

    public boolean canManageBeneficiaries() {
        return currentUser != null
                && currentUser.getId() > 0
                && currentUser.getUserType()
                        == UserType.BENEFICIARY_OFFICER;
    }

    public Stage getStage() {
        return stage;
    }

    public VolunteerService getVolunteerService() {
        return volunteerService;
    }

    public AssignmentService getAssignmentService() {
        return assignmentService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        this.selectedBeneficiary = null;
        this.beneficiaryFormReturnsToDetails = false;
    }

    public Beneficiary getSelectedBeneficiary() {
        return selectedBeneficiary;
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

    public void showBeneficiaryList() throws IOException {
        requireBeneficiaryOfficer();
        selectedBeneficiary = null;
        beneficiaryFormReturnsToDetails = false;
        show("beneficiary-list.fxml");
    }

    public void showCreateBeneficiary() throws IOException {
        requireBeneficiaryOfficer();
        selectedBeneficiary = null;
        beneficiaryFormReturnsToDetails = false;
        show("beneficiary-form.fxml");
    }

    public void showEditBeneficiary(Beneficiary beneficiary) throws IOException {
        showEditBeneficiary(beneficiary, false);
    }

    public void showEditBeneficiaryFromDetails(Beneficiary beneficiary) throws IOException {
        showEditBeneficiary(beneficiary, true);
    }

    private void showEditBeneficiary(Beneficiary beneficiary, boolean returnToDetails) throws IOException {
        requireBeneficiaryOfficer();
        requireSavedBeneficiary(beneficiary);
        selectedBeneficiary = beneficiary;
        beneficiaryFormReturnsToDetails = returnToDetails;
        show("beneficiary-form.fxml");
    }

    public void returnFromBeneficiaryForm(Beneficiary beneficiary) throws IOException {
        if (beneficiaryFormReturnsToDetails && beneficiary != null) {
            showBeneficiaryDetails(beneficiary);
        } else {
            showBeneficiaryList();
        }
    }

    public void showBeneficiaryDetails(Beneficiary beneficiary) throws IOException {
        requireBeneficiaryOfficer();
        requireSavedBeneficiary(beneficiary);
        selectedBeneficiary = beneficiary;
        beneficiaryFormReturnsToDetails = false;
        show("beneficiary-details.fxml");
    }

    public void showRecordAssistance(Beneficiary beneficiary) throws IOException {
        requireBeneficiaryOfficer();
        requireSavedBeneficiary(beneficiary);
        selectedBeneficiary = beneficiary;
        beneficiaryFormReturnsToDetails = false;
        show("record-assistance.fxml");
    }

    private void requireSavedBeneficiary(Beneficiary beneficiary) {
        if (beneficiary == null || beneficiary.getBeneficiaryID() <= 0) {
            throw new IllegalArgumentException("Select a saved beneficiary.");
        }
    }

    private void requireBeneficiaryOfficer() {
        if (!canManageBeneficiaries()) {
            throw new SecurityException("Sign in with a beneficiary officer account.");
        }
    }

    public void showVolunteerDashboard() throws IOException {
        show("volunteer.fxml");
    }

    public void showCoordinatorDashboard() throws IOException {
        show("coordinator.fxml");
    }

    private void show(String fileName) throws IOException {
        var viewResource = getClass().getResource("/org/catalyst/view/" + fileName);
        if (viewResource == null) {
            throw new IOException("View resource not found: " + fileName);
        }
        FXMLLoader loader = new FXMLLoader(viewResource);
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
