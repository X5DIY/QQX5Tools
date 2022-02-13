package com.menglei.qqx5tools.controller;

import com.menglei.qqx5tools.QQX5ToolsApplication;
import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;
import com.menglei.qqx5tools.SettingsAndUtils.MyThreadFactory;
import com.menglei.qqx5tools.SettingsAndUtils.OutputMode;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.menglei.qqx5tools.SettingsAndUtils.THREAD_NUM;
import static com.menglei.qqx5tools.SettingsAndUtils.df;
import static com.menglei.qqx5tools.SettingsAndUtils.getQQX5MapType;
import static com.menglei.qqx5tools.SettingsAndUtils.logError;
import static com.menglei.qqx5tools.SettingsAndUtils.logInfo;
import static com.menglei.qqx5tools.SettingsAndUtils.nanoTime;
import static com.menglei.qqx5tools.SettingsAndUtils.showWarn;
import static java.lang.Thread.sleep;

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
            rbtn_maxBurstPointsNum5.setVisible(visible);
            rbtn_maxBurstPointsNum10.setVisible(visible);
            rbtn_maxBurstPointsNum50.setVisible(visible);
            rbtn_maxBurstPointsNumDiy.setVisible(visible);
            textfield_maxBurstPointsNum.setVisible(visible);
            text3.setVisible(visible);
            rbtn_maxScoreDifference0.setVisible(visible);
            rbtn_maxScoreDifference1000.setVisible(visible);
            rbtn_maxScoreDifference5000.setVisible(visible);
            rbtn_maxScoreDifferenceDiy.setVisible(visible);
            textfield_maxScoreDifference.setVisible(visible);
        });
        rbtn_maxBurstPointsNum5.setToggleGroup(burstPointsNumGroup);
        rbtn_maxBurstPointsNum10.setToggleGroup(burstPointsNumGroup);
        rbtn_maxBurstPointsNum50.setToggleGroup(burstPointsNumGroup);
        rbtn_maxBurstPointsNumDiy.setToggleGroup(burstPointsNumGroup);
        rbtn_maxScoreDifference0.setToggleGroup(maxScoreDifferenceGroup);
        rbtn_maxScoreDifference1000.setToggleGroup(maxScoreDifferenceGroup);
        rbtn_maxScoreDifference5000.setToggleGroup(maxScoreDifferenceGroup);
        rbtn_maxScoreDifferenceDiy.setToggleGroup(maxScoreDifferenceGroup);
        // 修改初始情况
        outputModeGroup.selectToggle(rbtn_simpleMode);
        burstPointsNumGroup.selectToggle(rbtn_maxBurstPointsNum10);
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
    public RadioButton rbtn_maxBurstPointsNum5;

    @FXML
    public RadioButton rbtn_maxBurstPointsNum10;

    @FXML
    public RadioButton rbtn_maxBurstPointsNum50;

    @FXML
    public RadioButton rbtn_maxBurstPointsNumDiy;

    @FXML
    public TextField textfield_maxBurstPointsNum;

    public void textfield_burstPointsNum_click() {
        burstPointsNumGroup.selectToggle(rbtn_maxBurstPointsNumDiy);
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
    public Text text_xmlFilesInfo;

    private void freshFileInfo() {
        if (xmlMap.isEmpty()) {
            text_xmlFilesInfo.setText("未选择任何谱面文件");
        } else {
            text_xmlFilesInfo.setText("已选择" + xmlMap.size() + "个谱面文件");
        }
    }

    private final ConcurrentHashMap<File, QQX5MapType> xmlMap = new ConcurrentHashMap<>();

    @FXML
    public Button btn_addXmlFiles;

    public void btn_addXmlFiles_click() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("请选择一个或多个炫舞谱面文件");
        File dir = new File("x5Files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        fileChooser.setInitialDirectory(dir);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("炫舞谱面文件", "*.xml")
        );
        List<File> list = fileChooser.showOpenMultipleDialog(new Stage());
        if (list != null && !list.isEmpty()) {
            new Thread(() -> checkThenPutInMultiThreads(list)).start();
        }
    }

    @FXML
    public Button btn_addXmlDir;

    public void btn_addXmlDir_click() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("请选择炫舞谱面文件所在文件夹");
        File initDir = new File("x5Files");
        if (!initDir.exists()) {
            initDir.mkdirs();
        }
        dirChooser.setInitialDirectory(initDir);
        File dir = dirChooser.showDialog(new Stage());
        if (dir != null) {
            ArrayList<File> list = new ArrayList<>();
            getXmlList(dir, list);
            if (!list.isEmpty()) {
                new Thread(() -> checkThenPutInMultiThreads(list)).start();
            }
        }
    }

    public void getXmlList(File f, ArrayList<File> list) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (var file : files) {
                    getXmlList(file, list);
                }
            }
            return;
        }
        if (f.getName().endsWith(".xml")/* && !xmlMap.containsKey(f)*/) {
            list.add(f);
        }
    }

    /**
     * 多线程检查list的文件，并将符合要求的文件添加到xmlMap中.
     * <p>
     * 文件为炫舞谱面文件，且当前不在xmlMap中，才可添加。
     *
     * @param list 要检查的文件列表
     */
    private void checkThenPutInMultiThreads(List<File> list) {
        btn_addXmlFiles.setDisable(true);
        btn_addXmlDir.setDisable(true);
        btn_clearXmlList.setDisable(true);
        btn_next.setDisable(true);
        long time = System.nanoTime();
        ExecutorService pool = new ThreadPoolExecutor(
                THREAD_NUM, THREAD_NUM,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(THREAD_NUM),
                new MyThreadFactory());
        AddXmlFilesThread.list = list;
        AddXmlFilesThread.processedNum = 0;
        for (int i = 0; i < THREAD_NUM; i++) {
            pool.execute(new AddXmlFilesThread(i));
        }
        pool.shutdown();
        try {
            while (!pool.isTerminated()) {
                logInfo("已处理 " + df.format((double) AddXmlFilesThread.processedNum / list.size()));
                freshFileInfo();
                sleep(300);
            }
        } catch (InterruptedException e) {
            logError(e);
            Thread.currentThread().interrupt();
        }
        logInfo("处理完毕，共用时 " + nanoTime(time));
        freshFileInfo();
        btn_addXmlFiles.setDisable(false);
        btn_addXmlDir.setDisable(false);
        btn_clearXmlList.setDisable(false);
        btn_next.setDisable(false);
    }

    class AddXmlFilesThread implements Runnable {
        private final int threadNo;
        static List<File> list;
        static int processedNum;

        private AddXmlFilesThread(int threadNo) {
            this.threadNo = threadNo;
        }

        @Override
        public void run() {
            //long time = System.nanoTime();
            for (int i = 0; i < list.size(); i++) {
                if (i % THREAD_NUM == threadNo) {
                    File f = list.get(i);
                    if (!xmlMap.containsKey(f)) {
                        QQX5MapType type = getQQX5MapType(f);
                        if (type != null) {
                            xmlMap.put(f, type);
                        }
                    }
                    synchronized (AddXmlFilesThread.class) {
                        processedNum++;
                    }
                }
            }
            //logInfo("Thread " + threadNo + " 处理完毕，共用时 " + nanoTime(time));
        }
    }

    @FXML
    public Button btn_clearXmlList;

    public void btn_clearXmlList_click() {
        xmlMap.clear();
        freshFileInfo();
    }

    @FXML
    Button btn_back;

    @FXML
    public void btn_back_click() {
        app.gotoQQX5Tools();
    }

    @FXML
    Button btn_next;

    @FXML
    public void btn_next_click() {
        if (xmlMap.isEmpty()) {
            showWarn("参数错误", "未选择谱面文件！");
            return;
        }
        OutputMode outputMode = rbtn_simpleMode.isSelected() ? OutputMode.SIMPLE : OutputMode.FULL;
        int maxBurstPointsNum = 1;
        int maxScoreDifference = 0;
        if (outputMode == OutputMode.FULL) {
            if (rbtn_maxBurstPointsNum5.isSelected()) {
                maxBurstPointsNum = 5;
            } else if (rbtn_maxBurstPointsNum10.isSelected()) {
                maxBurstPointsNum = 10;
            } else if (rbtn_maxBurstPointsNum50.isSelected()) {
                maxBurstPointsNum = 50;
            } else {
                if (rbtn_maxBurstPointsNumDiy.isSelected()) {
                    try {
                        maxBurstPointsNum = Integer.parseInt(textfield_maxBurstPointsNum.getText());
                        if (maxBurstPointsNum <= 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        showWarn("参数错误", "爆点个数不是大于0的整数！");
                        return;
                    }
                }
            }
            if (rbtn_maxScoreDifference0.isSelected()) {
                maxScoreDifference = 0;
            } else if (rbtn_maxScoreDifference1000.isSelected()) {
                maxScoreDifference = 1000;
            } else if (rbtn_maxScoreDifference5000.isSelected()) {
                maxScoreDifference = 5000;
            } else {
                if (rbtn_maxScoreDifferenceDiy.isSelected()) {
                    try {
                        maxScoreDifference = Integer.parseInt(textfield_maxScoreDifference.getText());
                        if (maxScoreDifference < 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException e) {
                        showWarn("参数错误", "最大分差不是大于等于0的整数！");
                        return;
                    }
                }
            }
        }
        app.gotoCalculateBurstPoints2(xmlMap, outputMode, maxBurstPointsNum, maxScoreDifference);
    }
}
