package com.karimbouchareb.chromatographysimulator;

import java.util.function.Function;

/**
 * Each Peak object has two GaussianCurve fields: ascendingCurve and descendingCurve. This is necessary because
 * asymmetric curves are possible (peak fronting & peak tailing).
 */
public class GaussianCurve implements Function<Double, Double> {
    static final double IDEAL_PEAK_SIGMA = 0.1;
    private double amplitude;
    private double mu;
    private double sigma = IDEAL_PEAK_SIGMA; // width of peak will always be ideal unless changed by circumstances

    /**
     * @param amplitude is Detector Response. Amplitude is decided based on relative response factor (e.g. the
     * relative response of the analyte this curve represents compared to the response generated by 133 ng of
     * methyl-Octanoate (De Saint Laumer et. al 2015)).
     *
     * @param mu is Retention time. Mu is decided based on SolvationParameter equation from LSER model:
     * ---    logk = c+eE+sS+aA+lL    ----
     * NOTE: bB term is excluded because for all columns simulated, scientific data indicates that b system
     * constant == 0 (Poole et. al 2019)
     *
     * @param sigma is a measure related to peak width. The ascendingCurvePeakWidth (sigma) & descendingCurvePeakWidth
     * (sigma) are based on a variety of circumstances (see readMe).
     */
    public GaussianCurve(double amplitude, double mu, double sigma) {
        this.amplitude = amplitude;
        this.mu = mu;
        this.sigma = sigma;
    }

    public void setAmplitude(double amplitude) {
        this.amplitude = amplitude;
    }

    public void setMu(double mu) {
        this.mu = mu;
    }

    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    /**
     * {@link GaussianCurve#apply(Double)} returns the y-value of the curve as a function of x.
     */
    @Override
    public Double apply(Double currentTime) {
        double exponent = -Math.pow(currentTime - mu, 2) / (2 * Math.pow(sigma, 2));
        return amplitude * Math.exp(exponent);
    }

    /**
     * This method and {@link GaussianCurve#sharedAmplitude(double, GaussianCurve, GaussianCurve)} are used for
     * reshaping a Peak object. Note that the peak's area is always held constant despite changing shape.
     */
    double calcWidthOfHalfCurve() {
        double scaleY = amplitude * 0.001; // corresponds to 0.1% of peak amplitude

        // Calculate the width from the center (mu) to one side of the
        // curve where y = 0.1% of amplitude of the curve.
        double halfWidth = sigma * Math.sqrt(-2 * Math.log(scaleY / amplitude));

        return halfWidth;
    }

    /**
     * This method is used to find the value for amplitude that will hold a curve's area constant when it is constructed
     * from an ascending curve and descending curve with different widths.
     */
    static double sharedAmplitude(double asymmetricPeakArea, GaussianCurve ascending, GaussianCurve descending){
        return asymmetricPeakArea / (0.5 * Math.sqrt(2*Math.PI)
                * (ascending.sigma + descending.sigma));
    }
}
