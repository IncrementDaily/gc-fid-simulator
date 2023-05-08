package com.karimbouchareb.chromatographysimulator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.knowm.xchart.*;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolynomialFitting {
    public static final String SPB_OCTYL = "SPB_OCTYL";
    public static final String HP_5 = "HP_5";
    public static final String RXI_5SIL = "RXI_5SIL";
    public static final String RXI_17 = "RXI_17";
    public static final String RTX_440 = "RTX_440";
    public static final String DB_1701 = "DB_1701";
    public static final String RTX_OPP = "RTX_OPP";
    public static final String DB_225 = "DB_225";
    public static final String HP_INNOWAX = "HP_INNOWAX";

    public static class Datapoint{
        public double temp;
        public double constValue;

        public Datapoint(double temp, double constValue){
            this.temp = temp;
            this.constValue = constValue;
        }

        public String toString(){
            return "(" + temp + "," + constValue + ")";
        }
    }
    public static void main(String[] args) {
        String currentColumn = RXI_17;
        String currentConstant = "C";
        final int E = 2;
        final int S = 3;
        final int A = 4;
        final int L = 5;
        final int C = 6;

        int degree = 1;

        // Define the data points as coordinates
        ArrayList<Datapoint> data = new ArrayList<>();

        // Populate the datapoints
        String csvFile = "src/main/java/com/karimbouchareb/chromatographysimulator/columnLSERData.csv";
        try (FileReader fileReader = new FileReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.parse(fileReader)) {

            for (CSVRecord record : parser) {
//                if (record.get(0).equals(currentColumn) && Double.parseDouble(record.get(1)) < 160) continue;
                String check = "C";
                assert check.equals(currentConstant);
                if (record.get(0).equals(currentColumn)){
                    data.add(new Datapoint(Double.parseDouble(record.get(1)), Double.parseDouble(record.get(C)))); // Change record.get(currentConstant) appropriately
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(data);

        // Add the data points
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (Datapoint point : data) {
            points.add(point.temp, point.constValue);
        }

        // Perform the polynomial curve fitting
        PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
        double[] coefficients = fitter.fit(points.toList());
        System.out.println(Arrays.toString(coefficients));

        // Create a polynomial function with the fitted coefficients
        PolynomialFunction fittedPolynomial = new PolynomialFunction(coefficients);

        // Create XChart scatter plot with the original data points
        XYChart chart = new XYChartBuilder().width(800).height(600).title("Polynomial Fit for " + currentConstant + " constant of " + currentColumn).xAxisTitle("Temperature").yAxisTitle("e Value").build();
        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (Datapoint point : data) {
            xData.add(point.temp);
            yData.add(point.constValue);
        }
        XYSeries seriesData = chart.addSeries("Data Points", xData, yData);
        seriesData.setMarker(SeriesMarkers.CIRCLE);

        // Create XChart line plot with the fitted polynomial
        List<Double> xFitData = new ArrayList<>();
        List<Double> yFitData = new ArrayList<>();
        for (double t = 25; t <= 350; t += 1) {
            xFitData.add(t);
            yFitData.add(fittedPolynomial.value(t));
        }
        XYSeries seriesFit = chart.addSeries("Polynomial Fit", xFitData, yFitData);
        seriesFit.setMarker(SeriesMarkers.NONE);

        // Display the chart
        new SwingWrapper<>(chart).displayChart();
    }
}

