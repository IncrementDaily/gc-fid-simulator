package com.karimbouchareb.chromatographysimulator.ui;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Screen;

import java.io.FileNotFoundException;
import java.net.URL;

public class ScreenUtility {
    private static final Screen SCREEN = Screen.getPrimary();
    public static final Rectangle2D SCREEN_BOUNDS = SCREEN.getVisualBounds();


    public static ImageView makeImageView(String filePath) throws FileNotFoundException {
        URL imageUrl = ScreenUtility.class.getClassLoader().getResource(filePath);
        if (imageUrl == null) {
            throw new FileNotFoundException("Cannot initialize imageView: no imageURL could be made from filepath " + filePath);
        }
        Image image = new Image(imageUrl.toString());
        return new ImageView(image);
    }

}
