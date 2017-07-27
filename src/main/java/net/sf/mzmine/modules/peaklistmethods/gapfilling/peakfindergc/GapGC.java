/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc;

import static net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc.PeakFinderGCParameters.MIN_ABSOLUTE_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc.PeakFinderGCParameters.MIN_RELATIVE_HEIGHT;
import static net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc.PeakFinderGCParameters.PEAK_DURATION;
import static net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc.PeakFinderGCParameters.MIN_RATIO;
import static net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc.PeakFinderGCParameters.SEARCH_RT_RANGE;
import static net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetectorParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RowVsRowScoreGC;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table.PeakListTable;
import net.sf.mzmine.modules.peaklistmethods.merging.rt.MergedPeak;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.SimilarityMethodType;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.PeakResolver;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.ResolvedPeak;
import net.sf.mzmine.modules.peaklistmethods.peakpicking.deconvolution.minimumsearch.MinimumSearchPeakDetector;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.util.MathUtils;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import com.google.common.collect.Range;
import com.sun.tools.xjc.model.CNonElement;

class GapGC {


	static private boolean DEBUG = false;
	

    private Logger logger = Logger.getLogger(this.getClass().getName());

    
    private PeakListRow peakListRow;
    private RawDataFile rawDataFile;

    private Range<Double> mzRange, rtRange;
    private double intTolerance;

    // These store information about peak that is currently under construction
    private List<GapDataPoint> currentPeakDataPoints;
    private List<GapDataPoint> bestPeakDataPoints;
    private double bestPeakHeight;
    private double bestChemSimScore;
    private double minChemSimScore;
    
    private int peak_cnt = 1;
    
    private ParameterSet parameters;
    private final Range<Double> peakDuration;
    private final double searchRTRange;
    
    private PeakList sourcePeakList;
    private PeakListRow sourceRow;
    private int sourceRowNum;


    /**
     * Constructor: Initializes an empty gap
     * 
     * @param mz
     *            M/Z coordinate of this empty gap
     * @param rt
     *            RT coordinate of this empty gap
     */
    GapGC(ParameterSet gap_parameters, PeakList peakList, int row, PeakListRow peakListRow, RawDataFile rawDataFile,
	    Range<Double> mzRange, Range<Double> rtRange, double intTolerance, double minChemSimScore) {

	this.peakListRow = peakListRow;
	this.rawDataFile = rawDataFile;
	this.intTolerance = intTolerance;
	this.mzRange = mzRange;
	this.rtRange = rtRange;

	this.minChemSimScore = minChemSimScore;
	
	this.parameters = gap_parameters;
	this.sourcePeakList = peakList;
	this.sourceRow = peakList.getRow(row);
	this.sourceRowNum = row;
	
	//
        peakDuration = gap_parameters.getParameter(PEAK_DURATION).getValue();
        searchRTRange = gap_parameters.getParameter(SEARCH_RT_RANGE).getValue();
//        final double minRatio = parameters.getParameter(MIN_RATIO).getValue();
//        final double minAbsHeight = parameters.getParameter(MIN_ABSOLUTE_HEIGHT).getValue();
//        final double minRelHeight = parameters.getParameter(MIN_RELATIVE_HEIGHT).getValue();
        final double minRatio = MIN_RATIO.getValue();
        final double minAbsHeight = MIN_ABSOLUTE_HEIGHT.getValue();
        final double minRelHeight = MIN_RELATIVE_HEIGHT.getValue();
        //
        if (DEBUG) {
	        System.out.println("New gap GC :");
	        System.out.println("\t peakDuration=" + peakDuration);
	        System.out.println("\t searchRTRange=" + searchRTRange);
	        System.out.println("\t minRatio=" + minRatio);
	        System.out.println("\t minAbsHeight=" + minAbsHeight);
	        System.out.println("\t minRelHeight=" + minRelHeight);
        }
    }
    
