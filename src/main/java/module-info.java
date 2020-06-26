module IntOmics {
    requires com.google.common;
    requires org.apache.logging.log4j;
    requires jdom2;
    requires java.desktop;
    requires java.xml.bind;
    requires commons.math3;
    requires jcommander;

    opens org.pmoi;
    exports org.pmoi.validator;
}