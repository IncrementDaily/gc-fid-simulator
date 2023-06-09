module com.karimbouchareb.chromatographysimulator {
    requires javafx.controls;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.bootstrapfx.core;
    requires commons.math3;
    requires org.knowm.xchart;
    requires commons.csv;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.materialdesign;
    requires org.kordamp.ikonli.fluentui;

    opens com.karimbouchareb.chromatographysimulator to javafx.base;
    exports com.karimbouchareb.chromatographysimulator to javafx.graphics;
    exports com.karimbouchareb.chromatographysimulator.ui to javafx.graphics;
    opens com.karimbouchareb.chromatographysimulator.ui to javafx.base;
}