    private Feature detectBestMatchingPeak() {
        
        Feature bestPeak = null;
        
        // Abort if rtRange (once corrected or not) does not share any values with
        // the working RDF (rtRange and RDF range do not overlap each other!)
        if (!rtRange.isConnected(rawDataFile.getDataRTRange())) {
            
            if (DEBUG) {
            	System.out.println("DIFF:: " + rtRange + "-" + rawDataFile.getDataRTRange());
            }
            return null;
        }
//        // Make sure to have candidate range for peak large enough
//        if (Math.abs(rtRange.upperEndpoint() - rtRange.lowerEndpoint()) < peakDuration.lowerEndpoint()) {
//            System.out.println("NO PEAK WAS DECONVOLUTED in range " + rtRange 
//                    + " (for file '" + rawDataFile.getName() + "' - " + rawDataFile.getDataRTRange() + ")" +
//                                "\n\t=> Range too SMALL!");
//            return null;
//        }
//        // Make sure to have candidate range for peak small enough
//        if (Math.abs(rtRange.upperEndpoint() - rtRange.lowerEndpoint()) > peakDuration.upperEndpoint()) {
//            System.out.println("NO PEAK WAS DECONVOLUTED in range " + rtRange 
//                    + " (for file '" + rawDataFile.getName() + "' - " + rawDataFile.getDataRTRange() + ")" +
//                                "\n\t=> Range too SMALL!");
//            return null;
//        }
        
        //-- Deconvolve TIC signal in RT direction (in rtRange) from rawDataFile
        //Feature[] candidatePeaks = new SimpleFeature();
        MergedPeak newPeak = new MergedPeak(rawDataFile, FeatureStatus.ESTIMATED);
        // Build TIC signal (as chromatographic peak into a peaklist)
        Double mz = (mzRange.upperEndpoint() - mzRange.lowerEndpoint()) / 2.0;
        ArrayList<DataPoint> peakDps = new ArrayList<DataPoint>();
        int nb_scans = 0;//, nb_scans2 = 0;
        for (int i = 0; i < rawDataFile.getNumOfScans(); ++i) {
            int scan_num = rawDataFile.getScanNumbers()[i];
            Scan scan = rawDataFile.getScan(scan_num);
            //peakDps.add(new SimpleDataPoint(mzRange.lowerEndpoint(), scan.getTIC()));//ScanUtils.calculateTIC(scan, mzRange)));
            if (rtRange.contains(scan.getRetentionTime())) {
                newPeak.addMzPeak(scan_num, new SimpleDataPoint(mz, scan.getTIC()));
                nb_scans++;
            }
//            nb_scans2++;
        }
        newPeak.finishMergedPeak();
        
        // Make sure the peak is sized properly
        if (DEBUG) {
	
	        System.out.println("### newPeak - 2!! : " + newPeak + " > nbScans: " + newPeak.getScanNumbers().length + " > Range: " + newPeak.getRawDataPointsRTRange());
	//        for (int i = 0; i < newPeak.getScanNumbers().length; i++) {
	//        	DataPoint mzPeak = newPeak.getDataPoint(newPeak.getScanNumbers()[i]);
	//        	Scan scan = rawDataFile.getScan(rawDataFile.getScanNumbers()[i]);
	//        	System.out.println("\t* " + mzPeak + " guessing RT = " + scan.getRetentionTime());
	//        }
        }
        Range<Double> r = newPeak.getRawDataPointsRTRange();
        double peak_duration = Math.abs(r.upperEndpoint() - r.lowerEndpoint());
        double min_duration = peakDuration.lowerEndpoint();
        double max_duration = peakDuration.upperEndpoint();
        if (peak_duration < min_duration || peak_duration > max_duration) {
            if (DEBUG) {
	            System.out.println("NO PEAK WAS DECONVOLUTED in range " + rtRange 
	                    + " (for file '" + rawDataFile.getName() + "' - " + rawDataFile.getDataRTRange() + ")" +
	                                "\n\t=> Peak is BAD SIZED! (duration: " + peak_duration + ", nb_scans: " + nb_scans + ")");
            }
            return null;
        }
        // Make sure to have at least 3 scans (for a peak)
        if (nb_scans < 3) {
            if (DEBUG) {
	            System.out.println("NO PEAK WAS DECONVOLUTED in range " + rtRange 
	                    + " (for file '" + rawDataFile.getName() + "' - " + rawDataFile.getDataRTRange() + ")" +
	                                "\n\t=> Range too SMALL!");
            }
            return null;
        }
        //
            if (DEBUG) {
		        System.out.println("!! PEAK DECONVOLUTED! -> in range " + rtRange 
		                + " (for file '" + rawDataFile.getName() + "' - " + rawDataFile.getDataRTRange() + ")");
            }
        
//        logger.info("newPeak.getScanNumbers().length:" + newPeak.getScanNumbers().length + " [nb_scans: " + nb_scans + " / " + nb_scans2 + "]");
        PeakList peakList = null;
        try {
            String pl_name = "PL" + rtRange + " [#" + newPeak.getScanNumbers()[0] + ", #" + newPeak.getScanNumbers()[newPeak.getScanNumbers().length-1] + "]";
            peakList = new SimplePeakList(pl_name, rawDataFile);
        } catch (Exception e) {
            logger.severe("Could not create PeakList because: ");
            e.printStackTrace();
        }
        PeakListRow plRow = new SimplePeakListRow(1);
        plRow.addPeak(rawDataFile, newPeak);
        peakList.addRow(plRow);
        // Apply deconvolution
        peakList = resolvePeaks(peakList, parameters);
//        //-
//        MZmineProjectImpl project = (MZmineProjectImpl) MZmineCore
//                .getProjectManager().getCurrentProject();
//        project.addPeakList(peakList);
        
        if (peakList.getNumberOfRows() == 0) {
            if (DEBUG) {
	            System.out.println("NO PEAK WAS DECONVOLUTED in range " + rtRange 
	                    + " (for file '" + rawDataFile.getName() + "' - " + rawDataFile.getDataRTRange() + ")" +
	                    		"\n\t=> Deconvolution couldn't get one peak!");
            }
            return null;
        }
        
        
        // Find best candidate (best chem. sim. score at apex)
        // Ref. (column average) scan
        int halfNbMarginScans = 0; // Get apex scan only
        double avgRT = this.sourceRow.getAverageRT();
        //RawDataFile avgRDF = PeakListTable.buildAverageRDF(this.sourcePeakList, col-1, halfNbMarginScans, avgRT);
        //Scan avgApexScan = avgRDF.getScan(1);
        Scan avgApexScan =  PeakListTable.getAverageScanAt(this.sourcePeakList, this.sourceRowNum, avgRT);
        //-
        Map<Feature, Double> allScoringPeaks = new LinkedHashMap<>();
        double bestScore = Double.MIN_VALUE;
        for (PeakListRow a_row : peakList.getRows()) {
            Feature a_peak = a_row.getBestPeak();

            // Candidate scan
            Scan a_apexScan = rawDataFile.getScan(a_peak.getRepresentativeScanNumber());

            // Compute
            double[] vec1 = new double[JDXCompound.MAX_MZ];
            Arrays.fill(vec1, 0.0);
            double[] vec2 = new double[JDXCompound.MAX_MZ];
            Arrays.fill(vec2, 0.0);
            
            DataPoint[] dataPoints = avgApexScan.getDataPoints();
            for (int j=0; j < dataPoints.length; ++j) {
                DataPoint dp = dataPoints[j];
                vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
            }
            //-
            dataPoints = a_apexScan.getDataPoints();
            for (int j=0; j < dataPoints.length; ++j) {
                DataPoint dp = dataPoints[j];
                vec2[(int) Math.round(dp.getMZ())] += dp.getIntensity();
            }
            //-
            double simScore = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
            if (simScore > minChemSimScore && simScore > bestScore) {
                bestScore = simScore;
                bestPeak = a_peak;
            }
            
            if (simScore > bestScore)
            	bestScore = simScore;
            
            if (simScore > minChemSimScore) {
            	allScoringPeaks.put(a_peak, simScore);
            }
        }
        
        if (DEBUG) {
	        if (bestPeak == null) {
	            System.out.println("NO PEAK SCORED ENOUGH "
	            		+ "(best score so far: " + bestScore + " | expected at least: " + minChemSimScore + ")");
	        } else {
	        	System.out.println("!! SOME PEAK SCORED ENOUGH !! > " + bestPeak 
	        			+ " (best score so far: " + bestScore + " | expected at least: " + minChemSimScore + ")");
	        }
        }        	
        
    	// Rollback bestPeak if "already known"
        if (bestPeak != null) {
            
            // Add the peak if and only if this is not a "known" one
            boolean alreadyKnown = PeakFinderGCTask.checkPeak(bestPeak, this.sourcePeakList);
            if (alreadyKnown) {
            	
            	// Forbid to use the already known peak
            	bestPeak = null;
            	
            	if (DEBUG)
            		logger.info("...[DUPLICATE peak found and SKIPPED!] Peak: " + bestPeak + ".");// + " (score: " + bestScore + ").");
            	allScoringPeaks = sortByValue(allScoringPeaks, false);
            	
            	if (allScoringPeaks.size() > 1) {
	            	// Rollback: Try to find a replacement peak
	            	for (Map.Entry<Feature, Double> kv : allScoringPeaks.entrySet()) {
	            		
	            		Feature a_peak = kv.getKey();
	               		//logger.info("....... rollback: " + a_peak + " (score: " + kv.getValue() + ").");
	               		if (a_peak != bestPeak && PeakFinderGCTask.checkPeak(a_peak, this.sourcePeakList)) {
	            			bestPeak = a_peak;
	            			if (DEBUG)
	            				logger.info("...[LESS SCORING peak found and USED!] Peak: " + bestPeak + ".");// + " (score: " + kv.getValue() + ").");
	            			break;
	            		}
	             	}
            	}
            }
            else {
            	// Do nothing: bestPeak is just fine.
            }
        }

        
        return bestPeak;
    }
    
    
    /**
     * Deconvolve a chromatogram into separate peaks.
     * 
     * @param peakList
     *            holds the chromatogram to deconvolve.
     * @return a new peak list holding the resolved peaks.
     * @throws RSessionWrapperException
     */
    private PeakList resolvePeaks(final PeakList peakList, ParameterSet parameters) {

        // Get data file information.
        final RawDataFile dataFile = peakList.getRawDataFile(0);
        final int[] scanNumbers = dataFile.getScanNumbers(1);
        final int scanCount = scanNumbers.length;
        final double[] retentionTimes = new double[scanCount];
        for (int i = 0; i < scanCount; i++) {

            retentionTimes[i] = dataFile.getScan(scanNumbers[i])
                    .getRetentionTime();
        }

        // Peak resolver.

//        final MZmineProcessingStep<PeakResolver> resolver = parameters
//                .getParameter(PEAK_RESOLVER).getValue();
        
        // TODO: Ugly workaround: better use the original "MinimumSearchPeakDetector" class 
        //              and find a way to override its parameters or use it as-is...
        //final PeakResolver resolver = new MinimumSearchPeakDetector();
        final PeakResolver resolver = new MinLocPeakDetector();

        // Create new peak list.
//        final PeakList resolvedPeaks = new SimplePeakList(peakList + " "
//                + parameters.getParameter(SUFFIX).getValue(), dataFile);
        final PeakList resolvedPeaks = new SimplePeakList(peakList + " MinLocDeconv",
                dataFile);

        // Load previous applied methods.
        for (final PeakListAppliedMethod method : peakList.getAppliedMethods()) {

            resolvedPeaks.addDescriptionOfAppliedTask(method);
        }

        // Add task description to peak list.
//        resolvedPeaks
//        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
//                "Peak deconvolution by " + resolver, resolver
//                        .getParameterSet()));

        // Initialise counters.
//        processedRows = 0;
//        totalRows = peakList.getNumberOfRows();
        int peakId = 1;

        // Process each chromatogram.
        final Feature[] chromatograms = peakList.getPeaks(dataFile);
        final int chromatogramCount = chromatograms.length;
        for (int index = 0; /*!isCanceled() &&*/ index < chromatogramCount; index++) {

            final Feature chromatogram = chromatograms[index];

            // Load the intensities into array.
            final double[] intensities = new double[scanCount];
            for (int i = 0; i < scanCount; i++) {

                final DataPoint dp = chromatogram.getDataPoint(scanNumbers[i]);
                intensities[i] = dp != null ? dp.getIntensity() : 0.0;
            }

            // Resolve peaks.
//            final PeakResolver resolverModule = resolver.getModule();
            final ParameterSet resolverParams = parameters;//resolver.getParameterSet();
            Feature[] peaks = null;
            try {
                peaks = resolver.resolvePeaks(chromatogram,
                        scanNumbers, retentionTimes, intensities, resolverParams,
                        null);
            } catch (RSessionWrapperException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            if (peaks == null) return null;

            // Add peaks to the new peak list.
            for (final Feature peak : peaks) {

                final PeakListRow newRow = new SimplePeakListRow(peakId++);
                newRow.addPeak(dataFile, peak);
                resolvedPeaks.addRow(newRow);
            }

//            processedRows++;
        }

        return resolvedPeaks;
    }

    // TODO: Ugly lazy override (nothing changes here except input parameters)
    //          Find a nice wau to keep using "MinimumSearchPeakDetector"
    public class MinLocPeakDetector extends MinimumSearchPeakDetector {

        @Override
        public Feature[] resolvePeaks(final Feature chromatogram,
                final int[] scanNumbers, final double[] retentionTimes,
                final double[] intensities, ParameterSet parameters,
                RSessionWrapper rSession) {

            final int scanCount = scanNumbers.length;
            final int lastScan = scanCount - 1;

            assert scanCount > 0;

//            final Range<Double> peakDuration = parameters.getParameter(PEAK_DURATION).getValue();
//            final double searchRTRange = parameters.getParameter(SEARCH_RT_RANGE).getValue();
//            final double minRatio = parameters.getParameter(MIN_RATIO).getValue();
//            final double minHeight = Math.max(
//                    parameters.getParameter(MIN_ABSOLUTE_HEIGHT).getValue(),
//                    parameters.getParameter(MIN_RELATIVE_HEIGHT).getValue()
//                            * chromatogram.getHeight());
//
//            final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>(2);
//
//            // First, remove all data points below chromatographic threshold.
//            final double chromatographicThresholdLevel = MathUtils.calcQuantile(
//                    intensities,
//                    parameters.getParameter(CHROMATOGRAPHIC_THRESHOLD_LEVEL).getValue());
            final Range<Double> peakDuration = parameters.getParameter(PEAK_DURATION).getValue();
            final double searchRTRange = parameters.getParameter(SEARCH_RT_RANGE).getValue();
            final double minRatio = MIN_RATIO.getValue();
            final double minHeight = Math.max(
                    MIN_ABSOLUTE_HEIGHT.getValue(),
                    MIN_RELATIVE_HEIGHT.getValue()
                            * chromatogram.getHeight());

            final List<ResolvedPeak> resolvedPeaks = new ArrayList<ResolvedPeak>(2);

            // First, remove all data points below chromatographic threshold.
            final double chromatographicThresholdLevel = MathUtils.calcQuantile(
                    intensities,
                    CHROMATOGRAPHIC_THRESHOLD_LEVEL.getValue());
            for (int i = 0; i < intensities.length; i++) {

                if (intensities[i] < chromatographicThresholdLevel) {

                    intensities[i] = 0.0;
                }
            }

            // Current region is a region between two minima, representing a
            // candidate for a resolved peak.
            startSearch: for (int currentRegionStart = 0; currentRegionStart < lastScan - 2; currentRegionStart++) {

                // Find at least two consecutive non-zero data points
                if (intensities[currentRegionStart] != 0.0
                        && intensities[currentRegionStart + 1] != 0.0) {

                    double currentRegionHeight = intensities[currentRegionStart];

                    endSearch: for (int currentRegionEnd = currentRegionStart + 1; currentRegionEnd < scanCount; currentRegionEnd++) {

                        // Update height of current region.
                        currentRegionHeight = Math.max(currentRegionHeight,
                                intensities[currentRegionEnd]);

                        // If we reached the end, or if the next intensity is 0, we
                        // have to stop here.
                        if (currentRegionEnd == lastScan
                                || intensities[currentRegionEnd + 1] == 0.0) {

                            // Find the intensity at the sides (lowest data points).
                            final double peakMinLeft = intensities[currentRegionStart];
                            final double peakMinRight = intensities[currentRegionEnd];

                            // Check the shape of the peak.
                            if (currentRegionHeight >= minHeight
                                    && currentRegionHeight >= peakMinLeft
                                            * minRatio
                                    && currentRegionHeight >= peakMinRight
                                            * minRatio
                                    && peakDuration
                                            .contains(retentionTimes[currentRegionEnd]
                                                    - retentionTimes[currentRegionStart])) {

                                resolvedPeaks.add(new ResolvedPeak(chromatogram,
                                        currentRegionStart, currentRegionEnd));
                            }

                            // Set the next region start to current region end - 1
                            // because it will be immediately
                            // increased +1 as we continue the for-cycle.
                            currentRegionStart = currentRegionEnd - 1;
                            continue startSearch;
                        }

                        // Minimum duration of peak must be at least searchRTRange.
                        if (retentionTimes[currentRegionEnd]
                                - retentionTimes[currentRegionStart] >= searchRTRange) {

                            // Set the RT range to check
                            final Range<Double> checkRange = Range.closed(
                                    retentionTimes[currentRegionEnd]
                                            - searchRTRange,
                                    retentionTimes[currentRegionEnd]
                                            + searchRTRange);

                            // Search if there is lower data point on the left from
                            // current peak i.
                            for (int i = currentRegionEnd - 1; i > 0
                                    && checkRange.contains(retentionTimes[i]); i--) {

                                if (intensities[i] < intensities[currentRegionEnd]) {

                                    continue endSearch;
                                }
                            }

                            // Search on the right from current peak i.
                            for (int i = currentRegionEnd + 1; i < scanCount
                                    && checkRange.contains(retentionTimes[i]); i++) {

                                if (intensities[i] < intensities[currentRegionEnd]) {

                                    continue endSearch;
                                }
                            }

                            // Find the intensity at the sides (lowest data points).
                            final double peakMinLeft = intensities[currentRegionStart];
                            final double peakMinRight = intensities[currentRegionEnd];

                            // If we have reached a minimum which is non-zero, but
                            // the peak shape would not fulfill the
                            // ratio condition, continue searching for next minimum.
                            if (currentRegionHeight >= peakMinRight * minRatio) {

                                // Check the shape of the peak.
                                if (currentRegionHeight >= minHeight
                                        && currentRegionHeight >= peakMinLeft
                                                * minRatio
                                        && currentRegionHeight >= peakMinRight
                                                * minRatio
                                        && peakDuration
                                                .contains(retentionTimes[currentRegionEnd]
                                                        - retentionTimes[currentRegionStart])) {

                                    resolvedPeaks.add(new ResolvedPeak(
                                            chromatogram, currentRegionStart,
                                            currentRegionEnd));
                                }

                                // Set the next region start to current region end-1
                                // because it will be immediately
                                // increased +1 as we continue the for-cycle.
                                currentRegionStart = currentRegionEnd - 1;
                                continue startSearch;
                            }
                        }
                    }
                }
            }

            return resolvedPeaks.toArray(new Feature[resolvedPeaks.size()]);
        }
    }

    public Feature fillTheGap() {
        
        Feature bestPeak = detectBestMatchingPeak();
        if (bestPeak != null) {
        
//        // Peak to GapDataPoints
//        currentPeakDataPoints = new Vector<GapDataPoint>();
//        for (int i = 0; i < bestPeak.getScanNumbers().length; ++i) {
//            
//            int scan_num = bestPeak.getScanNumbers()[i];
//            
//            DataPoint dp = bestPeak.getDataPoint(scan_num);
//            Scan sc = bestPeak.getDataFile().getScan(scan_num);
//            
//            GapDataPoint gapDp = new GapDataPoint(i, dp.getMZ(), sc.getRetentionTime(), dp.getIntensity());
//            currentPeakDataPoints.add(gapDp);
//        }
//        
//        // Check peak that was last constructed
//        if (currentPeakDataPoints != null) {
//            checkCurrentPeak();
//            currentPeakDataPoints = null;
//        }
//
//        // If we have best peak candidate, construct a SimpleChromatographicPeak
//        if (bestPeakDataPoints != null) {
//
//            double area = 0, height = 0, mz = 0, rt = 0;
//            int scanNumbers[] = new int[bestPeakDataPoints.size()];
//            DataPoint finalDataPoint[] = new DataPoint[bestPeakDataPoints.size()];
//            Range<Double> finalRTRange = null, finalMZRange = null, finalIntensityRange = null;
//            int representativeScan = 0;
//
//            // Process all datapoints
//            for (int i = 0; i < bestPeakDataPoints.size(); i++) {
//
//                GapDataPoint dp = bestPeakDataPoints.get(i);
//
//                if (i == 0) {
//                    finalRTRange = Range.singleton(dp.getRT());
//                    finalMZRange = Range.singleton(dp.getMZ());
//                    finalIntensityRange = Range.singleton(dp.getIntensity());
//                } else {
//                    assert finalRTRange != null && finalMZRange != null
//                            && finalIntensityRange != null;
//                    finalRTRange = finalRTRange
//                            .span(Range.singleton(dp.getRT()));
//                    finalMZRange = finalMZRange
//                            .span(Range.singleton(dp.getMZ()));
//                    finalIntensityRange = finalIntensityRange.span(Range
//                            .singleton(dp.getIntensity()));
//                }
//
//                scanNumbers[i] = bestPeakDataPoints.get(i).getScanNumber();
//                finalDataPoint[i] = new SimpleDataPoint(dp.getMZ(),
//                        dp.getIntensity());
//                mz += bestPeakDataPoints.get(i).getMZ();
//
//                // Check height
//                if (bestPeakDataPoints.get(i).getIntensity() > height) {
//                    height = bestPeakDataPoints.get(i).getIntensity();
//                    rt = bestPeakDataPoints.get(i).getRT();
//                    representativeScan = bestPeakDataPoints.get(i)
//                            .getScanNumber();
//                }
//
//                // Skip last data point
//                if (i == bestPeakDataPoints.size() - 1)
//                    break;
//
//                // X axis interval length
//                double rtDifference = (bestPeakDataPoints.get(i + 1).getRT() - bestPeakDataPoints
//                        .get(i).getRT()) * 60d;
//
//                // intensity at the beginning and end of the interval
//                double intensityStart = bestPeakDataPoints.get(i)
//                        .getIntensity();
//                double intensityEnd = bestPeakDataPoints.get(i + 1)
//                        .getIntensity();
//
//                // calculate area of the interval
//                area += (rtDifference * (intensityStart + intensityEnd) / 2);
//
//            }
//
//            // Calculate average m/z value
//            mz /= bestPeakDataPoints.size();
//
//            // Find the best fragmentation scan, if available
//            int fragmentScan = ScanUtils.findBestFragmentScan(rawDataFile,
//                    finalRTRange, finalMZRange);
//
//            SimpleFeature newPeak = new SimpleFeature(rawDataFile, mz, rt,
//                    height, area, scanNumbers, finalDataPoint,
//                    FeatureStatus.ESTIMATED, representativeScan, fragmentScan,
//                    finalRTRange, finalMZRange, finalIntensityRange);

        
            Feature newPeak = new SimpleFeature(bestPeak);
            // Fill the gap
            peakListRow.addPeak(rawDataFile, newPeak);
//            //-
//            // Add the peak if and only if this is not a "known" one
//            boolean alreadyKnown = PeakFinderGCTask.checkPeak(newPeak, this.sourcePeakList);
//            if (!alreadyKnown) {
//            	peakListRow.addPeak(rawDataFile, newPeak);
//            }
//            else {
//	        	logger.info("...[DUPLICATE peak found and SKIPPED!] Peak: " + bestPeak + ".");
//            }
//            logger.info("Added peak '" + newPeak 
//            		+ "' to row '" + peakListRow + "' (avg rt:" + peakListRow.getAverageRT() + " | id:" + peakListRow.getID() + ")"
//            				+ " ~~~ RDF: " + rawDataFile.getName());
            System.out.println("# !!! Filled a gap   =>   Added peak '" + newPeak 
            		+ "' to row '" + peakListRow /*+ "' (avg rt:" + peakListRow.getAverageRT() + " | id:" + peakListRow.getID() + ")"*/
            				+ " ~~~ RDF: " + rawDataFile.getName());
        } else {
            System.out.println("FAILED a gap   =>   " 
            		+ "for row '" + peakListRow /*+ "' (avg rt:" + peakListRow.getAverageRT() + " | id:" + peakListRow.getID() + ")"*/
            				+ " ~~~ RDF: " + rawDataFile.getName());
        }
        
        return bestPeak;
    }

    
//    void offerNextScan(Scan scan) {
//
//        //System.out.println("#offerNextScan > " + scan.toString());
//
//        double scanRT = scan.getRetentionTime();
//
//	// If not yet inside the RT range
//	if (scanRT < rtRange.lowerEndpoint())
//	    return;
//
//	// If we have passed the RT range and finished processing last peak
//	if ((scanRT > rtRange.upperEndpoint())
//		&& (currentPeakDataPoints == null))
//	    return;
//
//	// Find top m/z peak in our range
//        ////DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);
//        DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);
//        double ticPeak = ScanUtils.calculateTIC(scan, mzRange);
//        DataPoint basePeak2 = new SimpleDataPoint(basePeak.getMZ(), ticPeak);
//        
//	GapDataPoint currentDataPoint;
//	if (basePeak2 != null) {
//	    currentDataPoint = new GapDataPoint(scan.getScanNumber(),
//		    basePeak2.getMZ(), scanRT, basePeak2.getIntensity());
//	} else {
//	    currentDataPoint = new GapDataPoint(scan.getScanNumber(),
//		    RangeUtils.rangeCenter(mzRange), scanRT, 0);
//	}
//
//	// If we have not yet started, just create a new peak
//	if (currentPeakDataPoints == null) {
//	    currentPeakDataPoints = new Vector<GapDataPoint>();
//	    currentPeakDataPoints.add(currentDataPoint);
//	    return;
//	}
//
//	// Check if this continues previous peak?
//	if (checkRTShape(currentDataPoint)) {
//	    // Yes, continue this peak.
//	    currentPeakDataPoints.add(currentDataPoint);
//	    System.out.println("#Continue > " + scan.toString());
//	} else {
//
//	    // No, new peak is starting
//            peak_cnt++;
//            System.out.println("#New Peak > " + scan.toString());
//
//	    // Check peak formed so far
//	    if (currentPeakDataPoints != null) {
//	        System.out.println("#Check > " + scan.toString());
//		checkCurrentPeak();
//		currentPeakDataPoints = null;
//	    }
//
//	}
//
//    }
//
//    public void noMoreOffers() {
//
//	// Check peak that was last constructed
//	if (currentPeakDataPoints != null) {
//	    checkCurrentPeak();
//	    currentPeakDataPoints = null;
//	}
//
//	// If we have best peak candidate, construct a SimpleChromatographicPeak
//	if (bestPeakDataPoints != null) {
//
//	    double area = 0, height = 0, mz = 0, rt = 0;
//	    int scanNumbers[] = new int[bestPeakDataPoints.size()];
//	    DataPoint finalDataPoint[] = new DataPoint[bestPeakDataPoints
//		    .size()];
//	    Range<Double> finalRTRange = null, finalMZRange = null, finalIntensityRange = null;
//	    int representativeScan = 0;
//
//	    // Process all datapoints
//	    for (int i = 0; i < bestPeakDataPoints.size(); i++) {
//
//		GapDataPoint dp = bestPeakDataPoints.get(i);
//
//		if (i == 0) {
//		    finalRTRange = Range.singleton(dp.getRT());
//		    finalMZRange = Range.singleton(dp.getMZ());
//		    finalIntensityRange = Range.singleton(dp.getIntensity());
//		} else {
//		    assert finalRTRange != null && finalMZRange != null
//			    && finalIntensityRange != null;
//		    finalRTRange = finalRTRange
//			    .span(Range.singleton(dp.getRT()));
//		    finalMZRange = finalMZRange
//			    .span(Range.singleton(dp.getMZ()));
//		    finalIntensityRange = finalIntensityRange.span(Range
//			    .singleton(dp.getIntensity()));
//		}
//
//		scanNumbers[i] = bestPeakDataPoints.get(i).getScanNumber();
//		finalDataPoint[i] = new SimpleDataPoint(dp.getMZ(),
//			dp.getIntensity());
//		mz += bestPeakDataPoints.get(i).getMZ();
//
//		// Check height
//		if (bestPeakDataPoints.get(i).getIntensity() > height) {
//		    height = bestPeakDataPoints.get(i).getIntensity();
//		    rt = bestPeakDataPoints.get(i).getRT();
//		    representativeScan = bestPeakDataPoints.get(i)
//			    .getScanNumber();
//		}
//
//		// Skip last data point
//		if (i == bestPeakDataPoints.size() - 1)
//		    break;
//
//		// X axis interval length
//		double rtDifference = (bestPeakDataPoints.get(i + 1).getRT() - bestPeakDataPoints
//			.get(i).getRT()) * 60d;
//
//		// intensity at the beginning and end of the interval
//		double intensityStart = bestPeakDataPoints.get(i)
//			.getIntensity();
//		double intensityEnd = bestPeakDataPoints.get(i + 1)
//			.getIntensity();
//
//		// calculate area of the interval
//		area += (rtDifference * (intensityStart + intensityEnd) / 2);
//
//	    }
//
//	    // Calculate average m/z value
//	    mz /= bestPeakDataPoints.size();
//
//	    // Find the best fragmentation scan, if available
//	    int fragmentScan = ScanUtils.findBestFragmentScan(rawDataFile,
//		    finalRTRange, finalMZRange);
//
//	    SimpleFeature newPeak = new SimpleFeature(rawDataFile, mz, rt,
//		    height, area, scanNumbers, finalDataPoint,
//		    FeatureStatus.ESTIMATED, representativeScan, fragmentScan,
//		    finalRTRange, finalMZRange, finalIntensityRange);
//
//	    // Fill the gap
//	    peakListRow.addPeak(rawDataFile, newPeak);
//	}
//
//    }
//
//    /**
//     * This function check for the shape of the peak in RT direction, and
//     * determines if it is possible to add given m/z peak at the end of the
//     * peak.
//     */
//    private boolean checkRTShape(GapDataPoint dp) {
//
//	if (dp.getRT() < rtRange.lowerEndpoint()) {
//	    double prevInt = currentPeakDataPoints.get(
//		    currentPeakDataPoints.size() - 1).getIntensity();
//	    if (dp.getIntensity() > (prevInt * (1 - intTolerance))) {
//	        System.out.println(">>> Up hill...");
//		return true;
//	    }
//	}
//
//	if (rtRange.contains(dp.getRT())) {
//            System.out.println(">>> Top hill...");
//	    return true;
//	}
//
//	if (dp.getRT() > rtRange.upperEndpoint()) {
//	    double prevInt = currentPeakDataPoints.get(
//		    currentPeakDataPoints.size() - 1).getIntensity();
//	    if (dp.getIntensity() < (prevInt * (1 + intTolerance))) {
//                System.out.println(">>> Down hill...");
//		return true;
//	    }
//	}
//
//	return false;
//
//    }

    private void checkCurrentPeak() {

	// 1) Check if currentpeak has a local maximum inside the search range
	int highestMaximumInd = -1;
	double currentMaxHeight = 0f;
	for (int i = 1; i < currentPeakDataPoints.size() - 1; i++) {

	    if (rtRange.contains(currentPeakDataPoints.get(i).getRT())) {

		if ((currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints
			.get(i + 1).getIntensity())
			&& (currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints
				.get(i - 1).getIntensity())) {

		    if (currentPeakDataPoints.get(i).getIntensity() > currentMaxHeight) {

			currentMaxHeight = currentPeakDataPoints.get(i)
				.getIntensity();
			highestMaximumInd = i;
		    }
		}
	    }
	}
//        double currentChemSimScore = Double.MIN_VALUE;
//        for (int i = 1; i < currentPeakDataPoints.size() - 1; i++) {
//
//            if (rtRange.contains(currentPeakDataPoints.get(i).getRT())) {
//
//                if ((currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints
//                        .get(i + 1).getIntensity())
//                        && (currentPeakDataPoints.get(i).getIntensity() >= currentPeakDataPoints
//                                .get(i - 1).getIntensity())) {
//
//                    if (currentPeakDataPoints.get(i).getIntensity() > currentMaxHeight) {
//
//                        currentMaxHeight = currentPeakDataPoints.get(i)
//                                .getIntensity();
//                        highestMaximumInd = i;
//                    }
//                }
//            }
//        }

	// If no local maximum, return
	if (highestMaximumInd == -1)
	    return;
	
	System.out.println(">> Found highestMaximumInd! = " + highestMaximumInd);

	// 2) Find elution start and stop
	int startInd = highestMaximumInd;
	double currentInt = currentPeakDataPoints.get(startInd).getIntensity();
	while (startInd > 0) {
	    double nextInt = currentPeakDataPoints.get(startInd - 1)
		    .getIntensity();
	    if (currentInt < (nextInt * (1 - intTolerance)))
		break;
	    startInd--;
	    if (nextInt == 0) { break; }
	    currentInt = nextInt;
	}
	
	// Since subList does not include toIndex value then find highest
	// possible value of stopInd+1 and currentPeakDataPoints.size()
	int stopInd = highestMaximumInd, toIndex = highestMaximumInd;
	currentInt = currentPeakDataPoints.get(stopInd).getIntensity();
	while (stopInd < (currentPeakDataPoints.size() - 1)) {
	    double nextInt = currentPeakDataPoints.get(stopInd + 1)
		    .getIntensity();
	    if (nextInt > (currentInt * (1 + intTolerance))) {
		toIndex = Math.min(currentPeakDataPoints.size(), stopInd+1);
		break;
	    }	
	    stopInd++;
	    toIndex = Math.min(currentPeakDataPoints.size(), stopInd+1);
	    if (nextInt == 0) { stopInd++; toIndex=stopInd; break; }
	    currentInt = nextInt;
	}
        
        System.out.println(">> startInd = " + startInd);
        System.out.println(">> stopInd = " + stopInd);


        // 3) Check if this is the best candidate for a peak
        if ((bestPeakDataPoints == null) || (bestPeakHeight < currentMaxHeight)) {
            bestPeakDataPoints = currentPeakDataPoints.subList(startInd,
                    toIndex);
            
            bestPeakHeight = currentMaxHeight;
            System.out.println("!!!!!!!! YES - BestPeak = " + peak_cnt);
            return;
        }
        System.out.println("!!!!!!!! NO - BestPeak = " + peak_cnt);
        
//        if ((bestPeakDataPoints == null) || (bestChemSimScore < currentChemSimScore)) {
//            bestPeakDataPoints = currentPeakDataPoints.subList(startInd,
//                    toIndex);
//        }
//        
//        // GLG
//        if (bestChemSimScore < currentChemSimScore) {
//            bestChemSimScore = currentChemSimScore;
//        }

    }

    public static <K, V extends Comparable<? super V>> Map<K, V> 
    sortByValue( Map<K, V> map, final boolean ascending )
    {
    	List<Map.Entry<K, V>> list =
    			new LinkedList<Map.Entry<K, V>>( map.entrySet() );
    	Collections.sort( list, new Comparator<Map.Entry<K, V>>()
    	{
    		public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
    		{
    			if (ascending)
    				return (o1.getValue()).compareTo( o2.getValue() );
    			else
    				return (o2.getValue()).compareTo( o1.getValue() );
    		}
    	} );

    	Map<K, V> result = new LinkedHashMap<K, V>();
    	for (Map.Entry<K, V> entry : list)
    	{
    		result.put( entry.getKey(), entry.getValue() );
    	}
    	return result;
    }
    
}
