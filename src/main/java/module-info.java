module com.menglei.qqx5tools {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires lombok;
    requires dom4j;

    opens com.menglei.qqx5tools to javafx.fxml;
    exports com.menglei.qqx5tools;
    exports com.menglei.qqx5tools.controller;
    opens com.menglei.qqx5tools.controller to javafx.fxml;

}