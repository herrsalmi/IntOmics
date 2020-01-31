module IntOmics {
    requires javafx.controls;
    requires com.google.common;
    requires org.apache.logging.log4j;
    requires jdom2;
    requires javafx.fxml;
    requires java.desktop;
    requires java.xml.bind;
    requires commons.math3;

    exports org.pmoi.ui;
    opens org.pmoi.ui;
}