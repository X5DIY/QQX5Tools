package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

public class Exit implements Initializable {

    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication application) {
        this.app = application;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }


    @FXML
    Button back;

    @FXML
    public void back() {
        //ExitStage.close();
    }

    @FXML
    Button exit;

    @FXML
    public void exit() {
       // ExitStage.close();
       // PrimaryStage.close();
        System.exit(0);
    }

}
