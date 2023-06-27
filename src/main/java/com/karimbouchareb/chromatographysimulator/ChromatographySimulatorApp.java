package com.karimbouchareb.chromatographysimulator;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.*;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.function.Logistic;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.kordamp.ikonli.fluentui.FluentUiFilledAL;
import org.kordamp.ikonli.fluentui.FluentUiFilledMZ;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static javafx.beans.binding.Bindings.bindContent;

public class ChromatographySimulatorApp extends Application {
// TOP-LEVEL FIELDS
    // DATA FIELDS
    private static final String CHEM_DATA_FILEPATH = "src/main/java/com/karimbouchareb/chromatographysimulator/ufz_LSERdataset.csv";
    private static int CHEM_DATA_SIZE = 0;
    private static final double MRF_PROPORTIONALITY_CONST = 5.952e11;

    // INTERNAL CLOCK OF SIMULATION FIELDS
    private static Timer simulationTimer = new Timer();
    private static AtomicInteger FRAME_LENGTH_MS = new AtomicInteger(50); // 50 milliseconds per frame
    private static final double FRAME_LENGTH_S = 0.05; // 0.05 seconds per frame
    private static double CURRENT_TIME = 0.0;  // elapsedTime in seconds
    private static SimpleBooleanProperty isPaused = new SimpleBooleanProperty(true);

    // SPLASH SCREEN FIELDS
    private StackPane splashScreenPane = new StackPane();

    // MAIN SCREEN FIELDS
    private Stage mainStage;
    private static final Screen SCREEN = Screen.getPrimary();
    private static final Rectangle2D SCREEN_BOUNDS = SCREEN.getVisualBounds();
    private static LineChart<Number, Number> lineChartSolBand;
    private static Map<Peak, ChangeListener> peakToSolBandChangeListener = new ConcurrentHashMap<>(512);
    private static Map<Peak, XYChart.Series> peakToSolBandDataSeries = new ConcurrentHashMap<>(512);
    private static Map<XYChart.Series, Peak> solBandDataSeriesToPeak = new ConcurrentHashMap<>(512);
    private static SimpleIntegerProperty soluteBandCountProperty = new SimpleIntegerProperty(0);

    // MAIN SCENE FIELDS
    private static Scene mainScene;
    private BorderPane root;

    // INJECT SCENE FIELDS
    private static final double PARETO_SCALE = 1.0;
    private static final double PARETO_SHAPE = 2.5;
    private static ObservableList<ChemicalView> obsListChemicalViews = FXCollections.observableArrayList();
    // 8192 initial capacity (4212 unique Chemicals / 0.75 load factor = 5616; 2**12 = 4096, 2**13 = 8192)
    private static ConcurrentHashMap<String,Chemical> casToChemical = new ConcurrentHashMap<>(8192);
    ArrayList<ChemicalView> finalUserInputs_ChemViews = new ArrayList<>();

    /**
     * In a JavaFX application, main() calls launch() which first calls {@link #init()} and then {@link #start(Stage)})}
     */

    public static void main(String[] args) {
        launch();
    }

    /**
     * init() run only once at beginning of application launch -- loads the splash screen with all its ui components
     */
    @Override
    public void init() {
        // SPLASH SCREEN OBJECTS FOR LOADING
        splashScreenPane.setBackground(Background.fill(Color.BLACK));
        // title
        Pane titlePane = new Pane();
        ImageView title = makeImageView("title.png");
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
        // background
        ImageView backgroundScatter = makeImageView("backgroundPopulator.gif");
        RotateTransition rtBackground = new RotateTransition(Duration.millis(220000), backgroundScatter);
        rtBackground.setByAngle(360);
        rtBackground.setAutoReverse(true);
        rtBackground.setCycleCount(RotateTransition.INDEFINITE);
        rtBackground.play();
        backgroundScatter.setFitWidth(SCREEN_BOUNDS.getWidth()*4);
        backgroundScatter.setFitHeight(SCREEN_BOUNDS.getHeight()*3);
        // wave
        Pane wavePane = new Pane();
        ImageView wave = makeImageView("wave.gif");
        wave.setRotate(1.8);
        wave.setFitWidth(title.getFitWidth()*1.15);
        wave.setFitHeight(title.prefHeight(title.getFitWidth())*0.65);
        wave.setX(SCREEN_BOUNDS.getMinX() + SCREEN_BOUNDS.getWidth() / 2 - wave.getFitWidth() / 2);
        wave.setY(SCREEN_BOUNDS.getMinY() + (SCREEN_BOUNDS.getHeight() / 2) + (title.prefHeight(title.getFitWidth())/2) - wave.getFitHeight() / 2);
        FadeTransition ftWave = new FadeTransition(Duration.millis(2500), wave);
        ftWave.setFromValue(0.0);
        ftWave.setToValue(0.0);
        ftWave.play();
        ftWave.setOnFinished(e->{
            FadeTransition ftWave2 = new FadeTransition(Duration.millis(1000), wave);
            ftWave2.setFromValue(0.0);
            ftWave2.setToValue(1.0);
            ftWave2.play();
        });
        wavePane.getChildren().add(wave);
        // Travis
        Pane travisHeadPane = new Pane();
        ImageView travisHead = makeImageView("travisHead.png");
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
        // launchPane
        Pane launchPane = new Pane();
        VBox launch = new VBox();
        launch.setAlignment(Pos.CENTER);

        // launch Button
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

        // LAUNCH BUTTON
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
                showMainStage(root); // Leave splash screen and show main stage
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

        // Place background, travis, wave, title, and launch button into scene
        splashScreenPane.getChildren().addAll(backgroundScatter,travisHeadPane, wavePane, titlePane, launchPane);
    }

    // Helper method used inside of init() called when launch button clicked
    private void showMainStage(Parent root) {
        mainStage = new Stage(StageStyle.DECORATED);
        mainStage.setTitle("Gas Chromatography Simulator");
        mainScene = new Scene(root, SCREEN_BOUNDS.getWidth()*0.98, SCREEN_BOUNDS.getHeight()*0.98);
        mainStage.setScene(mainScene);
        mainStage.setMaximized(true);
        mainStage.show();
        lineChartSolBand.requestFocus(); // remove focus from clear solute bands button at startup
    }

// TOP-LEVEL STATIC METHODS
    // INTERNAL CLOCK OF SIMULATION STATIC METHODS
    protected static double currentTime(){
        return CURRENT_TIME;
    }
    // This method is called every time run() is called. run() advances the state of the simulation by one discrete step.
    // Every call to run() advances the internal clock of the simulation by 0.05 seconds; this is true regardless of
    // whether the simulation is at 1X, 2X, 3X, 4X, or 5X speed. At 1X speed, run() is called every 50 milliseconds. At
    // 5X speed, run() is called every 10 milliseconds. Performance may therefore suffer at 5X speed as ALL computation
    // that must be performed to advance the state of the simulation to the next discrete step must be performed in 10
    // milliseconds rather than 50 milliseconds.
    private static void incCurrentTime(){
        CURRENT_TIME += FRAME_LENGTH_S;
    }
    private static void restartSimulation(){
        // Restart Peaks & Time (including removal of listener pointers for columnTraversal properties)
        CURRENT_TIME = 0.0;
        peakToSolBandChangeListener.clear();
        peakToSolBandDataSeries.clear();
        solBandDataSeriesToPeak.clear();
        lineChartSolBand.getData().clear();
        soluteBandCountProperty.set(0);
        MachineSettings.ANALYTES_IN_COLUMN.clear();

        // Restart Columns
        for (Column column : Column.values()){
            MachineSettings.columnToDamAndRem.put(column,new Double[]{0.0,1.0});
        }

        MachineSettings.CURRENT_COLUMN_REMAINING = 1.00;
        MachineSettings.CURRENT_COLUMN_DAMAGE = 0.00;
        MachineSettings.CURRENT_COLUMN = Column.SPB_OCTYL;
        // Restart Split
        MachineSettings.SPLIT_RATIO = 100.0;
        // Restart oven
        MachineSettings.OVEN_TEMPERATURE = 25;
        MachineSettings.OVEN_TEMPERATURE_TARGET = 25;
        // Restart Detector
        MachineSettings.isDetectorOn.set(true);
    }

