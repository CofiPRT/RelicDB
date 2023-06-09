module ro.cofi.relicdb {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.apache.logging.log4j;
    requires org.jsoup;
    requires com.google.gson;
    requires org.apache.commons.io;
    requires jdk.xml.dom;
    requires java.desktop;

    opens ro.cofi.relicdb to javafx.fxml;
    exports ro.cofi.relicdb;
    exports ro.cofi.relicdb.io;
    opens ro.cofi.relicdb.io to javafx.fxml;
    exports ro.cofi.relicdb.logic;
    opens ro.cofi.relicdb.logic to javafx.fxml;
    exports ro.cofi.relicdb.scoring;
    opens ro.cofi.relicdb.scoring to javafx.fxml;
}