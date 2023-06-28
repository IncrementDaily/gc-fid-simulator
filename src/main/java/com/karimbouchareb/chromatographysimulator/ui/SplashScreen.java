package com.karimbouchareb.chromatographysimulator.ui;

import com.karimbouchareb.chromatographysimulator.ChromatographySimulatorApp;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
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

import static com.karimbouchareb.chromatographysimulator.ChromatographySimulatorApp.SCREEN_BOUNDS;
import static com.karimbouchareb.chromatographysimulator.ChromatographySimulatorApp.makeImageView;

public class SplashScreen {
    private StackPane splashScreenPane = new StackPane();
    private ImageView title;
    private Pane titlePane;
    private Pane baselineAnimationPane;
    private Pane travisHeadPane;
    private ImageView backgroundScatter;
    private Pane launchPane;
    private ImageView travisHead;

    /**
     * This is called when {@link ChromatographySimulatorApp#init()} is called. It inits each of the ui components of
     * the splash screen: {@link #initBackground()}, {@link #initTravisHead()}, {@link #initTitle()},
     * {@link #initBaselineAnimation()}, and {@link #initLaunchButton()}.
     */
    public void initSplashScreen(){
        splashScreenPane.setBackground(Background.fill(Color.BLACK));
        // These methods must be called in this sequential order to avoid NPEs
        initBackground();
        initTravisHead();
        initTitle();
        initBaselineAnimation();
        initLaunchButton();
        // Place background, travis, baselineAnimation, title, and launch button into scene
        splashScreenPane.getChildren().addAll(backgroundScatter, travisHeadPane, baselineAnimationPane, titlePane, launchPane);
    }

    /**
     * Initialize the rotating background image
     */
    private void initBackground(){
        backgroundScatter = ChromatographySimulatorApp.makeImageView("backgroundPopulator.gif");
        RotateTransition rtBackground = new RotateTransition(Duration.millis(220000), backgroundScatter);
        rtBackground.setByAngle(360);
        rtBackground.setAutoReverse(true);
        rtBackground.setCycleCount(RotateTransition.INDEFINITE);
        rtBackground.play();
        backgroundScatter.setFitWidth(SCREEN_BOUNDS.getWidth()*4);
        backgroundScatter.setFitHeight(SCREEN_BOUNDS.getHeight()*3);
    }

    /**
     * Initialize travis' head which fades in slowly, floats around from random point to point, and rotates slowly
     */
    private void initTravisHead(){
        travisHeadPane = new Pane();
        travisHead = ChromatographySimulatorApp.makeImageView("travisHead.png");
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
        travisHeadPane.getChildren().add(travisHead);
    }

    /**
     * Initialize the title text. First fade transition holds it invisible for 2.5 seconds, then it fades in.
     */
    private void initTitle(){
        titlePane = new Pane();
        title = ChromatographySimulatorApp.makeImageView("title.png");
        title.setPreserveRatio(true);
        title.setFitWidth(SCREEN_BOUNDS.getWidth()*0.58);
        title.setX(SCREEN_BOUNDS.getMinX() + SCREEN_BOUNDS.getWidth() / 2 - title.getFitWidth() / 2);
        title.setY(SCREEN_BOUNDS.getMinY() + SCREEN_BOUNDS.getHeight() / 2 - title.prefHeight(title.getFitWidth()) / 2);
        FadeTransition ftTitle = new FadeTransition(Duration.millis(2500), title);
        ftTitle.setFromValue(0.0);
        ftTitle.setToValue(0.0);
        ftTitle.play();
        ftTitle.setOnFinished(e->{
            FadeTransition ftTitle2 = new FadeTransition(Duration.millis(1000), title);
            ftTitle2.setFromValue(0.0);
            ftTitle2.setToValue(1.0);
            ftTitle2.play();
        });
        titlePane.getChildren().add(title);
    }

    /**
     * Initialize the baseline animation. First fade transition holds it invisible for 2.5 seconds, then it fades in.
     */
    private void initBaselineAnimation(){
        baselineAnimationPane = new Pane();
        ImageView baselineAnimation = ChromatographySimulatorApp.makeImageView("wave.gif");
        baselineAnimation.setRotate(1.8);
        baselineAnimation.setFitWidth(title.getFitWidth()*1.15);
        baselineAnimation.setFitHeight(title.prefHeight(title.getFitWidth())*0.65);
        baselineAnimation.setX(SCREEN_BOUNDS.getMinX() + SCREEN_BOUNDS.getWidth() / 2 - baselineAnimation.getFitWidth() / 2);
        baselineAnimation.setY(SCREEN_BOUNDS.getMinY() + (SCREEN_BOUNDS.getHeight() / 2) + (title.prefHeight(title.getFitWidth())/2) - baselineAnimation.getFitHeight() / 2);
        FadeTransition ftWave = new FadeTransition(Duration.millis(2500), baselineAnimation);
        ftWave.setFromValue(0.0);
        ftWave.setToValue(0.0);
        ftWave.play();
        ftWave.setOnFinished(e->{
            FadeTransition ftWave2 = new FadeTransition(Duration.millis(1000), baselineAnimation);
            ftWave2.setFromValue(0.0);
            ftWave2.setToValue(1.0);
            ftWave2.play();
        });
        baselineAnimationPane.getChildren().add(baselineAnimation);
    }

    /**
     * Initialize the custom launch button which causes travisHead to rotate quickly and launches the main scene.
     * When clicked, launch button calls {@link #showMainStage()} to initialize the main stage and show the main scene.
     */
    private void initLaunchButton(){
        launchPane = new Pane();
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
        FadeTransition ftLaunch = new FadeTransition(Duration.millis(2500), launchButton);
        ftLaunch.setFromValue(0.0);
        ftLaunch.setToValue(0.0);
        ftLaunch.play();
        ftLaunch.setOnFinished(e->{
            FadeTransition ftLaunch2 = new FadeTransition(Duration.millis(1000), launchButton);
            ftLaunch2.setFromValue(0.0);
            ftLaunch2.setToValue(1.0);
            ftLaunch2.play();
        });
        launch.setLayoutX(SCREEN_BOUNDS.getMinX() + (SCREEN_BOUNDS.getWidth()/2) - launchButton.getPrefWidth()/2);
        launch.setLayoutY(SCREEN_BOUNDS.getMinY() + (SCREEN_BOUNDS.getHeight() / 2) + (title.prefHeight(title.getFitWidth())/2)*1.5 - launch.getHeight() / 2);
        launch.getChildren().add(launchButton);
        launchPane.getChildren().add(launch);
    }

    /**
     * Helper method that initializes the main stage, main scene, and shows the main scene in the application.
     * Called by {@link #initLaunchButton()} which is called when user clicks the launch button in the splash screen.
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
}
