module ro.cofi.relicdb {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.apache.logging.log4j;
    requires org.jsoup;
    requires com.google.gson;

    opens ro.cofi.relicdb to javafx.fxml;
    exports ro.cofi.relicdb;
}