    // UI STATIC METHODS
    private static Region createVSpacer() {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    private static Region createHSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    private static ImageView makeImageView(String filePath) {
        ImageView imageView = null;
        URL imageUrl = ChromatographySimulatorApp.class.getClassLoader().getResource(filePath);
        if (imageUrl != null) {
            Image image = new Image(imageUrl.toString());
            imageView = new ImageView(image);
            return imageView;
        } else {
            return null;
        }
    }
    private static String toHexString(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

// TOP-LEVEL STATIC MEMBER CLASSES
    protected static class MachineSettings{
        // PEAK & COLUMN FIELDS
        private static TreeSet<Peak> ANALYTES_IN_COLUMN = new TreeSet<>();
        private static Column CURRENT_COLUMN = Column.SPB_OCTYL;
        private static HashMap<Column, Double[]> columnToDamAndRem = new HashMap<>();
        private static double CURRENT_COLUMN_DAMAGE = 0.0;
        private static double CURRENT_COLUMN_REMAINING = 1.0;
        public static double INJECTION_VOLUME = 1.0; // microliters // TODO: 4/2/2023 Add Icon for "Machine Configurations" Button
        public static double SPLIT_RATIO = 100.0;
        public static SimpleDoubleProperty splitRatioProperty = new SimpleDoubleProperty(SPLIT_RATIO);
        public static double HE_GAS_LINEAR_VELOCITY = 40.0; // TODO: 5/20/2023 Default = 40 cm/s (taken from Poole 2019 experimental conditions)
        public static double OVEN_TEMPERATURE_MIN = 25;
        public static double OVEN_TEMPERATURE_MAX = 350;
        public static double OVEN_TEMPERATURE = 25; // degrees C
        public static SimpleDoubleProperty ovenTempProperty = new SimpleDoubleProperty(OVEN_TEMPERATURE);
        public static double OVEN_TEMPERATURE_TARGET = 25; // degrees C
        public static AtomicBoolean TEMP_RAMPING = new AtomicBoolean();
        public static AtomicBoolean TEMP_COOLING = new AtomicBoolean();
        public static SimpleBooleanProperty isDetectorOn = new SimpleBooleanProperty(true);
        public static boolean IS_COLUMN_CUT_POORLY = false; // TODO: 5/20/2023
        public static double INLET_TEMPERATURE = 25; // degrees C // TODO: 5/20/2023  

        // OVEN-TEMPERATURE OPERATIONAL METHODS
        public static void nudgeOvenTempUp(){
            OVEN_TEMPERATURE = Math.min(OVEN_TEMPERATURE + 0.10, OVEN_TEMPERATURE_TARGET);
        }
        public static void nudgeOvenTempDown(){
            OVEN_TEMPERATURE = Math.max(OVEN_TEMPERATURE - 0.06, OVEN_TEMPERATURE_TARGET);
        }
        public static void setOvenTemperature(int ovenTemperature) {
            OVEN_TEMPERATURE = ovenTemperature;
        }
        public static void setOvenTemperatureTarget(int ovenTemperatureTarget) {
            OVEN_TEMPERATURE_TARGET = ovenTemperatureTarget;
        }
        public static double getOvenTemperature(){
            return OVEN_TEMPERATURE;
        }
        public static double getOvenTemperatureTarget() {
            return OVEN_TEMPERATURE_TARGET;
        }
        // DETECTOR SIGNAL OPERATIONAL METHODS
        // Generate Noise Proportional to Oven Temp
        public static double nextNoiseValue(double meanNoise, double stdDevNoise) {
            // Generate a random value with a normal distribution to represent the baseline noise level
            Random random = new Random();
            if (CURRENT_COLUMN_REMAINING < 1.0){
                meanNoise = (meanNoise * CURRENT_COLUMN_REMAINING)+0.1;
                stdDevNoise = (stdDevNoise * CURRENT_COLUMN_REMAINING)+0.003;
            }
            double noise = meanNoise + stdDevNoise * random.nextGaussian();
            // Add extra noise to baseline if column is overheated & damaged
            if (CURRENT_COLUMN_DAMAGE > 0.0 && CURRENT_COLUMN_REMAINING >= 0.0001) {
                double meanColumnDamageNoise = (CURRENT_COLUMN_DAMAGE*9.0)*CURRENT_COLUMN_REMAINING;
                double stDevColumnDamageNoise = (CURRENT_COLUMN_DAMAGE*0.2)*CURRENT_COLUMN_REMAINING;
                double nextPosGaussian = Math.abs(random.nextGaussian());
                double columnDamageNoise = meanColumnDamageNoise + stDevColumnDamageNoise * nextPosGaussian;
                noise += columnDamageNoise;
            }
            return noise;
        }
        // Interpolation of noiseLevel based on two arbitrarily chosen key:value pairs of noise:temperature
        public static double ovenTempToMeanNoise(double temperature) {
            double noiseLevelAtTemp = 0.5 + (((temperature - 25.0) / 325.0) * 5.0);

            return noiseLevelAtTemp;
        }
        // Interpolation of noiseLevel based on two arbitrarily chosen key:value pairs of noise:temperature
        public static double ovenTempToStDevNoise(double temperature) {
            // MinStDev: stdev noise level at 25C (min oven temp) arbitrarily chosen as 0.6 pA based on visual realism
            // MaxStDev: stdev noise level at 350C (max oven temp) arbitrarily chosen as 1.7 pA based on visual realism
            // temperature range = 325 C
            // noiseStDev range = 1.1 pA

            // Example: you're 25% up the temp range? noiseStDevAtTemperature (nLAT) = minStDev + noiseStDevRange * 25%)
            // Example: you're 50% up the temp range? noiseStDevAtTemperature (nLAT) = minStDev + noiseStDevRange * 50%)
            double noiseStDevAtTemp = 0.1 + (((temperature - 25.0) / 325.0) * 0.2);
            return noiseStDevAtTemp;
        }

        // COLUMN METHODS
        private static boolean columnMaxTempExceeded(){
            return MachineSettings.OVEN_TEMPERATURE > MachineSettings.CURRENT_COLUMN.maxTemp;
        }
        private static void damageColumn(){
        if (MachineSettings.CURRENT_COLUMN_REMAINING == 0.0) return;
        if (MachineSettings.CURRENT_COLUMN_DAMAGE == 0.0) {
            MachineSettings.CURRENT_COLUMN_DAMAGE = 0.001;
            MachineSettings.CURRENT_COLUMN_REMAINING = 0.999;
        }
        double overMaxQuotient = (MachineSettings.OVEN_TEMPERATURE - MachineSettings.CURRENT_COLUMN.maxTemp) / MachineSettings.CURRENT_COLUMN.maxTemp;
        // min = 0.0002941 (HP-5 maxTemp = 340, ovenTemp = 341); max = 0.4583 (DB_225 maxTemp = 240, ovenTemp = 350)
        // if overMaxQuotient = 0.002941, then damageRate = 0.999849151 and bleedRate = 1.00026111
        // if overMaxQuotient = 0.4583, then damageRate = 0.997617625 and bleedRate = 1.00412375
        // damageRateRange = 0.002231526
        // bleedRateRange = 0.00386264
        double damageRate = 0.999849151 - ((overMaxQuotient-0.002941176)/0.455392157)*0.002231526;
        double bleedRate = 1.00026111 + ((overMaxQuotient-0.002941176)/0.455392157)*0.00386264;
        MachineSettings.CURRENT_COLUMN_DAMAGE = Math.min(Math.pow(MachineSettings.CURRENT_COLUMN_DAMAGE,damageRate), 1.0); // column reaches 100% damage after 120 seconds
        MachineSettings.CURRENT_COLUMN_REMAINING = Math.max(Math.pow(MachineSettings.CURRENT_COLUMN_REMAINING,bleedRate), 0.0);
    }

        // DETECTOR
        private static double detect(){
            double signalInPeakAreaUnits = 0.0;
            for (Peak peak : MachineSettings.ANALYTES_IN_COLUMN){
                signalInPeakAreaUnits+= peak.plot();
            }
            return signalInPeakAreaUnits;
        }

    }



    private static enum Column{
        SPB_OCTYL("SPB-Octyl",
                260,
                30.0,
                250.0,
                1.0,
                70.2,
                95.4,
                new PolynomialFunction(new double[]{0.15245000000000114, 2.1964285714274944E-5, 5.803571428571679E-7}),
                new PolynomialFunction(new double[]{0.1188727,-3.12272e-4}),
                new Logistic(0.45,65,-0.055,5.0,0,1.0), // No polynomial fit
                new PolynomialFunction(new double[]{1.0458,-0.004801,6.8822e-6}),
                new PolynomialFunction(new double[]{-2})), // Just estimates c as constant -2
        HP_5("HP-5",
                350,
                30.0,
                250.0,
                0.1,
                70.8,
                96.0,
                new PolynomialFunction(new double[]{-0.3627,0.0033,-5.8416e-6}),
                new PolynomialFunction(new double[]{0.7012787212787202, -0.005552430902430887, 2.2463786213786123E-5, -3.394522144522126E-8}),
                new PolynomialFunction(new double[]{0.6632737262737252, -0.0060150599400599246, 2.0638111888111807E-5, -2.2727272727272583E-8}),
                new PolynomialFunction(new double[]{0.9911,-0.0048,7.3464e-6}),
                new PolynomialFunction(new double[]{-2.65})), // Just estimates c as constant -2.65
        RXI_5SIL("RXI-5SIL",
                320,
                30.0,
                250.0,
                0.5,
                70.5,
                96.2,
                new PolynomialFunction(new double[]{-0.2200,0.0025,-7.3894e-6,8.0899e-9}),
                new PolynomialFunction(new double[]{0.5986,-0.0024,2.9361e-6}),
                new PolynomialFunction(new double[]{1.2722,-0.01851,1.2219e-4,-3.8133e-7,4.5471e-10}),
                new PolynomialFunction(new double[]{0.9823,-0.0042,5.2489e-6}),
                new PolynomialFunction(new double[]{-0.8121,-0.0391,3.0239e-4,-9.6197e-7,1.1069e-9})),
        RXI_17("RXI-17",
                320,
                30.0,
                250.0,
                0.5,
                70.5,
                96.2,
                new PolynomialFunction(new double[]{-0.17772261072260978, 0.0037361402486402297, -1.33653846153845E-5, 1.6729797979797765E-8}),
                new PolynomialFunction(new double[]{1.3410265734265738, -0.006103356643356645, 9.283216783216784E-6}),
                new Logistic(2.8,61.5,-0.0161,3.9,0.17,.847),
                new PolynomialFunction(new double[]{0.9151832167832167, -0.0037388927738927747, 4.350233100233109E-6}),
                new PolynomialFunction(new double[]{-2.6})),
        RTX_440("RTX-440",
                340,
                30.0,
                250.0,
                0.5,
                70.5,
                96.2,
                new Logistic(0.138,38,0.0145,2.15,-0.70,2.7), // no polynomial fit
                new PolynomialFunction(new double[]{0.7330069930069928, -0.003450861638361639, 5.489198301698308E-6, -3.2779720279719596E-10}),
                new PolynomialFunction(new double[]{0.7711758241758233, -0.005972727272727261, 1.8661963036962974E-5, -2.0760489510489386E-8}),
                new PolynomialFunction(new double[]{0.9549960039960036, -0.00382492507492507, 2.6098901098900845E-6, 6.993006993007044E-9}),
                new PolynomialFunction(new double[]{-2.2975952380952376, 1.3571428571427884E-4, -2.7976190476190314E-6})),
        DB_1701("DB-1701",
                280,
                30.0,
                250.0,
                1.0,
                70.6,
                96.3,
                new PolynomialFunction(new double[]{-0.36515664335664344, 0.002809020979020979, -4.423076923076924E-6}),
                new PolynomialFunction(new double[]{1.1593538461538457, -0.004367144522144518, 4.9271561771561605E-6}),
                new PolynomialFunction(new double[]{1.3931804195804196, -0.007323228438228436, 1.193764568764568E-5}),
                new PolynomialFunction(new double[]{0.9426125874125872, -0.004335792540792541, 6.349067599067604E-6}),
                new PolynomialFunction(new double[]{-2.2924999999999986, 0.001691071428571418, -3.4821428571428276E-6})),
        RTX_OPP("RTX-OPP",
                330,
                30.0,
                320.0,
                0.1,
                81.0,
                126.0,
                new PolynomialFunction(new double[]{-0.7796513486513476, 0.006399188311688292, -1.7312999500499377E-5, 1.7154720279720035E-8}),
                new PolynomialFunction(new double[]{1.5910149850149837, -0.007794647019646996, 1.4901973026972883E-5, -9.105477855477567E-9}),
                new PolynomialFunction(new double[]{0.5069770229770232, -0.0030762987012987026, 6.082667332667338E-6}),
                new PolynomialFunction(new double[]{0.8778341658341663, -0.0040773976023976075, 5.862887112887131E-6}),
                new PolynomialFunction(new double[]{-2.53})),
        DB_225("DB-225",
                240,
                15.0,
                250.0,
                0.25,
                27.0,
                40.8,
                new PolynomialFunction(new double[]{-0.3104727272727272, 0.003403863636363636, -6.988636363636364E-6}),
                new PolynomialFunction(new double[]{2.205145454545454, -0.010646174242424238, 1.8948863636363625E-5}),
                new PolynomialFunction(new double[]{2.2622727272727277, -0.012074924242424246, 2.1723484848484866E-5}),
                new PolynomialFunction(new double[]{0.8714727272727273, -0.004524015151515153, 7.784090909090914E-6}),
                new PolynomialFunction(new double[]{-2.9558545454545464, 5.996969696969789E-4})),
        HP_INNOWAX("HP-Innowax",
                260,
                60.0,
                250.0,
                1.0,
                189.6,
                240,
                new Logistic(0.236,160.5,0.0561,1.0,-0.7,8.847),
                new PolynomialFunction(new double[]{2.0339311688311694, -0.005760075757575763, 9.496753246753468E-7}),
                new PolynomialFunction(new double[]{3.538800000000003, -0.016778333333333367, 2.5416666666666792E-5}),
                new PolynomialFunction(new double[]{0.7775779220779229, -0.003031439393939405, 2.686688311688348E-6}),
                new PolynomialFunction(new double[]{-2.879699999999999, 0.0022299999999999963}));

        private final String name;
        private final double maxTemp;
        private final double columnLength;
        private final double internalDiameter;
        private final double filmThickness;
        private final double minHoldUpTime;
        private final double maxHoldUpTime;
//        private double percentDamaged = 0.0;
        private final UnivariateFunction eCurve;
        private final UnivariateFunction sCurve;
        private final UnivariateFunction aCurve;
        private final UnivariateFunction lCurve;
        private final UnivariateFunction cCurve;

        Column(String name,
               double maxTemp,
               double columnLength,
               double internalDiameter,
               double filmThickness,
               double minHoldUpTime,
               double maxHoldUpTime,
               UnivariateFunction eCurve,
               UnivariateFunction sCurve,
               UnivariateFunction aCurve,
               UnivariateFunction lCurve,
               UnivariateFunction cCurve) {
            this.name = name;
            this.maxTemp = maxTemp;
            this.columnLength = columnLength;
            this.internalDiameter = internalDiameter;
            this.filmThickness = filmThickness;
            this.minHoldUpTime = minHoldUpTime;
            this.maxHoldUpTime = maxHoldUpTime;
            this.eCurve = eCurve;
            this.sCurve = sCurve;
            this.aCurve = aCurve;
            this.lCurve = lCurve;
            this.cCurve = cCurve;
        }

        // Min and Max HoldUpTime's for each column calculated by reztek EZGC calculator (see ReadMe)
        // This method returns holdUpTime linearly interpolated between min and max holduptime based on temp
        // HoldUpTime is used in the calculation of retentionTime (see retention time methods)
        public double holdUpTime(double ovenTemperature){
            // Interpolate holdUpTime by temp
            double ratio = (ovenTemperature - MachineSettings.OVEN_TEMPERATURE_MIN) /
                    (MachineSettings.OVEN_TEMPERATURE_MAX - MachineSettings.OVEN_TEMPERATURE_MIN);
            double holdUpTimeRange = maxHoldUpTime - minHoldUpTime;
            return maxHoldUpTime - (holdUpTimeRange*ratio);
        }
        // If column has been damaged, column LSER constant values are lowered proportionally (column less able to
        // perform separatory function).
        public double E(double ovenTemperature){
            return eCurve.value(ovenTemperature)* MachineSettings.CURRENT_COLUMN_REMAINING;
        }
        public double S(double ovenTemperature){
            return sCurve.value(ovenTemperature)* MachineSettings.CURRENT_COLUMN_REMAINING;
        }
        public double A(double ovenTemperature){
            return aCurve.value(ovenTemperature)* MachineSettings.CURRENT_COLUMN_REMAINING;
        }
        public double L(double ovenTemperature){
            return lCurve.value(ovenTemperature)* MachineSettings.CURRENT_COLUMN_REMAINING;
        }
        public double C(double ovenTemperature){
            return cCurve.value(ovenTemperature)* MachineSettings.CURRENT_COLUMN_REMAINING;
        }
        public double lengthMeters(){
            return columnLength;
        }
        public String toString(){
            return name;
        }
    }


    private static class Chemical{
        private String casNumber;
        private String chemicalName;
        private double molarResponseFactor;
        private double molecularWeight;
        private double overloadMass_1;
        private double e;
        private double s;
        private double a;
        private double l;

        public Chemical(String chemicalName) {
            this.chemicalName = chemicalName;

            try (FileReader fileReader = new FileReader(CHEM_DATA_FILEPATH);
                 CSVParser parser = CSVFormat.DEFAULT.builder().setHeader(
                         "CAS","chemicalName","SMILES","label","MRF","molecularWeight",
                         "overloadMass_1", "E","S","A","L").build().parse(fileReader)) {
                for (CSVRecord record : parser) {
                    if (record.getRecordNumber() == 1) continue;
                    if (record.get(1).equals(chemicalName)){
                        this.casNumber = record.get("CAS");
                        this.chemicalName = record.get("chemicalName");
                        this.molarResponseFactor = Double.parseDouble(record.get("MRF"));
                        this.molecularWeight = Double.parseDouble(record.get("molecularWeight"));
                        this.overloadMass_1 = Double.parseDouble(record.get("overloadMass_1"));
                        this.e = Double.parseDouble(record.get("E"));
                        this.s = Double.parseDouble(record.get("S"));
                        this.a = Double.parseDouble(record.get("A"));
                        this.l = Double.parseDouble(record.get("L"));
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public Chemical(String casNumber,
                        String chemicalName,
                        double molarResponseFactor,
                        double molecularWeight,
                        double overloadMass_1,
                        double e,
                        double s,
                        double a,
                        double l) {
            this.casNumber = casNumber;
            this.chemicalName = chemicalName;
            this.molarResponseFactor = molarResponseFactor;
            this.molecularWeight = molecularWeight;
            this.overloadMass_1 = overloadMass_1;
            this.e = e;
            this.s = s;
            this.a = a;
            this.l = l;
        }

        // Retention Factor is recalculated every 10-50 ms for every peak in the ANALYTES_ON_COLUMN set
        // Retention Factor yields retention time which yields elution time: when the peak will reach the detector.
        private double calcRetentionFactor(){
            double logk = e* MachineSettings.CURRENT_COLUMN.E(MachineSettings.OVEN_TEMPERATURE)
                    + s* MachineSettings.CURRENT_COLUMN.S(MachineSettings.OVEN_TEMPERATURE)
                    + a* MachineSettings.CURRENT_COLUMN.A(MachineSettings.OVEN_TEMPERATURE)
                    + l* MachineSettings.CURRENT_COLUMN.L(MachineSettings.OVEN_TEMPERATURE)
                    + MachineSettings.CURRENT_COLUMN.C(MachineSettings.OVEN_TEMPERATURE);
            double k = Math.pow(10.0,logk);
            return k;
        }

        private double calcRetentionTime(){
            double currentHoldUpTime = MachineSettings.CURRENT_COLUMN.holdUpTime(MachineSettings.getOvenTemperature());
            double retentionFactor = calcRetentionFactor();
            double retentionTime = currentHoldUpTime + (currentHoldUpTime*retentionFactor);
            return retentionTime;
        }
        // See README regarding calculating overloadMass for explanation
        private double adjustedOverloadMass(){
            if (MachineSettings.CURRENT_COLUMN.filmThickness == 1.0) return overloadMass_1;
            else if (MachineSettings.CURRENT_COLUMN.filmThickness >= 0.5) return overloadMass_1/2;
            else if (MachineSettings.CURRENT_COLUMN.filmThickness >= 0.25) return overloadMass_1/4;
            else return overloadMass_1/8;
        }
        private double molecularWeight(){
            return molecularWeight;
        }
        private double molarResponseFactor(){
            return molarResponseFactor;
        }
        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (!(o instanceof Chemical)) return false;
            Chemical otherChem = (Chemical) o;
            return this.chemicalName.equals(otherChem.chemicalName)
                    || this.casNumber.equals(otherChem.casNumber);
        }
        @Override
        public int hashCode(){
            int result = chemicalName.hashCode();
            result = 31 * result + casNumber.hashCode();
            return result;
        }
        // Provide each chemical with a unique color in the UI
        public Color hashColor(){
            double r = Math.abs(chemicalName.hashCode()%100.0/100.0);
            double g = Math.abs(casNumber.hashCode()%100.0/100.0);
            double b = Math.abs(((Double) molecularWeight).hashCode()%100.0/100.0);
            Color hashColor = Color.color(r,g,b, 0.65);
            return hashColor;
        }
        @Override
    	public String toString(){
    		return chemicalName;
        }
    }


    private static class ChemicalView implements Comparable<ChemicalView> {
        private final String casNumber;
        private final SimpleStringProperty chemicalName;
        private final Image chemicalImage;
        private SimpleObjectProperty imageProperty;
        private SimpleDoubleProperty concentration = new SimpleDoubleProperty(0.0);

        public ChemicalView(String casNumber, String chemicalName) {
            this.casNumber = casNumber;
            this.chemicalName = new SimpleStringProperty(chemicalName);
            this.chemicalImage = makeImageView(casNumber + "noName.png").getImage();
            this.imageProperty = new SimpleObjectProperty(chemicalImage);
        }

        public String getCas(){
            return casNumber;
        }
        public String getName(){
            return chemicalName.getValue();
        }
        public SimpleStringProperty nameProperty(){
            return chemicalName;
        }
        public Image getImage(){
            return chemicalImage;
        }
        public SimpleObjectProperty imageProperty(){
            return imageProperty;
        }
        public Double getConcentration() {
            return concentration.getValue();
        }
        public SimpleDoubleProperty concentrationProperty(){
            return concentration;
        }
        public void setConcentration(Double newValue) {
            concentration.set(newValue);
        }
        @Override
        public boolean equals(Object o){
            if (this == o) return true;
            if (!(o instanceof ChemicalView)) return false;
            ChemicalView cV = (ChemicalView) o;
            return getName().equals(cV.getName())
                    && getCas().equals(cV.getCas())
                    && getConcentration().equals(cV.getConcentration());
        }
        @Override
        public int hashCode(){
            int result = casNumber.hashCode();
            result = 31 * result + chemicalName.getValue().hashCode();
            result = 31 * result + concentration.getValue().hashCode();
            return result;
        }
        @Override
        public int compareTo(ChemicalView otherChemView) {
            return Double.compare(this.getConcentration(),
                    otherChemView.getConcentration());
        }
        @Override
        public String toString(){
            return chemicalName.getValue();
        }
    }


    private static class Peak implements Comparable<Peak> {
        private final Chemical analyte;
        private final double peakArea; // recalculated using
        private final double injectionTime;
        private double proportionOfColumnTraversed = 0.0; // Range from 0.0 to 1.0;
        private SimpleDoubleProperty columnTraversedProperty = new SimpleDoubleProperty(0.0);
        private volatile AtomicBoolean isEluting = new AtomicBoolean(false);
        private double elutionTime;
        private GaussianCurve ascendingCurve; // Gaussian curve which draws the ascending half of the peak
        private GaussianCurve descendingCurve; // Gaussian curve which draws the descending half of the peak
        private static final double IDEAL_PEAK_BROADENING_INDEX = 1.0;
        private static final double IDEAL_PEAK_FRONTING_INDEX = 1.0;
        private static final double IDEAL_PEAK_TAILING_INDEX = 1.0;
        private double peakTailingIndex = 1.0;
        private double peakFrontingIndex = 1.0;
        private double peakBroadeningIndex = 1.0;
        private static final double PEAK_BROAD_COEFF = 0.022;// Arbitrarily pegged to a value of peak
                                                            // broadening that looked reasonable for a peak
                                                            // that spent 20 minutes diffusing through column

        private static class Builder{
        // Required Parameters
            // Immutable
            private final Chemical analyte;
            private final double peakArea;
            private final double injectionTime;
            // Mutable (shape can change)
            private GaussianCurve ascendingCurve;
            private GaussianCurve descendingCurve;
            // Curve-shape modulators
            private double peakTailingIndex = 1.0;
            private double peakFrontingIndex = 1.0;
            private double peakBroadeningIndex = 1.0;

            public Builder(Chemical analyte, double peakArea, double injectionTime){
                this.analyte = analyte;
                this.peakArea = peakArea;
                this.injectionTime = injectionTime;
            }
            private Builder ascendingCurve(double amplitude, double timeOfMaxResponse, double peakFrontingIndex)
                {this.ascendingCurve = new GaussianCurve(amplitude, timeOfMaxResponse, peakFrontingIndex); return this;}
            private Builder descendingCurve(double amplitude, double timeOfMaxResponse, double peakTailingIndex)
                {this.descendingCurve = new GaussianCurve(amplitude, timeOfMaxResponse, peakTailingIndex); return this;}
            private Builder peakTailingIndex(double peakTailingIndex)
                {this.peakTailingIndex = peakTailingIndex;                                                 return this;}
            private Builder peakFrontingIndex(double peakFrontingIndex)
                {this.peakFrontingIndex = peakFrontingIndex;                                               return this;}
            private Builder peakBroadeningIndex(double peakBroadeningIndex)
                {this.peakBroadeningIndex = peakBroadeningIndex;                                           return this;}
            private Peak build(){
                return new Peak(this);
            }
        }

        public Peak(Builder builder) {
            this.analyte = builder.analyte;
            this.peakArea = builder.peakArea;
            this.injectionTime = builder.injectionTime;
            this.ascendingCurve = builder.ascendingCurve;
            this.descendingCurve = builder.descendingCurve;
            this.peakFrontingIndex = builder.peakFrontingIndex;
            this.peakTailingIndex = builder.peakTailingIndex;
            elutionTime = builder.analyte.calcRetentionTime() + injectionTime;
        }

        // GETTERS & SETTERS
        public double getColumnTraversed() {
            return columnTraversedProperty.get();
        }
        public DoubleProperty columnTraversedProperty() {
            return columnTraversedProperty;
        }
        public double getElutionTime() {
            return elutionTime;
        }
        public void setColumnTraversed(double proportionOfColumnTraversed) {
            this.columnTraversedProperty.set(proportionOfColumnTraversed);
        }
        public Chemical analyte(){
            return analyte;
        }

    // BEGIN : UPDATE PEAK METHODS-GROUP
        // called on every peak in ANALYTES_ON_COLUMN every 10-50 ms to recalculate the new elutionTime
        // (when the peak will hit the detector) and to change the shape of the peak if necessary
        private void updatePeak(){
            // Update Elution Time
            elutionTime = calcElutionTime();

            // Update Peak Shape
            updatePeakShape();
        }
        public void updateAmplitudes(double peakArea, GaussianCurve ascendingCurve, GaussianCurve descendingCurve){
            double sharedAmplitude = GaussianCurve.sharedAmplitude(peakArea, ascendingCurve, descendingCurve);
            ascendingCurve.amplitude = sharedAmplitude;
            descendingCurve.amplitude = sharedAmplitude;
        }
        public void updatePeakShape(){
            // Update peak widths based on circumstances
            if (MachineSettings.TEMP_COOLING.get()) peakTailingIndex += 0.001;
            peakBroadeningIndex = IDEAL_PEAK_BROADENING_INDEX + analyte.calcRetentionTime()* PEAK_BROAD_COEFF;
            peakBroadeningIndex += MachineSettings.CURRENT_COLUMN_DAMAGE*(Math.random()*5.0); // non-deterministic behavior if column damaged
            ascendingCurve.sigma = GaussianCurve.IDEAL_PEAK_SIGMA*(peakFrontingIndex*peakBroadeningIndex);
            descendingCurve.sigma = GaussianCurve.IDEAL_PEAK_SIGMA*(peakTailingIndex*peakBroadeningIndex);

            // Maintain same peak amplitude, while continuing to sum to correct peakArea
            updateAmplitudes(peakArea, ascendingCurve, descendingCurve);

            // Maintain equal elutionTime
            ascendingCurve.mu = elutionTime;
            descendingCurve.mu = elutionTime;
        }
    // END : UPDATE PEAK METHODS-GROUP

    // BEGIN : TRAVERSAL / ELUTION TIME METHODS GROUP
        // called every 10-50 ms to advance the peak down the column (change the value of proportionOfColumnTraversed)
        private void traverseColumn(){
            if (isEluting.get()) {
                return;
            }
            proportionOfColumnTraversed = Math.min(proportionOfColumnTraversed
                    + traversalProgressPerSimulationStep() , 1.0);
            columnTraversedProperty.set(proportionOfColumnTraversed*100); // Binding these two values together ensures
                                                                            // ensures that solute bands move in the UI
            if (proportionOfColumnTraversed == 1.0) isEluting.set(true);
        }
        private double traversalProgressPerSimulationStep(){
            return FRAME_LENGTH_S/analyte.calcRetentionTime();
        }
        private double proportionOfColumnTraversed(){
            return proportionOfColumnTraversed;
        }

        private double proportionOfColumnUntraversed(){
            return 1.0 - proportionOfColumnTraversed();
        }

        private double simulationStepsRemainingUntilPeakElutes(){
            return Math.ceil(proportionOfColumnUntraversed()
                    /traversalProgressPerSimulationStep());
        }
        private double secondsRemainingUntilPeakElutes(){
            return simulationStepsRemainingUntilPeakElutes() * FRAME_LENGTH_S;
        }
        public double calcElutionTime(){
            elutionTime = currentTime() + secondsRemainingUntilPeakElutes();
            return elutionTime;
        }
    // END : TRAVERSAL / ELUTION TIME METHODS GROUP

        // Method which draws each peak in the UI line graph
        public double plot() {
            if (currentTime() <= elutionTime){
                return ascendingCurve.apply(currentTime());
            }else{
                return descendingCurve.apply(currentTime());
            }
        }
        @Override
        public int compareTo(Peak otherPeak){
            int result = Double.compare(getElutionTime(), otherPeak.getElutionTime());
            if (result == 0) result = Double.compare(peakArea, otherPeak.peakArea);
            if (result == 0) result = Double.compare(analyte.molecularWeight, otherPeak.analyte.molecularWeight);
            if (result == 0) result = analyte.chemicalName.compareTo(otherPeak.analyte.chemicalName);
            return result;
        }
        @Override
        public boolean equals(Object o){
            if (o == this) return true;
            if (!(o instanceof Peak)) return false;
            Peak otherPeak = (Peak) o;
            return otherPeak.analyte.equals(this.analyte)
                    && otherPeak.injectionTime == this.injectionTime; // TODO: 5/23/2023 Possible problems here
        }
        @Override
        public int hashCode(){
            int result = analyte.hashCode();
            result = 31 * result + Double.hashCode(injectionTime);
            return result;
        }

        public String toString(){
            return analyte.toString();
        }
    }


    private static class GaussianCurve implements Function<Double, Double> {
        static final double IDEAL_PEAK_SIGMA = 0.1;
        private double amplitude;
        private double mu;
        private double sigma = IDEAL_PEAK_SIGMA; // width of peak will always be ideal unless changed by circumstances
            // (0) Each peak will have two GaussianCurve instances: ascendingCurve and descendingCurve.
            //     This is necessary because asymmetric curves are possible (peak fronting & peak tailing).
            //
            // (1) Detector Response (amplitude) will be decided based on relative response factor (e.g. the
            //     relative response of the analyte this curve represents compared to the response generated
            //     by 133 ng of methyl-Octanoate (De Saint Laumer et. al 2015).
            //
            // (2) Retention time (mu) will be decided based on SolvationParameter equation from LSER model:
            //     ---    logk = c+eE+sS+aA+lL    ----
            //     NOTE: bB term is excluded because for all columns simulated, scientific data indicates that
            //     b system constant == 0 (Poole et. al 2019)
            //
            // (3) ascendingCurvePeakWidth (sigma) & descendingCurvePeakWidth (sigma) will be estimated based
            //     on a variety of circumstances including


        public GaussianCurve(double amplitude, double mu, double sigma) {
            this.amplitude = amplitude;
            this.mu = mu;
            this.sigma = sigma;
        }

        @Override
            public Double apply(Double currentTime) {
                double exponent = -Math.pow(currentTime - mu, 2) / (2 * Math.pow(sigma, 2));
                return amplitude * Math.exp(exponent);
            }

            // This method and sharedAmplitude() are used for reshaping peak due to broadening/fronting/tailing
            // PeakArea is held constant despite changing shape
            public double calcWidthOfHalfCurve() {
                double scaleY = amplitude * 0.001; // corresponds to 0.1% of peak amplitude

                // Calculate the width from the center (mu) to one side of the
                // curve where y = 0.1% of amplitude of the curve.
                double halfWidth = sigma * Math.sqrt(-2 * Math.log(scaleY / amplitude));

                return halfWidth;
            }
            public static double sharedAmplitude(double asymmetricPeakArea, GaussianCurve ascending, GaussianCurve descending){
                return asymmetricPeakArea / (0.5 * Math.sqrt(2*Math.PI)
                        * (ascending.sigma + descending.sigma));
            }
        }


// UI-MANAGEMENT METHODS & INTERFACE

    // This method called in start() method (which runs after init()) and shows splash screen
    private void showSplash(Stage initStage) {
        Scene splashScene = new Scene(splashScreenPane, SCREEN_BOUNDS.getWidth()*.98, SCREEN_BOUNDS.getHeight()*0.98);
        initStage.setMaximized(true);
        initStage.setScene(splashScene);
        initStage.initStyle(StageStyle.DECORATED);
        initStage.show();
    }

    // Start() method is a centrally important method of JavaFX framework and is called after init() completes.
    // Background tasks are initialized and started first, then rest of the UI components are constructed and placed in
    // the main stage which is what the user sees. UI application thread will begin.
    @Override
    public void start(Stage initStage) {
        // BACKGROUND TASK 1 -- Load All ChemicalViews in background so that user can launch application immediately
        final Task<Void> chemViewLoadingTask = new Task<Void>() {
            @Override
            protected Void call() {
                // Task #1 (unrelated side task: small and fast)
                for (Column column: Column.values()){
                    MachineSettings.columnToDamAndRem.put(column,new Double[]{0.0,1.0});
                }

                // Task #2
                try (FileReader fileReader = new FileReader(CHEM_DATA_FILEPATH);
                     CSVParser parser = CSVFormat.DEFAULT.builder().setHeader(
                             "CAS","chemicalName","SMILES","label","MRF","molecularWeight",
                             "overloadMass_1", "E","S","A","L").build().parse(fileReader)) {
//                    int iterations = 0;
                    for (CSVRecord record : parser) {
//                        if (iterations > 5) break;
                        if (record.getRecordNumber() == 1) continue;
                        String cas = record.get("CAS");
                        String chemicalName = record.get("chemicalName");
                        ChemicalView chemicalView = new ChemicalView(cas,chemicalName);
                        Platform.runLater(() -> obsListChemicalViews.add(chemicalView));
//                      iterations++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(chemViewLoadingTask).start();

        // BACKGROUND TASK 2 -- Cache all Chemical objects to optimize performance of constructing Peak objects during
        // normal application useage
        final Task<Void> chemicalLoadingTask = new Task<Void>() {
            @Override
            protected Void call() {
                try (FileReader fileReader = new FileReader(CHEM_DATA_FILEPATH);
                     CSVParser parser = CSVFormat.DEFAULT.builder().setHeader(
                             "CAS","chemicalName","SMILES","label","MRF","molecularWeight",
                             "overloadMass_1", "E","S","A","L").build().parse(fileReader)) {
                    int iterations = 0;
                    for (CSVRecord record : parser) {
                        if (record.getRecordNumber() == 1) continue;
                        String casNumber = record.get("CAS");
                        String chemicalName = record.get("chemicalName");
                        double molarResponseFactor = 0;
                        double overloadMass_1 = 0;
                        double molecularWeight = 0;
                        double e = 0;
                        double s = 0;
                        double a = 0;
                        double l = 0;
                        molarResponseFactor = Double.parseDouble(record.get("MRF"));
                        molecularWeight = Double.parseDouble(record.get("molecularWeight"));
                        overloadMass_1 = Double.parseDouble(record.get("overloadMass_1"));
                        e = Double.parseDouble(record.get("E"));
                        s = Double.parseDouble(record.get("S"));
                        a = Double.parseDouble(record.get("A"));
                        l = Double.parseDouble(record.get("L"));

                        Chemical chemical = new Chemical(casNumber,
                                chemicalName,
                                molarResponseFactor,
                                molecularWeight,
                                overloadMass_1,
                                e,
                                s,
                                a,
                                l);
                        casToChemical.put(casNumber, chemical);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(chemicalLoadingTask).start();

// UI COMPONENTS

    // DETECTOR LINE CHART
        // XAxis -- Detector Line Chart
        final NumberAxis xAxisDetector = new NumberAxis(0.0,100.0,10.0);
        xAxisDetector.setForceZeroInRange(false);
        xAxisDetector.setLabel("Time (s)");
        xAxisDetector.setAutoRanging(false);

        // YAxis -- Detector Line Chart
        final NumberAxis yAxisDetector = new NumberAxis(0.0,100.0,10.0);
        yAxisDetector.setLabel("Detector Signal (Relative Peak Area Units)");
        yAxisDetector.setAutoRanging(false);
        yAxisDetector.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.0f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });

        // LineChart -- Detector Line Chart
        final LineChart<Number, Number> lineChartDetector = new LineChart<>(xAxisDetector, yAxisDetector);
        lineChartDetector.setCreateSymbols(false);
        lineChartDetector.setAnimated(false);
        lineChartDetector.legendVisibleProperty().set(false);
        lineChartDetector.setCenterShape(true);
        lineChartDetector.setPadding(new Insets(0,40,0,0));
        lineChartDetector.setPrefHeight(SCREEN_BOUNDS.getHeight()*0.75);
        lineChartDetector.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.80);

        // DataSeries -- Detector Line Chart
        XYChart.Series<Number, Number> dataSeriesDetector = new XYChart.Series<>();
        lineChartDetector.getData().add(dataSeriesDetector);
        dataSeriesDetector.getNode().setStyle("-fx-stroke-width: 1; -fx-stroke: #1E90FF;");

    // SOLUTE BANDS TRAVERSAL LINE CHART
        // XAxis -- Solute Bands Traversal Line Chart
        final NumberAxis xAxisSolBand = new NumberAxis(0.0,100,10);

        // YAxis -- Solute Bands Traversal Line Chart
        final NumberAxis yAxisSolBand = new NumberAxis(0.0,1,0);
        yAxisSolBand.setLabel("Solute Bands");
        yAxisSolBand.setVisible(false);
        yAxisSolBand.setTickLabelsVisible(false);

        // Line Chart -- Solute Bands Traversal Line Chart
        lineChartSolBand = new LineChart<>(xAxisSolBand,yAxisSolBand);
        lineChartSolBand.applyCss();
        Node chartBackground = lineChartSolBand.lookup(".chart-plot-background");
        chartBackground.setStyle("-fx-background-color: #1e8fff20;");
        lineChartSolBand.setCreateSymbols(false);
        lineChartSolBand.setAnimated(false);
        lineChartSolBand.legendVisibleProperty().set(false);
        lineChartSolBand.setVerticalGridLinesVisible(false);
        lineChartSolBand.setPadding(new Insets(0,40,0,0));
        lineChartSolBand.setMaxHeight(SCREEN_BOUNDS.getHeight()*0.15);
        lineChartSolBand.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.80);

        // ROOT BORDERPANE -- All other UI elements contained inside this element
        root = new BorderPane();
          BackgroundFill backgroundFill = new BackgroundFill(
            Color.FLORALWHITE,
            CornerRadii.EMPTY,
            Insets.EMPTY);
        root.setBackground(new Background(backgroundFill));

        VBox leftControls = new VBox(10);
          leftControls.setPadding(new Insets(10,20,10,30));
          leftControls.setAlignment(Pos.CENTER);
        VBox centerDisplay = new VBox();
          centerDisplay.setPadding(new Insets(10));
          centerDisplay.setAlignment(Pos.CENTER_RIGHT);
          centerDisplay.getChildren().addAll(lineChartSolBand,lineChartDetector);
        root.setCenter(centerDisplay);
        root.setLeft(leftControls);

        // PAUSE/PLAY BUTTON
        FontIcon pause = new FontIcon(FontAwesome.PAUSE);
        pause.setIconColor(Color.BLACK);
        pause.setIconSize(24);
        FontIcon play = new FontIcon(FontAwesome.PLAY);
        play.setIconColor(Color.DODGERBLUE);
        play.setIconSize(22);
        Button simulationStateButton = new Button();
        simulationStateButton.setGraphic(play);
        simulationStateButton.setPrefWidth(140);
        simulationStateButton.setPrefHeight(45);
            // Action
            simulationStateButton.setOnAction(e -> {
                isPaused.set(!isPaused.get());

                if (isPaused.get()) {
                    simulationStateButton.setGraphic(play);
                } else {
                    simulationStateButton.setGraphic(pause);
                }
            });

        // RESTART SIMULATION BUTTON
        Button restartButton = new Button();
        FontIcon restartIcon = new FontIcon(FluentUiFilledAL.ARROW_HOOK_UP_LEFT_16);
        restartIcon.setIconColor(Color.BLACK);
        restartIcon.setIconSize(32);
        restartButton.setGraphic(restartIcon);
        restartButton.setPrefWidth(140);
        restartButton.setPrefHeight(45);
        restartButton.setOnAction(e -> {
            Alert confirmRestart = new Alert(Alert.AlertType.CONFIRMATION);
            confirmRestart.getDialogPane().setPrefWidth(SCREEN_BOUNDS.getWidth()*0.20);
            confirmRestart.setHeaderText("Restart Simulation?");
            Button okButton = (Button) confirmRestart.getDialogPane().lookupButton(ButtonType.OK);
            Button cancelButton = (Button) confirmRestart.getDialogPane().lookupButton(ButtonType.CANCEL);
            okButton.setDefaultButton(false);
            cancelButton.setDefaultButton(true);
            Optional<ButtonType> choice = confirmRestart.showAndWait();
            if (choice.get().equals(ButtonType.OK)){
                restartSimulation();
                dataSeriesDetector.getData().clear();
            };
        });

        // ELUTION TIMES BUTTON
        Button elutionTimesButton = new Button("get ElutionTimes");
            // Action
            elutionTimesButton.setOnAction(e -> {
            for(Peak peak : MachineSettings.ANALYTES_IN_COLUMN){
                System.out.print(peak.analyte.toString() + " ");
                System.out.print(String.format("%.1f", peak.getElutionTime()) + " eTime");
                System.out.println();
                System.out.println();
              System.out.println(" ProportionTraversed = " + String.format("%.2f",peak.proportionOfColumnTraversed()));
            }
                System.out.println("SIZE = " + MachineSettings.ANALYTES_IN_COLUMN.size());
                System.out.println();
        });

        // ColumnDamage Button
        Button columnDamage = new Button("column damage");
            // Action
            columnDamage.setOnAction(e -> {
            System.out.println("Current Damage = " + MachineSettings.CURRENT_COLUMN_DAMAGE);
        });

        // ColumnDamage Button
        Button columnRem = new Button("column Rem");
            // Action
            columnRem.setOnAction(e -> {
            System.out.println("Column Rem = " + MachineSettings.CURRENT_COLUMN_REMAINING);
        });

        // SET OVEN-TEMP BUTTON
        FontIcon thermometer0 = new FontIcon(FontAwesome.THERMOMETER_0);
        FontIcon thermometer1 = new FontIcon(FontAwesome.THERMOMETER_1);
        FontIcon thermometer2 = new FontIcon(FontAwesome.THERMOMETER_2);
        FontIcon thermometer3 = new FontIcon(FontAwesome.THERMOMETER_3);
        FontIcon thermometer4 = new FontIcon(FontAwesome.THERMOMETER_4);
        thermometer0.setIconColor(Color.CRIMSON);
        thermometer0.setIconSize(30);
        thermometer1.setIconColor(Color.CRIMSON);
        thermometer1.setIconSize(30);
        thermometer2.setIconColor(Color.CRIMSON);
        thermometer2.setIconSize(30);
        thermometer3.setIconColor(Color.CRIMSON);
        thermometer3.setIconSize(30);
        thermometer4.setIconColor(Color.CRIMSON);
        thermometer4.setIconSize(30);
        Button setOvenTempButton = new Button();
        setOvenTempButton.setGraphic(thermometer0);
        setOvenTempButton.setFont(Font.font(null, FontWeight.EXTRA_BOLD, 20));
        SimpleStringProperty colonSetOvenTemp = new SimpleStringProperty(" : ");
        setOvenTempButton.textProperty().bind(colonSetOvenTemp.concat(MachineSettings.ovenTempProperty.asString("%.0f")));
        MachineSettings.ovenTempProperty.addListener(e -> {
            if (MachineSettings.ovenTempProperty.doubleValue() >= 330.0) {setOvenTempButton.setGraphic(thermometer4); return;}
            if (MachineSettings.ovenTempProperty.doubleValue() >= 280.0) {setOvenTempButton.setGraphic(thermometer3); return;}
            if (MachineSettings.ovenTempProperty.doubleValue() >= 180.0) {setOvenTempButton.setGraphic(thermometer2); return;}
            if (MachineSettings.ovenTempProperty.doubleValue() >= 90.0) {setOvenTempButton.setGraphic(thermometer1); return;}
            if (MachineSettings.ovenTempProperty.doubleValue() < 90.0) {setOvenTempButton.setGraphic(thermometer0); return;}
        });
        setOvenTempButton.setPrefWidth(140);
        setOvenTempButton.setPrefHeight(45);
            // Action
            setOvenTempButton.setOnAction(e -> {
                TextInputDialog inputDialog = new TextInputDialog();
                inputDialog.setTitle("Set Oven Temperature");
                inputDialog.setHeaderText(null);
                inputDialog.setContentText("Enter Temperature:");
                inputDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

                TextField inputField = inputDialog.getEditor();
                Label validationLabel = new Label("Enter Integer Value: 25 - 350");
                validationLabel.setTextFill(Color.CRIMSON);
                validationLabel.setVisible(false);
                SimpleDoubleProperty validNumber = new SimpleDoubleProperty(inputField, "inputValue");

                Label info = new Label("Tip: The oven temperature(s) you select for your method are of primary importance for ensuring good separation of peaks. Low temperatures cause chemicals to spend more time in the column. High temperatures speed them through. The selectivity parameters of your columns change with temperature as well. Play around!");
                info.setWrapText(true);
                info.setTextFill(Color.DODGERBLUE);
                info.setFont(Font.font(null, FontPosture.ITALIC, 14));
                info.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);

                inputDialog.getDialogPane().setContent(new VBox(2, validationLabel, inputField, info));

                // Input Validation
                inputField.textProperty().addListener((observable, oldValue, newValue) -> {
                    boolean valid = false;
                    if (!newValue.matches("\\d+")) {
                        validationLabel.setVisible(true);
                    } else {
                        int temp = Integer.parseInt(newValue);
                        if (temp < 25 || temp > 350) {
                            validationLabel.setVisible(true);
                        } else {
                            validationLabel.setVisible(false);
                            valid = true;
                        }
                    }
                    inputDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
                });

                // Show the input dialog and get the entered value
                inputDialog.showAndWait().ifPresent(value -> {
                    int tempTarget = 0;
                        tempTarget = Integer.parseInt(value);
                    // Set temp ramping
                    if (tempTarget > MachineSettings.OVEN_TEMPERATURE){
                        MachineSettings.OVEN_TEMPERATURE_TARGET = Integer.parseInt(value);
                        MachineSettings.TEMP_RAMPING.set(true);
                        MachineSettings.TEMP_COOLING.set(false);
                    }
                    // Set temp cooling
                    if (tempTarget < MachineSettings.OVEN_TEMPERATURE){
                        MachineSettings.OVEN_TEMPERATURE_TARGET = Integer.parseInt(value);
                        MachineSettings.TEMP_COOLING.set(true);
                        MachineSettings.TEMP_RAMPING.set(false);
                    }
                });
            });
            // "Sensor" Reading
            ProgressBar progressBar = new ProgressBar();
            progressBar.setPrefWidth(140);
            progressBar.progressProperty().bind(MachineSettings.ovenTempProperty.divide((350)));
            progressBar.setStyle("-fx-accent: crimson");
        // Wrapper
        VBox setOvenTempVBox = new VBox(setOvenTempButton,progressBar);
        setOvenTempVBox.setAlignment(Pos.CENTER);

        // SPLIT RATIO BUTTON
        FontIcon splitRatio = FontIcon.of(MaterialDesign.MDI_CALL_SPLIT);
        splitRatio.setIconSize(30);
        splitRatio.setIconColor(Color.DODGERBLUE);
        Button splitRatioButton = new Button();
        splitRatioButton.setGraphic(splitRatio);
        splitRatioButton.setFont(Font.font(null, FontWeight.EXTRA_BOLD, 20));
        SimpleStringProperty colon = new SimpleStringProperty(" : ");
        splitRatioButton.textProperty().bind(colon.concat(MachineSettings.splitRatioProperty.asString("%.0f")));
        splitRatioButton.setPrefWidth(140);
        splitRatioButton.setPrefHeight(45);
            // Action
            splitRatioButton.setOnAction(e -> {
                TextInputDialog inputDialog = new TextInputDialog();
                inputDialog.setTitle("Set Split Ratio");
                inputDialog.setHeaderText(null);
                inputDialog.setContentText("Enter Split Ratio:");
                inputDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

                TextField inputField = inputDialog.getEditor();
                Label validationLabel = new Label("Enter Integer Value: 1 - 500");
                validationLabel.setTextFill(Color.CRIMSON);
                validationLabel.setVisible(false);
                SimpleDoubleProperty validNumber = new SimpleDoubleProperty(inputField, "inputValue");

                Label info = new Label("Tip: A split ratio of 126 means that only 1 out of every 127 gas particles injected into the inlet actually enters the column. The other 126 are \"split off\" and vented to waste. This reduces the mass of each analyte that enters the column. This can prevent asymmetric peak shapes caused by column overloading. Also, peaks will be smaller.");
                info.setWrapText(true);
                info.setTextFill(Color.DODGERBLUE);
                info.setFont(Font.font(null, FontPosture.ITALIC, 14));
                info.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);

                inputDialog.getDialogPane().setContent(new VBox(2, validationLabel, inputField, info));

                // Input Validation
                inputField.textProperty().addListener((observable, oldValue, newValue) -> {
                    boolean valid = false;
                    if (!newValue.matches("\\d+")) {
                        validationLabel.setVisible(true);
                    } else {
                        int temp = Integer.parseInt(newValue);
                        if (temp < 1 || temp > 500) {
                            validationLabel.setVisible(true);
                        } else {
                            validationLabel.setVisible(false);
                            valid = true;
                        }
                    }
                    inputDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
                });

                // Show the input dialog and get the entered value
                inputDialog.showAndWait().ifPresent(value -> {
                    MachineSettings.SPLIT_RATIO = Integer.parseInt(value);
                    Platform.runLater(() ->{
                        MachineSettings.splitRatioProperty.set(MachineSettings.SPLIT_RATIO);
                    });
                });
            });

    // BEGIN INJECT STAGE COMPONENT
    // BEGIN INJECT STAGE COMPONENT

        // INJECT BUTTON
        FontIcon needle = FontIcon.of(MaterialDesign.MDI_NEEDLE);
        needle.setIconColor(Color.DODGERBLUE);
        needle.setIconSize(34);
        Button injectButton = new Button("", needle);
        injectButton.setPrefWidth(140);
        injectButton.setPrefHeight(45);
        injectButton.setOnAction(e1 -> {
            if (!isPaused.get()) simulationStateButton.fire();
            Stage injectStage = new Stage(StageStyle.DECORATED);
            injectStage.initOwner(mainStage);
            injectStage.initModality(Modality.APPLICATION_MODAL);
            injectStage.setTitle("Injection");
            // Ensure that concentrations return to 0 if injectStage closed unexpectedly
            injectStage.setOnCloseRequest(e -> {
                for (ChemicalView chemView : obsListChemicalViews){
                    chemView.setConcentration(0.0);
                }
            });

            // CUSTOM SAMPLE MIXTURE PANE -- TABLEVIEW
                // CUSTOM SAMPLE MIXTURE PANE -- SEARCH FILTERS
                FilteredList<ChemicalView> filteredData1 = new FilteredList<>(obsListChemicalViews, s -> true);
                FilteredList<ChemicalView> filteredData2 = new FilteredList<>(filteredData1, s -> true);
                FilteredList<ChemicalView> filteredData3 = new FilteredList<>(filteredData2, s -> true);
                FilteredList<ChemicalView> filteredData4 = new FilteredList<>(filteredData3, s -> true);

                TextField searchField1 = new TextField();
                searchField1.setPromptText("Try typing \"ox\" <AND FILTER>...");
                searchField1.textProperty().addListener(obs->{
                    String filter = searchField1.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData1.setPredicate(s -> true);
                    }
                    else {
                        filteredData1.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
                    }
                });

                TextField searchField2 = new TextField();
                searchField2.setPromptText("Try typing \"dehyde\" <AND FILTER>...");
                searchField2.textProperty().addListener(obs->{
                    String filter = searchField2.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData2.setPredicate(s -> true);
                    }
                    else {
                        filteredData2.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
                    }
                });
                TextField searchField3 = new TextField();
                searchField3.setPromptText("Try typing \"ethoxy\" <NOT FILTER>...");
                searchField3.textProperty().addListener(obs->{
                    String filter = searchField3.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData3.setPredicate(s -> true);
                    }
                    else {
                        filteredData3.setPredicate(s -> !(s.getName().toLowerCase().contains(filter.toLowerCase())));
                    }
                });
                TextField searchField4 = new TextField();
                searchField4.setPromptText("Try typing \"cyclo\" <NOT FILTER>...");
                searchField4.textProperty().addListener(obs->{
                    String filter = searchField4.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData4.setPredicate(s -> true);
                    }
                    else {
                        filteredData4.setPredicate(s -> !(s.getName().toLowerCase().contains(filter.toLowerCase())));
                    }
                });
                // -- END SEARCH FILTERS

            TableView<ChemicalView> tableViewCustomSampleMixture = new TableView<ChemicalView>(filteredData4);
            tableViewCustomSampleMixture.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            // DEFINE SAMPLE MIX COLUMNS
            // NAME COLUMN
            TableColumn<ChemicalView, String> nameColumn1 = new TableColumn<>("Chemical Name");
            nameColumn1.setCellValueFactory(cellData -> {
                return cellData.getValue().nameProperty();
            });
            nameColumn1.styleProperty().set("-fx-alignment: CENTER; -fx-wrap-text: true;");

            // IMAGE COLUMN
            TableColumn<ChemicalView, ImageView> imageColumn = new TableColumn<>("Structure");
            imageColumn.setCellValueFactory(cellData -> {
                Image chemicalViewImage = cellData.getValue().getImage();
                ImageView imageView = new ImageView(chemicalViewImage);
                imageView.setPreserveRatio(true);
                imageView.fitWidthProperty().bind(cellData.getTableColumn().widthProperty().multiply(0.8));
                return new SimpleObjectProperty<>(imageView);
            });
            tableViewCustomSampleMixture.getColumns().addAll(nameColumn1, imageColumn);
            // -- END TABLEVIEW

            // DEFINE SAMPLE MIXTURE PANE -- BUTTONS
            VBox defineCustomMixtureButtonsRoot = new VBox();
            defineCustomMixtureButtonsRoot.setSpacing(5);

                // SHOW/HIDE STRUCTURES BUTTON
                Button showHideStructuresButton = new Button("Hide Structures");
                BooleanProperty isStructureColumnVisible = new SimpleBooleanProperty(true);
                imageColumn.visibleProperty().bind(isStructureColumnVisible);
                showHideStructuresButton.setOnAction(e -> {
                    isStructureColumnVisible.set(!isStructureColumnVisible.get());
                    if (isStructureColumnVisible.get()) {
                        showHideStructuresButton.setText("Hide Structures");
                    } else {
                        showHideStructuresButton.setText("Show Structures");
                    }
                 });

                // SELECT ALL DISPLAYED BUTTON
                Button selectAllButton = new Button("Select All Displayed");
                selectAllButton.setOnAction(e ->{
                    tableViewCustomSampleMixture.getSelectionModel().clearSelection();
                    for (ChemicalView chemView : filteredData4){
                        tableViewCustomSampleMixture.getSelectionModel().select(chemView);
                    }
                });

                // TIPS BUTTON
                Button defineCustomMixTipsButton = new Button("Tips");
                defineCustomMixTipsButton.setAlignment(Pos.CENTER);
                FontIcon tip1 = new FontIcon(FontAwesome.LIGHTBULB_O);
                tip1.setIconSize(30);
                tip1.setIconColor(Color.DODGERBLUE);
                defineCustomMixTipsButton.setGraphic(tip1);
                defineCustomMixTipsButton.setFont(Font.font(null,FontWeight.EXTRA_BOLD,15));
                defineCustomMixTipsButton.setOnAction(e->{
                    Alert defineCustomMixInfo = new Alert(Alert.AlertType.INFORMATION);
                    defineCustomMixInfo.getDialogPane().setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                    defineCustomMixInfo.setTitle("Tips");
                    defineCustomMixInfo.setHeaderText("Tips For Setting Your Sample Mixture");
                    Label content = new Label("""
                            (1) Try the AND and NOT filters
                            (2) (HOLD)CTRL + CLICK to select multiple chemicals one at a time
                            (3) (HOLD)SHIFT + CLICK to select multiple chemicals all at once
                            (4) Resize the Structure Column by CLICK-N-DRAG on the column heading edges
                            (5) Hide the Structure Column for easier selection"""
                    );
                    content.setWrapText(true);
                    content.setTextFill(Color.DODGERBLUE);
                    content.setFont(Font.font(null, FontPosture.ITALIC, 14));
                    content.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                    defineCustomMixInfo.getDialogPane().setContent(content);
                    defineCustomMixInfo.show();
                });

            defineCustomMixtureButtonsRoot.getChildren().addAll(
                    defineCustomMixTipsButton,
                    createVSpacer(),
                    showHideStructuresButton,
                    createVSpacer(),
                    searchField1,
                    searchField2,
                    searchField3,
                    searchField4,
                    selectAllButton,
                    createVSpacer());
            defineCustomMixtureButtonsRoot.setPadding(new Insets(10));
            defineCustomMixtureButtonsRoot.setAlignment(Pos.CENTER);
            SplitPane defineSampMixSplitPane = new SplitPane(tableViewCustomSampleMixture, defineCustomMixtureButtonsRoot);
            defineSampMixSplitPane.setDividerPosition(0,0.69);
            defineSampMixSplitPane.setPadding(new Insets(10));


            // SET CONCENTRATIONS PANE -- TABLEVIEW
            ObservableList<ChemicalView> chemicalViewsInSampleMixture = FXCollections.observableArrayList();
                // SET CONCENTRATIONS PANE -- SEARCH FILTERS
                FilteredList<ChemicalView> filteredData5 = new FilteredList<>(chemicalViewsInSampleMixture, s -> true);
                FilteredList<ChemicalView> filteredData6 = new FilteredList<>(filteredData5, s -> true);
                FilteredList<ChemicalView> filteredData7 = new FilteredList<>(filteredData6, s -> true);
                FilteredList<ChemicalView> filteredData8 = new FilteredList<>(filteredData7, s -> true);

                TextField searchField5 = new TextField();
                searchField5.setPromptText("<AND FILTER>...");
                searchField5.textProperty().addListener(obs->{
                    String filter = searchField5.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData5.setPredicate(s -> true);
                    }
                    else {
                        filteredData5.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
                    }
                });

                TextField searchField6 = new TextField();
                searchField6.setPromptText("<AND FILTER>...");
                searchField6.textProperty().addListener(obs->{
                    String filter = searchField6.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData6.setPredicate(s -> true);
                    }
                    else {
                        filteredData6.setPredicate(s -> s.getName().toLowerCase().contains(filter.toLowerCase()));
                    }
                });
                TextField searchField7 = new TextField();
                searchField7.setPromptText("<NOT FILTER>...");
                searchField7.textProperty().addListener(obs->{
                    String filter = searchField7.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData7.setPredicate(s -> true);
                    }
                    else {
                        filteredData7.setPredicate(s -> !(s.getName().toLowerCase().contains(filter.toLowerCase())));
                    }
                });
                TextField searchField8 = new TextField();
                searchField8.setPromptText("<NOT FILTER>...");
                searchField8.textProperty().addListener(obs->{
                    String filter = searchField8.getText();
                    if(filter == null || filter.length() == 0) {
                        filteredData8.setPredicate(s -> true);
                    }
                    else {
                        filteredData8.setPredicate(s -> !(s.getName().toLowerCase().contains(filter.toLowerCase())));
                    }
                });
                // -- END SEARCH FILTERS
            TableView<ChemicalView> tableViewSetConcentrations = new TableView<ChemicalView>(filteredData8);

            tableViewSetConcentrations.setEditable(true);
            tableViewSetConcentrations.getSelectionModel().setCellSelectionEnabled(true);
            tableViewSetConcentrations.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            tableViewSetConcentrations.setPlaceholder(makeImageView("sorryNoData.png"));

            // SET CONCENTRATIONS COLUMNS
                // NAME COLUMN
                TableColumn<ChemicalView, String> nameColumn2 = new TableColumn<>("Chemical Name");
                nameColumn2.prefWidthProperty().bind(tableViewSetConcentrations.widthProperty().multiply(0.35));
                nameColumn2.setStyle("-fx-alignment: CENTER");
                nameColumn2.setCellValueFactory(cell -> {
                    return cell.getValue().nameProperty();
                });

                // CONCENTRATION COLUMN
                TableColumn<ChemicalView, Double> concentrationColumn = new TableColumn<>("% Conc.");
                concentrationColumn.prefWidthProperty().bind(tableViewSetConcentrations.widthProperty().multiply(0.15));
                concentrationColumn.setStyle(" -fx-font-size: 15; -fx-font-weight: bold; -fx-alignment: CENTER");
                concentrationColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().concentrationProperty().asObject();
                });
                concentrationColumn.setEditable(false);

                // IMAGE COLUMN
                TableColumn<ChemicalView, ImageView> imageColumn2 = new TableColumn<>("Structure");
                imageColumn2.setCellValueFactory(cellData -> {
                    Image chemicalViewImage = cellData.getValue().getImage();
                    ImageView imageView = new ImageView(chemicalViewImage);
                    imageView.setPreserveRatio(true);
                    imageView.fitWidthProperty().bind(cellData.getTableColumn().widthProperty().multiply(0.8));
                    return new SimpleObjectProperty<>(imageView);
                });
            tableViewSetConcentrations.getColumns().addAll(nameColumn2, imageColumn2, concentrationColumn);
            // -- END TABLEVIEW

            // SET CONCENTRATIONS PANE -- BUTTONS (+CARVE-OUT FROM DEFINE SAMPLE MIXTURE PANE)
            // BUTTONS ROOT
            VBox setConcButtonsVBoxRoot = new VBox();
            setConcButtonsVBoxRoot.setSpacing(5);
                // -- BEGIN CARVE-OUT: ADD SELECTED BUTTON
                Button addSelectedChemsButton = new Button("Add Selected To Sample Mixture");
                addSelectedChemsButton.setWrapText(true);
                addSelectedChemsButton.textAlignmentProperty().set(TextAlignment.CENTER);
                addSelectedChemsButton.setTextFill(Color.DODGERBLUE);
                addSelectedChemsButton.setFont(Font.font(null,FontWeight.BOLD,15));
                    Label helperLabelAddSel = new Label ("Next: Navigate to \"Set Concentrations\" Tab Below");
                    helperLabelAddSel.setTextFill(Color.DODGERBLUE);
                    helperLabelAddSel.setFont(Font.font(null, FontPosture.ITALIC, 10));
                    RotateTransition rtHelperLabel = new RotateTransition(Duration.millis(110), helperLabelAddSel);
                    rtHelperLabel.setAutoReverse(true);
                    rtHelperLabel.setByAngle(2.5);
                    rtHelperLabel.setCycleCount(2);
                addSelectedChemsButton.setOnAction(e->{
                    chemicalViewsInSampleMixture.addAll(FXCollections.observableArrayList(
                            tableViewCustomSampleMixture
                                    .getSelectionModel()
                                    .getSelectedItems()
                    ));
                    chemicalViewsInSampleMixture.setAll(chemicalViewsInSampleMixture.stream().distinct().collect(Collectors.toList()));
                    tableViewSetConcentrations.setItems(filteredData8);
                    rtHelperLabel.play();
                });
                defineCustomMixtureButtonsRoot.getChildren().addAll(addSelectedChemsButton, helperLabelAddSel, createVSpacer());
                // -- END CARVE-OUT

                // SET ALL SELECTED TO BUTTON
                HBox setValToSelectedHBox = new HBox(5);
                setValToSelectedHBox.setAlignment(Pos.CENTER);
                    SimpleDoubleProperty concentrationInputsSum = new SimpleDoubleProperty(0.0);
                    Button setAllSelectedButton = new Button("Set All SELECTED To:");
                    setAllSelectedButton.setWrapText(true);
                    setAllSelectedButton.textAlignmentProperty().set(TextAlignment.CENTER);
                    setAllSelectedButton.setTextFill(Color.DODGERBLUE);
                    setAllSelectedButton.setFont(Font.font(null,FontWeight.BOLD,12));
                    TextField setAllSelectedInputField = new TextField();
                    setAllSelectedInputField.setOnAction(e -> {
                        setAllSelectedButton.fire();
                    });
                    setAllSelectedInputField.setOnKeyPressed(keyPress -> {
                        if (keyPress.getCode().equals(KeyCode.ESCAPE)) {
                            setAllSelectedInputField.textProperty().set("");
                            tableViewSetConcentrations.getSelectionModel().clearSelection();
                        }
                    });
                    setAllSelectedButton.setOnAction(e-> {
                        ObservableList<ChemicalView> selectedInputs =
                            tableViewSetConcentrations
                            .getSelectionModel()
                            .getSelectedItems();

                        try {
                            for (ChemicalView chemView : selectedInputs){
                                    if (setAllSelectedInputField.textProperty().getValue().trim().equals("")) break;
                                    double value = Double.parseDouble(setAllSelectedInputField.textProperty().getValue().trim());
                                    if (value >= 0.0 && value <= 100.0) chemView.concentrationProperty().set(value);
                                    else throw new Exception();
                            }
                            double sum = 0.0;
                            for (ChemicalView chemView : filteredData8){
                                sum += chemView.getConcentration().doubleValue();
                            }
                            concentrationInputsSum.set(sum);
                        } catch (Exception ex) {
                            Alert validationWarning = new Alert(Alert.AlertType.ERROR);
                            validationWarning.getDialogPane().setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            validationWarning.setTitle("Invalid Concentration");
                            validationWarning.setHeaderText("Enter Value: 0.0 - 100.0");
                            Label content = new Label("Values are % weight of 1 microliter injection");
                            content.setWrapText(true);
                            content.setTextFill(Color.DODGERBLUE);
                            content.setFont(Font.font(null, FontPosture.ITALIC, 14));
                            content.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            validationWarning.getDialogPane().setContent(content);
                            validationWarning.show();
                        }
                    });
                setValToSelectedHBox.getChildren().addAll(setAllSelectedButton, setAllSelectedInputField);

                // SET ALL DISPLAYED TO BUTTON
                HBox setValForAllHBox = new HBox(5);
                setValForAllHBox.setAlignment(Pos.CENTER);
                    Button setAllDisplayedButton = new Button("Set All DISPLAYED To:");
                    setAllDisplayedButton.setWrapText(true);
                    setAllDisplayedButton.textAlignmentProperty().set(TextAlignment.CENTER);
                    setAllDisplayedButton.setTextFill(Color.DODGERBLUE);
                    setAllDisplayedButton.setFont(Font.font(null,FontWeight.BOLD,12));
                    TextField setValAllInputField = new TextField();
                    setValAllInputField.setOnKeyPressed(keyPress -> {
                        if (keyPress.getCode().equals(KeyCode.ESCAPE)) {
                            setValAllInputField.textProperty().set("");
                            tableViewSetConcentrations.getSelectionModel().clearSelection();
                        }
                    });
                    setValAllInputField.setOnAction(e -> {
                        setAllDisplayedButton.fire();
                    });
                    setAllDisplayedButton.setOnAction(e->{
                        try {
                            for (ChemicalView chemView : filteredData8){
                                if (setValAllInputField.textProperty().getValue().trim().equals("")) break;
                                double value = Double.parseDouble(setValAllInputField.textProperty().getValue().trim());
                                if (value >= 0.0 && value <= 100.0) chemView.concentrationProperty().set(value);
                                else throw new Exception();
                            }
                            double sum = 0.0;
                            for (ChemicalView chemView : filteredData8){
                                sum += chemView.getConcentration().doubleValue();
                            }
                            concentrationInputsSum.set(sum);
                        } catch (Exception ex) {
                            Alert validationWarning = new Alert(Alert.AlertType.ERROR);
                            validationWarning.getDialogPane().setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            validationWarning.setTitle("Invalid Concentration");
                            validationWarning.setHeaderText("Enter Value: 0.0 - 100.0");
                            Label content = new Label("Values are % weight of 1 microliter injection");
                            content.setWrapText(true);
                            content.setTextFill(Color.DODGERBLUE);
                            content.setFont(Font.font(null, FontPosture.ITALIC, 14));
                            content.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            validationWarning.getDialogPane().setContent(content);
                            validationWarning.show();
                        }
                    });
                setValForAllHBox.getChildren().addAll(setAllDisplayedButton, setValAllInputField);

                // REMOVE SELECTED BUTTON
                Button removeSelectedButton = new Button("Remove Selected");
                removeSelectedButton.setOnAction(e->{
                    // Remove them
                    ObservableList<ChemicalView> selectedInputs =
                            tableViewSetConcentrations
                                    .getSelectionModel()
                                    .getSelectedItems();
                    List<ChemicalView> toRemove = new ArrayList<>(selectedInputs);
                    for (ChemicalView chemView : toRemove){
                        chemView.setConcentration(0.0);
                    }
                    chemicalViewsInSampleMixture.removeAll(toRemove);

                    // Recalculate concentration inputs
                        double sum = 0.0;
                        for (ChemicalView chemView : filteredData8){
                            sum += chemView.getConcentration().doubleValue();
                        }
                        concentrationInputsSum.set(sum);
                });

                // REMOVE ALL BUTTON
                Button removeAllButton = new Button("Remove All");
                removeAllButton.setOnAction(e->{
                    for (ChemicalView chemView : chemicalViewsInSampleMixture){
                        chemView.setConcentration(0.0);
                    }
                    chemicalViewsInSampleMixture.clear();
                    concentrationInputsSum.set(0);
                });

                // TIPS BUTTON
                Button setConcTipsButton = new Button("Tips");
                setConcTipsButton.setAlignment(Pos.CENTER);
                FontIcon tip2 = new FontIcon(FontAwesome.LIGHTBULB_O);
                tip2.setIconSize(30);
                tip2.setIconColor(Color.DODGERBLUE);
                setConcTipsButton.setGraphic(tip2);
                setConcTipsButton.setFont(Font.font(null,FontWeight.EXTRA_BOLD,15));
                setConcTipsButton.setOnAction(e->{
                    Alert defineCustomMixInfo = new Alert(Alert.AlertType.INFORMATION);
                    defineCustomMixInfo.getDialogPane().setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                    defineCustomMixInfo.setTitle("Tips");
                    defineCustomMixInfo.setHeaderText("Tips For Setting Concentrations");
                    Label content = new Label("(1) First, define your Sample Mixture above (otherwise, you'll see a sad little guy)"
                            + "\n(2) Set values using the buttons below"
                            + "\n(3) Use the ESCAPE key to clear your current selections"
                            + "\n(4) If your peaks come out asymmetrical, consider reduced concentrations"
                    );
                    content.setWrapText(true);
                    content.setTextFill(Color.DODGERBLUE);
                    content.setFont(Font.font(null, FontPosture.ITALIC, 14));
                    content.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                    defineCustomMixInfo.getDialogPane().setContent(content);
                    defineCustomMixInfo.show();
                });

                // SHOW/HIDE STRUCTURES BUTTON
                Button showHideStructuresButton2 = new Button("Hide Structures");
                BooleanProperty isStructureColumnVisible2 = new SimpleBooleanProperty(true);
                imageColumn2.visibleProperty().bind(isStructureColumnVisible2);
                showHideStructuresButton2.setOnAction(e -> {
                    isStructureColumnVisible2.set(!isStructureColumnVisible2.get());
                    if (isStructureColumnVisible2.get()) {
                        showHideStructuresButton2.setText("Hide Structures");
                    } else {
                        showHideStructuresButton2.setText("Show Structures");
                    }
                });

                Label helperLabelConcSet = new Label ("When Finished: Click \"Finalize Sample\" on the Right");
                helperLabelConcSet.setTextFill(Color.DODGERBLUE);
                helperLabelConcSet.setFont(Font.font(null, FontPosture.ITALIC, 14));
            // BUTTONS ROOT
            setConcButtonsVBoxRoot.getChildren().addAll(
                    setConcTipsButton,
                    createVSpacer(),
                    showHideStructuresButton2,
                    createVSpacer(),
                    searchField5,
                    searchField6,
                    searchField7,
                    searchField8,
                    createVSpacer(),
                    setValToSelectedHBox,
                    setValForAllHBox,
                    createVSpacer(),
                    new HBox(7, removeSelectedButton, removeAllButton){
                        {setAlignment(Pos.CENTER);}
                    },
                    createVSpacer(),
                    helperLabelConcSet,
                    createVSpacer());
            setConcButtonsVBoxRoot.setPadding(new Insets(10));
            setConcButtonsVBoxRoot.setAlignment(Pos.CENTER);
            SplitPane setConcSplitPane = new SplitPane(tableViewSetConcentrations, setConcButtonsVBoxRoot);
            setConcSplitPane.setPadding(new Insets(10));
            setConcSplitPane.setDividerPosition(0,0.60);
            // END SET CONCENTRATIONS BUTTONS

            // SAMPLE DEFINITION ACCORDION (ROOT OF LEFT SIDE OF SPLIT PANE)
            Accordion sampleDefinitionMenus = new Accordion();
                // DEFINE CUSTOM SAMPLE MIXTURE PANE
                TitledPane defineSampMixTitlePane = new TitledPane("Define Custom Sample Mixture", defineSampMixSplitPane);
                    // Create the custom title for the TitledPane
                    Label sampMixTitle = new Label();
                    sampMixTitle.setTextFill(Color.BLACK);

                    // If chemViewLoadingTask hasn't completed, softly warn user
                    BooleanBinding chemViewloadingBinding = chemViewLoadingTask.stateProperty()
                            .isNotEqualTo(Worker.State.SUCCEEDED);
                    sampMixTitle.textProperty().bind(
                            Bindings.when(chemViewloadingBinding)
                                    .then("(Warning: Some Chemicals Still Loading!!)")
                                    .otherwise("")
                    );
                    sampMixTitle.styleProperty().set("-fx-font-weight: bold; -fx-text-fill: blue;");
                    defineSampMixTitlePane.setGraphic(sampMixTitle);
                    defineSampMixTitlePane.setContentDisplay(ContentDisplay.RIGHT);
                // SET CONCENTRATIONS PANE
                TitledPane setConcTitlePane = new TitledPane("Set Concentrations", setConcSplitPane);
                // ADD PRESET SAMPLE MIXTURES PANE
                TitledPane addPresetsTitlePane = new TitledPane("Add Preset Sample Mixtures", new Button("B3"));
            sampleDefinitionMenus.getPanes().addAll(defineSampMixTitlePane, setConcTitlePane, addPresetsTitlePane);


            // Create finalizingButtons BorderPane
            BorderPane finalizingButtons = new BorderPane();
            // Top -- User Input Info
            VBox topInfo = new VBox();
            topInfo.setPadding(new Insets(0,15,15,15));
            HBox topInfoValues = new HBox();
            topInfoValues.setPadding(new Insets(10,0,0,0));
            topInfoValues.setStyle("-fx-font-size: 20px");
            Label analyteValueLabel = new Label("TOTAL %WEIGHT: ");
            analyteValueLabel.setTextFill(Color.DODGERBLUE);
            Label loadValue = new Label();
              loadValue.textProperty().addListener(e -> {
                  if (concentrationInputsSum.doubleValue() > 100.0) {
                      loadValue.setTextFill(Color.CRIMSON);

                  } else {
                      loadValue.setTextFill(Color.DODGERBLUE);
                  }
              });
            loadValue.setStyle("-fx-font-weight: bold");
            loadValue.textProperty().bind(concentrationInputsSum.asString("%.2f"));
            topInfoValues.getChildren().addAll(analyteValueLabel,loadValue);
            topInfo.getChildren().addAll(topInfoValues);
            topInfo.setAlignment(Pos.TOP_CENTER);
            topInfoValues.setAlignment(Pos.CENTER);
            finalizingButtons.setTop(topInfo);
            BorderPane.setAlignment(topInfo, Pos.CENTER);
            // Center -- Injection MiniGame
            StackPane injectionMinigame = new StackPane();
            injectionMinigame.setPadding(new Insets(10));
            // Create the targetBox
            Rectangle targetBox = new Rectangle(29, 150);
            targetBox.setFill(null);
            targetBox.setStroke(Color.BLACK);
            targetBox.setStrokeWidth(2.0);
            targetBox.setX(150);
            targetBox.setY(50);
            targetBox.setArcHeight(10.0);
            targetBox.setArcWidth(10.0);
            // Create the oscillator
            Rectangle oscillator = new Rectangle(13, 140, Color.BLACK);
            oscillator.setX(150);
            oscillator.setY(50);
            oscillator.setArcHeight(10.0);
            oscillator.setArcWidth(10.0);
            // Set up the oscillator's back and forth translation
            TranslateTransition oscillatorTranslator = new TranslateTransition(Duration.millis(900), oscillator);
            oscillatorTranslator.setFromX(-150);
            oscillatorTranslator.setToX(150);
            oscillatorTranslator.setCycleCount(Animation.INDEFINITE);
            oscillatorTranslator.setAutoReverse(true);
            oscillatorTranslator.setInterpolator(Interpolator.LINEAR);
            // Tick Mark Background Image
            ImageView tickmarkImage = makeImageView("tickmarks.png");
            tickmarkImage.setFitWidth(SCREEN_BOUNDS.getWidth()*0.20);
            // Set up the Injection MiniGame
            injectionMinigame.getChildren().addAll(tickmarkImage,targetBox,oscillator);
            finalizingButtons.setCenter(injectionMinigame);
            // Bottom -- All Clear Buttons
            VBox bottomInfo = new VBox();
            bottomInfo.setSpacing(5);
            bottomInfo.setPadding(new Insets(15,15,55,15));
            Button finalizeButton = new Button("Finalize Sample");
            finalizeButton.setPadding(new Insets(10,50,10,50));
            BorderPane.setAlignment(finalizeButton, Pos.CENTER);
            SimpleBooleanProperty isFinalized = new SimpleBooleanProperty(finalizeButton,"isFinalized",false);
            SimpleBooleanProperty soluteBandsOn = new SimpleBooleanProperty(true);
            finalizeButton.setDisable(true);
            finalizeButton.setOnAction(e3 -> {
            // First, warn against potential performance issues with too many solute bands being initialized
                // Count up how many total solute bands there will be after this injection
                int sizeOfUserInputs = 0;
                for(ChemicalView chemical : chemicalViewsInSampleMixture){
                    if (chemical.getConcentration() == 0) continue;
                    sizeOfUserInputs++;
                }
                int currentNumSoluteBands = peakToSolBandChangeListener.values().size();
                int newTotalSoluteBands = sizeOfUserInputs + currentNumSoluteBands;

                // Warn user if there is a potential issue but allow them to choose caution or safety
                if (newTotalSoluteBands > 500 && finalizeButton.getText().equals("Finalize Sample")){
                    Alert performanceWarning = new Alert(Alert.AlertType.WARNING);
                    performanceWarning.getDialogPane().setPrefWidth(injectStage.getWidth()*0.51);
                    performanceWarning.setHeaderText("Warning: Performance Issues Likely!");

                    // Buttons in alert
                    performanceWarning.getButtonTypes().add(ButtonType.APPLY);
                    Button safeProceedButton = (Button) performanceWarning.getDialogPane().lookupButton(ButtonType.APPLY);
                    safeProceedButton.setText("Safe: Solute Bands OFF");
                    safeProceedButton.setWrapText(true);

                    Button cautiousProceedButton = (Button) performanceWarning.getDialogPane().lookupButton(ButtonType.OK);
                    cautiousProceedButton.setText("Caution: Solute Bands ON");
                    cautiousProceedButton.setWrapText(true);

                    performanceWarning.getButtonTypes().add(ButtonType.CANCEL);
                    Button cancelButton = (Button) performanceWarning.getDialogPane().lookupButton(ButtonType.CANCEL);
                    cancelButton.setText("Cancel");
                    cancelButton.setWrapText(true);

                    cautiousProceedButton.setDefaultButton(false);
                    safeProceedButton.setDefaultButton(true);

                    // Content of Alert
                    Label content = new Label("Displaying "
                            + (newTotalSoluteBands)
                            + " solute bands may cause performance issues (aim for < 500)!"
                            + "\nIf you TURN OFF solute bands, no performance issues will occur."
                            + "\nUsing 1x or 2x simulation speed improves performance significantly."
                            );
                    content.setWrapText(true);
                    content.setTextFill(Color.DODGERBLUE);
                    content.setFont(Font.font(null, FontPosture.REGULAR, 12));
                    content.setMaxWidth(injectStage.getWidth());
                    performanceWarning.getDialogPane().setContent(content);

                    // Handle user Choice inside Alert
                    Optional<ButtonType> choice = performanceWarning.showAndWait();
                    if (choice.get().equals(ButtonType.APPLY)){
                        soluteBandsOn.set(false); // This will lead to solute bands being deleted and not initialized
                    } if (choice.get().equals(ButtonType.CANCEL)){
                        return;
                    };
                }

                if(finalizeButton.getText().equals("Finalize Sample")){
                    finalizeButton.setText("Edit Sample");
                    setConcTitlePane.setDisable(!setConcTitlePane.isDisabled());
                    defineSampMixTitlePane.setDisable(!defineSampMixTitlePane.isDisabled());
                    addPresetsTitlePane.setDisable(!addPresetsTitlePane.isDisabled());
                    isFinalized.set(true);
                    oscillatorTranslator.play();
                    return;
                }
                if(finalizeButton.getText().equals("Edit Sample")){
                    finalizeButton.setText("Finalize Sample");
                    setConcTitlePane.setDisable(!setConcTitlePane.isDisabled());
                    defineSampMixTitlePane.setDisable(!defineSampMixTitlePane.isDisabled());
                    addPresetsTitlePane.setDisable(!addPresetsTitlePane.isDisabled());
                    isFinalized.set(false);
                    oscillatorTranslator.stop();
                    soluteBandsOn.set(true); // ensure true because user may have changed their mind; don't want to
                                            // delete bands later undesirably
                    return;
                }
            });
            Button techniqueButton = new Button("Set Technique Score");
            DoubleProperty techniqueScore = new SimpleDoubleProperty(techniqueButton, "techniqueScore",0.0);
            techniqueButton.setPadding(new Insets(10,50,10,50));
            BorderPane.setAlignment(techniqueButton, Pos.CENTER);
            techniqueButton.setOnAction(e4 -> {
                Platform.runLater(oscillatorTranslator::stop);
                double techniqueScoreCalc = Math.abs(oscillator.getBoundsInParent().getCenterX()
                        - targetBox.getBoundsInParent().getCenterX());
                techniqueScore.setValue(techniqueScoreCalc);
                if(techniqueScoreCalc < 19.0){
                    oscillator.setFill(Color.GREEN);
                }else{
                    oscillator.setFill(Color.GREY);
                }
            });
            techniqueButton.disableProperty().bind(techniqueScore.greaterThan(0.0)
                    .or(isFinalized.not()));
            finalizeButton.disableProperty().bind(techniqueScore.greaterThan(0.0)
                            .or(concentrationInputsSum.isEqualTo(0))
                            .or(concentrationInputsSum.greaterThan(100.0)));
            Button allClearButton = new Button("All Clear: Inject");
            allClearButton.setPadding(new Insets(10,50,10,50));
            SimpleBooleanProperty allClearClicked = new SimpleBooleanProperty(false);
            BorderPane.setAlignment(allClearButton, Pos.CENTER);
                allClearButton.disableProperty().bind(
                    concentrationInputsSum.greaterThan(100.0)
                    .or(techniqueScore.isEqualTo(0))
                    .or(allClearClicked)); // used for disabling immediately to prevent double clicks
            allClearButton.setOnAction(e5 -> {
                // disable the all clear button (no double clicks)
                allClearClicked.set(true);

                // Pause if its not paused
                if (!isPaused.get()){
                    simulationStateButton.fire();
                }

                // Delete all the solute bands if user input indicates as much
                if (!soluteBandsOn.get()){
                    peakToSolBandChangeListener.clear();
                    peakToSolBandDataSeries.clear();
                    solBandDataSeriesToPeak.clear();
                    lineChartSolBand.getData().clear();
                    soluteBandCountProperty.set(0);
                }

                // Identify all chemicals the user actually input a concentration value for
                for(ChemicalView chemicalView : chemicalViewsInSampleMixture){
                    if (chemicalView.getConcentration() == 0) continue;
                    else finalUserInputs_ChemViews.add(chemicalView);
                }

                // Initialize peaks for finalUserInputs (do computationally intensive parts as background task)
                final Task<Void> initializePeaksTask = new Task<Void>() {
                    protected Void call() {
                        List<Peak> finalUserInputs_Peaks = new ArrayList<>();
                        try (FileReader fileReader = new FileReader(CHEM_DATA_FILEPATH);
                             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader(
                                     "CAS","chemicalName","SMILES","label","MRF","molecularWeight",
                                     "overloadMass_1", "E","S","A","L").build().parse(fileReader)) {

                            int iterations = 0;
                            for (CSVRecord record : parser){
                                String chemicalNameOfInput;
                                String chemicalNameOfRecord;
                                Chemical analyte;
                                double percentWeight;
                                double mgAnalyteInjectedToInlet;
                                double gAnalyteEnteringColumn;
                                double ngAnalyteEnteringColumn;
                                double molecularWeightAnalyte;
                                double molesAnalyteEnteringColumn;
                                double MRFAnalyte;
                                double peakArea;
                                double injectionTime;
                                double elutionTime;
                                double columnAdjustedOverloadMass;
                                double peakFrontingIndex;
                                double peakTailingIndex;
                                // Skip first row
                                if (record.getRecordNumber() == 1) continue;
                                chemicalNameOfRecord = record.get("chemicalName");

                                // CSV Parser finds a chemicalName for each record; next, we iterate through
                                // finalUserInputs and if finalUserInputs contains that chemicalName, we build a Peak
                                // from that Chemical
                                for (ChemicalView input : finalUserInputs_ChemViews) {
                                    chemicalNameOfInput = input.getName();
                                    if (!chemicalNameOfRecord.equals(chemicalNameOfInput)) continue; // no match? next

                                    // Construct analyte if it is not found in casToChemical top level hashmap
                                    // (casToChemical finishes populating within 1-2s of application launch)
                                    String casNumber = input.getCas();
                                    if (casToChemical.containsKey(casNumber)) analyte = casToChemical.get(casNumber);
                                    else analyte = new Chemical(chemicalNameOfInput);

                                    percentWeight = input.getConcentration()/100.0;
                                    // percentWeight = %Weight of 1 uL injection
                                    // (total weight assumed 1 mg / uL; densities NOT factored in)
                                    mgAnalyteInjectedToInlet = percentWeight*MachineSettings.INJECTION_VOLUME;
                                    ngAnalyteEnteringColumn = mgAnalyteInjectedToInlet*1_000_000.0 / MachineSettings.SPLIT_RATIO;
                                    gAnalyteEnteringColumn = ngAnalyteEnteringColumn/1_000_000_000.0;
                                    molecularWeightAnalyte = analyte.molecularWeight();
                                    molesAnalyteEnteringColumn = (gAnalyteEnteringColumn/molecularWeightAnalyte);
                                    MRFAnalyte = analyte.molarResponseFactor();

                                    // Define peak's characteristics
                                    peakArea = molesAnalyteEnteringColumn*MRFAnalyte*MRF_PROPORTIONALITY_CONST;
                                    injectionTime = currentTime();
                                    elutionTime = injectionTime+analyte.calcRetentionTime();
                                    columnAdjustedOverloadMass = analyte.adjustedOverloadMass();

                                    // Adjust Peak Shape
                                    peakFrontingIndex = Peak.IDEAL_PEAK_FRONTING_INDEX;
                                    if (ngAnalyteEnteringColumn > columnAdjustedOverloadMass){
                                        peakFrontingIndex = Peak.IDEAL_PEAK_FRONTING_INDEX +
                                            (ngAnalyteEnteringColumn/columnAdjustedOverloadMass);
                                    }
                                    peakTailingIndex = Peak.IDEAL_PEAK_TAILING_INDEX; // TODO: 5/30/2023
                                    if (MachineSettings.IS_COLUMN_CUT_POORLY) {
                                        peakTailingIndex = 4.1*Math.random(); // TODO: 5/3/2023 IMPLEMENT THIS WELL EVENTUALLY
                                    }

                                    // Build Peak
                                    Peak currentPeak = new Peak.Builder(analyte, peakArea, injectionTime)
                                            .ascendingCurve(peakArea, elutionTime, GaussianCurve.IDEAL_PEAK_SIGMA)
                                            .descendingCurve(peakArea, elutionTime, GaussianCurve.IDEAL_PEAK_SIGMA)
                                            .peakFrontingIndex(peakFrontingIndex)
                                            .peakTailingIndex(peakTailingIndex)
                                            .build();
                                    finalUserInputs_Peaks.add(currentPeak);
                                }
                                iterations++;
                                updateProgress(iterations, finalUserInputs_ChemViews.size());
                            }
                            // Add all userInputPeaks to Top-Level Field of Simulator
                            MachineSettings.ANALYTES_IN_COLUMN.addAll(finalUserInputs_Peaks);
                        } catch(IOException e){
                            throw new RuntimeException("Bro, your csv datafile is messed up", e);
                        }

                        // Create Solute Bands for Monitoring Progress inside Column Visually
                        List<XYChart.Series<Number, Number>> listSoluteBandSeries = new ArrayList<>();
                        for (Peak peak : finalUserInputs_Peaks) {
                            if (soluteBandsOn.get()) { // Total solute bands recommended < 500 -- performance limit
                                // Create data series (soluteBandView) made of 2 datapoints: one at y = 0 and the other
                                // at y = 1 (this forms a vertical line that runs the entire height of the y-axis)
                                XYChart.Series<Number, Number> soluteBandView = new XYChart.Series<>();
                                soluteBandView.getData().add(new XYChart.Data<>(peak.proportionOfColumnTraversed(), 0));
                                soluteBandView.getData().add(new XYChart.Data<>(peak.proportionOfColumnTraversed(), 1));

                                // Listen for changes in value of columnTraversedProperty (DoubleProperty) and change
                                // the x-values of both datapoints in the soluteBandView to be equal to the value of
                                // the columnTraversalProperty (this moves the soluteBandView along the x-axis).
                                ChangeListener<Number> listener = (observable, oldValue, newValue) -> {
                                    soluteBandView.getData().get(0).setXValue(newValue);
                                    soluteBandView.getData().get(1).setXValue(newValue);
                                };
                                WeakChangeListener<Number> weakListener = new WeakChangeListener<>(listener);
                                peak.columnTraversedProperty().addListener(weakListener);

                                // Store the strong reference to the weak listener in a structure where they can be found
                                // and clear()'ed easily for garbage collection to avoid memory leaks
                                peakToSolBandChangeListener.put(peak, listener);
                                peakToSolBandDataSeries.put(peak,soluteBandView);
                                solBandDataSeriesToPeak.put(soluteBandView,peak);
                                listSoluteBandSeries.add(soluteBandView);
                            }
                        }
                        // Add all solute band series into lineChart on JavaFX App thread to prevent concurrency issues
                        Platform.runLater(() -> {
                            lineChartSolBand.getData().addAll(listSoluteBandSeries);
                            int currentCount = soluteBandCountProperty.get();
                            soluteBandCountProperty.set(currentCount + listSoluteBandSeries.size());

                            // OBLIGATE: Style of each series can ONLY be set after it has been added to its linechart.
                            // Set Style: Each Solute Band is backed by a Chemical; that Chemical has a unique color
                            for (XYChart.Series series : lineChartSolBand.getData()){
                                Peak peak = solBandDataSeriesToPeak.get(series);
                                Color chemicalColor = peak.analyte().hashColor();
                                String hexadecimalColor = toHexString(chemicalColor);
                                series.getNode().setStyle("-fx-stroke-width: 7; -fx-opacity: 0.65; " +
                                        "-fx-stroke: " + hexadecimalColor + ";");
                            }
                        });

                        // After injection complete, reset all conc to 0 and clear finalUserInputs
                        for (ChemicalView chemView : chemicalViewsInSampleMixture){
                            chemView.setConcentration(0.0);
                        }
                        finalUserInputs_ChemViews.clear();
                        return null;
                    }
                };
                new Thread(initializePeaksTask).start();

                // If initializePeaksTask takes a long time, loadingStage will be seen by viewer for a short while
                Stage loadingStage = new Stage(StageStyle.TRANSPARENT);
                loadingStage.initOwner(mainStage);
                loadingStage.initModality(Modality.APPLICATION_MODAL);
                initializePeaksTask.setOnRunning(e -> {
                    Platform.runLater(() -> {
                        injectStage.close();
                        // Overlay greys out mainstage
                        StackPane overlayPane = new StackPane();
                        overlayPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.3);"); // Adjust the opacity as needed
                        overlayPane.setPrefSize(mainStage.getWidth(), mainStage.getHeight());
                        overlayPane.setMouseTransparent(true);
                        // Loading animation
                        ImageView loadingPeaks = makeImageView("peaksLoading.gif");
                        loadingPeaks.setPreserveRatio(true);
                        loadingPeaks.fitWidthProperty().set(SCREEN_BOUNDS.getWidth()*0.18);
                        // Loading progress bar
                        ProgressBar initPeaksLoadingBar = new ProgressBar();
                            initPeaksLoadingBar.setStyle("-fx-accent: #ffaf00");
                            initPeaksLoadingBar.setVisible(true);
                            initPeaksLoadingBar.setPrefWidth(SCREEN_BOUNDS.getWidth()*0.16);
                            initPeaksLoadingBar.progressProperty().bind(initializePeaksTask.progressProperty());
                        // VBox contains progress bar and loading animation
                        VBox column = new VBox(2,loadingPeaks,initPeaksLoadingBar);
                            column.setAlignment(Pos.CENTER);
                        // Root of loadingStage
                        StackPane loadingStackPane = new StackPane();
                           loadingStackPane.getChildren().addAll(overlayPane, column);
                           loadingStackPane.setBackground(Background.EMPTY);
                        Scene loadingScene = new Scene(loadingStackPane);
                        loadingScene.setFill(Color.TRANSPARENT);
                        loadingStage.setScene(loadingScene);
                        loadingStage.show();
                    });
                });
                initializePeaksTask.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        loadingStage.close();
                        injectStage.close();
                    });
                });
            });
            bottomInfo.getChildren().addAll(finalizeButton,techniqueButton, allClearButton);
            finalizingButtons.setBottom(bottomInfo);
            bottomInfo.setAlignment(Pos.CENTER);

            // Add ScrollPane and Vbox to SplitPane
            SplitPane splitPane = new SplitPane();
            splitPane.setDividerPosition(0,0.75);
            splitPane.getItems().addAll(sampleDefinitionMenus,finalizingButtons);

            // Create the Scene and add the ScrollPane to it
            Scene injectScene = new Scene(splitPane,SCREEN_BOUNDS.getWidth()*0.80, SCREEN_BOUNDS.getHeight()*0.80);
                // Scene Event Handlers
                tableViewSetConcentrations.setOnKeyPressed(keypress -> {
                    if (keypress.getCode().equals(KeyCode.ESCAPE))
                        tableViewSetConcentrations.getSelectionModel().clearSelection();
                });
                tableViewCustomSampleMixture.setOnKeyPressed(keypress -> {
                    if (keypress.getCode().equals(KeyCode.ESCAPE))
                        tableViewCustomSampleMixture.getSelectionModel().clearSelection();
                });
            injectStage.setScene(injectScene);
            injectStage.show();
        });

    // END INJECT STAGE COMPONENT
    // END INJECT STAGE COMPONENT


        // DETECTOR-OFF BUTTON
        Button detectorOnOffButton = new Button();
        FontIcon fire = FontIcon.of(FontAwesome.FIRE);
        fire.setIconColor(Color.DODGERBLUE);
        fire.setIconSize(24);
        Label fid = new Label("FID");
        fid.setFont(Font.font(null,FontWeight.BOLD,11));
        HBox fidDetector = new HBox(fid,fire);
        fidDetector.setAlignment(Pos.CENTER);
        detectorOnOffButton.setGraphic(fidDetector);
        detectorOnOffButton.setOnAction(e -> {
            MachineSettings.isDetectorOn.set(MachineSettings.isDetectorOn.not().get());
            if (MachineSettings.isDetectorOn.get()) {
                fire.setIconColor(Color.DODGERBLUE);
            }else{
                fire.setIconColor(Color.BLACK);
            }
        });

        // SWITCH COLUMN BUTTON
        Button switchColumnButton = new Button();
        FontIcon column = FontIcon.of(MaterialDesign.MDI_BLUR_LINEAR); // MDI CLOCK START
        column.setIconColor(Color.DODGERBLUE);
        column.setIconSize(34);
        Label columnName = new Label(MachineSettings.CURRENT_COLUMN.toString());
        columnName.setFont(Font.font(null,FontWeight.BOLD,10));
        columnName.setTextFill(Color.DODGERBLUE);
        VBox switchColumnButtonGraphic = new VBox(column,columnName/*,forward*/);
        switchColumnButtonGraphic.setSpacing(0);
        switchColumnButtonGraphic.setAlignment(Pos.CENTER);
        switchColumnButton.setGraphic(switchColumnButtonGraphic);
        switchColumnButton.setPrefWidth(140);
        switchColumnButton.setPrefHeight(45);
        switchColumnButton.setOnAction( e1 -> {
                ChoiceDialog<Column> columnChoices = new ChoiceDialog<>();

                columnChoices.setTitle("Choose A Column");
                SimpleBooleanProperty noneSelected = new SimpleBooleanProperty();
                noneSelected.bind(columnChoices.selectedItemProperty().isNull());

                columnChoices.getItems().addAll(Column.SPB_OCTYL,
                        Column.HP_5,Column.HP_INNOWAX, Column.DB_1701,
                        Column.DB_225, Column.RTX_440, Column.RTX_OPP,
                        Column.RXI_5SIL, Column.RXI_17);

                // Only allow the column to be changed if oven is cold enough and FID detector is off and
                // a selection has been made
                Button installColumnButton = (Button) columnChoices.getDialogPane().lookupButton(ButtonType.OK);
                installColumnButton.setText("Install Column");
                installColumnButton.disableProperty()
                        .bind(MachineSettings.ovenTempProperty.greaterThan(40.0)
                        .or(noneSelected));

                columnChoices.getDialogPane().setPrefWidth(750);

                VBox columnChoicesRoot = new VBox();
                columnChoicesRoot.setAlignment(Pos.BOTTOM_CENTER);
                columnChoicesRoot.setPrefWidth(Region.USE_PREF_SIZE);
                columnChoicesRoot.setPadding(new Insets(20,10,0,10));
                columnChoicesRoot.setSpacing(3);
                    // Grid of Columns
                    GridPane columnChoicesGrid = new GridPane();
                    columnChoicesGrid.setAlignment(Pos.BOTTOM_CENTER);
                    columnChoicesGrid.setVgap(10);
                    columnChoicesGrid.setHgap(10);

                    FontIcon column1 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column1.setIconSize(55);
                    Label column1Label = new Label("DB-1701\nMax-Temp: 280\nLength: 30 m\nFilm-Thickness: 1.0 um");
                    column1Label.setAlignment(Pos.CENTER);
                    column1Label.setTextFill(Color.DODGERBLUE);
                    column1Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox db1701 = new VBox(column1,column1Label);
                    db1701.setPadding(new Insets(7));
                    db1701.setAlignment(Pos.CENTER);
                    Button db1701Button = new Button();
                    db1701Button.setGraphic(db1701);
                    db1701Button.setOnAction(e -> {
                        Alert db1701Info = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) db1701Info.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.DB_1701);
                        });
                        db1701Info.getDialogPane().setPrefWidth(450);
                        db1701Info.setHeaderText("DB-1701");
                        ImageView stationaryPhase = makeImageView("DB-1701_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("14%: (poly)cyanopropylphenylsiloxane 86%: (poly)dimethylsiloxane");
                        Label eInfo = new Label("Very Slight e-LonePair/Pi-Pi selection");
                        Label sInfo = new Label("Moderate Dipole/Dipole Selection");
                        Label aInfo = new Label("Moderate H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        db1701Info.getDialogPane().setContent(selectivityInfo);
                        db1701Info.show();
                        // Engineered to be the same as HP-5, but with better thermal stability (higher maxTemp)
                        // e: e- lone-pair interactions (e-0.201 -> e0.067) VERY SLIGHT RT INCREASE ; Label= Very Slight e-LonePair/Pi-Pi selection
                        // s: Dipole/Dipole interactions (s0.877 -> s0.371) MODERATE RT INCREASE ; Label= Moderate Dipole/Dipole Selection
                        // a: H-Bond accepting interactions (a0.984 -> a0.301) MODERATE RT INCREASE ; Label= Moderate H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.703 -> l0.244) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column2 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column2.setIconSize(55);
                    Label column2Label = new Label("DB-225\nMax-Temp: 240\nLength: 15 m\nFilm-Thickness: 0.25 um");
                    column2Label.setTextFill(Color.DODGERBLUE);
                    column2Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox db225 = new VBox(column2,column2Label);
                    db225.setPadding(new Insets(7));
                    db225.setAlignment(Pos.BOTTOM_CENTER);
                    Button db225Button = new Button();
                    db225Button.setGraphic(db225);
                    db225Button.setOnAction(e -> {
                        Alert db225Info = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) db225Info.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.DB_225);
                        });
                        db225Info.getDialogPane().setPrefWidth(450);
                        db225Info.setHeaderText("DB-225");
                        ImageView stationaryPhase = makeImageView("DB-225_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("50%: (poly)cyanopropylphenylsiloxane 50%: (poly)dimethylsiloxane");
                        Label eInfo = new Label("Very Slight e-LonePair/Pi-Pi selection");
                        Label sInfo = new Label("Strong Dipole/Dipole selection");
                        Label aInfo = new Label("Strong H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        db225Info.getDialogPane().setContent(selectivityInfo);
                        db225Info.show();
                        // Engineered to be the same as HP-5, but with better thermal stability (higher maxTemp)
                        // e: e- lone-pair interactions (e-0.149 -> e0.109) VERY SLIGHT RT INCREASE ; Label= Very Slight e-LonePair/Pi-Pi selection
                        // s: Dipole/Dipole interactions (s1.636 -> s0.735) STRONG RT INCREASE ; Label= Strong Dipole/Dipole selection
                        // a: H-Bond accepting interactions (a1.595 -> a0.617) STRONG RT INCREASE ; Label= Strong H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.632 -> l0.226) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection

                    });

                    FontIcon column3 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column3.setIconSize(55);
                    Label column3Label = new Label("HP-5\nMax-Temp: 350\nLength: 30 m\nFilm-Thickness: 0.1 um");
                    column3Label.setTextFill(Color.DODGERBLUE);
                    column3Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox hp5 = new VBox(column3,column3Label);
                    hp5.setPadding(new Insets(7));
                    hp5.setAlignment(Pos.BOTTOM_CENTER);
                    Button hp5Button = new Button();
                    hp5Button.setGraphic(hp5);
                    hp5Button.setOnAction(e -> {
                        Alert hp5Info = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) hp5Info.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.HP_5);
                        });
                        hp5Info.getDialogPane().setPrefWidth(450);
                        hp5Info.setHeaderText("HP-5");
                        ImageView stationaryPhase = makeImageView("HP-5_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("5%: (poly)diphenylsiloxane 95%: (poly)dimethylsiloxane");
                        Label eInfo = new Label("Very Slight e-LonePair/Pi-Pi selection");
                        Label sInfo = new Label("Mild Dipole/Dipole selection");
                        Label aInfo = new Label("Slight H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        hp5Info.getDialogPane().setContent(selectivityInfo);
                        hp5Info.show();
                        // INFO:
                        // e: e- lone-pair interactions (e-0.191 -> e0.099) VERY SLIGHT RT INCREASE ; label= Very Slight e-LonePair/Pi-Pi selection
                        // s: Dipole/Dipole interactions (s0.436 -> s0.135) MILD RT INCREASE ; Label= Mild Dipole/Dipole selection
                        // a: H-Bond accepting interactions (a0.380 -> a0.112) SLIGHT RT INCREASE ; Label= Slight H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.735 -> l0.210) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column4 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column4.setIconSize(55);
                    Label column4Label = new Label("Rxi-17\nMax-Temp: 320\nLength: 30 m\nFilm-Thickness: 0.5 um");
                    column4Label.setTextFill(Color.DODGERBLUE);
                    column4Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox rxi17 = new VBox(column4,column4Label);
                    rxi17.setPadding(new Insets(7));
                    rxi17.setAlignment(Pos.BOTTOM_CENTER);
                    Button rxi17Button = new Button();
                    rxi17Button.setGraphic(rxi17);
                    rxi17Button.setOnAction(e -> {
                        Alert rxi17Info = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) rxi17Info.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.RXI_17);
                        });
                        rxi17Info.getDialogPane().setPrefWidth(450);
                        rxi17Info.setHeaderText("Rxi-17");
                        ImageView stationaryPhase = makeImageView("Rxi-17_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("50%: (poly)diphenylsiloxane 50%: (poly)dimethylsiloxane");
                        Label eInfo = new Label("Slight e-LonePair/Pi-PI selection");
                        Label sInfo = new Label("Moderate Dipole/Dipole Selection");
                        Label aInfo = new Label("Mild H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        rxi17Info.getDialogPane().setContent(selectivityInfo);
                        rxi17Info.show();
                        // Engineered to be the same as HP-5, but with better thermal stability (higher maxTemp)
                        // e: e- lone-pair interactions (e0.006 -> e0.182) SLIGHT RT INCREASE ; Label= Slight e-LonePair/Pi-Pi selection
                        // s: Dipole/Dipole interactions (s0.977 -> s0.380) MODERATE RT INCREASE ; Label= Moderate Dipole/Dipole Selection
                        // a: H-Bond accepting interactions (a0.597 -> a0.180) MILD RT INCREASE ; Label= Mild H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.692 -> l0.240) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column5 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column5.setIconSize(55);
                    Label column5Label = new Label("Rxi-5Sil\nMax-Temp: 320\nLength: 30 m\nFilm-Thickness: 0.5 um");
                    column5Label.setTextFill(Color.DODGERBLUE);
                    column5Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox rxi5Sil = new VBox(column5,column5Label);
                    rxi5Sil.setPadding(new Insets(7));
                    rxi5Sil.setAlignment(Pos.BOTTOM_CENTER);
                    Button rxi5SilButton = new Button();
                    rxi5SilButton.setGraphic(rxi5Sil);
                    rxi5SilButton.setOnAction(e -> {
                        Alert rxi5SilInfo = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) rxi5SilInfo.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.RXI_5SIL);
                        });
                        rxi5SilInfo.getDialogPane().setPrefWidth(450);
                        rxi5SilInfo.setHeaderText("Rxi-5Sil");
                        ImageView stationaryPhase = makeImageView("Rxi-5Sil_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("5%: 1,4-Bis(dimethylsiloxy)phenylene 95%: (poly)dimethylsiloxane");
                        Label eInfo = new Label("Slight e-LonePair/Pi-PI selection");
                        Label sInfo = new Label("Mild Dipole/Dipole selection");
                        Label aInfo = new Label("Slight H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        rxi5SilInfo.getDialogPane().setContent(selectivityInfo);
                        rxi5SilInfo.show();
                        // Engineered to be the same as HP-5, but with better thermal stability (higher maxTemp)
                        // e: e- lone-pair interactions (e-0.094 -> e0.111) SLIGHT RT INCREASE ; Label= Slight e-LonePair/Pi-PI selection
                        // s: Dipole/Dipole interactions (s0.458 -> s0.121) MILD RT INCREASE ; Label= Mild Dipole/Dipole selection
                        // a: H-Bond accepting interactions (a0.535 -> a0.133) LOW TEMP: MILD RT INCREASE ; Label= Low-Temp: Mild H-Bond selection
                                                                            // HIGH TEMP: SLIGHT RT INCREASE ; Label= High-Temp: Slight H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.739 -> l0.185) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column6 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column6.setIconSize(55);
                    Label column6Label = new Label("HP-Innowax\nMax-Temp: 260\nLength: 60 m\nFilm-Thickness: 1.0 um");
                    column6Label.setTextFill(Color.DODGERBLUE);
                    column6Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox hpInnowax = new VBox(column6,column6Label);
                    hpInnowax.setPadding(new Insets(7));
                    hpInnowax.setAlignment(Pos.BOTTOM_CENTER);
                    Button hpInnowaxButton = new Button();
                    hpInnowaxButton.setGraphic(hpInnowax);
                    hpInnowaxButton.setOnAction(e -> {
                        Alert hpInnowaxInfo = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) hpInnowaxInfo.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.HP_INNOWAX);
                        });
                        hpInnowaxInfo.getDialogPane().setPrefWidth(450);
                        hpInnowaxInfo.setHeaderText("HP-Innowax");
                        ImageView stationaryPhase = makeImageView("HP-Innowax_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("100% Polyethyleneglycol (cross-linking not shown)");
                        Label eInfo = new Label("Slight e-LonePair/Pi-Pi selection");
                        Label sInfo = new Label("Strong Dipole/Dipole selection");
                        Label aInfo = new Label("Very Strong H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        hpInnowaxInfo.getDialogPane().setContent(selectivityInfo);
                        hpInnowaxInfo.show();
                        // e: e- lone-pair interactions (e0.203 -> e0.232) SLIGHT RT INCREASE ; Label= Slight e-LonePair/Pi-Pi selection
                        // s: Dipole/Dipole interactions (s1.682 -> s0.834) STRONG RT INCREASE ; Label= Strong Dipole/Dipole selection
                        // a: H-Bond accepting interactions (a2.638 -> a1.068) VERY STRONG RT INCREASE ; Label= Very Strong H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.600 -> l0.246) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column7 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column7.setIconSize(55);
                    Label column7Label = new Label("RTX-OPP\nMax-Temp: 330\nLength: 30 m\nFilm-Thickness: 0.1 um");
                    column7Label.setTextFill(Color.DODGERBLUE);
                    column7Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox rtxOPP = new VBox(column7,column7Label);
                    rtxOPP.setPadding(new Insets(7));
                    rtxOPP.setAlignment(Pos.BOTTOM_CENTER);
                    Button rtxOPPButton = new Button();
                    rtxOPPButton.setGraphic(rtxOPP);
                    rtxOPPButton.setOnAction(e -> {
                        Alert rtxOPPInfo = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) rtxOPPInfo.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.RTX_OPP);
                        });
                        rtxOPPInfo.getDialogPane().setPrefWidth(450);
                        rtxOPPInfo.setHeaderText("Rtx-OPP");
                        ImageView stationaryPhase = makeImageView("Rtx-OPP_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("\"Similar to 20% (poly)methyltrifluoropropylsiloxane 80% dimethyl\"");
                        Label eInfo = new Label("Slight to Almost No e-LonePair/Pi-Pi Selection");
                        Label sInfo = new Label("Moderate Dipole/Dipole selection");
                        Label aInfo = new Label("Slight H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        rtxOPPInfo.getDialogPane().setContent(selectivityInfo);
                        rtxOPPInfo.show();
                        // e: e- lone-pair interactions (e-0.451 -> e0.052) LOW TEMP: MILD TO SLIGHT RT DECREASE ; Label=Low Temp: Mild to Slight e-LonePair/Pi-Pi Anti-Selection
                        //                                                  HIGH TEMP: SLIGHT TO ALMOST NO RT INCREASE ; Label=High Temp: Slight to Almost No e-LonePair/Pi-Pi Selection
                        // s: Dipole/Dipole interactions (s1.161 -> s0.323) MODERATE RT INCREASE ; Label= Moderate Dipole/Dipole selection
                        // a: H-Bond accepting interactions (a0.363 -> a0.120) SLIGHT RT INCREASE ; Label= Slight H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.650 -> l0.180) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column8 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column8.setIconSize(55);
                    Label column8Label = new Label("RTX-440\nMax-Temp: 340\nLength: 30 m\nFilm-Thickness: 0.5 um");
                    column8Label.setTextFill(Color.DODGERBLUE);
                    column8Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox rtx440 = new VBox(column8,column8Label);
                    rtx440.setPadding(new Insets(7));
                    rtx440.setAlignment(Pos.BOTTOM_CENTER);
                    Button rtx440Button = new Button();
                    rtx440Button.setGraphic(rtx440);
                    rtx440Button.setOnAction(e -> {
                        Alert rtx440Info = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) rtx440Info.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.RTX_440);
                        });
                        rtx440Info.getDialogPane().setPrefWidth(450);
                        rtx440Info.setHeaderText("Rtx-440");
                        ImageView stationaryPhase = makeImageView("Rtx-440_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("\"Similar to 6% (poly)cyanopropylphenylsiloxane 94% dimethyl\"");
                        Label eInfo = new Label("Slight e-LonePair/Pi-Pi selection");
                        Label sInfo = new Label("Mild Dipole/Dipole selection");
                        Label aInfo = new Label("Slight To Mild H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        rtx440Info.getDialogPane().setContent(selectivityInfo);
                        rtx440Info.show();
                        // e: e- lone-pair interactions (e-0.097 -> e0.131) SLIGHT RT INCREASE ; Label= Slight e-LonePair/Pi-Pi selection
                        // s: Dipole/Dipole interactions (s0.541 -> s0.179) MILD RT INCREASE ; Label= Mild Dipole/Dipole selection
                        // a: H-Bond accepting interactions (a0.480 -> a0.095) LOW TEMP: MILD RT INCREASE ; Label= Low-Temp: Mild H-Bond selection
                                                                            // HIGH TEMP: SLIGHT RT INCREASE ; Label= High-Temp: Slight H-Bond selection
                        // l: Cavity formation & dispersion interactions (l0.733 -> l0.226) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });

                    FontIcon column9 = FontIcon.of(MaterialDesign.MDI_NEST_PROTECT);
                    column9.setIconSize(55);
                    Label column9Label = new Label("SPB-Octyl\nMax-Temp: 260\nLength: 30 m\nFilm-Thickness: 1.0 um");
                    column9Label.setTextFill(Color.DODGERBLUE);
                    column9Label.setFont(Font.font(null,FontWeight.BOLD,10));
                    VBox spbOctyl = new VBox(column9,column9Label);
                    spbOctyl.setPadding(new Insets(7));
                    spbOctyl.setAlignment(Pos.BOTTOM_CENTER);
                    Button spbOctylButton = new Button();
                    spbOctylButton.setGraphic(spbOctyl);
                    spbOctylButton.setOnAction(e -> {
                        Alert spbOctylInfo = new Alert(Alert.AlertType.CONFIRMATION);
                        Button selectButton = (Button) spbOctylInfo.getDialogPane().lookupButton(ButtonType.OK);
                        selectButton.setText("Select Column");
                        selectButton.setOnAction(e2 -> {
                            columnChoices.setSelectedItem(Column.SPB_OCTYL);
                        });
                        spbOctylInfo.getDialogPane().setPrefWidth(450);
                        spbOctylInfo.setHeaderText("SPB-Octyl");
                        ImageView stationaryPhase = makeImageView("SPB-Octyl_stationaryPhase.jpg");
                        stationaryPhase.setPreserveRatio(true);
                        stationaryPhase.setFitHeight(300.0);
                        Label genInfo = new Label("50%: (poly)octylsiloxane 50%: (poly)dimethylsiloxane");
                        Label eInfo = new Label("Slight e-LonePair/Pi-Pi Selection");
                        Label sInfo = new Label("Almost No Dipole/Dipole Selection");
                        Label aInfo = new Label("Almost No H-Bond Selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfo,lInfo);
                        selectivityInfo.setPadding(new Insets(20));
                        selectivityInfo.setSpacing(7.0);
                        selectivityInfo.setAlignment(Pos.CENTER);
                        for (Node node : selectivityInfo.getChildren()){
                            if (node instanceof ImageView) {
                                ImageView stationaryPhaseImage = (ImageView) node;
                                stationaryPhaseImage.setFitWidth(300);
                                continue;
                            }
                            Label lserConstantInfo = (Label) node;
                            lserConstantInfo.setWrapText(true);
                            lserConstantInfo.setTextFill(Color.DODGERBLUE);
                            lserConstantInfo.setFont(Font.font(null, FontWeight.BOLD,11));
                            lserConstantInfo.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
                            lserConstantInfo.setAlignment(Pos.CENTER);
                        }
                        spbOctylInfo.getDialogPane().setContent(selectivityInfo);
                        spbOctylInfo.show();
                        // INFO:
                        // e: e- lone-pair interactions (e0.142 -> e0.199) SLIGHT RT INCREASE ; Label=Slight e-LonePair/Pi-Pi Selection
                        // s: Dipole/Dipole interactions (s0.092 -> s0.037) ALMOST NO RT INCREASE ; Label=Almost No Dipole/Dipole Selection
                        // a: H-Bond accepting interactions (a0.088 -> a0.000) ALMOST NO RT INCREASE ; Label=Almost No H-Bond Selection
                        // l: Cavity formation & dispersion interactions (l0.775 -> l0.263) MILD-MODERATE RT INCREASE ; Label=Mild-Moderate Cavity/Dispersion Selection
                    });
                    columnChoicesGrid.addRow(0,spbOctylButton,hp5Button,hpInnowaxButton);
                    columnChoicesGrid.addRow(1,db1701Button,db225Button,rtx440Button);
                    columnChoicesGrid.addRow(2,rtxOPPButton,rxi5SilButton,rxi17Button);
                    for (Node node: columnChoicesGrid.getChildren()){
                        Button currentButton = (Button) node;
                        currentButton.setPrefWidth(145);
                        currentButton.setPrefHeight(155);
                    }

                    // Warning message
                    Label warningMessage1 = new Label("Warning: FID Detector Active (Turn off using FID button)");
                    warningMessage1.setTextFill(Color.DODGERBLUE);
                    warningMessage1.setFont(Font.font(null,FontWeight.NORMAL,12));
                    warningMessage1.visibleProperty().bind(MachineSettings.isDetectorOn);
                    Label warningMessage2 = new Label("Error: Oven Temperature >= 40 degrees C");
                    warningMessage2.setTextFill(Color.RED);
                    warningMessage2.setFont(Font.font(null,FontWeight.BOLD,12));
                    warningMessage2.visibleProperty().bind(MachineSettings.ovenTempProperty.greaterThan(40.0));
                    columnChoicesRoot.getChildren().addAll(columnChoicesGrid, warningMessage1,warningMessage2);
                columnChoices.getDialogPane().setHeader(columnChoicesRoot);

                Optional<Column> result = columnChoices.showAndWait();
                if (result.isPresent()) {
                    // Delete state of current column; restore to defaults
                    peakToSolBandChangeListener.clear();
                    peakToSolBandDataSeries.clear();
                    solBandDataSeriesToPeak.clear();
                    lineChartSolBand.getData().clear();
                    soluteBandCountProperty.set(0);
                    MachineSettings.ANALYTES_IN_COLUMN.clear();

                    // Keep state of damage though... because... iono???
                    MachineSettings.columnToDamAndRem.put(MachineSettings.CURRENT_COLUMN,
                            new Double[]{MachineSettings.CURRENT_COLUMN_DAMAGE,
                                    MachineSettings.CURRENT_COLUMN_REMAINING});
                    MachineSettings.CURRENT_COLUMN = result.get();
                    MachineSettings.CURRENT_COLUMN_DAMAGE
                            = MachineSettings.columnToDamAndRem.get(MachineSettings.CURRENT_COLUMN)[0];
                    MachineSettings.CURRENT_COLUMN_REMAINING
                            = MachineSettings.columnToDamAndRem.get(MachineSettings.CURRENT_COLUMN)[1];
                    columnName.setText(MachineSettings.CURRENT_COLUMN.toString());
                }
        });


        // FAST-FORWARD BUTTON
        Button fastForwardButton = new Button();
        FontIcon fastForward = FontIcon.of(FluentUiFilledAL.FAST_FORWARD_24);
        fastForward.setIconSize(24);
        fastForwardButton.setGraphic(fastForward);
        fastForwardButton.setText("1X");
        fastForwardButton.setTextFill(Color.DODGERBLUE);
        List<String> speeds = Arrays.asList("5X","4X","3X","2X","1X");
        fastForwardButton.setFont(Font.font(null,FontWeight.BOLD,15));
        fastForwardButton.setOnAction(e ->{
            int currentFrameRate = FRAME_LENGTH_MS.get();
            int newFrameRate = (currentFrameRate-10)%50;
            if (newFrameRate == 0) newFrameRate = 50;
            int speedIndex = (newFrameRate/10)-1;
            fastForwardButton.setText(speeds.get(speedIndex));
            FRAME_LENGTH_MS.set(newFrameRate);

            // To change period of simulationTimer, old Timer must be cancelled, entirely new TimerTask() must be called
            simulationTimer.cancel();
            simulationTimer = new Timer();
            simulationTimer.schedule(
                    new TimerTask() {
                        double detectorSignal = 0;
                        double noise = 0;
                        int runCounter = 0;

                        @Override
                        public void run() {
                            // Check for pause
                            if (isPaused.get()) {
                                // Don't generate new data points when paused
                                return;
                            }

                            Platform.runLater(() -> {
                                // Increment Internal Clock
                                incCurrentTime();

                                // Column reaches 100% damage after many thousands of calls to damageColumn()
                                // The amount of time this takes depends on how badly the column's max temp is exceeded
                                if (MachineSettings.columnMaxTempExceeded()) MachineSettings.damageColumn();

                                // Update ovenTempProperty & check if temp is ramping or cooling, then nudge temp
                                MachineSettings.ovenTempProperty.set(MachineSettings.OVEN_TEMPERATURE);
                                if (MachineSettings.TEMP_RAMPING.get()){
                                    MachineSettings.nudgeOvenTempUp();
                                    if (MachineSettings.getOvenTemperature()
                                            == MachineSettings.getOvenTemperatureTarget()){
                                        MachineSettings.TEMP_RAMPING.set(false);
                                    }
                                }
                                if (MachineSettings.TEMP_COOLING.get()){
                                    MachineSettings.nudgeOvenTempDown();
                                    if (MachineSettings.getOvenTemperature()
                                            == MachineSettings.getOvenTemperatureTarget()){
                                        MachineSettings.TEMP_COOLING.set(false);
                                    }
                                }

                                // Update all the peaks and make them traverse the column
                                for (Peak peak : MachineSettings.ANALYTES_IN_COLUMN) {
                                    // Check if the peak is eluting; if it is, don't update it.
                                    if (peak.isEluting.get()) {
                                        // Remove the solute band from the lineChart and remove pointer to its listener
                                        // to ensure garbage collection
                                        peakToSolBandChangeListener.remove(peak);
                                        soluteBandCountProperty.set(peakToSolBandChangeListener.size());
                                        XYChart.Series<Number,Number> soluteBandDataSeries = peakToSolBandDataSeries.get(peak);
                                        lineChartSolBand.getData().remove(soluteBandDataSeries);
                                        continue;
                                    } else {
                                        peak.updatePeak();
                                        peak.traverseColumn();
                                    }
                                }

                                // Iterate through all the peaks and remove them if they have already eluted
                                Iterator<Peak> peakIterator = MachineSettings.ANALYTES_IN_COLUMN.iterator();
                                while (peakIterator.hasNext()) {
                                    Peak peak = peakIterator.next();
                                    // Make sure the currentTime is well past the peaks non-negligible portion
                                    if (currentTime() >= (peak.getElutionTime() + peak.descendingCurve.calcWidthOfHalfCurve()*2)) {
                                        peakIterator.remove();
                                    }
                                }


                                // Generate detector signal value (noise + peak detection)
                                noise = MachineSettings.nextNoiseValue(
                                        MachineSettings.ovenTempToMeanNoise(MachineSettings.OVEN_TEMPERATURE),
                                        MachineSettings.ovenTempToStDevNoise(MachineSettings.OVEN_TEMPERATURE)
                                );
                                if (MachineSettings.isDetectorOn.get()){
                                    detectorSignal = MachineSettings.detect() + noise;
                                }else detectorSignal = 0;

                                // Add datapoint every 50 ms (x = currentTime, y = detectorSignal)
                                dataSeriesDetector.getData().add(new XYChart.Data<>(currentTime(), detectorSignal));

                                // Remove any datapoints that are older than 30 minutes
                                if (dataSeriesDetector.getData().size() > 24000) {
                                    dataSeriesDetector.getData().remove(0);
                                }
                            });
                        }
                    }, 0, FRAME_LENGTH_MS.get());
        });

        // ZOOM TO ALL DATA BUTTON
        Button zoomAllData = new Button("Zoom\nAll Data");
        zoomAllData.setFont(Font.font(null, FontWeight.BOLD, 10));
        zoomAllData.setPrefWidth(70);
        zoomAllData.setPrefHeight(45);

        // Action
        zoomAllData.setOnAction(e -> {
            // Set xAxis
            xAxisDetector.setAutoRanging(false);
            yAxisDetector.setAutoRanging(false);
            List<XYChart.Data<Number, Number>> xDataList = dataSeriesDetector.getData();
            DoubleStream xDoubleStream1 = xDataList.stream()
                    .mapToDouble(data -> data.getXValue().doubleValue());
            double minX = xDoubleStream1.min().orElse(0);
            DoubleStream xDoubleStream2 = xDataList.stream()
                    .mapToDouble(data -> data.getXValue().doubleValue());
            double maxX = xDoubleStream2.max().getAsDouble();
            xAxisDetector.setLowerBound(minX);
            xAxisDetector.setUpperBound(maxX + 30); // 30 seconds out from currentTime
            xAxisDetector.setTickUnit(xAxisDetector.getUpperBound()/10.0);

            // Set yAxis
            yAxisDetector.setLowerBound(0);
            List<XYChart.Data<Number, Number>> yDataList = dataSeriesDetector.getData();
            double maxY = yDataList.stream()
                    .mapToDouble(data -> data.getYValue().doubleValue())
                    .max()
                    .orElse(100); // return maxY in the dataList or 100 if there is no XYChart.Data (null)
            yAxisDetector.setUpperBound(maxY + 100);
            yAxisDetector.setTickUnit(yAxisDetector.getUpperBound()/10.0);
        });

        // TOGGLE AUTOZOOM BUTTON
        Button toggleAutoZoom = new Button("Toggle\nAutozoom");
        toggleAutoZoom.setFont(Font.font(null, FontWeight.BOLD, 10));
        toggleAutoZoom.setPrefWidth(70);
        toggleAutoZoom.setPrefHeight(45);
        // Action
        toggleAutoZoom.setOnAction(e -> {
            xAxisDetector.setAutoRanging(xAxisDetector.autoRangingProperty().not().get());
            yAxisDetector.setAutoRanging(yAxisDetector.autoRangingProperty().not().get());
        });

        // CLICK-AND-DRAG-DEFINED ZOOMING FUNCTIONALITY
        Button clickAndDragZoomButton = new Button();
        FontIcon clickAndDrag = FontIcon.of(MaterialDesign.MDI_CURSOR_DEFAULT);
        clickAndDrag.setIconColor(Color.DODGERBLUE);
        clickAndDrag.setIconSize(20);
        FontIcon zoomSquare = FontIcon.of(FluentUiFilledMZ.ZOOM_IN_24);
        zoomSquare.setIconSize(20);
        HBox clickAndDragZoomGraphic = new HBox(clickAndDrag,zoomSquare);
        clickAndDragZoomButton.setGraphic(clickAndDragZoomGraphic);
        clickAndDragZoomButton.setOnAction(e->{
            Alert zoomInfo = new Alert(Alert.AlertType.INFORMATION);
            zoomInfo.getDialogPane().setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
            zoomInfo.setTitle("Click-n-Drag Zoom");
            zoomInfo.setHeaderText("Examine peaks closely!");
            Label content = new Label("Tip: To see closeup(s) of peak(s), HOLD CTRL + CLICK & DRAG to draw a zooming rectangle. Examine peaks for asymmetry and overlapping with other peaks. This is especially important when your sample mixture contains dozens or hundreds of analytes!");
            content.setWrapText(true);
            content.setTextFill(Color.DODGERBLUE);
            content.setFont(Font.font(null, FontPosture.ITALIC, 14));
            content.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.30);
            zoomInfo.getDialogPane().setContent(content);
            zoomInfo.show();
        });

        Rectangle clickDragRectangle = new Rectangle();
        clickDragRectangle.setFill(null);
        clickDragRectangle.setStroke(Color.DODGERBLUE);
        clickDragRectangle.setStrokeWidth(3);
        clickDragRectangle.setVisible(false);
        SimpleDoubleProperty minX = new SimpleDoubleProperty(clickDragRectangle,"minX");
        SimpleDoubleProperty maxX = new SimpleDoubleProperty(clickDragRectangle,"maxX");
        SimpleDoubleProperty minY = new SimpleDoubleProperty(clickDragRectangle,"minY");
        SimpleDoubleProperty maxY = new SimpleDoubleProperty(clickDragRectangle,"maxY");
        root.getChildren().add(clickDragRectangle);
        clickDragRectangle.setViewOrder(-Double.MAX_VALUE);
        // ACTION 1 -- ESTABLISH ORIGIN OF ZOOMING RECTANGLE (AND GET minX & maxY)
        lineChartDetector.addEventHandler(MouseEvent.MOUSE_PRESSED, e1 -> {
            // 205 pixels is area of LineChart that I don't want users to be able to click on
            if (e1.isControlDown() && e1.getSceneX() > 205.0){
                xAxisDetector.setAutoRanging(false);
                yAxisDetector.setAutoRanging(false);

                Point2D mousePressPointInScene = new Point2D(e1.getSceneX(), e1.getSceneY());
                double xPosInNumberAxis = xAxisDetector.sceneToLocal(new Point2D(mousePressPointInScene.getX(), 0)).getX();
                double yPosInNumberAxis = yAxisDetector.sceneToLocal(new Point2D(0, mousePressPointInScene.getY())).getY();
                double x = xAxisDetector.getValueForDisplay(xPosInNumberAxis).doubleValue();
                double y = yAxisDetector.getValueForDisplay(yPosInNumberAxis).doubleValue();

                clickDragRectangle.setX(e1.getSceneX());
                minX.set(x);
                clickDragRectangle.setY(e1.getSceneY());
                maxY.set(y);
                clickDragRectangle.setWidth(0);
                clickDragRectangle.setHeight(0);
                clickDragRectangle.setVisible(true);
            }
        });

        // ACTION 2 -- DRAG MOUSE FOR SETTING WIDTH & HEIGHT OF ZOOMING RECTANGLE (GET maxX & minY)
        lineChartDetector.addEventHandler(MouseEvent.MOUSE_DRAGGED, e2 -> {
            // 205 pixels is area of LineChart that I don't want users to be able to click on
            if (e2.isControlDown() && clickDragRectangle.isVisible() && e2.getSceneX() > 205.0) {

                Point2D dragToPointInScene = new Point2D(e2.getSceneX(), e2.getSceneY());
                double xPosInNumberAxis = xAxisDetector.sceneToLocal(new Point2D(dragToPointInScene.getX(), 0)).getX();
                double yPosInNumberAxis = yAxisDetector.sceneToLocal(new Point2D(0, dragToPointInScene.getY())).getY();
                double x = xAxisDetector.getValueForDisplay(xPosInNumberAxis).doubleValue();
                double y = yAxisDetector.getValueForDisplay(yPosInNumberAxis).doubleValue();

                // Establish ZoomRectangle's width & height in ScenePixels
                clickDragRectangle.setWidth(Math.abs(e2.getSceneX() - clickDragRectangle.getX()));
                clickDragRectangle.setHeight(Math.abs(e2.getSceneY() - clickDragRectangle.getY()));
                maxX.set(x);
                minY.set(y);
            }
        });
        // ACTION 3 -- UPDATE X AND Y AXES UPPER AND LOWER BOUNDS (ZOOM TO zoomRectangle)
        lineChartDetector.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (clickDragRectangle.isVisible()) {
                xAxisDetector.setLowerBound(Math.max(minX.get(), 0));
                xAxisDetector.setUpperBound(maxX.get());
                yAxisDetector.setLowerBound(Math.max(minY.get(),0));
                yAxisDetector.setUpperBound(maxY.get());
                clickDragRectangle.setVisible(false);

                // Set tickUnits
                xAxisDetector.setTickUnit((int)(xAxisDetector.getUpperBound()/10.0));
                yAxisDetector.setTickUnit((int)(yAxisDetector.getUpperBound()/10.0));
            }
        });

        // ZOOM-IN BUTTON
        Button zoomInButton = new Button();
        FontIcon zoomIn = FontIcon.of(FluentUiFilledMZ.ZOOM_IN_24);
        zoomIn.setIconSize(20);
        zoomInButton.setGraphic(zoomIn);
        zoomInButton.setOnAction(e -> {
            xAxisDetector.setAutoRanging(false);
            yAxisDetector.setAutoRanging(false);
            double currentLowX = xAxisDetector.getLowerBound();
            double currentHighX = xAxisDetector.getUpperBound();
            double xRange = Math.abs(currentHighX - currentLowX);
            double currentLowY = yAxisDetector.getLowerBound();
            double currentHighY = yAxisDetector.getUpperBound();
            double yRange = Math.abs(currentHighY - currentLowY);

            xAxisDetector.setLowerBound(currentLowX+(0.10*xRange));
            xAxisDetector.setUpperBound(currentHighX-(0.10*xRange));
//            yAxis.setLowerBound(currentLowY+(0.10*yRange));
            yAxisDetector.setUpperBound(currentHighY-(0.10*yRange));

            // Set tickUnits
            xAxisDetector.setTickUnit((int)(currentHighX/10.0));
            yAxisDetector.setTickUnit((int)(currentHighY/10.0));
        });

        Button zoomOutButton = new Button();
        FontIcon zoomOut = FontIcon.of(FluentUiFilledMZ.ZOOM_OUT_24);
        zoomOut.setIconSize(20);
        zoomOutButton.setGraphic(zoomOut);

        // ZOOM-OUT BUTTON
        zoomOutButton.setOnAction(e -> {
            xAxisDetector.setAutoRanging(false);
            yAxisDetector.setAutoRanging(false);
            double currentLowX = xAxisDetector.getLowerBound();
            double currentHighX = xAxisDetector.getUpperBound();
            double xRange = Math.abs(currentHighX - currentLowX);
            double currentLowY = yAxisDetector.getLowerBound();
            double currentHighY = yAxisDetector.getUpperBound();
            double yRange = Math.abs(currentHighY - currentLowY);

            xAxisDetector.setLowerBound(Math.max(currentLowX-(xRange*0.10),0));
            xAxisDetector.setUpperBound(currentHighX+(xRange*0.10));
            yAxisDetector.setUpperBound(currentHighY*1.10);
            yAxisDetector.setLowerBound(Math.max(currentLowY-(yRange*0.10),0));

            // Set tickUnits
            xAxisDetector.setTickUnit((int)(currentHighX/10.0));
            yAxisDetector.setTickUnit((int)(currentHighY/10.0));
        });

        // CLEAR SOLUTE BANDS BUTTON
        Button clearSoluteBandsButton = new Button();
        clearSoluteBandsButton.setPrefWidth(140);
        FontIcon clearIcon = FontIcon.of(FluentUiFilledAL.DELETE_FOREVER_24);
        clearIcon.setIconColor(Color.BLACK);
        clearIcon.setIconSize(25);
        SimpleStringProperty colonClearSol = new SimpleStringProperty(" : ");
        clearSoluteBandsButton.setGraphic(clearIcon);
        clearSoluteBandsButton.setFont(Font.font(null, FontWeight.EXTRA_BOLD, 20));
        clearSoluteBandsButton.textProperty().bind(colonClearSol.concat(soluteBandCountProperty.asString()));
        clearSoluteBandsButton.setOnAction(e -> {
            Alert clearAlert = new Alert(Alert.AlertType.CONFIRMATION);
            clearAlert.getDialogPane().setPrefWidth(SCREEN_BOUNDS.getWidth()*0.31);
            clearAlert.setHeaderText("Clear All Solute Bands?");
            Button okButton = (Button) clearAlert.getDialogPane().lookupButton(ButtonType.OK);
            Button cancelButton = (Button) clearAlert.getDialogPane().lookupButton(ButtonType.CANCEL);
            okButton.setDefaultButton(false);
            cancelButton.setDefaultButton(true);
            Label content = new Label("Tip: Clearing solute bands significantly improves application performance");
            content.setWrapText(true);
            content.setTextFill(Color.DODGERBLUE);
            content.setFont(Font.font(null, FontPosture.ITALIC, 12));
            content.setMaxWidth(SCREEN_BOUNDS.getWidth()*0.31);
            clearAlert.getDialogPane().setContent(content);
            Optional<ButtonType> choice = clearAlert.showAndWait();
            if (choice.get().equals(ButtonType.OK)){
                solBandDataSeriesToPeak.clear();
                peakToSolBandDataSeries.clear();
                peakToSolBandChangeListener.clear();
                lineChartSolBand.getData().clear();
            };
        });

        // UTILITY BUTTON
        Button utilityButton = new Button("utility");
        SimpleBooleanProperty toggleProp = new SimpleBooleanProperty(true);
        utilityButton.setOnAction(e->{
            System.out.println("peakToSolBandChangeListener.values().size() = " + peakToSolBandChangeListener.values().size());
            /*System.out.println("casToChemical.size() = " + casToChemical.size());*/
           /* Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            System.out.println("Used heap memory: " + usedMemory + " bytes");
            System.out.println("Max heap memory: " + maxMemory + " bytes");*/
        });

        // UI-VIEW-PANEL PARENT NODE -- Stores the buttons which change the UI View
        VBox uiViewPanel= new VBox();
        HBox zoomButtons1 = new HBox(zoomInButton,clickAndDragZoomButton,zoomOutButton);
        zoomButtons1.setSpacing(10);
        HBox zoomButtons2 = new HBox(toggleAutoZoom, zoomAllData);
        zoomButtons2.setSpacing(10);
        HBox simSpeed = new HBox(fastForwardButton, detectorOnOffButton);
        simSpeed.setSpacing(10);
        simSpeed.setAlignment(Pos.CENTER);
        uiViewPanel.getChildren().addAll(zoomButtons1, zoomButtons2, simSpeed);
        uiViewPanel.setAlignment(Pos.CENTER);
        uiViewPanel.setSpacing(10);
        uiViewPanel.setPrefWidth(140);


        // CONTROLS ORDER -- To change the order of the buttons in the UI, change the order of these lines of code
        // LEFT CONTROLS BUTTON ORDER
        leftControls.getChildren().add(createVSpacer());
        leftControls.getChildren().add(uiViewPanel);
        leftControls.getChildren().add(createVSpacer());
        leftControls.getChildren().add(switchColumnButton);
        leftControls.getChildren().add(setOvenTempVBox);
        leftControls.getChildren().add(splitRatioButton);
        leftControls.getChildren().add(injectButton);
        leftControls.getChildren().add(simulationStateButton);
        leftControls.getChildren().add(clearSoluteBandsButton);
        leftControls.getChildren().add(restartButton);
        leftControls.getChildren().add(createVSpacer());
