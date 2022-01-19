module com.menglei.qqx5tools {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.menglei.qqx5tools to javafx.fxml;
    exports com.menglei.qqx5tools;

}