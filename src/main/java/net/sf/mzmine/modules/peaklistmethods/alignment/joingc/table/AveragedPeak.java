/*
 * @Author Gauthier Boaglio
 */

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;


public class AveragedPeak implements Feature {

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
    
    
    // Avg stuffs
//    HashMap<Double, ArrayList<DataPoint>> perMzDataPointsMapping = new HashMap<Double, ArrayList<DataPoint>>();
    HashMap<Integer, ArrayList<DataPoint>> perScanDataPointsMapping = new HashMap<Integer, ArrayList<DataPoint>>();
    

    private int rowId;

    public int getRowID() {
        return this.rowId;
    }

    public void setRowID(int rowID) {
        this.rowId = rowID;
    }

    /**
     * Initializes this MergedPeak
     */
    public AveragedPeak(RawDataFile dataFile, int rowId) {
        this.dataFile = dataFile;

        this.rowId = rowId;

        // Create a copy, not a reference
        rawDataPointsRTRange = Range.closed(dataFile.getDataRTRange(1)
                .lowerEndpoint(), dataFile.getDataRTRange(1).upperEndpoint());
        // new Range(dataFile.getDataRTRange(1));

        dataPointsMap = new Hashtable<Integer, DataPoint>();
    }

    public AveragedPeak(RawDataFile dataFile) {
        this(dataFile, -1);
    }

    /**
     * This method adds a MzPeak to this Feature.
     * 
     * @param mzValue
     */
    public void addMzPeak(int scanNumber, DataPoint mzValue) {
        dataPointsMap.put(scanNumber, mzValue);
        /*ArrayList<DataPoint> perScanDps = perMzDataPointsMapping.get(mzValue.getMZ());
        if (perScanDps == null) {
            perMzDataPointsMapping.put(mzValue.getMZ(), new ArrayList<DataPoint>());
            perScanDps = perMzDataPointsMapping.get(mzValue.getMZ());
        }
        perScanDps.add(mzValue);*/
        ArrayList<DataPoint> perScanDps = perScanDataPointsMapping.get(scanNumber);
        if (perScanDps == null) {
            perScanDataPointsMapping.put(scanNumber, new ArrayList<DataPoint>());
            perScanDps = perScanDataPointsMapping.get(mzValue.getMZ());
        }
        perScanDps.add(mzValue);
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
        return "Averaged peak "
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
        return FeatureStatus.DETECTED;
    }

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

    public void finishAveragedPeak() {
        
//        int allScanNumbers[] = Ints.toArray(dataPointsMap.keySet());
//        Arrays.sort(allScanNumbers);

        // Reset "twisted" dataPointsMap
        dataPointsMap.clear();
        // Do averages
        // "fake" scan num: Scan numbers do not mean anything in this "Averaged Feature" 
        //                      context, cause we are working across Features belonging
        //                      to various DataFiles...
//        int scan_num = 460; //1;
//      for (Double mz : perMzDataPointsMapping.keySet()) {
      for (Integer scan_num : perScanDataPointsMapping.keySet()) {
            ArrayList<DataPoint> scanDps = perScanDataPointsMapping.get(scan_num);
            Double avg = 0d;
            for (DataPoint dp : scanDps) {
                avg += dp.getIntensity();
            }
            avg /= scanDps.size();
            // New DataPoint at "fake" scan num
            DataPoint avg_dp = new SimpleDataPoint(mz, avg);
            dataPointsMap.put(scan_num, avg_dp);
            
            scan_num++;
        }
        
        // Keep going with regular behavior  
        //...
        
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
                rawDataPointsIntensityRange = Range.singleton(mzPeak.getIntensity());
                rawDataPointsMZRange = Range.singleton(mzPeak.getMZ());
//                rawDataPointsRTRange = Range.singleton(aScan.getRetentionTime());
            } else {
                rawDataPointsIntensityRange.span(Range.singleton(mzPeak.getIntensity()));
                rawDataPointsMZRange.span(Range.singleton(mzPeak.getMZ()));
//                rawDataPointsRTRange.span(Range.singleton(aScan.getRetentionTime()));
            }

            if (height < mzPeak.getIntensity()) {
                height = mzPeak.getIntensity();
//                rt = aScan.getRetentionTime();
                representativeScan = allScanNumbers[i];
            }
        }

//        // Update area
//        area = 0;
//
//        for (int i = 1; i < allScanNumbers.length; i++) {
//            // For area calculation, we use retention time in seconds
//            double previousRT = dataFile.getScan(allScanNumbers[i - 1])
//                    .getRetentionTime() * 60d;
//            double currentRT = dataFile.getScan(allScanNumbers[i])
//                    .getRetentionTime() * 60d;
//
//            double previousHeight = dataPointsMap.get(allScanNumbers[i - 1])
//                    .getIntensity();
//            double currentHeight = dataPointsMap.get(allScanNumbers[i])
//                    .getIntensity();
//            area += (currentRT - previousRT) * (currentHeight + previousHeight)
//                    / 2;
//        }

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