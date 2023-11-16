module com.example.lab4 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.slf4j;

    opens app to javafx.fxml;
    exports app;
}