package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class QQX5ToolsController implements Initializable {
    @FXML
    public BorderPane root;

    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication app) {
        this.app = app;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //do nothing. this method is only used for stage change.
    }

    @FXML
    Button btn_calculateBurstPoint;

    @FXML
    private void gotoCalculateBurstPoints1() {
        app.gotoCalculateBurstPoints1();
    }

    @FXML
    Button btn_openBurstPointsInfoDir;

    @FXML
    public void openBurstPointsInfoDir() {
        File dir = new File("burstPointsInfo");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            Runtime.getRuntime().exec("explorer \"" + dir.getCanonicalPath() + "\"");
        } catch (IOException e) {
            logError(e);
        }
    }

    @FXML
    Button btn_adjustBpm1;

    @FXML
    public void gotoAdjustBpm1() {
        app.gotoAdjustBpm1();
    }

    @FXML
    Button btn_uploadXml;

    @FXML
    public void gotoUploadXml1() {
        app.gotoUploadXml1();
    }

    @FXML
    Button btn_bytesToXml1;

    @FXML
    public void gotoBytesToXml1() {
        app.gotoBytesToXml1();
    }

    @FXML
    Button btn_downloadNotRankSongs;

    @FXML
    public void gotoDownloadNotRankSongs() {
        app.gotoDownloadNotRankSongs();
    }

    @FXML
    Button btn_help;

    @FXML
    public void gotoHelp() {
        app.gotoHelp();
    }

    @FXML
    Button btn_exit;

    @FXML
    public void exit() {
        System.exit(0);
    }
}