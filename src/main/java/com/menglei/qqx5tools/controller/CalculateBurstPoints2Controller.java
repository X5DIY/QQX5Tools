package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

import static com.menglei.qqx5tools.SettingsAndUtils.df;

public class CalculateBurstPoints2Controller implements Initializable {
    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication app) {
        this.app = app;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setProgress(0);
        btn_finish.setText("计算中");
        btn_finish.setDisable(true);
    }

    public void finished() {
        setProgress(100);
        // 从非主线程刷新UI时，必须使用 Platform.runLater
        Platform.runLater(() -> {
            btn_finish.setText("完成");
            btn_finish.setDisable(false);
        });
    }

    @FXML
    public ProgressBar progressBar;

    @FXML
    public Text text1;

    public void setProgress(double v) {
        Platform.runLater(() -> {
            progressBar.setProgress(v);
            text1.setText(df.format(v));
        });
    }

    @FXML
    private TextArea textArea_outputInfo;

    private boolean outputIsEmpty = true;

    public void appendLine(String line) {
        Platform.runLater(() -> {
            if (outputIsEmpty) {
                textArea_outputInfo.appendText(line);
                outputIsEmpty = false;
            } else {
                textArea_outputInfo.appendText("\n" + line);
            }
        });
    }

    @FXML
    public Button btn_finish;

    @FXML
    public void btn_finish_click() {
        app.gotoQQX5Tools();
    }
}
