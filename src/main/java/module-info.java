module com.bookloop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.bookloop to javafx.fxml, javafx.graphics;
    opens com.bookloop.controller to javafx.fxml;
    opens com.bookloop.model to javafx.base;
    exports com.bookloop;
}
