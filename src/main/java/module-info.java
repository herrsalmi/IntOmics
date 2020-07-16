module IntOmics {
    requires com.google.common;
    requires org.apache.logging.log4j;
    requires jdom2;
    requires java.xml.bind;
    requires jcommander;
    requires com.google.gson;

    opens org.pmoi;
    opens org.pmoi.model.vis;
    exports org.pmoi.validator;
}