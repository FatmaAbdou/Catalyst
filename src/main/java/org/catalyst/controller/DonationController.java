package org.catalyst.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.catalyst.model.donation.Donation;
import org.catalyst.model.donation.DonationType;
import org.catalyst.model.payment.PaymentMethod;
import org.catalyst.model.receipt.Receipt;
import org.catalyst.model.user.User;
import org.catalyst.service.DonationService;
import org.catalyst.view.SceneManager;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reads the donation form, passes it to {@link DonationService}, and shows the receipt. */
public final class DonationController {
    private static final Logger LOGGER = Logger.getLogger(DonationController.class.getName());

    private final SceneManager sceneManager;
    private final DonationService donationService;
    private final User user;

    @FXML private VBox donorEmailSection;
    @FXML private TextField donorEmailField;
    @FXML private ComboBox<DonationType> typeBox;
    @FXML private VBox monetarySection;
    @FXML private TextField amountField;
    @FXML private ComboBox<PaymentMethod> methodBox;
    @FXML private VBox paymentDetailSection;
    @FXML private TextField paymentDetailField;
    @FXML private VBox inKindSection;
    @FXML private TextField itemField;
    @FXML private TextField quantityField;
    @FXML private TextField unitField;
    @FXML private TextField conditionField;
    @FXML private Label messageLabel;
    @FXML private Button submitButton;
    @FXML private TextArea receiptArea;

    public DonationController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
        this.donationService = sceneManager.getDonationService();
        this.user = sceneManager.getCurrentUser();
    }

    @FXML
    private void initialize() {
        receiptArea.visibleProperty().bind(receiptArea.textProperty().isNotEmpty());
        receiptArea.managedProperty().bind(receiptArea.visibleProperty());
        amountField.setPromptText("Amount (" + Donation.CURRENCY + ")");

        if (!donationService.canDonate(user)) {
            setShown(donorEmailSection, false);
            setShown(monetarySection, false);
            setShown(inKindSection, false);
            typeBox.setDisable(true);
            submitButton.setDisable(true);
            setMessage("Log in as a donor or a donation officer to record a donation.", true);
            return;
        }

        setShown(donorEmailSection, donationService.canRecordForOthers(user));
        typeBox.getItems().setAll(donationService.availableTypes(user));
        typeBox.setValue(typeBox.getItems().get(0));
        setShown(typeBox, typeBox.getItems().size() > 1);
        methodBox.getItems().setAll(donationService.availableMethods(user));
        methodBox.setValue(methodBox.getItems().get(0));

        typeBox.valueProperty().addListener((observable, oldValue, newValue) -> updateSections());
        methodBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            paymentDetailField.clear();
            updateSections();
        });
        updateSections();
    }

    @FXML
    private void submitDonation() {
        setMessage("", false);
        receiptArea.clear();
        try {
            Receipt receipt = typeBox.getValue() == DonationType.MONETARY ? donateMoney() : donateItems();
            receiptArea.setText(receipt.PrintReceipt());
            setMessage("Donation recorded. Reference " + receipt.getReference() + ".", false);
            clearForm();
        } catch (IllegalArgumentException exception) {
            setMessage(exception.getMessage(), true);
        } catch (SQLException exception) {
            LOGGER.log(Level.SEVERE, "Could not save the donation.", exception);
            setMessage("We could not save this donation right now. Please try again.", true);
        }
    }

    @FXML
    private void openWelcome() {
        try {
            sceneManager.showWelcome();
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Could not open the welcome page.", exception);
            setMessage("The welcome page could not be opened.", true);
        }
    }

    private Receipt donateMoney() throws SQLException {
        BigDecimal amount = parseDecimal(amountField.getText(), "amount");
        if (amount == null) {
            throw new IllegalArgumentException("Enter the amount.");
        }
        return donationService.donateMoney(
                user,
                donorEmailField.getText(),
                amount,
                methodBox.getValue(),
                paymentDetailField.getText());
    }

    private Receipt donateItems() throws SQLException {
        return donationService.donateItems(
                user,
                donorEmailField.getText(),
                itemField.getText(),
                parseDecimal(quantityField.getText(), "quantity"),
                unitField.getText(),
                conditionField.getText());
    }

    private void updateSections() {
        boolean money = typeBox.getValue() == DonationType.MONETARY;
        PaymentMethod method = methodBox.getValue();
        setShown(monetarySection, money);
        setShown(inKindSection, !money);
        setShown(paymentDetailSection, money && method != null && method != PaymentMethod.CASH);
        if (method != null) {
            paymentDetailField.setPromptText(switch (method) {
                case CASH -> "";
                case CHECK -> "Check number";
                case CARD -> "Card number";
                case INSTAPAY -> "InstaPay phone number";
            });
        }
    }

    /** Returns {@code null} for a blank field so the model can report missing values in form order. */
    private BigDecimal parseDecimal(String text, String label) {
        String clean = text == null ? "" : text.trim().replace(",", "");
        if (clean.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Enter a valid " + label + ".", exception);
        }
    }

    private void clearForm() {
        donorEmailField.clear();
        amountField.clear();
        paymentDetailField.clear();
        itemField.clear();
        quantityField.clear();
        unitField.clear();
        conditionField.clear();
    }

    private void setMessage(String message, boolean error) {
        messageLabel.getStyleClass().removeAll("error-message", "success-message");
        if (!message.isEmpty()) {
            messageLabel.getStyleClass().add(error ? "error-message" : "success-message");
        }
        messageLabel.setText(message);
    }

    private void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}