package net.sf.mzmine.modules.peaklistmethods.merging.rt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter.FilterShapeModel;
import net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter.ShapeFilterTask;
import net.sf.mzmine.parameters.ParameterSet;
//import net.sf.mzmine.parameters.parametertypes.MZTolerance;
//import net.sf.mzmine.parameters.parametertypes.RTTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataFileUtils;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.ScanUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

//import net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter.FilterShapeModel;
//import net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter.ShapeFilterTask;


/**
 * 
 */
class PeakMergerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // RDF
    RawDataFile dataFile, ancestorDataFile, workingDataFile;

    private final MZmineProject project;

    // peaks lists
    private PeakList peakList, mergedPeakList;

    // peaks counter
    private int processedPeaks, totalPeaks;

    // parameter values
    private String suffix;
//    private MZTolerance mzTolerance;
//    private RTTolerance rtTolerance;
    public static MZTolerance mzTolerance;
    public static RTTolerance rtTolerance;
    private boolean useOldestRDFAncestor;
    private double detectedMZSearchWidth;
    private boolean useOnlyDetectedPeaks;
    private boolean cumulativeComputing;
    private boolean removeOriginal;
    private ParameterSet parameters;

    // private static final String ancestor_suffix = "#Ancestor";
//    private static final String unpastableSep = MZmineCore.getUnpastableSep();
//    private static final String autogenPrefix = MZmineCore.getAutogenPrefix();
    private static final String unpastableSep = " \u00BB ";// (UTF-8)               //" \uD834\uDF06 "// (UTF-16);  
    private static final String autogenPrefix = "AUTOGEN" + unpastableSep;

    private static final double doublePrecision = 0.000001;
    
    private ArrayList<PeakListRow> mergedPeakListRows;

    /**
     * @param rawDataFile
     * @param parameters
     */
    PeakMergerTask(final MZmineProject project, final PeakList peakList,
            final ParameterSet parameters) {

        this.project = project;
        this.peakList = peakList;
        this.parameters = parameters;

        // Get parameter values for easier use
        suffix = parameters.getParameter(PeakMergerParameters.suffix)
                .getValue();
//        mzTolerance = parameters.getParameter(PeakMergerParameters.mzTolerance)
//                .getValue();
//        rtTolerance = parameters.getParameter(PeakMergerParameters.rtTolerance)
//                .getValue();
        mzTolerance = parameters.getParameter(PeakMergerParameters.mzTolerance).getValue();
        rtTolerance = parameters.getParameter(PeakMergerParameters.rtTolerance).getValue();


        this.useOldestRDFAncestor = parameters.getParameter(
                PeakMergerParameters.useOldestRDFAncestor).getValue();
        this.detectedMZSearchWidth = parameters.getParameter(
                PeakMergerParameters.detectedMZSearchWidth).getValue();
        this.useOnlyDetectedPeaks = parameters.getParameter(
                PeakMergerParameters.useOnlyDetectedPeaks).getValue();
        this.cumulativeComputing = parameters.getParameter(
                PeakMergerParameters.cumulativeComputing).getValue();

        removeOriginal = parameters.getParameter(
                PeakMergerParameters.autoRemove).getValue();

        
        //---
        mergedPeakListRows = new ArrayList<PeakListRow>();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "RT peaks merger on " + peakList;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalPeaks == 0)
            return 0.0f;
        return (double) processedPeaks / (double) totalPeaks;
    }
    
    
    /**
     * @see Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);
        logger.info("Running RT peaks merger on " + peakList);

        // //////////////////////////////////////////////////////////////////////////////////
        // We assume source peakList contains one single data file
        this.dataFile = peakList.getRawDataFile(0);
        
        // TODO: Which kind of sorting shall be applied here? By height??? Nothing???
        //       => For now, nothing is done...
        List<Feature> sortedPeaks = Arrays.asList(peakList.getPeaks(this.dataFile));
        
        this.ancestorDataFile = DataFileUtils.getAncestorDataFile(this.project, this.dataFile, true);
        this.workingDataFile = this.getWorkingDataFile(/*sortedPeaks*/);
        
        
        //--------------------     <BAD SHAPED>     --------------------//
        // Store all "bad shaped" (bs) peaks.
        PeakList badShapedPeaksList = ShapeFilterTask.getBadShapedPeakListRows(peakList, "", 1.5, 0.0, FilterShapeModel/*.Triangle*/.None);
        List<Feature> badShapedPeaks = Arrays.asList(badShapedPeaksList.getPeaks(this.dataFile));
        // Note: Shall we sort in some way the badShapedPeaks array?
        
        int[] working_df_scans = this.workingDataFile.getScanNumbers();
        // "TIC" profile. Total Ion count for the RDF used.
        double[] ticProfile = new double[working_df_scans.length];
        // "TIC_bs" profile (all defaulted to 0.0d - java language specifications).
        // Total Ion count for the "bad shaped" peaks to be removed/merged.
        double[] ticBsProfile = new double[ticProfile.length];
        Scan sc;
        for (int i=0; i < ticProfile.length; ++i) {
            
            int scanNum = working_df_scans[i];
            
            // Build "TIC" profile.
            sc = this.workingDataFile.getScan(scanNum);
            ticProfile[i] = sc.getTIC();
            
            // Build "TIC_bs" profile.
            // TODO: optimize the bellow: do we really need to loop over all badShaped?
            for (Feature p: badShapedPeaks) {
                if (Ints.contains(p.getScanNumbers(), scanNum) && (p.getDataPoint(scanNum) != null)) {
                    ticBsProfile[i] += p.getDataPoint(scanNum).getIntensity();
                }
            }
        }
        //--------------------     <BAD SHAPED>     --------------------//


        logger.log(Level.INFO, "Base raw data file: " + this.dataFile.getName());
        logger.log(Level.INFO, "Working raw data file: " + this.workingDataFile.getName());
        // //////////////////////////////////////////////////////////////////////////////////

        // Create a new RT merged peakList
        // mergedPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());
        this.initMergedPeakList();

        // Loop through all peaks
        totalPeaks = sortedPeaks.size();
        int nb_empty_peaks = 0;

        // <a/> Merging peaks
        for (int ind = 0; ind < totalPeaks; ind++) {

            // Task canceled by user
            if (isCanceled())
                return;

            Feature aPeak = sortedPeaks.get(ind);

            // Delete (and skip) if peak is a Bad Shaped one
            if (badShapedPeaks.contains(aPeak)) {
                sortedPeaks.set(sortedPeaks.indexOf(aPeak), null);
                aPeak = null;
            }
            // Skip if peak was already deleted (BS or treated)
            if (aPeak == null) {
                processedPeaks++;
                continue;
            }
           

            // Build RT group
            ArrayList<Feature> groupedPeaks = this.getPeaksGroupByRT(aPeak, sortedPeaks);
            // Sort by intensity (descending)
            Collections.sort(groupedPeaks, new PeakSorter(SortingProperty.Height, SortingDirection.Descending));

            // TODO: debug stuffs here !!!!

            Feature oldPeak = groupedPeaks.get(0);
            // Get scan numbers of interest
            //                      List<Integer> scan_nums = Arrays.asList(ArrayUtils.toObject(oldPeak.getScanNumbers()));
            // Do not get scan nums from highest peak, but from the whole group (largest RT range)
            // TODO: use "getPeaksGroupByRT()" with an RTrange as parameter passed in reference, instead of looping again:
            //       (since group's scans bounds are already determined in that function).
            int minScanNumber = Integer.MAX_VALUE;
            int maxScanNumber = Integer.MIN_VALUE;
            double futureMz = groupedPeaks.get(0).getMZ();
//            double height = -1.0d;
            for (int i = 0; i < groupedPeaks.size(); i++) {
                int[] scanNums = groupedPeaks.get(i).getScanNumbers();
                if (scanNums[0] < minScanNumber) {
                    minScanNumber = scanNums[0];
                }
                if (scanNums[scanNums.length - 1] > maxScanNumber) {
                    maxScanNumber = scanNums[scanNums.length - 1];
                }
//                // Use the opportunity to define the mz of the future merged peak
//                // from the highest peak of the group to be merged.
//                if (groupedPeaks.get(i).getHeight() > height)
//                {
//                    futureMz = groupedPeaks.get(i).getMZ();
//                    height = groupedPeaks.get(i).getHeight();
//                }
            }

            // Apex of the most representative (highest) peak of the group.
            int originScanNumber = oldPeak.getRepresentativeScanNumber();
            MergedPeak newPeak = new MergedPeak(this.workingDataFile);
            ////int totalScanNumber = this.workingDataFile.getNumOfScans();
            // No MZ requirement (take the full dataFile MZ Range)
            Range<Double> mzRange = this.workingDataFile.getDataMZRange(1);
            Scan scan;
            DataPoint dataPoint;

            // Look for dataPoint related to this main peak to the left
            int scanNumber = originScanNumber;
            scanNumber--;
            while (scanNumber >= minScanNumber) {
                
                scan = this.workingDataFile.getScan(scanNumber);

                if (scan == null) {
                    scanNumber--;
                    continue;
                }

                if (scan.getMSLevel() != 1) {
                    scanNumber--;
                    continue;
                }

                // Switch accordingly to option "Only DETECTED peaks"
                dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange, futureMz);

                if (dataPoint != null) {
                    newPeak.addMzPeak(scanNumber, dataPoint);
                }

                scanNumber--;
            }

            // Add original DataPoint
            scanNumber = originScanNumber;
            scan = this.workingDataFile.getScan(originScanNumber);
            dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange, futureMz);
            if (dataPoint == null && this.useOnlyDetectedPeaks) {
                this.useOnlyDetectedPeaks = false;
                dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange, futureMz);
                this.useOnlyDetectedPeaks = true;

                logger.log(
                        Level.WARNING,
                        "DETECTED Middle / Main peak (DataPoint) not found for scan: #"
                                + scanNumber
                                + ". Using \"Base Peak Intensity\" instead (May lead to inaccurate results).");
            }
            //
            if (dataPoint != null) {
                newPeak.addMzPeak(scanNumber, dataPoint);
            }

            // Look to the right
            scanNumber++;
            while (scanNumber <= maxScanNumber) {
                
                scan = this.workingDataFile.getScan(scanNumber);

                if (scan == null) {
                    scanNumber++;
                    continue;
                }

                if (scan.getMSLevel() != 1) {
                    scanNumber++;
                    continue;
                }

                dataPoint = getMergedDataPointFromPeakGroup(scan, groupedPeaks, mzRange, futureMz);

                if (dataPoint != null) {
                    newPeak.addMzPeak(scanNumber, dataPoint);
                }

                scanNumber++;
            }

            // Finishing the merged peak
            newPeak.finishMergedPeak();

            // Get peak list row to be updated
            PeakListRow oldRow = peakList.getPeakRow(oldPeak);

            // TODO: Might be VIOLENT (quick and dirty => to be verified)
            if (newPeak.getScanNumbers().length == 0) {
                ++nb_empty_peaks;
                // #continue;
                logger.log(Level.WARNING, "0 scans peak found !");
                break;
            }

            // Add new merged peak to "mergedPeakList", keeping the original ID.
            //this.updateMergedPeakList(oldRow, newPeak);
            updateMergedPeakListRow(oldRow, newPeak);

            // Clear already treated peaks
            for (int g_i = 0; g_i < groupedPeaks.size(); g_i++) {
                sortedPeaks.set(sortedPeaks.indexOf(groupedPeaks.get(g_i)), null);
            }

            // Update completion rate
            processedPeaks++;

        }

        if (nb_empty_peaks > 0)
            logger.log(Level.WARNING, "Skipped \"" + nb_empty_peaks
                    + "\" empty peaks !");
        
        //---
        
        // Latest adjustments: dispatch of remaining intensities to merged peaks
        // (depending on the users parameters choices)
        // i.e. [!cumulativeComputing] => No adjustments.
        // Must grab remaining intensities (cumulativeComputing).
        if (this.cumulativeComputing) {
    
            //List<Feature> mergedPeaks = Arrays.asList(mergedPeakList.getPeaks(workingDataFile));
            int scNum0 = workingDataFile.getScanNumbers()[0];
            
            // 
            for (PeakListRow row: this.mergedPeakListRows) {
                
                Feature p = row.getBestPeak();
                MergedPeak new_p = new MergedPeak(workingDataFile);
                
                int[] scNums = p.getScanNumbers();
                for (int i=0; i < scNums.length; ++i) {
                    
                    int scNum = scNums[i];
                    
                    ArrayList<Feature> overlapPeaks = this.getOverlappingPeaksAtScan(scNum, this.mergedPeakListRows);
                    int nbOverlapPeaks = overlapPeaks.size();
                    
                    // Total ion intensity at scan i
                    double sumTic = ticProfile[scNum - scNum0];
                    // Total ion intensity from "bad shaped detected peaks" at scan i
                    double sumTicBs = ticBsProfile[scNum - scNum0];
                    // Total ion intensity from "well shaped peaks" at scan i
                    double sumOverlapPeaks = 0.0d;
                    for (Feature op: overlapPeaks) {
                        sumOverlapPeaks += op.getDataPoint(scNum).getIntensity();
                    }
                    
                    double intensity = p.getDataPoint(scNum).getIntensity();
                    
                    // Must grab remaining intensities (cumulativeComputing).
                    // <b1/> Dispatch equally to every merged peak the ions intensities
                    //       which weren't detected but the user wants to be taken into
                    //       account (!useOnlyDetectedPeaks).
                    if (!this.useOnlyDetectedPeaks) {
                        intensity += (sumTic - sumOverlapPeaks) / (double)nbOverlapPeaks;
                    }
                    // <b2/> Dispatch equally the "bad shaped" peaks ions intensities,
                    //       which were detected but not taken into account, to every
                    //       merged peak (useOnlyDetectedPeaks).
                    else {
                        intensity += sumTicBs / (double)nbOverlapPeaks;
                    }
                    
                    DataPoint new_dp = new SimpleDataPoint(p.getDataPoint(scNum).getMZ(), intensity);
                    new_p.addMzPeak(scNum, new_dp);
                }
                
                new_p.finishMergedPeak();
                this.updateMergedPeakListRow(row, new_p);
    
            }
        }
        
        //---
        
        
        // Setup and add new peakList to the project
        this.finishMergedPeakList();

        // Remove the original peakList if requested
        if (removeOriginal)
            this.project.removePeakList(peakList);

        logger.info("Finished RT peaks merger on " + peakList);
        setStatus(TaskStatus.FINISHED);

    }

