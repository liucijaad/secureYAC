module ie.dcu.secureYAC {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;


    opens ie.dcu.secureYAC to javafx.fxml;
    exports ie.dcu.secureYAC;
}