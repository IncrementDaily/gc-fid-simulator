package com.karimbouchareb.chromatographysimulator;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.DoubleStream;

import static com.karimbouchareb.chromatographysimulator.ChromatographySimulator.MachineSettings.*;


public class ChromatographySimulator extends Application {
// TOP-LEVEL FIELDS // TODO: 4/11/2023 For "FAST-TRAVELING" through a simulation, if the retention times of a given peak are a long time off
                    // TODO: 4/11/2023 then allow the user to "fast-forward" until the next peak will elute (do this by advancing the currentTime until
                    // TODO: 4/11/2023 currentTime = Math.max(nextRetentionTime - 5 seconds, currentTime());
    // DATA
    private static final String CHEM_DATA_FILEPATH = "src/main/java/com/karimbouchareb/chromatographysimulator/ufz_LSERdataset.csv";
    private static final double MRF_PROPORTIONALITY_CONST = 5.952e11;
    // INTERNAL CLOCK OF SIMULATION
    private static Timer simulationTimer = new Timer();
    private static AtomicInteger FRAME_LENGTH_MS = new AtomicInteger(50); // 50 milliseconds per frame
    private static final double FRAME_LENGTH_S = 0.05; // 0.05 seconds per frame
    private static double CURRENT_TIME = 0;  // elapsedTime in seconds
    // PEAK & COLUMN FIELDS
    private static TreeSet<Peak> ANALYTES_IN_COLUMN = new TreeSet<>();
    private static Column CURRENT_COLUMN = Column.SPB_OCTYL;
    private static HashMap<Column, Double[]> columnToDamAndRem = new HashMap<>();
    private static double CURRENT_COLUMN_DAMAGE = 0.0;
    private static double CURRENT_COLUMN_REMAINING = 1.0;

// TOP-LEVEL STATIC METHODS
    // DATA
    private static CSVParser getDataParser(){
        try {
            FileReader fileReader = new FileReader(CHEM_DATA_FILEPATH);
            return CSVFormat.DEFAULT.parse(fileReader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // INTERNAL CLOCK OF SIMULATION
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
    // DETECTOR
    private static double detect(){
        double signalInPeakAreaUnits = 0.0;
        for (Peak peak : ANALYTES_IN_COLUMN){
            signalInPeakAreaUnits+= peak.plot();
        }
        return signalInPeakAreaUnits;
    }
    @SuppressWarnings("ReassignedVariable")
    private static void initializePeaks(List<InjectionUIRecord> userInputs){
        try (FileReader fileReader = new FileReader(CHEM_DATA_FILEPATH);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader(
                     "CAS","chemicalName","SMILES","label","MRF","molecularWeight",
                     "overloadMass_1", "E","S","A","L").build().parse(fileReader)) {
            String chemicalNameOfInput;
            String chemicalNameOfRecord;
            Chemical analyte;
            double percentWeight;
            double mgAnalyteInjectedToInlet;
            double ngAnalyteEnteringColumn;
            double gAnalyteEnteringColumn;
            double molecularWeightAnalyte;
            double molesAnalyteEnteringColumn;
            double MRFAnalyte;
            double peakArea;
            double elutionTime;
            double injectionTime;
            double columnAdjustedOverloadMass;
            double peakFrontingIndex;
            double peakTailingIndex;

            for (CSVRecord record : parser){
                if (record.getRecordNumber() == 1) continue;
                chemicalNameOfRecord = record.get("chemicalName");
                for (InjectionUIRecord input : userInputs){
                    chemicalNameOfInput = input.chemicalName();
                    if (!chemicalNameOfRecord.equals(chemicalNameOfInput)) continue;
                    analyte = new Chemical(chemicalNameOfInput);
                    percentWeight = input.concentration()/100.0; // percentWeight = %Weight of 1 uL injection (total weight assumed 1 mg / uL, densities NOT factored in)
                    mgAnalyteInjectedToInlet = percentWeight*MachineSettings.INJECTION_VOLUME;
                    ngAnalyteEnteringColumn = mgAnalyteInjectedToInlet*1_000_000.0 / MachineSettings.SPLIT_RATIO;
                    gAnalyteEnteringColumn = ngAnalyteEnteringColumn/1_000_000_000.0;
                    molecularWeightAnalyte = analyte.molecularWeight;
                    molesAnalyteEnteringColumn = (gAnalyteEnteringColumn/molecularWeightAnalyte);
                    MRFAnalyte = analyte.molarResponseFactor;

                    peakArea = molesAnalyteEnteringColumn*MRFAnalyte*MRF_PROPORTIONALITY_CONST;
                    injectionTime = currentTime();
                    elutionTime = injectionTime+analyte.calcRetentionTime();

                    columnAdjustedOverloadMass = analyte.adjustedOverloadMass();
                    peakFrontingIndex = Peak.IDEAL_PEAK_FRONTING_INDEX;
                    if (ngAnalyteEnteringColumn > columnAdjustedOverloadMass){
                        System.out.println("too much");
                        System.out.println("ng analyte = " + ngAnalyteEnteringColumn);
                        System.out.println("OverloadMass = " + columnAdjustedOverloadMass);
                        peakFrontingIndex = Peak.IDEAL_PEAK_FRONTING_INDEX + (ngAnalyteEnteringColumn/columnAdjustedOverloadMass);
                    };
                    peakTailingIndex = Peak.IDEAL_PEAK_TAILING_INDEX;
                    if(MachineSettings.IS_COLUMN_CUT_POORLY){
                        peakTailingIndex = 2.1; // TODO: 5/3/2023 IMPLEMENT THIS WELL EVENTUALLY
                    }

                    Peak currentPeak = new Peak.Builder(analyte,peakArea,injectionTime)
                            .ascendingCurve(peakArea,elutionTime, GaussianCurve.IDEAL_PEAK_SIGMA)
                            .descendingCurve(peakArea,elutionTime,GaussianCurve.IDEAL_PEAK_SIGMA)
                            .peakFrontingIndex(peakFrontingIndex)
                            .peakTailingIndex(peakTailingIndex)
                            .build();
                    ANALYTES_IN_COLUMN.add(currentPeak);

                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    // COLUMN / OVEN METHODS
    private static boolean columnMaxTempExceeded(){
        return MachineSettings.OVEN_TEMPERATURE > CURRENT_COLUMN.maxTemp;
    }
    private static void damageColumn(){
        if (CURRENT_COLUMN_REMAINING == 0.0) return;
        if (CURRENT_COLUMN_DAMAGE == 0.0) {
            CURRENT_COLUMN_DAMAGE = 0.001;
            CURRENT_COLUMN_REMAINING = 0.999;
        }
        double overMaxQuotient = (MachineSettings.OVEN_TEMPERATURE - CURRENT_COLUMN.maxTemp) / CURRENT_COLUMN.maxTemp;
        // min = 0.0002941 (SPB_OCTYL maxTemp = 340, ovenTemp = 341); max = 0.4583 (DB_225 maxTemp = 240, ovenTemp = 350)
        // if overMaxQuotient = 0.002941, then damageRate = 0.999849151 and bleedRate = 1.00026111
        // if overMaxQuotient = 0.4583, then damageRate = 0.997617625 and bleedRate = 1.00412375
        // damageRateRange = 0.002231526
        // bleedRateRange = 0.00386264
        double damageRate = 0.999849151 - ((overMaxQuotient-0.002941176)/0.455392157)*0.002231526;
        double bleedRate = 1.00026111 + ((overMaxQuotient-0.002941176)/0.455392157)*0.00386264;
        CURRENT_COLUMN_DAMAGE = Math.min(Math.pow(CURRENT_COLUMN_DAMAGE,damageRate), 1.0); // column reaches 100% damage after 120 seconds
        CURRENT_COLUMN_REMAINING = Math.max(Math.pow(CURRENT_COLUMN_REMAINING,bleedRate), 0.0);
    }
    // UI
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
    private ImageView makeImageView(String filePath) {
        ImageView imageView = null;
        URL imageUrl = getClass().getClassLoader().getResource(filePath);
        if (imageUrl != null) {
            Image image = new Image(imageUrl.toString());
            imageView = new ImageView(image);
            return imageView;
        } else {
            System.out.println("Shit fuck fuck FUCK file not found");
            return null;
        }
    }

// TOP-LEVEL STATIC MEMBER CLASSES
    // TODO: 4/17/2023 WILL HOLD CARRIER GAS VELOCITY CONSTANT AT 40 cm/s ... PERHAPS IN FUTURE THIS COULD BE ADJUSTABLE
    static class MachineSettings{
        public static double INJECTION_VOLUME = 1.0; // microliters // TODO: 4/2/2023 Add Icon for "Machine Configurations" Button
        public static double SPLIT_RATIO = 100.0; // ratio 1:50 -- 1 part of sample sent to column, 50 parts of sample vented to waste.
        public static SimpleDoubleProperty splitRatioProperty = new SimpleDoubleProperty(SPLIT_RATIO);
        public static double HE_GAS_LINEAR_VELOCITY = 40.0; // Default = 40 cm/s (taken from Poole 2019 experimental conditions)
        public static double OVEN_TEMPERATURE_MIN = 25;
        public static double OVEN_TEMPERATURE_MAX = 350;
        public static double OVEN_TEMPERATURE = 25; // degrees C
        public static SimpleDoubleProperty ovenTempProperty = new SimpleDoubleProperty(OVEN_TEMPERATURE);
        public static double OVEN_TEMPERATURE_TARGET = 25; // degrees C
        public static AtomicBoolean TEMP_RAMPING = new AtomicBoolean();
        public static AtomicBoolean TEMP_COOLING = new AtomicBoolean();
        public static SimpleBooleanProperty isDetectorOn = new SimpleBooleanProperty(true);
        public static boolean IS_COLUMN_CUT_POORLY = false;
        public static double INLET_TEMPERATURE = 25; // degrees C
        public static int MEASURED_RUNTIME = 0;

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
                stdDevNoise = (stdDevNoise * CURRENT_COLUMN_REMAINING)+0.001;
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

    }
    // All polynomial fits are based on data inside the columnLSERData.csv file.
    // This data was taken from Poole's paper referenced in the README.
    // Apply global compound loading capacity debuff/buff for columns that are bigger/smaller than 1 um thickness
    // REFERENCE: HP-1 Column (0.25 mm ID, 1 um film, 15 m length) had holdup time of ~40 seconds.
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
        public double holdUpTime(double ovenTemperature){
            // Interpolate holdUpTime by temp
            double ratio = (ovenTemperature - MachineSettings.OVEN_TEMPERATURE_MIN) /
                    (MachineSettings.OVEN_TEMPERATURE_MAX - MachineSettings.OVEN_TEMPERATURE_MIN);
            double holdUpTimeRange = maxHoldUpTime - minHoldUpTime;
            return minHoldUpTime + (holdUpTimeRange*ratio);
        }
        // If column has been damaged, column LSER constant values are lowered proportionally (column less able to
        // perform separatory function).
        public double E(double ovenTemperature){
            return eCurve.value(ovenTemperature)*CURRENT_COLUMN_REMAINING;
        }
        public double S(double ovenTemperature){
            return sCurve.value(ovenTemperature)*CURRENT_COLUMN_REMAINING;
        }
        public double A(double ovenTemperature){
            return aCurve.value(ovenTemperature)*CURRENT_COLUMN_REMAINING;
        }
        public double L(double ovenTemperature){
            return lCurve.value(ovenTemperature)*CURRENT_COLUMN_REMAINING;
        }
        public double C(double ovenTemperature){
            return cCurve.value(ovenTemperature)*CURRENT_COLUMN_REMAINING;
        }
        public double lengthMeters(){
            return columnLength;
        }
        // VAN DEEMTER SIMULATION // TODO: 4/5/2023 FIND A BETTER SCIENTIFIC RATIONALE FOR THIS?
        // TODO: 4/5/2023 ALLOW FOR ADJUSTMENT OF THIS TOO LOW TO CAUSE GENERAL PEAK BROADENING (B TERM TOO HIGH
        // // TODO: 4/5/2023 AND ADJUSTMENT OF THIS TOO LOW TO CAUSE GENERAL PEAK CONVOLUTION (SQUISHIFY THE RETENTION TIMES TO BE CLOSER TO EACH OTHER
        // Height Equivalent Theoretical Plates in mm. Column length / HETP = number Of theoretical plates
        private static double hetpInMillimeters() {
            // A, B, and C were picked somewhat arbitrarily: they are taken from one example
            // of a plausible Van Deemter Plot for Helium Carrier Gas found on wikipedia
            double A = 1.5; // Eddy Diffusion term (Experimentally derived)
            double B = 25; // Longitudinal Diffusion term (Experimentally derived)
            double C = 0.025; // Resistance To Mass Transfer Term (Experimentally Derived)

            return A + (B / MachineSettings.HE_GAS_LINEAR_VELOCITY) + (C * MachineSettings.HE_GAS_LINEAR_VELOCITY);
        }
        // Number of Theoretical Plates of the current column is related to resolving/separating power.
        // More plates = more resolution/more separation.
        public double numTheoreticalPlates(){
            return ((lengthMeters() * 1000) / hetpInMillimeters());
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
                    if (record.get(1).equals(chemicalName)){
                        this.chemicalName = record.get(1);
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

        private double calcRetentionFactor(){
            double logk = e*CURRENT_COLUMN.E(MachineSettings.OVEN_TEMPERATURE)
                    + s*CURRENT_COLUMN.S(MachineSettings.OVEN_TEMPERATURE)
                    + a*CURRENT_COLUMN.A(MachineSettings.OVEN_TEMPERATURE)
                    + l*CURRENT_COLUMN.L(MachineSettings.OVEN_TEMPERATURE)
                    + CURRENT_COLUMN.C(MachineSettings.OVEN_TEMPERATURE);
            double k = Math.pow(10.0,logk);
            return k;
        }
        private double calcRetentionTime(){
            double currentHoldUpTime = CURRENT_COLUMN.holdUpTime(MachineSettings.getOvenTemperature());
            double retentionFactor = calcRetentionFactor();
            double retentionTime = currentHoldUpTime + (currentHoldUpTime*retentionFactor);
            return retentionTime;
        }
        // See README regarding calculating overloadMass
        private double adjustedOverloadMass(){
            if (CURRENT_COLUMN.filmThickness == 1.0) return overloadMass_1;
            else if (CURRENT_COLUMN.filmThickness >= 0.5) return overloadMass_1/2;
            else if (CURRENT_COLUMN.filmThickness >= 0.25) return overloadMass_1/4;
            else return overloadMass_1/8;
        }

        @Override
    	public String toString(){
    		return chemicalName;
        }
    }
    private static class Peak implements Comparable {
        private final Chemical analyte;
        private final double peakArea;
        private final double injectionTime;
        private double proportionOfColumnTraversed = 0.0; // Range from 0.0 to 1.0;
        private AtomicBoolean isEluting = new AtomicBoolean(false);
        private double elutionTime;
        private GaussianCurve ascendingCurve;
        private GaussianCurve descendingCurve;
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
            // Mutable
            private GaussianCurve ascendingCurve;
            private GaussianCurve descendingCurve;
            // Curve-shape modulators
            private double peakTailingIndex = 1.0; // TODO: 4/5/2023
            private double peakFrontingIndex = 1.0;// TODO: 4/5/2023
            private double peakBroadeningIndex = 1.0;// TODO: 4/5/2023
            // Implement these indices so that each peak's degree of peak tailing, fronting,
            // or symmetric broadening depends on the factors according to the README.txt

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
                        {this.peakTailingIndex = peakTailingIndex;                      return this;}
            private Builder peakFrontingIndex(double peakFrontingIndex)
                        {this.peakFrontingIndex = peakFrontingIndex;                    return this;}
            private Builder peakBroadeningIndex(double peakBroadeningIndex)
                        {this.peakBroadeningIndex = peakBroadeningIndex;                return this;}
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
        /*private double calcElutionTime(){
            double currentHoldUpTime = CURRENT_COLUMN.holdUpTime(MachineSettings.getOvenTemperature());
            double retentionFactor = analyte.calcRetentionFactor();
            double elutionTime = currentHoldUpTime + (currentHoldUpTime*retentionFactor) + injectionTime;
            // cause non-deterministic behavior to the extent that column is severely damaged (default damage = 0.0)
            elutionTime += CURRENT_COLUMN_DAMAGE*(Math.random()*100.0);
            return elutionTime;
        }*/

        // Math.max() method ensures that the updated retentionTime will not be LESS than currentTime()
        // otherwise the peak would not plot correctly. The updated retentionTime will be 3 frames after the currentTime().
        private void updatePeak(){
            // Update Elution Time
            elutionTime = calcElutionTime(); // // TODO: 4/25/2023 Gotta fix bug that occurs when elutionTime crashes super hard due to column damage

            // Update Peak Shape
            updatePeakShape();
        }
        private void traverseColumn(){ // called every time run() is called
            if (isEluting.get()) {
                return;
            }
            proportionOfColumnTraversed = Math.min(proportionOfColumnTraversed
                    + traversalProgressPerSimulationStep() , 1.0);
            if (proportionOfColumnTraversed == 1.0) isEluting.set(true);
        }
        private double traversalProgressPerSimulationStep(){
            return FRAME_LENGTH_S/analyte.calcRetentionTime();
        }
        private double proportionOfColumnUntraversed(){
            return 1.0 - proportionOfColumnTraversed();
        }
        private double proportionOfColumnTraversed(){
            return proportionOfColumnTraversed;
        }
        private int simulationStepsRemainingUntilPeakElutes(){
            return (int) Math.ceil(proportionOfColumnUntraversed()
                    /traversalProgressPerSimulationStep());
        }
        private double secondsRemainingUntilPeakElutes(){
            return simulationStepsRemainingUntilPeakElutes() * FRAME_LENGTH_S;
        }
        public double calcElutionTime(){
            elutionTime = currentTime() + secondsRemainingUntilPeakElutes();
            return elutionTime;
        }

        public double getElutionTime() {
            return elutionTime;
        }
        public Chemical analyte(){
            return analyte;
        }
        public void updateAmplitudes(double peakArea, GaussianCurve ascendingCurve, GaussianCurve descendingCurve){
            double sharedAmplitude = GaussianCurve.sharedAmplitude(peakArea, ascendingCurve, descendingCurve);
            ascendingCurve.amplitude = sharedAmplitude;
            descendingCurve.amplitude = sharedAmplitude;
        }
        public void updatePeakShape(){
            // Update peak widths based on circumstances
            peakBroadeningIndex = IDEAL_PEAK_BROADENING_INDEX + analyte.calcRetentionTime()* PEAK_BROAD_COEFF;
//            peakBroadeningIndex += CURRENT_COLUMN_DAMAGE*(Math.random()*2.0); // non-deterministic behavior if column damaged
            ascendingCurve.sigma = GaussianCurve.IDEAL_PEAK_SIGMA*(peakFrontingIndex*peakBroadeningIndex);
            descendingCurve.sigma = GaussianCurve.IDEAL_PEAK_SIGMA*(peakTailingIndex*peakBroadeningIndex);

            // Maintain same peak amplitude, while continuing to sum to correct peakArea
            updateAmplitudes(peakArea, ascendingCurve, descendingCurve);

            // Maintain equal elutionTime
            ascendingCurve.mu = elutionTime;
            descendingCurve.mu = elutionTime;
        }
        public double plot() {
            if (currentTime() <= elutionTime){
                return ascendingCurve.apply(currentTime());
            }else{
                return descendingCurve.apply(currentTime());
            }
        }
        @Override
        public int compareTo(Object o){
            Peak otherPeak = (Peak) o;
            return Double.compare(getElutionTime(), otherPeak.getElutionTime());
        }
    }
    private static class InjectionUIRecord{
        private final TextField textfield;
        private final String chemicalName;
        private SimpleStringProperty concentration = new SimpleStringProperty();

        public InjectionUIRecord(TextField textfield, String chemicalName) {
            this.textfield = textfield;
            this.chemicalName = chemicalName;
        }

        public TextField textField() {
            return textfield;
        }
        public String chemicalName() {
            return chemicalName;
        }
        public SimpleStringProperty concentrationProperty(){
            return concentration;
        }
        public double concentration(){
            return Double.parseDouble(concentration.getValue());
        }
        public String toString(){
            return chemicalName;
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

    @Override
    public void start(Stage stage) {
        // Get the primary screen
        Screen screen = Screen.getPrimary();
        // Get the bounds of the screen
        Rectangle2D screenBounds = screen.getVisualBounds();

        // Create a NumberAxis for the x-axis (autoranging)
        final NumberAxis xAxis = new NumberAxis(0.0,60.0,1.0);
        xAxis.setForceZeroInRange(false);
        xAxis.setLabel("Time (s)");
        xAxis.setAutoRanging(false);


        // Create a NumberAxis for the y-axis (autoranging)
        final NumberAxis yAxis = new NumberAxis(0.0,80.0,10.0);
        yAxis.setLabel("Signal (Peak Area Units)");
        yAxis.setAutoRanging(false);
        yAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return String.format("%.0f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        });

        // Create a LineChart and don't create symbols
        final LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.setPadding(new Insets(40,30,0,0));

        // Create a data series and add to lineChart
        XYChart.Series<Number, Number> dataSeries = new XYChart.Series<>();
        lineChart.getData().add(dataSeries);
        dataSeries.getNode().setStyle("-fx-stroke-width: 1;");
        lineChart.legendVisibleProperty().set(false);


        // Define Root Layout Container, populate lineChart at center, left & right controls
        BorderPane root = new BorderPane();
          BackgroundFill backgroundFill = new BackgroundFill(
            Color.FLORALWHITE,
            CornerRadii.EMPTY,
            Insets.EMPTY);
        root.setBackground(new Background(backgroundFill));

        VBox leftControls = new VBox(10);
          leftControls.setPadding(new Insets(10,20,10,20));
        leftControls.setAlignment(Pos.CENTER);
        root.setCenter(lineChart);
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
            AtomicBoolean isPaused = new AtomicBoolean(false);
            simulationStateButton.setOnAction(e -> {
                isPaused.set(!isPaused.get());

                if (isPaused.get()) {
                    simulationStateButton.setGraphic(play);
                } else {
                    simulationStateButton.setGraphic(pause);
                }
            });

        // RESTART SIMULATION BUTTON
       /* FontIcon restart = new FontIcon(FontAwesome.PAUSE);
        pause.setIconColor(Color.BLACK);
        pause.setIconSize(24);
        Button restartSimulationButton = new Button();
        simulationStateButton.setGraphic(play);
        simulationStateButton.setPrefWidth(140);
        simulationStateButton.setPrefHeight(45);*/

        // ELUTION TIME BUTTON
        Button elutionTimesButton = new Button("get ElutionTimes");
            // Action
            elutionTimesButton.setOnAction(e -> {
            for(Peak peak : ANALYTES_IN_COLUMN){
                System.out.print(peak.analyte.toString() + " ");
                System.out.print(String.format("%.1f", peak.getElutionTime()) + " eTime");
                System.out.println(" ProportionTraversed = " + String.format("%.2f",peak.proportionOfColumnTraversed()));
                /*System.out.print(" peak Broad = " + peak.peakBroadeningIndex);
                System.out.print(" peak Front = " + peak.peakFrontingIndex);
                System.out.print(" peak Tail = " + peak.peakTailingIndex);*/
//                System.out.println();
            }
            System.out.println();
        });

        // ColumnDamage Button
        Button columnDamage = new Button("column damage");
            // Action
            columnDamage.setOnAction(e -> {
            System.out.println("Current Damage = " + CURRENT_COLUMN_DAMAGE);
        });

        // ColumnDamage Button
        Button columnRem = new Button("column Rem");
            // Action
            columnRem.setOnAction(e -> {
            System.out.println("Column Rem = " + CURRENT_COLUMN_REMAINING);
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
        Button setOvenTempButton = new Button("");
        setOvenTempButton.setGraphic(thermometer0);
        ovenTempProperty.addListener(e -> {
            if (ovenTempProperty.doubleValue() >= 330.0) {setOvenTempButton.setGraphic(thermometer4); return;}
            if (ovenTempProperty.doubleValue() >= 280.0) {setOvenTempButton.setGraphic(thermometer3); return;}
            if (ovenTempProperty.doubleValue() >= 180.0) {setOvenTempButton.setGraphic(thermometer2); return;}
            if (ovenTempProperty.doubleValue() >= 90.0) {setOvenTempButton.setGraphic(thermometer1); return;}
            if (ovenTempProperty.doubleValue() < 90.0) {setOvenTempButton.setGraphic(thermometer0); return;}
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

                Label currentTempMarker = new Label("Current Temperature: ");
                currentTempMarker.setFont(Font.font(null, FontWeight.BOLD, 10));
                Label currentTempVal = new Label();
                currentTempVal.setFont(Font.font(null, FontWeight.BOLD, 10));
                currentTempVal.textProperty().bind(ovenTempProperty.asString("%.0f"));
                HBox currentTemp = new HBox(currentTempMarker,currentTempVal);

                Label info = new Label("Tip: The oven temperature(s) you select for your method are of primary importance for ensuring good separation of peaks. Low temperatures cause chemicals to spend more time in the column. High temperatures speed them through. The selectivity parameters of your columns change with temperature as well. Play around!");
                info.setWrapText(true);
                info.setTextFill(Color.DODGERBLUE);
                info.setFont(Font.font(null, FontPosture.ITALIC, 10));
                info.setMaxWidth(250);

                inputDialog.getDialogPane().setContent(new VBox(2, validationLabel, currentTemp, inputField, info));

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
            progressBar.progressProperty().bind(ovenTempProperty.divide((350)));
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
        SimpleStringProperty colon = new SimpleStringProperty(": ");
        splitRatioButton.textProperty().bind(colon.concat(splitRatioProperty.asString("%.0f")));
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
                info.setFont(Font.font(null, FontPosture.ITALIC, 10));
                info.setMaxWidth(270);

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
                    SPLIT_RATIO = Integer.parseInt(value);
                    Platform.runLater(() ->{
                        splitRatioProperty.set(SPLIT_RATIO);
                    });
                });
            });

        // INJECT BUTTON
        FontIcon needle = FontIcon.of(MaterialDesign.MDI_NEEDLE);
        needle.setIconColor(Color.DODGERBLUE);
        needle.setIconSize(34);
        Button injectButton = new Button("", needle);
        injectButton.setPrefWidth(140);
        injectButton.setPrefHeight(45);
        injectButton.setOnAction(e1 -> {
            Stage injectStage = new Stage();
            injectStage.initOwner(stage);
            injectStage.initModality(Modality.APPLICATION_MODAL);
            injectStage.setTitle("Injection");

            // List of Observables for tracking / interacting with global list of UI elements in the
            // injectStage
            ObservableList<BooleanProperty> invalidInputs = FXCollections.observableArrayList();
            ObservableList<DoubleProperty> concentrationValues = FXCollections.observableArrayList();

            // Get chemData and move the CSVParser iterator past the header record
            CSVParser parser = getDataParser();
            parser.iterator().next();

            // Use chemData to populate the uiListOfChems GridPane
            GridPane uiListOfChems = new GridPane();
            int row = 0;
            InjectionUIRecord concentrationInput;
            ArrayList<InjectionUIRecord> concentrationInputs = new ArrayList<>();

            for (CSVRecord record : parser) {
                // Add chemical names
                String chemicalName = record.get(1);
                Label label = new Label(chemicalName);
                label.setPadding(new Insets(5, 0, 5, 15));
                GridPane.setHalignment(label, HPos.RIGHT);
                uiListOfChems.add(label, 0, row);

                // Add user input of concentration and add them to the observable list that watches
                // whether they are disabled (invalid user input)
                concentrationInput = new InjectionUIRecord(new TextField(), chemicalName);
                concentrationInput.concentrationProperty().bind(concentrationInput.textField().textProperty());
                concentrationInput.textField().setPadding(new Insets(5,0,5,0));
                uiListOfChems.add(concentrationInput.textField(), 2, row);

                Label validationLabel1 = new Label("X");
                validationLabel1.setTextFill(Color.CRIMSON);
                validationLabel1.setVisible(false);
                validationLabel1.setPadding(new Insets(0, 3, 0, 3));
                uiListOfChems.add(validationLabel1, 1, row);

                Label validationLabel2 = new Label("Must be: 0.0% to 100.0%");
                validationLabel2.setTextFill(Color.CRIMSON);
                validationLabel2.setVisible(false);
                validationLabel2.setPadding(new Insets(0, 0, 0, 3));
                uiListOfChems.add(validationLabel2, 3, row);

                // Validate user input of concentration
                DoubleProperty concentrationValue = new SimpleDoubleProperty(0.0);
                concentrationValues.add(concentrationValue);
                BooleanProperty isInvalid = new SimpleBooleanProperty(false);
                invalidInputs.add(isInvalid);

                final Label finalValidationLabel1 = validationLabel1;
                final Label finalValidationLabel2 = validationLabel2;

                // LISTEN FOR VALID INPUT PART 1
                concentrationInput.textField().textProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        if (newValue.equals("")) {
                            concentrationValue.set(0.0);
                            finalValidationLabel1.setVisible(false);
                            finalValidationLabel2.setVisible(false);
                            isInvalid.set(false);
                            return;
                        }
                        try {
                            double value = Double.parseDouble(newValue);
                            if (value >= 0.0 && value <= 100.0) {
                                concentrationValue.set(value);
                                finalValidationLabel1.setVisible(false);
                                finalValidationLabel2.setVisible(false);
                                isInvalid.set(false);
                            } else {
                                finalValidationLabel1.setVisible(true);
                                finalValidationLabel2.setVisible(true);
                                isInvalid.set(true);
                            }
                        } catch (NumberFormatException e) {
                            finalValidationLabel1.setVisible(true);
                            finalValidationLabel2.setVisible(true);
                            isInvalid.set(true);
                        }
                    }
                });
                concentrationInputs.add(concentrationInput);
                row++;
            }

            BooleanBinding anyInvalidInput = invalidInputs.stream()
                    .map(BooleanProperty::not)
                    .reduce(BooleanBinding::and)
                    .orElseThrow(IllegalStateException::new)
                    .not();

            DoubleBinding totalValueAllInputs = Bindings.createDoubleBinding(() -> {
                double sum = 0.0;
                for (DoubleProperty concentrationValue : concentrationValues) {
                    sum += concentrationValue.get();
                }
                return sum;
            }, concentrationValues.toArray(new DoubleProperty[0]));

            // Make the uiListOfChems scrollable
            ScrollPane chemListScrollPane = new ScrollPane();
            chemListScrollPane.setContent(uiListOfChems);
            chemListScrollPane.setPadding(new Insets(20));

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
                          totalValueAllInputs.addListener(e -> {
                              if (totalValueAllInputs.doubleValue() > 100.0) {
                                  loadValue.setTextFill(Color.CRIMSON);
                              } else {
                                  loadValue.setTextFill(Color.DODGERBLUE);
                              }
                          });
                          loadValue.setStyle("-fx-font-weight: bold");
                          loadValue.textProperty().bind(totalValueAllInputs.asString("%.2f"));
                      topInfoValues.getChildren().addAll(analyteValueLabel,loadValue);
                    Label loadValueInvalidMarker = new Label();
                      loadValueInvalidMarker.visibleProperty().bind(anyInvalidInput);
                      loadValueInvalidMarker.setPadding(new Insets(15,0,0,0));
                      loadValueInvalidMarker.setText("Invalid Value Found: Check Left Window");
                      loadValueInvalidMarker.setTextFill(Color.CRIMSON);
                      loadValueInvalidMarker.setStyle("-fx-font-weight: bold");
                  topInfo.getChildren().addAll(loadValueInvalidMarker,topInfoValues);
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
                        ImageView imageView = null;
                        URL imageUrl = getClass().getClassLoader().getResource("tickmarks.png");
                        if (imageUrl != null) {
                            Image image = new Image(imageUrl.toString());
                            imageView = new ImageView(image);
                            imageView.setFitWidth(280);
                        } else {
                            System.err.println("Image not found");
                        }
                  // Set up the Injection MiniGame
                  injectionMinigame.getChildren().addAll(imageView,targetBox,oscillator);
                finalizingButtons.setCenter(injectionMinigame);
                // Bottom -- All Clear Buttons
                VBox bottomInfo = new VBox();
                bottomInfo.setSpacing(5);
                bottomInfo.setPadding(new Insets(15,15,55,15));
                  Button finalizeButton = new Button("Finalize Sample");
                    finalizeButton.setPadding(new Insets(10,50,10,50));
                    BorderPane.setAlignment(finalizeButton, Pos.CENTER);
                    SimpleBooleanProperty isFinalized = new SimpleBooleanProperty(finalizeButton,"isFinalized",false);
                    finalizeButton.setOnAction(e3 -> {
                        if(finalizeButton.getText().equals("Finalize Sample")){
                            finalizeButton.setText("Edit Sample");
                            chemListScrollPane.setDisable(!chemListScrollPane.isDisabled());
                            isFinalized.set(true);
                            oscillatorTranslator.play();
                            return;
                        }
                        if(finalizeButton.getText().equals("Edit Sample")){
                            finalizeButton.setText("Finalize Sample");
                            chemListScrollPane.setDisable(!chemListScrollPane.isDisabled());
                            isFinalized.set(false);
                            oscillatorTranslator.stop();
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
                            .or(anyInvalidInput)
                            .or(totalValueAllInputs.greaterThan(100.0)));
                  Button allClearButton = new Button("All Clear: Inject");
                    allClearButton.setPadding(new Insets(10,50,10,50));
                    BorderPane.setAlignment(allClearButton, Pos.CENTER);
                    allClearButton.disableProperty().bind(anyInvalidInput
                            .or(totalValueAllInputs.greaterThan(100.0))
                            .or(techniqueScore.lessThan(0.0001)));
                    allClearButton.setOnAction(e5 -> {
                        ArrayList<InjectionUIRecord> userInputs = new ArrayList<>();
                        for(InjectionUIRecord chemical : concentrationInputs){
                            if(!chemical.textField().getText().equals("")){
                                userInputs.add(chemical);
                            }
                        }
                        simulationStateButton.fire();
                        initializePeaks(userInputs);
                        injectStage.close();
                        simulationStateButton.fire();
                    });
                bottomInfo.getChildren().addAll(finalizeButton,techniqueButton, allClearButton);
                finalizingButtons.setBottom(bottomInfo);
                bottomInfo.setAlignment(Pos.CENTER);

            // Add ScrollPane and Vbox to SplitPane
            SplitPane splitPane = new SplitPane();
            splitPane.setDividerPosition(0,0.61);
            splitPane.getItems().addAll(chemListScrollPane,finalizingButtons);

            // Create the Scene and add the ScrollPane to it
            Scene scene = new Scene(splitPane, stage.getWidth()-100, stage.getHeight()-200);
            injectStage.setScene(scene);
            injectStage.show();
        });

        // DETECTOR-OFF BUTTON
        Button detectorOnOffButton = new Button();
        FontIcon fire = FontIcon.of(FontAwesome.FIRE);
        fire.setIconColor(Color.DODGERBLUE);
        fire.setIconSize(24);
        Label fid = new Label("FID");
        fid.setFont(Font.font(null,FontWeight.BOLD,10));
        HBox fidDetector = new HBox(fid,fire);
        fidDetector.setAlignment(Pos.CENTER);
        detectorOnOffButton.setGraphic(fidDetector);
        detectorOnOffButton.setOnAction(e -> {
            isDetectorOn.set(isDetectorOn.not().get());
            if (isDetectorOn.get()) {
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
        Label columnName = new Label(CURRENT_COLUMN.toString());
        columnName.setFont(Font.font(null,FontWeight.BOLD,10));
        columnName.setTextFill(Color.DODGERBLUE);
        VBox switchColumnButtonGraphic = new VBox(column,columnName/*,forward*/);
        switchColumnButtonGraphic.setSpacing(0);
        switchColumnButtonGraphic.setAlignment(Pos.CENTER);
        switchColumnButton.setGraphic(switchColumnButtonGraphic);
        switchColumnButton.setPrefWidth(140);
        switchColumnButton.setPrefHeight(45);
        switchColumnButton.setOnAction( e1 -> {
            /*Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.getDialogPane().setMaxWidth(350);
            alert.setHeaderText("Warning: Installing New Columns");
            Label columnWarning = new Label("Tip: If you uninstall your current column, it will still contain any uneluted analytes if you later reinstall it.");
            columnWarning.setFont(Font.font(null, FontPosture.ITALIC,10));
            columnWarning.setTextFill(Color.DODGERBLUE);
            columnWarning.setWrapText(true);
            columnWarning.setMaxWidth(200);
            alert.getDialogPane().setContent(columnWarning);

            var result1 = alert.showAndWait();

            if (result1.get().getButtonData().equals(ButtonBar.ButtonData.OK_DONE)){*/
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
                        .bind(ovenTempProperty.greaterThan(40.0)
                        .or(isDetectorOn)
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
                            lserConstantInfo.setMaxWidth(400);
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
                            lserConstantInfo.setMaxWidth(400);
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
                            lserConstantInfo.setMaxWidth(400);
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
                            lserConstantInfo.setMaxWidth(400);
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
                        Label aInfoLOW = new Label("Mild H-Bond selection (Low Temp)");
                        Label aInfoHIGH = new Label("Slight H-Bond selection (High Temp)");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfoLOW,aInfoHIGH,lInfo);
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
                            lserConstantInfo.setMaxWidth(400);
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
                            lserConstantInfo.setMaxWidth(400);
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
                        Label eInfoLOW = new Label("Mild to Slight e-LonePair/Pi-Pi Anti-Selection (Low Temp)");
                        Label eInfoHIGH = new Label("Slight to Almost No e-LonePair/Pi-Pi Selection (High Temp)");
                        Label sInfo = new Label("Moderate Dipole/Dipole selection");
                        Label aInfo = new Label("Slight H-Bond selection");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfoLOW,eInfoHIGH,sInfo,aInfo,lInfo);
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
                            lserConstantInfo.setMaxWidth(400);
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
                        Label aInfoLOW = new Label("Mild H-Bond selection (Low Temp)");
                        Label aInfoHIGH = new Label("Slight H-Bond selection (High Temp)");
                        Label lInfo = new Label("Mild-Moderate Cavity/Dispersion selection");
                        VBox selectivityInfo = new VBox(stationaryPhase,genInfo,eInfo,sInfo,aInfoLOW,aInfoHIGH,lInfo);
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
                            lserConstantInfo.setMaxWidth(400);
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
                            lserConstantInfo.setMaxWidth(400);
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
                    Label warningMessage1 = new Label("Error: FID Detector is Active");
                    warningMessage1.setTextFill(Color.RED);
                    warningMessage1.setFont(Font.font(null,FontWeight.BOLD,10));
                    warningMessage1.visibleProperty().bind(isDetectorOn);
                    Label warningMessage2 = new Label("Error: Oven Temperature >= 40 degrees C");
                    warningMessage2.setTextFill(Color.RED);
                    warningMessage2.setFont(Font.font(null,FontWeight.BOLD,10));
                    warningMessage2.visibleProperty().bind(ovenTempProperty.greaterThan(40.0));
                    Label columnWarning = new Label("Tip: If you uninstall your current column, it will still contain any uneluted analytes if you later reinstall it.");
                    columnWarning.setFont(Font.font(null, FontPosture.ITALIC,10));
                    columnWarning.setTextFill(Color.DODGERBLUE);
                    columnWarning.setWrapText(true);
                    columnWarning.setMaxWidth(250);
                    columnChoicesRoot.getChildren().addAll(columnWarning,
                        columnChoicesGrid, warningMessage1,warningMessage2);
                columnChoices.getDialogPane().setHeader(columnChoicesRoot);

                Optional<Column> result = columnChoices.showAndWait();
                if (result.isPresent()) {
                    CURRENT_COLUMN = result.get();
                    columnName.setText(CURRENT_COLUMN.toString());
                }
            /*}*/
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
                                if (columnMaxTempExceeded()) damageColumn();

                                // Update split ratio
                                splitRatioProperty.set(MachineSettings.SPLIT_RATIO);

                                // Update ovenTempProperty & check if temp is ramping or cooling, then nudge temp
                                ovenTempProperty.set(MachineSettings.OVEN_TEMPERATURE);
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

                                // Update the peaks shape and elution times if the oven temp is ramping or cooling, if the
                                // column is damaged at all, or run() has been called 10 times with no updating
                                if (MachineSettings.TEMP_RAMPING.get()
                                        || MachineSettings.TEMP_COOLING.get()
                                        || CURRENT_COLUMN_DAMAGE > 0.0
                                        || runCounter%10 == 0) {
                                    for (Peak peak : ANALYTES_IN_COLUMN) {
                                        // Check if the peak is eluting; if it is, don't update it.
                                        if (currentTime() >= (peak.getElutionTime()
                                                - peak.ascendingCurve.calcWidthOfHalfCurve())) {
                                            continue;
                                        } else {
                                            peak.updatePeak();
                                        }
                                    }
                                    // reset runCounter, wait for another 10 run() calls
                                    runCounter=0;
                                }
                                runCounter++;
                                // After updating, make all peaks traverse the column
                                for (Peak peak : ANALYTES_IN_COLUMN) {
                                        peak.traverseColumn();
                                }

                                // Iterate through all the peaks and remove them if they have already eluted
                                Iterator<Peak> peakIterator = ANALYTES_IN_COLUMN.iterator();
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
                                if (isDetectorOn.get()){
                                    detectorSignal = detect() + noise;
                                }else detectorSignal = 0;

                                // Add datapoint every 50 ms (x = currentTime, y = detectorSignal)
                                dataSeries.getData().add(new XYChart.Data<>(currentTime(), detectorSignal));

                                // Remove any datapoints that are older than 30 minutes
                                if (dataSeries.getData().size() > 24000) {
                                    dataSeries.getData().remove(0);
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
            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);
            List<XYChart.Data<Number, Number>> xDataList = dataSeries.getData();
            DoubleStream xDoubleStream1 = xDataList.stream()
                    .mapToDouble(data -> data.getXValue().doubleValue());
            double minX = xDoubleStream1.min().orElse(0);
            DoubleStream xDoubleStream2 = xDataList.stream()
                    .mapToDouble(data -> data.getXValue().doubleValue());
            double maxX = xDoubleStream2.max().getAsDouble();
            xAxis.setLowerBound(minX);
            xAxis.setUpperBound(maxX + 30); // 30 seconds out from currentTime
            xAxis.setTickUnit(xAxis.getUpperBound()/10.0);

            // Set yAxis
            yAxis.setLowerBound(0);
            List<XYChart.Data<Number, Number>> yDataList = dataSeries.getData();
            double maxY = yDataList.stream()
                    .mapToDouble(data -> data.getYValue().doubleValue())
                    .max()
                    .orElse(100); // return maxY in the dataList or 100 if there is no XYChart.Data (null)
            yAxis.setUpperBound(maxY + 100);
            yAxis.setTickUnit(yAxis.getUpperBound()/10.0);
        });

        // TOGGLE AUTOZOOM BUTTON
        Button toggleAutoZoom = new Button("Toggle\nAutozoom");
        toggleAutoZoom.setFont(Font.font(null, FontWeight.BOLD, 10));
        toggleAutoZoom.setPrefWidth(70);
        toggleAutoZoom.setPrefHeight(45);
        // Action
        toggleAutoZoom.setOnAction(e -> {
            xAxis.setAutoRanging(xAxis.autoRangingProperty().not().get());
            yAxis.setAutoRanging(yAxis.autoRangingProperty().not().get());
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
            zoomInfo.getDialogPane().setMaxWidth(350);
            zoomInfo.setTitle("Click-n-Drag Zoom");
            zoomInfo.setHeaderText("Examine peaks closely!");
            Label content = new Label("Tip: To see closeup(s) of peak(s), HOLD CTRL + CLICK & DRAG to draw a zooming rectangle. Examine peaks for asymmetry and overlapping with other peaks. This is especially important when your sample mixture contains dozens or hundreds of analytes!");
            content.setWrapText(true);
            content.setTextFill(Color.DODGERBLUE);
            content.setFont(Font.font(null, FontPosture.ITALIC, 10));
            content.setMaxWidth(250);
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
        // ACTION 1 -- ESTABLISH ORIGIN OF ZOOMING RECTANGLE (AND GET minX & maxY)
        lineChart.addEventHandler(MouseEvent.MOUSE_PRESSED, e1 -> {
            // 205 pixels is area of LineChart that I don't want users to be able to click on
            if (e1.isControlDown() && e1.getSceneX() > 205.0){
                xAxis.setAutoRanging(false);
                yAxis.setAutoRanging(false);

                Point2D mousePressPointInScene = new Point2D(e1.getSceneX(), e1.getSceneY());
                double xPosInNumberAxis = xAxis.sceneToLocal(new Point2D(mousePressPointInScene.getX(), 0)).getX();
                double yPosInNumberAxis = yAxis.sceneToLocal(new Point2D(0, mousePressPointInScene.getY())).getY();
                double x = xAxis.getValueForDisplay(xPosInNumberAxis).doubleValue();
                double y = yAxis.getValueForDisplay(yPosInNumberAxis).doubleValue();

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
        lineChart.addEventHandler(MouseEvent.MOUSE_DRAGGED, e2 -> {
            // 205 pixels is area of LineChart that I don't want users to be able to click on
            if (e2.isControlDown() && clickDragRectangle.isVisible() && e2.getSceneX() > 205.0) {

                Point2D dragToPointInScene = new Point2D(e2.getSceneX(), e2.getSceneY());
                double xPosInNumberAxis = xAxis.sceneToLocal(new Point2D(dragToPointInScene.getX(), 0)).getX();
                double yPosInNumberAxis = yAxis.sceneToLocal(new Point2D(0, dragToPointInScene.getY())).getY();
                double x = xAxis.getValueForDisplay(xPosInNumberAxis).doubleValue();
                double y = yAxis.getValueForDisplay(yPosInNumberAxis).doubleValue();

                // Establish ZoomRectangle's width & height in ScenePixels
                clickDragRectangle.setWidth(Math.abs(e2.getSceneX() - clickDragRectangle.getX()));
                clickDragRectangle.setHeight(Math.abs(e2.getSceneY() - clickDragRectangle.getY()));
                maxX.set(x);
                minY.set(y);
            }
        });
        // ACTION 3 -- UPDATE X AND Y AXES UPPER AND LOWER BOUNDS (ZOOM TO zoomRectangle)
        lineChart.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (clickDragRectangle.isVisible()) {
                xAxis.setLowerBound(Math.max(minX.get(), 0));
                xAxis.setUpperBound(maxX.get());
                yAxis.setLowerBound(Math.max(minY.get(),0));
                yAxis.setUpperBound(maxY.get());
                clickDragRectangle.setVisible(false);

                // Set tickUnits
                xAxis.setTickUnit((int)(xAxis.getUpperBound()/10.0));
                yAxis.setTickUnit((int)(yAxis.getUpperBound()/10.0));
            }
        });

        // ZOOM-IN BUTTON
        Button zoomInButton = new Button();
        FontIcon zoomIn = FontIcon.of(FluentUiFilledMZ.ZOOM_IN_24);
        zoomIn.setIconSize(20);
        zoomInButton.setGraphic(zoomIn);
        zoomInButton.setOnAction(e -> {
            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);
            double currentLowX = xAxis.getLowerBound();
            double currentHighX = xAxis.getUpperBound();
            double xRange = Math.abs(currentHighX - currentLowX);
            double currentLowY = yAxis.getLowerBound();
            double currentHighY = yAxis.getUpperBound();
            double yRange = Math.abs(currentHighY - currentLowY);

            // TODO: 4/29/2023
            xAxis.setLowerBound(currentLowX+(0.10*xRange));
            xAxis.setUpperBound(currentHighX-(0.10*xRange));
//            yAxis.setLowerBound(currentLowY+(0.10*yRange));
            yAxis.setUpperBound(currentHighY-(0.10*yRange));

            // Set tickUnits
            xAxis.setTickUnit((int)(currentHighX/10.0));
            yAxis.setTickUnit((int)(currentHighY/10.0));
        });

        Button zoomOutButton = new Button();
        FontIcon zoomOut = FontIcon.of(FluentUiFilledMZ.ZOOM_OUT_24);
        zoomOut.setIconSize(20);
        zoomOutButton.setGraphic(zoomOut);

        // ZOOM-OUT BUTTON
        zoomOutButton.setOnAction(e -> {
            xAxis.setAutoRanging(false);
            yAxis.setAutoRanging(false);
            double currentLowX = xAxis.getLowerBound();
            double currentHighX = xAxis.getUpperBound();
            double xRange = Math.abs(currentHighX - currentLowX);
            double currentLowY = yAxis.getLowerBound();
            double currentHighY = yAxis.getUpperBound();
            double yRange = Math.abs(currentHighY - currentLowY);

            // TODO: 4/29/2023
            xAxis.setLowerBound(Math.max(currentLowX-(xRange*0.10),0));
            xAxis.setUpperBound(currentHighX+(xRange*0.10));
            yAxis.setUpperBound(currentHighY*1.10);
            yAxis.setLowerBound(Math.max(currentLowY-(yRange*0.10),0));

            // Set tickUnits
            xAxis.setTickUnit((int)(currentHighX/10.0));
            yAxis.setTickUnit((int)(currentHighY/10.0));
        });



        // UI-VIEW-PANEL PARENT NODE
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


        // CONTROLS ORDER
        // LEFT CONTROLS BUTTON ORDER
        leftControls.getChildren().add(createVSpacer());
        leftControls.getChildren().add(uiViewPanel);
        leftControls.getChildren().add(createVSpacer());
        leftControls.getChildren().add(switchColumnButton);
        leftControls.getChildren().add(setOvenTempVBox);
        leftControls.getChildren().add(splitRatioButton);
        leftControls.getChildren().add(injectButton);
        leftControls.getChildren().add(simulationStateButton);
        leftControls.getChildren().add(createVSpacer());
//        leftControls.getChildren().addAll(columnDamage,columnRem);
        leftControls.getChildren().add(elutionTimesButton);
        leftControls.setAlignment(Pos.CENTER);
        ImageView title = makeImageView("50-06-6noName.png");
        leftControls.getChildren().add(title);


        // Create main scene and show main stage
        Scene scene = new Scene(root, screenBounds.getWidth()-100, screenBounds.getHeight()-100 );
        stage.setScene(scene);
        stage.show();

        // Run Simulator
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
                    // Don't generate new data points when paused
                    return;
                }

                Platform.runLater(() -> {
                    // Increment Internal Clock
                    incCurrentTime();

                    // Column reaches 100% damage after many thousands of calls to damageColumn()
                    // The amount of time this takes depends on how badly the column's max temp is exceeded
                    if (columnMaxTempExceeded()) damageColumn();

                    // Update ovenTempProperty & check if temp is ramping or cooling, then nudge temp
                    ovenTempProperty.set(MachineSettings.OVEN_TEMPERATURE);
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

                    // Update the peaks shape and elution times if the oven temp is ramping or cooling, if the
                    // column is damaged at all, or run() has been called 10 times
                    /*if (MachineSettings.TEMP_RAMPING.get()
                            || MachineSettings.TEMP_COOLING.get()
                            || CURRENT_COLUMN_DAMAGE > 0.0
                            || runCounter%10 == 0) {
                        for (Peak peak : ANALYTES_IN_COLUMN) {
                            // Check if the peak is eluting; if it is, don't update it.
                            if (currentTime() >= (peak.getElutionTime()
                                    - peak.ascendingCurve.calcWidthOfHalfCurve())) {
                                continue;
                            } else {
                                peak.updatePeak();
                            }
                        }
                        // reset runCounter, wait for another 10 run() calls
                        runCounter=0;
                    }*/

                    for (Peak peak : ANALYTES_IN_COLUMN) {
                        // Check if the peak is eluting; if it is, don't update it.
                        if (currentTime() >= (peak.getElutionTime()
                                - peak.ascendingCurve.calcWidthOfHalfCurve())) {
                            continue;
                        } else {
                            peak.updatePeak();
                            peak.traverseColumn();
                        }
                    }

                    // Update split ratio
                    splitRatioProperty.set(MachineSettings.SPLIT_RATIO);

                    // Iterate through all the peaks and remove them if they have already eluted
                    Iterator<Peak> peakIterator = ANALYTES_IN_COLUMN.iterator();
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
                    if (isDetectorOn.get()){
                        detectorSignal = detect() + noise;
                    }else detectorSignal = 0;

                    // Add datapoint every 50 ms (x = currentTime, y = detectorSignal)
                    dataSeries.getData().add(new XYChart.Data<>(currentTime(), detectorSignal));

                    // Remove any datapoints that are older than 30 minutes
                    if (dataSeries.getData().size() > 24000) {
                        dataSeries.getData().remove(0);
                    }
                    runCounter++;
                });
            }
        }, 0, FRAME_LENGTH_MS.get());
    }

    public static void main(String[] args) {
        launch();
    }
}