//    private ArrayList<Feature> getOverlappingPeaks(Feature peak,
//            ArrayList<MergedPeak> peaksList) {
//
//        ArrayList<Feature> overlapPeaks = new ArrayList<Feature>();
//        for (int i = 0; i < peaksList.size(); ++i) {
//            Feature aPeak = peaksList.get(i);
//            // If given 'peak' contains at least one scan of 'aPeak', or the
//            // other way, they're said
//            // overlapping.
//            // if
//            // (peak.getRawDataPointsRTRange().contains(aPeak.getRawDataPointsRTRange().getMin())
//            // ||
//            // peak.getRawDataPointsRTRange().contains(aPeak.getRawDataPointsRTRange().getMax())
//            // ||
//            // aPeak.getRawDataPointsRTRange().contains(peak.getRawDataPointsRTRange().getMin())
//            // ||
//            // aPeak.getRawDataPointsRTRange().contains(peak.getRawDataPointsRTRange().getMax()))
//            // {
//            // overlapPeaks.add(aPeak);
//            // }
//            if (peak.getRawDataPointsRTRange().contains(
//                    aPeak.getRawDataPointsRTRange().lowerEndpoint())
//                    || peak.getRawDataPointsRTRange().contains(
//                            aPeak.getRawDataPointsRTRange().upperEndpoint())
//                    || aPeak.getRawDataPointsRTRange().contains(
//                            peak.getRawDataPointsRTRange().lowerEndpoint())
//                    || aPeak.getRawDataPointsRTRange().contains(
//                            peak.getRawDataPointsRTRange().upperEndpoint())) {
//                overlapPeaks.add(aPeak);
//            }
//        }
//        return overlapPeaks;
//    }