//        leftControls.getChildren().addAll(columnDamage,columnRem);
//        leftControls.getChildren().add(elutionTimesButton);
//        leftControls.getChildren().add(utilityButton);
        leftControls.setAlignment(Pos.CENTER);

        // First stage shown is splash screen
        showSplash(initStage);


// CORE BUSINESS LOGIC
    // Run() method is called on repeat every 50 ms (or 40, 30, 20, 10 ms if fast forwarded). All the core business
    // logic is centered in run().
        simulationTimer.schedule(
                new TimerTask() {
            double detectorSignal = 0;
            double noise = 0;
            int runCounter = 0;
            boolean isInitialization = true;

            @Override
            public void run() {
                // Initialize Simulation in the Paused state
                if (isInitialization) {
                    isPaused.set(true);
                    isInitialization = false;
                }

                // Check for pause
                if (isPaused.get()) {
                    // Don't generate new data points or run any business logic when simulation paused
                    return;
                }

                // Platform.runLater() ensures that any action inside the closure runs on the Java UI application thread
                Platform.runLater(() -> {
                    // Advance the internal simulation clock by 1 step
                    incCurrentTime();

                    // Column reaches 100% damage after many thousands of calls to damageColumn()
                    // The amount of time this takes depends on how badly the column's max temp is exceeded
                    if (MachineSettings.columnMaxTempExceeded()) MachineSettings.damageColumn();

                    // Update ovenTempProperty & check if temp is ramping or cooling, then nudge temp
                    MachineSettings.ovenTempProperty.set(MachineSettings.OVEN_TEMPERATURE);
                    if (MachineSettings.TEMP_RAMPING.get()){
                        MachineSettings.nudgeOvenTempUp();
                        if (MachineSettings.getOvenTemperature()
                                == MachineSettings.getOvenTemperatureTarget()){
                            MachineSettings.TEMP_RAMPING.set(false);
                        }
                    }
                    if (MachineSettings.TEMP_COOLING.get()){
                        MachineSettings.nudgeOvenTempDown();
                        if (MachineSettings.getOvenTemperature()
                                == MachineSettings.getOvenTemperatureTarget()){
                            MachineSettings.TEMP_COOLING.set(false);
                        }
                    }

                    // Update all the peaks and make them traverse the column
                    for (Peak peak : MachineSettings.ANALYTES_IN_COLUMN) {
                        // Check if the peak is eluting; if it is, don't update it.
                        if (peak.isEluting.get()) {
                            // Remove the solute band from the lineChart and remove pointer to its listener
                            // to ensure garbage collection
                            peakToSolBandChangeListener.remove(peak);
                            XYChart.Series<Number,Number> soluteBandDataSeries = peakToSolBandDataSeries.get(peak);
                            lineChartSolBand.getData().remove(soluteBandDataSeries);
                            continue;
                        } else {
                            peak.updatePeak();
                            peak.traverseColumn();
                        }
                    }

                    // Iterate through all the peaks and remove them if they have already eluted
                    Iterator<Peak> peakIterator = MachineSettings.ANALYTES_IN_COLUMN.iterator();
                    while (peakIterator.hasNext()) {
                        Peak peak = peakIterator.next();
                        // Make sure the currentTime is well past the peaks non-negligible portion
                        if (currentTime() >= (peak.getElutionTime() + peak.descendingCurve.calcWidthOfHalfCurve()*3)) {
                            peakIterator.remove();
                        }
                    }

                    // Generate detector signal value (noise + peak detection)
                    noise = MachineSettings.nextNoiseValue(
                            MachineSettings.ovenTempToMeanNoise(MachineSettings.OVEN_TEMPERATURE),
                            MachineSettings.ovenTempToStDevNoise(MachineSettings.OVEN_TEMPERATURE)
                    );
                    if (MachineSettings.isDetectorOn.get()){
                        detectorSignal = MachineSettings.detect() + noise;
                    }else detectorSignal = 0;

                    // Add datapoint every 50 ms (x = currentTime, y = detectorSignal)
                    dataSeriesDetector.getData().add(new XYChart.Data<>(currentTime(), detectorSignal));

                    // Remove any datapoints that are older than 30 minutes
                    if (dataSeriesDetector.getData().size() > 24000) {
                        dataSeriesDetector.getData().remove(0);
                    }
                    runCounter++;
                });
            }
        }, 0, FRAME_LENGTH_MS.get());
    }


}

