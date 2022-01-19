package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.File;
import java.io.IOException;

public class QQX5ToolsController {
    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication application) {
        this.app = application;
    }

    @FXML
    Button calcu;

    @FXML
    private void gotoCalcu1() {
        app.gotoCalculateBurstPoints1();
    }

    @FXML
    Button x5apk;

    @FXML
    public void gotoX5Apk1() {
        app.gotoX5Apk1();
    }

    @FXML
    Button bytes;

    @FXML
    public void gotoBytes1() {
        app.gotoBytesToXml1();
    }

    @FXML
    Button scan;

    @FXML
    public void showScan() {
        File dir = new File("burstPointsInfo");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            Runtime.getRuntime().exec("explorer \"" + dir.getCanonicalPath() + "\"");
        } catch (IOException e) {
            e.printStackTrace();
        }

        //EventQueue.invokeLater(() -> {
        //    try {
        //        desktop.open(dir);
        //    } catch (IOException ex) {
        //        ex.printStackTrace();
        //    }
        //});
    }

    @FXML
    Button info;

    @FXML
    public void info() {
        // todo:将原窗口变为不可操作
        // InfoStage.showAndWait();
    }

    @FXML
    Button exit;

    @FXML
    public void exit() {
        // todo:将原窗口变为不可操作
        // ExitStage.showAndWait();
    }

}