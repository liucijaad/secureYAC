module ie.dcu.secureYAC {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    requires java.desktop;
    requires org.bouncycastle.provider;


    opens ie.dcu.secureYAC to javafx.fxml;
    exports ie.dcu.secureYAC;
}