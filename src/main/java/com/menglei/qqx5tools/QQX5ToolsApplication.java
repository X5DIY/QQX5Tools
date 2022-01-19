package com.menglei.qqx5tools;

import com.menglei.qqx5tools.controller.BytesToXml1Controller;
import com.menglei.qqx5tools.controller.CalculateBurstPoints1Controller;
import com.menglei.qqx5tools.controller.CalculateBurstPoints2;
import com.menglei.qqx5tools.controller.QQX5ToolsController;
import com.menglei.qqx5tools.model.CalculateBurstPoints2Controller;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QQX5ToolsApplication extends Application {
    private Stage stage;

    @Override
    public void start(Stage stage) {
        // 参数转存，便于方法外使用
        this.stage = stage;
        this.stage.setWidth(1000);
        this.stage.setHeight(618);
        // 固定窗口大小
        this.stage.setResizable(false);
        gotoQQX5Tools();
        this.stage.show();
    }

    public void gotoQQX5Tools() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - By 萌泪");
            QQX5ToolsController c = (QQX5ToolsController) replaceSceneContent("view/qqx5Tools.fxml");
            c.setApp(this);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoCalculateBurstPoints1() {
        try {
            stage.setTitle("萌泪爆气表 - 爆点计算");
            CalculateBurstPoints1Controller c = (CalculateBurstPoints1Controller) replaceSceneContent("view/calculateBurstPoint1.fxml");
            c.setApp(this);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoCalculateBurstPoints2(File[] initialFiles, int outMode, int fireMaxNum, int maxDiff) {
        try {
            CalculateBurstPoints2 c = (CalculateBurstPoints2) replaceSceneContent("view/calculateBurstPoint2.fxml");
            c.setApp(this);
            new CalculateBurstPoints2Controller(c, initialFiles, outMode, fireMaxNum, maxDiff).start();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoX5Apk1() {

    }

    public void gotoBytesToXml1() {
        try {
            stage.setTitle("萌泪爆气表 - 文件转化");
            BytesToXml1Controller c = (BytesToXml1Controller) replaceSceneContent("view/bytesToXml1.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    private Initializable replaceSceneContent(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        loader.setLocation(getClass().getResource(fxml));
        stage.setScene(new Scene(loader.load(getClass().getResourceAsStream(fxml))));
        stage.sizeToScene();
        return loader.getController();
    }

    public static void main(String[] args) {
        launch();
    }
}