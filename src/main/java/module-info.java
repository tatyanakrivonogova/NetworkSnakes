module com.example.lab4 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.example.lab4 to javafx.fxml;
    exports com.example.lab4;
}