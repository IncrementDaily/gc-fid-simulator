package com.karimbouchareb.chromatographysimulator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.controlsfx.control.CheckComboBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scratch {

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

        public double calcHalfArea(){
            return (amplitude * (sigma * Math.sqrt(2*Math.PI)))/2.0;
        }

        public static double findAsymmetricAmplitude(double asymmetricPeakArea, GaussianCurve ascending, GaussianCurve descending){
            return asymmetricPeakArea / (0.5 * Math.sqrt(2*Math.PI)
                    * (ascending.sigma + descending.sigma));
        }
    }
    public static void test(double[] array) {
        System.out.println(Arrays.toString(array));
    }

    public static void test2(int[][] array){
        int randomIndex = (int)(Math.random() * 100);
        int randomValue = (int)(Math.random() * 10);
        array[randomIndex][randomIndex] = randomValue;
    }

    public static void main(String[] args) {
        int[][] testy = new int[101][101];

        for (int i = 0; i < 10000; i++) {
            test2(testy);
        }

        for (int[] row : testy){
            System.out.println(Arrays.toString(row));
        }


    }
}

