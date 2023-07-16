package com.karimbouchareb.chromatographysimulator.ui;

import com.karimbouchareb.chromatographysimulator.ChromatographySimulatorApp;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.File;
import java.io.FileNotFoundException;

import static com.karimbouchareb.chromatographysimulator.ui.ScreenUtility.SCREEN_BOUNDS;
import static com.karimbouchareb.chromatographysimulator.ui.ScreenUtility.makeImageView;

public class SplashScreen {
    private final StackPane splashScreenPane = new StackPane();
    private static final String BACKGROUND_SCATTER_FILE = "backgroundPopulator.gif";
    private static final String TRAVIS_HEAD_FILE = "travisHead.png";
    private static final String TITLE_FILE = "title.png";
    private static final String BASELINE_ANIMATION_FILE = "baselineAnimation.gif";

    /**
     * Returns the StackPane root container of the splash screen. This root container is needed during the construction
     * of the splash screen scene that is made when {@link ChromatographySimulatorApp#showSplash(Stage)} is called
     * [this link will work in-source but not in the final generated javadoc since showSplash is private].
     * showSplash() cannot be called during {@link ChromatographySimulatorApp#init()} because the JavaFX application
     * thread cannot run during calls to {@link javafx.application.Application#init()} and constructing scenes is done
     * on the JavaFX application thread.
     */
    public StackPane getRoot(){
        return splashScreenPane;
    }

    /**
     * This is called when {@link ChromatographySimulatorApp#init()} is called. It inits each of the ui components of
     * the splash screen: {@link #initBackground()}, {@link #initTravisHead()}, {@link #initTitle()},
     * {@link #initBaselineAnimation(double, double)}, and {@link #initLaunchButton(double, ImageView)}
     */
    public void initSplashScreen() throws FileNotFoundException {
        splashScreenPane.setBackground(Background.fill(Color.BLACK));
        // Background Scatter
        ImageView backgroundScatter = initBackground();
        // Travis Head
        ImageView travisHead = initTravisHead();
        Pane travisHeadPane = new Pane(travisHead);
        // Title
        ImageView title = initTitle();
        Pane titlePane = new Pane(title);
        double titleWidth = title.getFitWidth();
        double titleHeight = title.prefHeight(titleWidth);
        // Baseline Animation
        Pane baselineAnimationPane = initBaselineAnimation(titleWidth, titleHeight); // baseline animation's size depends on title's size
        // Launch Button
        Pane launchPane = initLaunchButton(titleHeight, travisHead); // launchButton's size depends on title's size and mutates travisHead

        // Place background, travis, baselineAnimation, title, and launch button into scene
        splashScreenPane.getChildren().addAll(backgroundScatter, travisHeadPane, baselineAnimationPane, titlePane, launchPane);
    }

    /**
     * Initialize the rotating background image.
     */
    private ImageView initBackground() throws FileNotFoundException {
        ImageView backgroundScatter;
        try {
            backgroundScatter = makeImageView(BACKGROUND_SCATTER_FILE);
        } catch (FileNotFoundException e){
            throw new FileNotFoundException("No file named " + BACKGROUND_SCATTER_FILE + "; this file needed for background animation in splashScreen initialization");
        }
        RotateTransition rtBackground = new RotateTransition(Duration.millis(220000), backgroundScatter);
        rtBackground.setByAngle(360);
        rtBackground.setAutoReverse(true);
        rtBackground.setCycleCount(RotateTransition.INDEFINITE);
        rtBackground.play();
        backgroundScatter.setFitWidth(SCREEN_BOUNDS.getWidth()*4);
        backgroundScatter.setFitHeight(SCREEN_BOUNDS.getHeight()*3);
        return backgroundScatter;
    }

