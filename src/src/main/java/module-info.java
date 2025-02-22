module ie.dcu.secureyac.ui {
    requires javafx.controls;
    requires javafx.fxml;


    opens ie.dcu.secureyac.ui to javafx.fxml;
    exports ie.dcu.secureyac.ui;
}