//    private int getNbOverlappingPeaksAtScan(int scanNum,
//            List<Feature> peaksList) {
//
//        int nb = 0;
//        for (int i = 0; i < peaksList.size(); ++i) {
//            Feature aPeak = peaksList.get(i);
//            // If given 'peak' has the specified scan.
//            if (Ints.contains(aPeak.getScanNumbers(), scanNum)) {
//                ++nb;
//            }
//        }
//        return nb;
//    }

//    private ArrayList<Feature> getOverlappingPeaksAtScan(int scanNum,
//            List<Feature> peaksList) {
//
//        ArrayList<Feature> overlapPeaks = new ArrayList<Feature>();
//        for (int i = 0; i < peaksList.size(); ++i) {
//            Feature aPeak = peaksList.get(i);
//            // If given 'peak' has the specified scan.
//            if (Ints.contains(aPeak.getScanNumbers(), scanNum)) {
//                overlapPeaks.add(aPeak);
//            }
//        }
//        return overlapPeaks;
//    }
    
    private ArrayList<Feature> getOverlappingPeaksAtScan(int scanNum,
            ArrayList<PeakListRow> rowsList) {

        ArrayList<Feature> overlapPeaks = new ArrayList<Feature>();
        for (int i = 0; i < rowsList.size(); ++i) {
            Feature aPeak = rowsList.get(i).getBestPeak();
            // If given 'peak' has the specified scan.
            if (Ints.contains(aPeak.getScanNumbers(), scanNum)) {
                overlapPeaks.add(aPeak);
            }
        }
        return overlapPeaks;
    }

    static final public Double getDoublePrecision() {
        return doublePrecision;
    }

    
    // <a/>
    private DataPoint getMergedDataPointFromPeakGroup(Scan refScan,
            ArrayList<Feature> peaksGroup, Range<Double> mzRange, double newMz) {
        
        DataPoint dp = null;

        double mzHalfSearch = this.detectedMZSearchWidth / 2.0;

        int scanNumber = refScan.getScanNumber();
        double intensity;
        double mz;

        
        // Switch accordingly to option "Cumulative computing"
        if (this.cumulativeComputing) {
            
            // Get the total ion count over all grouped peaks
            ArrayList<Double> mzes = new ArrayList<Double>();
            double tic = 0.0d;
            for (int i = 0; i < peaksGroup.size(); ++i) {
            
                DataPoint dp0 = peaksGroup.get(i).getDataPoint(scanNumber);
                
                if (dp0 != null) {
                    // Avoid counting more than once on given mz.
                    if (mzes.contains(dp0.getMZ())) continue;               
                    
                    mz = dp0.getMZ();
                    mzes.add(mz);
                    
                    // If we are working on a different file than the one used to detect the peaks in the first place,
                    // (in which case this is a non-sense to look for the exact m/z for this DETECTED peak),
                    // then, we extend the search!                 (to the user-specified MZ search window).
                    if (this.dataFile != this.workingDataFile)
                    {
                        DataPoint[] dp_arr = refScan.getDataPointsByMass(Range.closed(mz - mzHalfSearch, mz + mzHalfSearch));
                        dp0 =  ScanUtils.findTopDataPoint(dp_arr);
                        if (dp0 == null) continue;
                    }
                    
                    tic += dp0.getIntensity();
                }
            }

            intensity = tic;
            
        } else {
            // Get the top intensity among grouped peaks
            double max_intensity = Double.MIN_VALUE;
            for (int i = 0; i < peaksGroup.size(); ++i) {
                
                DataPoint dp0 = peaksGroup.get(i).getDataPoint(scanNumber);
                
                if (dp0 != null) {
                    
                    mz = dp0.getMZ();

                    // If we are working on a different file than the one used to detect the peaks in the first place,
                    // (in which case this is a non-sense to look for the exact m/z for this DETECTED peak),
                    // then, we extend the search!                 (to the user-specified MZ search window).
                    if (this.dataFile != this.workingDataFile)
                    {
                        DataPoint[] dp_arr = refScan.getDataPointsByMass(Range.closed(mz - mzHalfSearch, mz + mzHalfSearch));
                        dp0 =  ScanUtils.findTopDataPoint(dp_arr);
                        if (dp0 == null) continue;
                    }
                    
                    if (dp0.getIntensity() > max_intensity) {
                        max_intensity = dp0.getIntensity();
                    }
                }
            }
            
            intensity = max_intensity;

        }
        
        boolean ok = (intensity >= 0.0d);
        if (ok) 
        {
            dp = new SimpleDataPoint(newMz, intensity);
        } 
        else 
        {
            logger.log(Level.WARNING, "No DataPoint found for current group at scan #" + scanNumber 
                    + " ! Maybe try with a larger \"DETECTED m/z search window\", "
                    + "or simply uncheck the \"Use original raw data file\" option...");
        }
        
        return dp;
    }


    private ArrayList<Feature> getPeaksGroupByRT(Feature mainPeak,
            List<Feature> sortedPeaks) {
        
        HashSet<Feature> groupedPeaks = new HashSet<Feature>();

        double mainMZ = mainPeak.getMZ();
        double mainRT = mainPeak.getRT();
        int mainScanNum = mainPeak.getRepresentativeScanNumber();

        // Loop through following peaks, and collect candidates for the n:th
        // peak in the RT range
        Vector<Feature> goodCandidates = new Vector<Feature>();
        for (int ind = 0; ind < sortedPeaks.size(); ind++) {

            Feature candidatePeak = sortedPeaks.get(ind);

            if (candidatePeak == null)
                continue;

            // Get properties of the candidate peak
            double candidatePeakMZ = candidatePeak.getMZ();
            double candidatePeakRT = candidatePeak.getRT();

            // Check if current peak is in RT range
            if (rtTolerance.checkWithinTolerance(candidatePeakRT, mainRT)
                    && mzTolerance.checkWithinTolerance(candidatePeakMZ, mainMZ)
                    && (!groupedPeaks.contains(candidatePeak))) {
                
                goodCandidates.add(candidatePeak);
            }

        }

        // Add all good candidates to the group
        if (!goodCandidates.isEmpty()) {
            
            groupedPeaks.addAll(goodCandidates);
        }

        // Detect and remove "False goodCandidates" !
        // Merge scans: Use HashSet to preserve unicity
        HashSet<Integer> hs = new HashSet<Integer>();
        for (Feature p : groupedPeaks) {
            hs.addAll(Ints.asList(p.getScanNumbers()));
        }
        
        // Sort HashSet of scans (TreeSet) by ascending order
        List<Integer> scan_nums = new ArrayList<Integer>(new TreeSet<Integer>(hs));
        
        // Check for gaps
        if (scan_nums.get(scan_nums.size() - 1) - scan_nums.get(0) != scan_nums.size() - 1) {
            
            // Gaps exist, extract valid scans sequence (left connected scans)
            // The correct sequence MUST contain the "MainPeak"
            logger.log(Level.INFO, "Gaps in sequence: " + scan_nums.toString());
            List<Integer> scan_nums_ok = new ArrayList<Integer>();

            int it = scan_nums.indexOf(mainScanNum);
            scan_nums_ok.add(scan_nums.get(it));
            // Get left side of the sequence, if applicable
            if ((it > 0) && (scan_nums.get(it - 1) == scan_nums.get(it) - 1)) {
                do {
                    --it;
                    scan_nums_ok.add(0, scan_nums.get(it));
                } while ((it != 0)
                        && (scan_nums.get(it - 1) == scan_nums.get(it) - 1));
            }

           // Get right side of the sequence, if applicable
            it = scan_nums.indexOf(mainScanNum);
            if ((it < scan_nums.size() - 1)
                    && (scan_nums.get(it + 1) == scan_nums.get(it) + 1)) {
                do {
                    ++it;
                    scan_nums_ok.add(scan_nums.get(it));
                } while ((it + 1 - scan_nums.size() != 0)
                        && (scan_nums.get(it + 1) == scan_nums.get(it) + 1));
            }

            logger.log(Level.INFO,
                    "Valid sequence is: " + scan_nums_ok.toString());

            double rt_min = this.workingDataFile.getScan(scan_nums_ok.get(0))
                    .getRetentionTime();
            double rt_max = this.workingDataFile.getScan(
                    scan_nums_ok.get(scan_nums_ok.size() - 1))
                    .getRetentionTime();

            Range<Double> rt_range_ok = Range.closed(rt_min - doublePrecision, rt_max
                    + doublePrecision);

            // List bad candidates
            Vector<Feature> badCandidates = new Vector<Feature>();
            for (Feature p : groupedPeaks) {
                if (!rt_range_ok.encloses(p.getRawDataPointsRTRange())) {
                    badCandidates.add(p);
                    logger.log(
                            Level.INFO,
                            "Bad candidate found at: "
                                    + p.getRepresentativeScanNumber());
                    logger.log(Level.INFO,
                            "rt_range_ok :" + rt_range_ok.toString());
                    logger.log(Level.INFO, "rt_badCandidates: "
                            + p.getRawDataPointsRTRange().toString());
                }
            }

            // Remove bad candidates
            groupedPeaks.removeAll(badCandidates);

        }

        // return groupedPeaks;
        return new ArrayList<Feature>(groupedPeaks);
    }

    RawDataFile getWorkingDataFile() {
        RawDataFile working_rdf = null;

        // Choose the RDF to work with
        if (this.useOldestRDFAncestor && this.ancestorDataFile != null) {
                working_rdf = this.ancestorDataFile;
        }

        // Otherwise, use the regular RawDataFile
        if (working_rdf == null)
            working_rdf = this.dataFile;

        // Such that never NULL is returned...
        return working_rdf;
    }


    void initMergedPeakList() {
//        // Populate with reference RDFs
//        // (just for display purpose)
//        ArrayList<RawDataFile> l_rdfs = new ArrayList<RawDataFile>();
//        l_rdfs.add(this.dataFile);
//        if (this.ancestorDataFile != null
//                && this.ancestorDataFile != this.dataFile) {
//            l_rdfs.add(this.ancestorDataFile);
//        }
//        if (this.cumulativeComputing) {
//            l_rdfs.add(this.workingDataFile);
//        }
////        // RDFs list to array
////        Object[] lst2arr = l_rdfs.toArray();
////        RawDataFile[] rdfs = Arrays.copyOf(lst2arr, lst2arr.length,
////                RawDataFile[].class);
////
////        // Create PL
////        this.mergedPeakList = new SimplePeakList(peakList + " " + suffix, rdfs);
////        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
////        // TODO: This is temporary, do not let it like this:
////        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        this.mergedPeakList = new SimplePeakList(peakList + " " + suffix, this.workingDataFile);
    }

