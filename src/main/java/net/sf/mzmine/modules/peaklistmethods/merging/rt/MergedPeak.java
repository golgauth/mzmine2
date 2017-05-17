/*
 * @Author Gauthier Boaglio
 */

package net.sf.mzmine.modules.peaklistmethods.merging.rt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.Nonnull;

import org.openscience.cdk.interfaces.IMolecularFormula;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;


public class MergedPeak implements Feature {

    // Data file of this chromatogram
    private RawDataFile dataFile;

    // Data points of the merged peak (map of scan number -> m/z peak)
    private Hashtable<Integer, DataPoint> dataPointsMap;

    // Chromatogram m/z, RT, height, area
    private double mz, rt, height, area;
    private Double fwhm = null, tf = null, af = null;

    // Top intensity scan, fragment scan
    private int representativeScan = -1, fragmentScan = -1;

    // Ranges of raw data points
    private Range<Double> rawDataPointsIntensityRange, rawDataPointsMZRange,
            rawDataPointsRTRange;

    // Keep track of last added data point
    private DataPoint lastMzPeak;

    // Isotope pattern. Null by default but can be set later by deisotoping
    // method.
    private IsotopePattern isotopePattern;
    private int charge = 0;

    // Array of scan numbers
    private int[] scanNumbers;

    private int rowId;
    
    private FeatureStatus peakStatus;

    public int getRowID() {
        return this.rowId;
    }
    public void setRowID(int rowID) {
        this.rowId = rowID;
    }
    
    //-
    private DataPoint dataPointsPerScan[];
    // Boundaries of the peak raw data points
    private Range<Double> rtRange, mzRange, intensityRange;
    // Number of most intense fragment scan
    private int fragmentScanNumber;

    

    /**
     * Initializes this MergedPeak
     */
    public MergedPeak(RawDataFile dataFile, int rowId, FeatureStatus status) {
        this.dataFile = dataFile;

        this.rowId = rowId;

        // Create a copy, not a reference
        rawDataPointsRTRange = Range.closed(dataFile.getDataRTRange(1)
                .lowerEndpoint(), dataFile.getDataRTRange(1).upperEndpoint());
        // new Range(dataFile.getDataRTRange(1));

        dataPointsMap = new Hashtable<Integer, DataPoint>();
        
        peakStatus = status;
    }

    public MergedPeak(RawDataFile dataFile) {
        this(dataFile, -1, FeatureStatus.UNKNOWN);
    }

    public MergedPeak(RawDataFile dataFile, FeatureStatus status) {
        this(dataFile, -1, status);
    }

    
    /**
     * This method adds a MzPeak to this MergedPeak.
     * 
     * @param mzValue
     */
    public void addMzPeak(int scanNumber, DataPoint mzValue) {
        dataPointsMap.put(scanNumber, mzValue);
    }

    public DataPoint getDataPoint(int scanNumber) {
        return dataPointsMap.get(scanNumber);
    }

    /**
     * Returns m/z value of last added data point
     */
    public DataPoint getLastMzPeak() {
        return lastMzPeak;
    }

    /**
     * This method returns m/z value of the merged peak
     */
    public double getMZ() {
        return mz;
    }

    /**
     * This method returns a string with the basic information that defines this
     * peak
     * 
     * @return String information
     */
    public String getName() {
        return "Merged peak "
                + MZmineCore.getConfiguration().getMZFormat().format(mz)
                + " m/z";
    }

    public double getArea() {
        return area;
    }

    public double getHeight() {
        return height;
    }

    public int getMostIntenseFragmentScanNumber() {
        return fragmentScan;
    }

    /**
     * Overwrite the scan number of fragment scan
     * 
     * @param scanNumber
     */
    public void setMostIntenseFragmentScanNumber(int scanNumber) {
        this.fragmentScan = scanNumber;
    }

