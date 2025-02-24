module ie.dcu.secureYAC {
    requires javafx.controls;
    requires javafx.fxml;


    opens ie.dcu.secureYAC to javafx.fxml;
    exports ie.dcu.secureYAC;
}