package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class CalculateBurstPoints1Controller implements Initializable {
    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication app) {
        this.app = app;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        rbtn_simpleMode.setToggleGroup(outputModeGroup);
        rbtn_fullMode.setToggleGroup(outputModeGroup);
        outputModeGroup.selectedToggleProperty().addListener((observableValue, oldToggle, newToggle) -> {
            RadioButton rbtn = (RadioButton) newToggle;
            boolean visible = rbtn == rbtn_fullMode;
            text2.setVisible(visible);
            rbtn_burstPointsNum5.setVisible(visible);
            rbtn_burstPointsNum10.setVisible(visible);
            rbtn_burstPointsNum50.setVisible(visible);
            rbtn_burstPointsNumDiy.setVisible(visible);
            textfield_burstPointsNum.setVisible(visible);
            text3.setVisible(visible);
            rbtn_maxScoreDifference0.setVisible(visible);
            rbtn_maxScoreDifference1000.setVisible(visible);
            rbtn_maxScoreDifference5000.setVisible(visible);
            rbtn_maxScoreDifferenceDiy.setVisible(visible);
            textfield_maxScoreDifference.setVisible(visible);
        });
        rbtn_burstPointsNum5.setToggleGroup(burstPointsNumGroup);
        rbtn_burstPointsNum10.setToggleGroup(burstPointsNumGroup);
        rbtn_burstPointsNum50.setToggleGroup(burstPointsNumGroup);
        rbtn_burstPointsNumDiy.setToggleGroup(burstPointsNumGroup);
        rbtn_maxScoreDifference0.setToggleGroup(maxScoreDifferenceGroup);
        rbtn_maxScoreDifference1000.setToggleGroup(maxScoreDifferenceGroup);
        rbtn_maxScoreDifference5000.setToggleGroup(maxScoreDifferenceGroup);
        rbtn_maxScoreDifferenceDiy.setToggleGroup(maxScoreDifferenceGroup);
        // 修改初始情况
        outputModeGroup.selectToggle(rbtn_simpleMode);
        burstPointsNumGroup.selectToggle(rbtn_burstPointsNum10);
        maxScoreDifferenceGroup.selectToggle(rbtn_maxScoreDifference0);
    }

    @FXML
    public Text text1;

    @FXML
    public Text text2;

    @FXML
    public Text text3;

    @FXML
    public Text text4;

    ToggleGroup outputModeGroup = new ToggleGroup();

    @FXML
    public RadioButton rbtn_simpleMode;

    @FXML
    public RadioButton rbtn_fullMode;

    ToggleGroup burstPointsNumGroup = new ToggleGroup();

    @FXML
    public RadioButton rbtn_burstPointsNum5;

    @FXML
    public RadioButton rbtn_burstPointsNum10;

    @FXML
    public RadioButton rbtn_burstPointsNum50;

    @FXML
    public RadioButton rbtn_burstPointsNumDiy;

    @FXML
    public TextField textfield_burstPointsNum;

    public void textfield_burstPointsNum_click() {
        burstPointsNumGroup.selectToggle(rbtn_burstPointsNumDiy);
    }

    ToggleGroup maxScoreDifferenceGroup = new ToggleGroup();

    @FXML
    public RadioButton rbtn_maxScoreDifference0;

    @FXML
    public RadioButton rbtn_maxScoreDifference1000;

    @FXML
    public RadioButton rbtn_maxScoreDifference5000;

    @FXML
    public RadioButton rbtn_maxScoreDifferenceDiy;

    @FXML
    public TextField textfield_maxScoreDifference;

    public void textfield_maxScoreDifference_click() {
        maxScoreDifferenceGroup.selectToggle(rbtn_maxScoreDifferenceDiy);
    }

    @FXML
    public Text text_filePath;

    private ArrayList<File> fileList = null;

    public void selectFile() {
        FileChooser xmlChooser = new FileChooser();
        xmlChooser.setTitle("请选择一个或多个炫舞xml谱面文件");
        File dir = new File("x5Files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        xmlChooser.setInitialDirectory(dir);
        xmlChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("炫舞谱面文件", "*.xml")
                // 这里可以仿照上一行，加入更多格式
        );
        fileList = (ArrayList<File>) xmlChooser.showOpenMultipleDialog(new Stage());
        if (fileList != null) {
            if (fileList.size() >= 1) {
                text_filePath.setText(fileList.get(0).getName() + "等" + fileList.size() + "个文件");
            } else {
                fileList = null;
            }
        }
    }

    public void selectDir() {
        DirectoryChooser xmlChooser = new DirectoryChooser();
        xmlChooser.setTitle("请选择炫舞xml谱面文件所在文件夹");
        File dir = new File("x5Files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        xmlChooser.setInitialDirectory(dir);
        File selectedDir = xmlChooser.showDialog(new Stage());
        if (selectedDir != null) {
            //text_filePath.setText(fileList.get(0).getName() + "等" + fileList.size() + "个文件");
        }
    }

    @FXML
    Button back;

    @FXML
    public void back() {
        app.gotoQQX5Tools();
    }

    @FXML
    Button next;

    @FXML
    public void next() {
        if (fileList == null || fileList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("弹窗标题");
            alert.setContentText("请选择文件！");
            alert.setHeaderText("弹窗信息");
            alert.showAndWait();
            return;
        }
        int outputMode = rbtn_simpleMode.isSelected() ? 1 : 2;
        int fireMaxNum;
        RadioButton rbtn = (RadioButton) burstPointsNumGroup.getSelectedToggle();
        if (rbtn == rbtn_burstPointsNum5) {
            fireMaxNum = 5;
        } else if (rbtn == rbtn_burstPointsNum10) {
            fireMaxNum = 10;
        } else {
            fireMaxNum = 50;
        }
        int maxDiff;
        if (rbtn_maxScoreDifference0.isSelected()) {
            maxDiff = 0;
        } else {
            maxDiff = rbtn_maxScoreDifference1000.isSelected() ? 1000 : 5000;
        }
        app.gotoCalculateBurstPoints2(fileList, outputMode, fireMaxNum, maxDiff);
    }
}
