module com.karimbouchareb.chromatographysimulator {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires org.jsoup;
    requires commons.math3;
    requires org.knowm.xchart;
    requires commons.csv;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.materialdesign;
    requires org.kordamp.ikonli.fluentui;
    requires java.sql;

    opens com.karimbouchareb.chromatographysimulator to javafx.fxml;
    exports com.karimbouchareb.chromatographysimulator;
    exports Scripts;
    opens Scripts to javafx.fxml;
}