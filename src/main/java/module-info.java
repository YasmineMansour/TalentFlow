module org.example.talentflow {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.example to javafx.fxml;
    opens org.example.GUI to javafx.fxml;
    opens org.example.model to javafx.base;

    exports org.example;
    exports org.example.GUI;
    exports org.example.model;
}