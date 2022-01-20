package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class CalculateBurstPoints2Controller implements Initializable {
    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication app) {
        this.app = app;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //do nothing. this method is only used for stage change.
    }

    @FXML
    public ProgressBar progressBar;

    @FXML
    public Text text;

    @FXML
    public TextArea textArea;

    @FXML
    public Button finish;

    @FXML
    public void finish() {
        app.gotoQQX5Tools();
    }

}