    public @Nonnull
    FeatureStatus getFeatureStatus() {
        return peakStatus;
    }
//    public void setFeatureStatus(FeatureStatus status) {
//        this.peakStatus = status;
//    }


    public double getRT() {
        return rt;
    }

    public @Nonnull
    Range getRawDataPointsIntensityRange() {
        return rawDataPointsIntensityRange;
    }

    public @Nonnull
    Range getRawDataPointsMZRange() {
        return rawDataPointsMZRange;
    }

    public @Nonnull
    Range getRawDataPointsRTRange() {
        return rawDataPointsRTRange;
    }

    public int getRepresentativeScanNumber() {
        return representativeScan;
    }

    public @Nonnull
    int[] getScanNumbers() {
        return scanNumbers;
    }

    public @Nonnull
    RawDataFile getDataFile() {
        return dataFile;
    }

    public IsotopePattern getIsotopePattern() {
        return isotopePattern;
    }

    public void setIsotopePattern(@Nonnull IsotopePattern isotopePattern) {
        this.isotopePattern = isotopePattern;
    }

    public void finishMergedPeak() {

        int allScanNumbers[] = Ints.toArray(dataPointsMap
                .keySet());
        Arrays.sort(allScanNumbers);

        scanNumbers = allScanNumbers;

        // Calculate median m/z
        double allMzValues[] = new double[allScanNumbers.length];
        for (int i = 0; i < allScanNumbers.length; i++) {
            allMzValues[i] = dataPointsMap.get(allScanNumbers[i]).getMZ();
        }
        mz = MathUtils.calcQuantile(allMzValues, 0.5f);

        // Update raw data point ranges, height, rt and representative scan
        height = Double.MIN_VALUE;
        for (int i = 0; i < allScanNumbers.length; i++) {

            DataPoint mzPeak = dataPointsMap.get(allScanNumbers[i]);
            Scan aScan = dataFile.getScan(allScanNumbers[i]);

            // Replace the MzPeak instance with an instance of SimpleDataPoint,
            // to reduce the memory usage. After we finish this merged peak,
            // we don't need the additional data provided by the MzPeak
            SimpleDataPoint newDataPoint = new SimpleDataPoint(mzPeak);
            dataPointsMap.put(allScanNumbers[i], newDataPoint);

            if (i == 0) {
//                rawDataPointsIntensityRange = new Range(mzPeak.getIntensity());
//                rawDataPointsMZRange = new Range(mzPeak.getMZ());
//                rawDataPointsRTRange = new Range(aScan.getRetentionTime());
                rawDataPointsIntensityRange = Range.singleton(mzPeak.getIntensity());
                rawDataPointsMZRange = Range.singleton(mzPeak.getMZ());
                rawDataPointsRTRange = Range.singleton(aScan.getRetentionTime());
            } else {
//                rawDataPointsIntensityRange.extendRange(mzPeak.getIntensity());
//                rawDataPointsMZRange.extendRange(mzPeak.getMZ());
//                rawDataPointsRTRange.extendRange(aScan.getRetentionTime());
                rawDataPointsIntensityRange.span(Range.singleton(mzPeak.getIntensity()));
                rawDataPointsMZRange.span(Range.singleton(mzPeak.getMZ()));
                rawDataPointsRTRange.span(Range.singleton(aScan.getRetentionTime()));
            }

            if (height < mzPeak.getIntensity()) {
                height = mzPeak.getIntensity();
                rt = aScan.getRetentionTime();
                representativeScan = allScanNumbers[i];
            }
        }

        // TODO: Change it to be cumulative and according to option
        // "Only DETECTED"
        // Update area
        area = 0;

        for (int i = 1; i < allScanNumbers.length; i++) {
            // For area calculation, we use retention time in seconds
            double previousRT = dataFile.getScan(allScanNumbers[i - 1])
                    .getRetentionTime() * 60d;
            double currentRT = dataFile.getScan(allScanNumbers[i])
                    .getRetentionTime() * 60d;

            double previousHeight = dataPointsMap.get(allScanNumbers[i - 1])
                    .getIntensity();
            double currentHeight = dataPointsMap.get(allScanNumbers[i])
                    .getIntensity();
            area += (currentRT - previousRT) * (currentHeight + previousHeight)
                    / 2;
        }

        // Update fragment scan
        fragmentScan = ScanUtils.findBestFragmentScan(dataFile,
                dataFile.getDataRTRange(1), rawDataPointsMZRange);

        if (fragmentScan > 0) {
            Scan fragmentScanObject = dataFile.getScan(fragmentScan);
            int precursorCharge = fragmentScanObject.getPrecursorCharge();
            if ((precursorCharge > 0) && (this.charge == 0))
                this.charge = precursorCharge;
        }

        
    }
    
//    public void setSpectrumOfInterest(List<DataPoint> spectrumOfInterest) {
//        
//        
//    }
    
