module ro.cofi.relicdb {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens ro.cofi.relicdb to javafx.fxml;
    exports ro.cofi.relicdb;
}