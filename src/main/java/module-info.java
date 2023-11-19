module com.example.lab4 {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.slf4j;
    requires protobuf.java;
    requires io.reactivex.rxjava3;

    opens app to javafx.fxml;
    exports app;
    exports lab4.gui.controller;
    opens lab4.gui.controller to javafx.fxml;
    exports lab4.gui.view;
    opens lab4.gui.view to javafx.fxml;
}