    /**
     * Initialize travis' head which fades in slowly, floats around from random point to point, and rotates slowly.
     */
    private ImageView initTravisHead() throws FileNotFoundException {
        ImageView travisHead;
        try {
            travisHead = makeImageView(TRAVIS_HEAD_FILE);
        } catch (FileNotFoundException e){
            throw new FileNotFoundException("No file named " + TRAVIS_HEAD_FILE + "; this file needed for travisHead animation in splashScreen initialization");
        }
        travisHead.setPreserveRatio(true);
        travisHead.setFitWidth(SCREEN_BOUNDS.getWidth()*0.1);
        FadeTransition ftTravisHead1 = new FadeTransition(Duration.millis(5000), travisHead);
        ftTravisHead1.setFromValue(0.0);
        ftTravisHead1.setToValue(0.0);
        ftTravisHead1.play();
        ftTravisHead1.setOnFinished(e->{
            FadeTransition ftTravisHead2 = new FadeTransition(Duration.millis(7000), travisHead);
            ftTravisHead2.setFromValue(0.0);
            ftTravisHead2.setToValue(1.0);
            ftTravisHead2.play();
        });
        RotateTransition rtTravisHead = new RotateTransition(Duration.millis(10000), travisHead);
        rtTravisHead.setByAngle(360);
        rtTravisHead.setCycleCount(RotateTransition.INDEFINITE);
        rtTravisHead.play();
        TranslateTransition ttTravisHead = new TranslateTransition(Duration.millis(3000),travisHead);
        ttTravisHead.setFromX(SCREEN_BOUNDS.getWidth()-Math.random()*SCREEN_BOUNDS.getWidth());
        ttTravisHead.setFromY(SCREEN_BOUNDS.getHeight()-Math.random()*SCREEN_BOUNDS.getHeight());
        ttTravisHead.setToX(SCREEN_BOUNDS.getWidth() - Math.random()*SCREEN_BOUNDS.getWidth());
        ttTravisHead.setToY(SCREEN_BOUNDS.getHeight() - Math.random()*SCREEN_BOUNDS.getHeight());
        ttTravisHead.play();
        ttTravisHead.setOnFinished(e->{
            ttTravisHead.setFromX(travisHead.getTranslateX());
            ttTravisHead.setFromY(travisHead.getTranslateY());
            ttTravisHead.setToX(SCREEN_BOUNDS.getWidth() - Math.random()*SCREEN_BOUNDS.getWidth());
            ttTravisHead.setToY(SCREEN_BOUNDS.getHeight() - Math.random()*SCREEN_BOUNDS.getHeight());
            ttTravisHead.play();
        });
        return travisHead;
    }

    /**
     * Initialize the title text. First fade transition holds it invisible for 2.5 seconds, then it fades in.
     */
    private ImageView initTitle() throws FileNotFoundException {
        ImageView title;
        try {
            title = makeImageView(TITLE_FILE);
        }catch (FileNotFoundException e) {
            throw new FileNotFoundException("No file named " + TITLE_FILE + "; this file needed for title in splashScreen initialization");
        }
        title.setPreserveRatio(true);
        title.setFitWidth(SCREEN_BOUNDS.getWidth()*0.58);
        title.setX(SCREEN_BOUNDS.getMinX() + SCREEN_BOUNDS.getWidth() / 2 - title.getFitWidth() / 2);
        title.setY(SCREEN_BOUNDS.getMinY() + SCREEN_BOUNDS.getHeight() / 2 - title.prefHeight(title.getFitWidth()) / 2);
        fadeIn(title);
        return title;
    }

    /**
     * Initialize the baseline animation. First fade transition holds it invisible for 2.5 seconds, then it fades in.
     */
    private Pane initBaselineAnimation(double titleWidth, double titleHeight) throws FileNotFoundException {
        Pane baselineAnimationPane = new Pane();
        ImageView baselineAnimation;
        try {
            baselineAnimation = makeImageView(BASELINE_ANIMATION_FILE);
        }catch (FileNotFoundException e){
            throw new FileNotFoundException("No filed named " + BASELINE_ANIMATION_FILE + "; this file needed for baseline animation in splashScreen initialization");
        }
        baselineAnimation.setRotate(1.8);
        baselineAnimation.setFitWidth(titleWidth*1.15);
        baselineAnimation.setFitHeight(titleHeight*0.65);
        baselineAnimation.setX(SCREEN_BOUNDS.getMinX() + SCREEN_BOUNDS.getWidth() / 2 - baselineAnimation.getFitWidth() / 2);
        baselineAnimation.setY(SCREEN_BOUNDS.getMinY() + (SCREEN_BOUNDS.getHeight() / 2) + (titleHeight/2) - baselineAnimation.getFitHeight() / 2);
        fadeIn(baselineAnimation);
        baselineAnimationPane.getChildren().add(baselineAnimation);
        return baselineAnimationPane;
    }

