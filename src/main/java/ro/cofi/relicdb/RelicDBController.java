package ro.cofi.relicdb;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RelicDBController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}