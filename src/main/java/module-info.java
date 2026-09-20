module com.bookloop {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    opens com.bookloop to javafx.fxml;
    opens com.bookloop.controller to javafx.fxml;
    opens com.bookloop.model to javafx.base;
}