//    void updateMergedPeakList(PeakListRow oldRow, MergedPeak newPeak) {
//        
//        // Preserve row ID
//        int oldID = oldRow.getID();
//        
//        // Rebuild new row
//        SimplePeakListRow newRow = new SimplePeakListRow(oldID);
//        // Preserve row Properties
//        PeakUtils.copyPeakListRowProperties(oldRow, newRow);
////        // Remove old row
////        mergedPeakList.removeRow(oldRow);
//
//        // Add peak to PLRow
//        newRow.addPeak(this.workingDataFile, newPeak);
//        // Add row to PL
//        mergedPeakList.addRow(newRow);
//    }
    
    void updateMergedPeakListRow(PeakListRow oldRow, MergedPeak newPeak) {
        
        // Preserve row ID
        int oldID = oldRow.getID();
        
        // Rebuild new row
        PeakListRow newRow = new SimplePeakListRow(oldID);
        // Preserve row Properties
        PeakUtils.copyPeakListRowProperties(oldRow, newRow);

        // Add peak to PL row
        newRow.addPeak(this.workingDataFile, newPeak);
        // Set row to PL
        int index = this.mergedPeakListRows.indexOf(oldRow);
        if (index == -1)
            // Add
            this.mergedPeakListRows.add(newRow);
        else
            // Update
            this.mergedPeakListRows.set(index, newRow);
    }

    void finishMergedPeakList() {
        
        // Add all final rows to peaklist
        for (PeakListRow row: this.mergedPeakListRows) {
            mergedPeakList.addRow(row);
        }
        
        // Add new peakList to the project
        this.project.addPeakList(mergedPeakList);

        // Load previous applied methods
        for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
            mergedPeakList.addDescriptionOfAppliedTask(proc);
        }

        // Add task description to peakList
        mergedPeakList
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "RT peaks merger", parameters));
    }

    /** 
     * GLG HACK: 
     * Added constructor for "SimpleScan" that guarantees DEEP COPY.
     */
    public class SimpleScanClonable extends SimpleScan {
        
        private RawDataFile dataFile;
        private int fragmentScans[];
        private DataPoint dataPoints[];

        public SimpleScanClonable(Scan sc, RawDataFile rawDataFile) {
            
            // Call above clone constructor
            super(sc.getDataFile(), sc.getScanNumber(), sc.getMSLevel(), sc
                    .getRetentionTime(), sc
                    .getPrecursorMZ(), sc.getPrecursorCharge(), sc
                    .getFragmentScanNumbers(), sc.getDataPoints(), sc
                    .getSpectrumType(), sc.getPolarity(), sc.getScanDefinition(),
                    sc.getScanningMZRange());
            
            
            // Handle non-primitive attributes
            
            if (rawDataFile != null) { this.dataFile = rawDataFile; }
            
            if (sc.getFragmentScanNumbers() != null) 
                    this.fragmentScans = Arrays.copyOf(sc.getFragmentScanNumbers(), sc.getFragmentScanNumbers().length);
            else 
                    this.fragmentScans = sc.getFragmentScanNumbers();
            this.setFragmentScanNumbers(fragmentScans);
            
            this.dataPoints = new DataPoint[sc.getNumberOfDataPoints()]; 
            for (int i=0; i < sc.getNumberOfDataPoints(); ++i)
                    this.dataPoints[i] = new SimpleDataPoint(sc.getDataPoints()[i]);
            
            if (this.dataPoints != null) { this.setDataPoints(this.dataPoints); }
        }
        
        @Override
        public @Nonnull RawDataFile getDataFile() {
            return dataFile;
        }
    }

}