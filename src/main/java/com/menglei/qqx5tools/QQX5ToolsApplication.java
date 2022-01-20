package com.menglei.qqx5tools;

import com.menglei.qqx5tools.controller.AdjustBpm1Controller;
import com.menglei.qqx5tools.controller.AdjustBpm2Controller;
import com.menglei.qqx5tools.controller.BytesToXml1Controller;
import com.menglei.qqx5tools.controller.BytesToXml2Controller;
import com.menglei.qqx5tools.controller.CalculateBurstPoints1Controller;
import com.menglei.qqx5tools.controller.CalculateBurstPoints2Controller;
import com.menglei.qqx5tools.controller.DownloadNotRankSongsController;
import com.menglei.qqx5tools.controller.HelpController;
import com.menglei.qqx5tools.controller.QQX5ToolsController;
import com.menglei.qqx5tools.controller.UploadXml1Controller;
import com.menglei.qqx5tools.controller.UploadXml2Controller;
import com.menglei.qqx5tools.model.AdjustBpm;
import com.menglei.qqx5tools.model.BytesToXml;
import com.menglei.qqx5tools.model.CalculateBurstPoints;
import com.menglei.qqx5tools.model.DownloadNotRankSongs;
import com.menglei.qqx5tools.model.UploadXml;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QQX5ToolsApplication extends Application {
    public static void main(String[] args) {
        launch();
    }

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

    private Initializable replaceSceneContent(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        loader.setLocation(getClass().getResource(fxml));
        stage.setScene(new Scene(loader.load(getClass().getResourceAsStream(fxml))));
        stage.sizeToScene();
        return loader.getController();
    }

    public void gotoQQX5Tools() {
        try {
            stage.setTitle("QQ炫舞手游工具箱");
            QQX5ToolsController c = (QQX5ToolsController) replaceSceneContent("view/qqx5Tools.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoCalculateBurstPoints1() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 计算爆点");
            CalculateBurstPoints1Controller c = (CalculateBurstPoints1Controller) replaceSceneContent("view/calculateBurstPoint1.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoCalculateBurstPoints2(File[] initialFiles, int outMode, int fireMaxNum, int maxDiff) {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 计算爆点");
            CalculateBurstPoints2Controller c = (CalculateBurstPoints2Controller) replaceSceneContent("view/calculateBurstPoint2.fxml");
            c.setApp(this);
            new CalculateBurstPoints(c, initialFiles, outMode, fireMaxNum, maxDiff).start();
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoAdjustBpm1() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 调整谱面bpm");
            AdjustBpm1Controller c = (AdjustBpm1Controller) replaceSceneContent("view/adjustBpm1.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    //todo: 倍数要使用枚举

    public void gotoAdjustBpm2(ArrayList<File> fileList, double beishu) {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 调整谱面bpm");
            AdjustBpm2Controller c = (AdjustBpm2Controller) replaceSceneContent("view/adjustBpm2.fxml");
            c.setApp(this);
            new AdjustBpm(c, fileList, beishu).start();
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoUploadXml1() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 上传自制谱");
            UploadXml1Controller c = (UploadXml1Controller) replaceSceneContent("view/uploadXml1.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoUploadXml2(File m4aFile, File xmlFile) {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 上传自制谱");
            UploadXml2Controller c = (UploadXml2Controller) replaceSceneContent("view/uploadXml2.fxml");
            c.setApp(this);
            new UploadXml(c, m4aFile, xmlFile).start();
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoBytesToXml1() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - bytes转xml");
            BytesToXml1Controller c = (BytesToXml1Controller) replaceSceneContent("view/bytesToXml1.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoBytesToXml2(File rootPath) {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - bytes转xml");
            BytesToXml2Controller c = (BytesToXml2Controller) replaceSceneContent("view/bytesToXml2.fxml");
            c.setApp(this);
            new BytesToXml(c, rootPath).process();
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoDownloadNotRankSongs() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 下载非排位歌曲");
            DownloadNotRankSongsController c = (DownloadNotRankSongsController) replaceSceneContent("view/downloadNotRankSongs.fxml");
            c.setApp(this);
            new DownloadNotRankSongs(c).process();
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }

    public void gotoHelp() {
        try {
            stage.setTitle("QQ炫舞手游工具箱 - 帮助");
            HelpController c = (HelpController) replaceSceneContent("view/help.fxml");
            c.setApp(this);
        } catch (IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);
        }
    }
}