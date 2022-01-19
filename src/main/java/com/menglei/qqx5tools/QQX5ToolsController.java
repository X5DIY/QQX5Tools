package com.menglei.qqx5tools;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class QQX5ToolsController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}