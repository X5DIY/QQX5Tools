package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CalculateBurstPoints1Controller implements Initializable {

    private QQX5ToolsApplication app;

    public void setApp(QQX5ToolsApplication application) {
        this.app = application;
    }

    @FXML
    RadioButton outMode1;

    @FXML
    RadioButton outMode2;

    @FXML
    public void selectOutputMode1() {
        outMode1.setSelected(true);
        outMode2.setSelected(false);
        scoreDiff2.setVisible(true);
        scoreDiff3.setVisible(true);
    }

    @FXML
    public void selectOutputMode2() {
        outMode1.setSelected(false);
        outMode2.setSelected(true);
        scoreDiff1.setSelected(true);
        scoreDiff2.setSelected(false);
        scoreDiff3.setSelected(false);
        scoreDiff2.setVisible(false);
        scoreDiff3.setVisible(false);
    }

    @FXML
    RadioButton fireNum1;

    @FXML
    RadioButton fireNum2;

    @FXML
    RadioButton fireNum3;

    @FXML
    public void selectFireNum1() {
        fireNum1.setSelected(true);
        fireNum2.setSelected(false);
        fireNum3.setSelected(false);
    }

    @FXML
    public void selectFireNum2() {
        fireNum1.setSelected(false);
        fireNum2.setSelected(true);
        fireNum3.setSelected(false);
    }

    @FXML
    public void selectFireNum3() {
        fireNum1.setSelected(false);
        fireNum2.setSelected(false);
        fireNum3.setSelected(true);
    }

    @FXML
    RadioButton scoreDiff1;

    @FXML
    RadioButton scoreDiff2;

    @FXML
    RadioButton scoreDiff3;

    @FXML
    public void selectScoreDiff1() {
        scoreDiff1.setSelected(true);
        scoreDiff2.setSelected(false);
        scoreDiff3.setSelected(false);
    }

    @FXML
    public void selectScoreDiff2() {
        if (outMode1.isSelected()) {
            scoreDiff1.setSelected(false);
            scoreDiff2.setSelected(true);
            scoreDiff3.setSelected(false);
        } else {
            scoreDiff1.setSelected(true);
            scoreDiff2.setSelected(false);
            scoreDiff3.setSelected(false);
        }
    }

    @FXML
    public void selectScoreDiff3() {
        if (outMode1.isSelected()) {
            scoreDiff1.setSelected(false);
            scoreDiff2.setSelected(false);
            scoreDiff3.setSelected(true);
        } else {
            scoreDiff1.setSelected(true);
            scoreDiff2.setSelected(false);
            scoreDiff3.setSelected(false);
        }
    }

    @FXML
    Text filePath;

    private File[] initialFiles;// 初始文件合集，可能为多个文件，或一个目录。会传入 gotoCalcu2
    private int fileNum;// initialFiles 为多个文件时，该数值为文件个数。不会传入 gotoCalcu2()

    public void selectFile() {
        FileChooser xmlChooser = new FileChooser();
        xmlChooser.setTitle("请选择一个或多个炫舞xml谱面文件");
        File dir = new File("x5Files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        xmlChooser.setInitialDirectory(dir);
        xmlChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("炫舞xml谱面文件", "*.xml")
                // 这里可以仿照上一行，加入更多格式
        );
        List<File> fileList = xmlChooser.showOpenMultipleDialog(new Stage());
        if (fileList != null) {
            // 先将所有文件放到一个大的 File[] 中
            initialFiles = new File[5000];
            fileNum = 0;
            fileList.forEach((file) -> {
                initialFiles[fileNum] = file;
                fileNum++;
            });
            // 再调整至合适大小
            File[] files = new File[fileNum];
            System.arraycopy(initialFiles, 0, files, 0, fileNum);
            initialFiles = files;
            try {
                if (fileNum == 1) {
                    filePath.setText(fileList.get(0).getCanonicalPath());
                } else {
                    filePath.setText(fileList.get(0).getCanonicalPath() + ",...");
                }
            } catch (IOException e) {
                e.printStackTrace();
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
        File file = xmlChooser.showDialog(new Stage());
        if (file != null) {
            try {
                initialFiles = new File[1];
                initialFiles[0] = file;
                filePath.setText(file.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        if (!filePath.getText().equals("未选择")) {
            int outMode;
            int fireMaxNum;
            int maxDiff;
            outMode = outMode1.isSelected() ? 1 : 2;
            if (fireNum1.isSelected()) {
                fireMaxNum = 5;
            } else {
                fireMaxNum = fireNum2.isSelected() ? 10 : 50;
            }
            if (scoreDiff1.isSelected()) {
                maxDiff = 0;
            } else {
                maxDiff = scoreDiff2.isSelected() ? 1000 : 5000;
            }
            app.gotoCalculateBurstPoints2(initialFiles, outMode, fireMaxNum, maxDiff);
        } else {
            // todo:弹出error，提示选择文件
            System.out.println("请选择文件！");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }
}