    public void setSpectrumOfInterest(List<Feature> mergedPeaks) {
        
        
        // Use isotope pattern property to keep significant mz resulting from merge
        isotopePattern = calculateIsotopePattern(representativeScan, mergedPeaks);
        setIsotopePattern(isotopePattern);    
        
    }
    
    
    private IsotopePattern calculateIsotopePattern(
            /*IMolecularFormula cdkFormula, double minAbundance, int charge,
            PolarityType polarity*/
            int apexScanNumber, List<Feature> mergedPeaks
            ) {

        /*
        // TODO: check if the formula is not too big (>100 of a single atom?).
        // if so, just cancel the prediction

        // Set the minimum abundance of isotope
        IsotopePatternGenerator generator = new IsotopePatternGenerator(
                minAbundance);

        org.openscience.cdk.formula.IsotopePattern pattern = generator
                .getIsotopes(cdkFormula);

        int numOfIsotopes = pattern.getNumberOfIsotopes();
        
        DataPoint dataPoints[] = new DataPoint[numOfIsotopes];

        for (int i = 0; i < numOfIsotopes; i++) {
            IsotopeContainer isotope = pattern.getIsotope(i);

            // For each unit of charge, we have to add or remove a mass of a
            // single electron. If the charge is positive, we remove electron
            // mass. If the charge is negative, we add it.
            double mass = isotope.getMass()
                    + (polarity.getSign() * -1 * charge * ELECTRON_MASS);

            if (charge != 0)
                mass /= charge;

            double intensity = isotope.getIntensity();

            dataPoints[i] = new SimpleDataPoint(mass, intensity);
        }

        String formulaString = MolecularFormulaManipulator
                .getString(cdkFormula);
         */
        
        List<DataPoint> dataPoints = new ArrayList<>();
        
        for (int i=0; i < mergedPeaks.size(); i++) {
            
            if (mergedPeaks.get(i).getDataPoint(apexScanNumber) != null)
                dataPoints.add(mergedPeaks.get(i).getDataPoint(apexScanNumber));
        }
        
        DataPoint[] dataPointsArr = dataPoints.toArray(new DataPoint[dataPoints.size()]);
        SimpleIsotopePattern newPattern = new SimpleIsotopePattern(dataPointsArr, IsotopePatternStatus.PREDICTED, "DETECTED-from-merger");

        return newPattern;

    }

    public int getCharge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return PeakUtils.peakToString(this);
    }

    @Override
    public Double getFWHM() {
        return fwhm;
    }

    @Override
    public Double getTailingFactor() {
        return tf;
    }

    @Override
    public Double getAsymmetryFactor() {
        return af;
    }

    public void setFWHM(Double fwhm) {
        this.fwhm = fwhm;
    }

    public void setTailingFactor(Double tf) {
        this.tf = tf;
    }

    public void setAsymmetryFactor(Double af) {
        this.af = af;
    }
    
}