    /**
     * Initialize the custom launch button which causes travisHead to rotate quickly and launches the main scene.
     * When clicked, launch button calls {@link #showMainStage()} to initialize the main stage and show the main scene.
     */
    private Pane initLaunchButton(double titleHeight, ImageView travisHead){
        Pane launchPane = new Pane();
        VBox launch = new VBox();
        launch.setAlignment(Pos.CENTER);
        Button launchButton = new Button();
        launchButton.setBackground(new Background(
                new BackgroundFill(Color.web("#ffaf00")
                        ,new CornerRadii(30)
                        ,Insets.EMPTY)));
        FontIcon launchIcon = new FontIcon(MaterialDesign.MDI_ARROW_RIGHT_BOLD_HEXAGON_OUTLINE);
        launchIcon.setIconSize((int) (SCREEN_BOUNDS.getWidth()*0.05));
        launchIcon.setFill(Color.web("#ff6900"));
        launchButton.setGraphic(launchIcon);
        launchButton.setPrefWidth(SCREEN_BOUNDS.getWidth()*0.10);
        launchButton.setOnMouseEntered(e->{
            launchButton.setBackground(new Background(
                    new BackgroundFill(Color.web("#8f1b00")
                            ,new CornerRadii(30)
                            ,Insets.EMPTY)));
        });
        launchButton.setOnMouseExited(e->{
            launchButton.setBackground(new Background(
                    new BackgroundFill(Color.web("#ffaf00")
                            ,new CornerRadii(30)
                            ,Insets.EMPTY)));
        });

        launchButton.setOnAction(e -> {
            RotateTransition rtPermanent = new RotateTransition(Duration.millis(190), travisHead);
            rtPermanent.setByAngle(360);
            rtPermanent.setCycleCount(RotateTransition.INDEFINITE);
            launchButton.setDisable(true);
            rtPermanent.play();
            FadeTransition fadeSplash = new FadeTransition(Duration.seconds(2.2), splashScreenPane);
            fadeSplash.setFromValue(1.0);
            fadeSplash.setToValue(0.0);
            fadeSplash.play();
            fadeSplash.setOnFinished(actionEvent -> {
                showMainStage(); // Leave splash screen and show main stage
                launchButton.sceneProperty().get().getWindow().hide();
            });
        });
        fadeIn(launchButton);
        launch.setLayoutX(SCREEN_BOUNDS.getMinX() + (SCREEN_BOUNDS.getWidth()/2) - launchButton.getPrefWidth()/2);
        launch.setLayoutY(SCREEN_BOUNDS.getMinY() + (SCREEN_BOUNDS.getHeight() / 2) + (titleHeight/2)*1.5 - launch.getHeight() / 2);
        launch.getChildren().add(launchButton);
        launchPane.getChildren().add(launch);
        return launchPane;
    }

    /**
     * Helper method that initializes the main stage, main scene, and shows the main scene in the application.
     * Called by {@link #initLaunchButton(double, ImageView)} which is called when user clicks the launch button in the splash screen.
     */
    private static void showMainStage() {
        ChromatographySimulatorApp.mainStage = new Stage(StageStyle.DECORATED);
        ChromatographySimulatorApp.mainStage.setTitle("Gas Chromatography Simulator");
        ChromatographySimulatorApp.mainScene = new Scene(ChromatographySimulatorApp.root, SCREEN_BOUNDS.getWidth()*0.98, SCREEN_BOUNDS.getHeight()*0.98);
        ChromatographySimulatorApp.mainStage.setScene(ChromatographySimulatorApp.mainScene);
        ChromatographySimulatorApp.mainStage.setMaximized(true);
        ChromatographySimulatorApp.mainStage.show();
        ChromatographySimulatorApp.lineChartSolBand.requestFocus(); // remove focus from clear solute bands button at startup
    }

    private void fadeIn(Node node){
        FadeTransition initializeAsInvisible = new FadeTransition(Duration.millis(2500), node);
        initializeAsInvisible.setFromValue(0.0);
        initializeAsInvisible.setToValue(0.0);
        initializeAsInvisible.play();
        initializeAsInvisible.setOnFinished(e->{
            FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), node);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
    }

}
