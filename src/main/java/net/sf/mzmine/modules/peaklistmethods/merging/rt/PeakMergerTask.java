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
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
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
import net.sf.mzmine.util.PeakListRowSorter;
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
	private static final boolean DEBUG = false;
	
	
    // RDF
    RawDataFile dataFile, ancestorDataFile, workingDataFile;

    private final MZmineProject project;

    // peaks lists
    private PeakList peakList, mergedPeakList;

//  // peaks counter
//  private int processedPeaks, totalPeaks;
    // Progress
    private int processedIter, totalIter;
    private int processedGroups, totalGroups;
    private double progressMax;

    // parameter values
    private String suffix;
    public static MZTolerance mzTolerance;
    public static RTTolerance rtTolerance;
    int nbScansTolerance;
    private boolean useOldestRDFAncestor;
    private double detectedMZSearchWidth;
    private boolean useOnlyDetectedPeaks;
    private boolean cumulativeComputing;
    private FilterShapeModel shapeFilterModel;
    private boolean removeOriginal;
    private ParameterSet parameters;

    // private static final String ancestor_suffix = "#Ancestor";
//    private static final String unpastableSep = MZmineCore.getUnpastableSep();
//    private static final String autogenPrefix = MZmineCore.getAutogenPrefix();
    private static final String unpastableSep = " \u00BB ";// (UTF-8)               //" \uD834\uDF06 "// (UTF-16);  
    private static final String autogenPrefix = "AUTOGEN" + unpastableSep;

    // For comparing small differences.
    private static final double EPSILON = 0.0000001;

    
    private ArrayList<PeakListRow> mergedPeakListRows;

    
    final private static boolean ON_THE_FLY_MERGING = false;
    
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
        //rtTolerance = parameters.getParameter(PeakMergerParameters.rtTolerance).getValue();
        nbScansTolerance = parameters.getParameter(PeakMergerParameters.nbScansTolerance).getValue();

        this.useOldestRDFAncestor = parameters.getParameter(
                PeakMergerParameters.useOldestRDFAncestor).getValue();
        this.detectedMZSearchWidth = parameters.getParameter(
                PeakMergerParameters.detectedMZSearchWidth).getValue();
        this.useOnlyDetectedPeaks = parameters.getParameter(
                PeakMergerParameters.useOnlyDetectedPeaks).getValue();
        this.cumulativeComputing = parameters.getParameter(
                PeakMergerParameters.cumulativeComputing).getValue();
        
        this.shapeFilterModel = parameters.getParameter(
                PeakMergerParameters.shapeFilterModel).getValue();
        
        
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
//    public double getFinishedPercentage() {
//        if (totalPeaks == 0)
//            return 0.0f;
//        return (double) processedPeaks / (double) totalPeaks;
//        
//    }
    public double getFinishedPercentage() {
        if (totalIter == 0)
            return 0.0f;
        double progressMax2 = 0d;
        if (totalGroups == 0) {
            progressMax2 = ((double) processedIter / (double) totalIter)*0.7;
            //return progressMax;
        } else {
            progressMax2 = 0.7 + ((double) processedGroups / (double) totalGroups)*0.3;
            //return progressMax;
        }
        if (progressMax2 > progressMax)
            progressMax = progressMax2;
        return Math.max(progressMax, progressMax2);
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
        // Sort by RT (Ascending) => *[1]
        Collections.sort(sortedPeaks, new PeakSorter(SortingProperty.RT, SortingDirection.Ascending));

        
        this.ancestorDataFile = DataFileUtils.getAncestorDataFile(this.project, this.dataFile, true);
        this.workingDataFile = this.getWorkingDataFile(/*sortedPeaks*/);
        
        
        //--------------------     <BAD SHAPED>     --------------------//
        // Store all "bad shaped" (bs) peaks.
        PeakList badShapedPeaksList = ShapeFilterTask.getBadShapedPeakListRows(peakList, "", 1.1, 0.0, this.shapeFilterModel);
        logger.info(">>>>>>>>>>>>>>>> Found bad shaped (non-triangular) peaks: " + badShapedPeaksList.getNumberOfRows());
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
        //totalPeaks = sortedPeaks.size();
        int totalPeaks = sortedPeaks.size();
        int nb_empty_peaks = 0;


        
        if (ON_THE_FLY_MERGING) {
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
                    //processedPeaks++;
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
                // Maximized bounds (min/max scan numbers)
                
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
                
    //            int[] scanNums = groupedPeaks.get(0).getScanNumbers();
    //            minScanNumber = scanNums[0];
    //            maxScanNumber = scanNums[scanNums.length - 1];
                
                
    //            // Averaged bounds (min/max scan numbers)
    //            // Do not get scan nums from highest peak, but from the whole group (averaged RT range)
    //            minScanNumber = 0;
    //            maxScanNumber = 0;
    //            for (int i = 0; i < groupedPeaks.size(); i++) {
    //                
    //                int[] scanNums = groupedPeaks.get(i).getScanNumbers();
    //                
    //                //if (scanNums[0] < minScanNumber) {
    //                    minScanNumber += scanNums[0];
    //                //}
    //                
    //                //if (scanNums[scanNums.length - 1] > maxScanNumber) {
    //                    maxScanNumber += scanNums[scanNums.length - 1];
    //                //}
    //            }
    //            minScanNumber = (int) Math.round(minScanNumber / (double) groupedPeaks.size());
    //            maxScanNumber = (int) Math.round(maxScanNumber / (double) groupedPeaks.size());
                
    
                // Apex of the most representative (highest) peak of the group.
                int originScanNumber = oldPeak.getRepresentativeScanNumber();
                MergedPeak newPeak = new MergedPeak(this.workingDataFile, FeatureStatus.DETECTED);
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
    
                    if (DEBUG )
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
                    if (DEBUG)
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
    
//                // Update completion rate
//                processedPeaks++;
    
            }
        } 
        
        
        
        
        
        
        
        
        
        /** ********************************************************************
         >>> PEAKS MERGING (new way!)
         ******************************************************************** */
        
        else {
            
            // Build groups nicely, aka iteratively
            // 1/ Group peaks with exact same apex
            List<List<Feature>> baseGroups = new ArrayList<List<Feature>>();
            List<Integer> apexScans = new ArrayList<Integer>();
            List<Range<Double>> groupBounds = new ArrayList<>();
            for (int ind = 0; ind < totalPeaks; ind++) {
                
                Feature aPeak = sortedPeaks.get(ind);
                
                if (DEBUG)
	                for (int s_i: aPeak.getScanNumbers()) { 
	                    if(aPeak.getDataPoint(s_i).getIntensity() <= EPSILON) {
	                        logger.info("!!! Found input peak with zero intensity scan: " + aPeak);
	                        break;
	                    }
	                }
                
                
                // Delete (and skip) if peak is a Bad Shaped one
                if (badShapedPeaks.contains(aPeak)) {
                    //processedPeaks++;
                    continue;
                }
                
                // Search groups for identical apex
                int apexScan = aPeak.getRepresentativeScanNumber();
                boolean found_group = false;
                
                for (int g_i=0; g_i < baseGroups.size(); g_i++) {
                    
                    for (int p_i=0; p_i < baseGroups.get(g_i).size(); p_i++) {
                        
                        // Found proper group (exact same apexRT, aka exact same apex scan number!)
                        if (baseGroups.get(g_i).get(p_i).getRepresentativeScanNumber() == apexScan) {
                            
                            baseGroups.get(g_i).add(aPeak);
                            found_group = true;
                            break;
                        }
                    }
                    if (found_group)
                        break;
                }
                // If not found, create a new group, then add isolated peak
                if (!found_group) {
                    baseGroups.add(new ArrayList<Feature>());
                    baseGroups.get(baseGroups.size() - 1).add(aPeak);
                    apexScans.add(apexScan);
                    groupBounds.add(aPeak.getRawDataPointsRTRange());
                }
            }
            
            
            // 
            // 2/ Start scan by scan groups grouping
            int halfWidth = (nbScansTolerance / 2);
//            double scanWidth = this.workingDataFile.getScan(this.workingDataFile.getScanNumbers()[1]).getRetentionTime() 
//                    - this.workingDataFile.getScan(this.workingDataFile.getScanNumbers()[0]).getRetentionTime();
//            int max_step = Math.max(1, (int) ((rtTolerance.getTolerance() / scanWidth)));
            int max_step = Math.max(1, nbScansTolerance);//halfWidth);
            //-
            int sc_step = 1;
            boolean changes_occurred = false;
            
            List<List<Feature>> mergedGroups = new ArrayList<List<Feature>>();
            
            
            // Progress
            processedIter = 0;
            totalIter = baseGroups.size() * max_step;
            
            // 
            while (sc_step <= max_step) {//1) {//
                
                for (int g_i=0; g_i < baseGroups.size(); g_i++) {
                //for (int g_i = baseGroups.size()-1; g_i >= 0; --g_i) {
                    
                    processedIter++;
                    
                    List<Feature> curGroup = baseGroups.get(g_i);
                    
                    // Skip already merged group
                    if (mergedGroups.contains(curGroup)) {
                        continue;
                    }
                    
                    
                    int curGroupApexScanNumber = apexScans.get(g_i);
                    double curGroupApexRT = this.workingDataFile.getScan(curGroupApexScanNumber).getRetentionTime();
                    double curGroupApexIntensity = this.workingDataFile.getScan(curGroupApexScanNumber).getTIC();
                    
                    // Find candidates in other groups
                    for (int cg_i=0; cg_i < baseGroups.size(); cg_i++) {
                        
                        // Skip current group 
                        if (cg_i == g_i) { continue; }
                        
                        List<Feature> candidateGroup = baseGroups.get(cg_i);
                        
                        // Skip already merged group
                        if (mergedGroups.contains(candidateGroup)) {
                            continue;
                        }
                        
                        
                        int candidateGroupApexScanNumber = apexScans.get(cg_i);
                        double candidateGroupApexRT = this.workingDataFile.getScan(candidateGroupApexScanNumber).getRetentionTime();
                        
                        // If group apex is out of reach because out of RT tolerance window, skip it...
                        //if (!rtTolerance.checkWithinTolerance(curGroupApexRT, candidateGroupApexRT))
                        //    continue;
                        if (curGroupApexScanNumber < candidateGroupApexScanNumber - nbScansTolerance//- halfWidth 
                        		|| curGroupApexScanNumber > candidateGroupApexScanNumber + nbScansTolerance)//halfWidth)
                        	continue;
                            
//                         If group apex is close enough from current group (considering scan by scan stepping)
//                         integrate it...
                        //-
                        // Integrate group to current group if and ONLY if:
                        //      1) Apex is close enough from current group (considering scan by scan stepping)
                        //      2) Group is overlapping current group in some way 
                        //              2.1) -> at least one scan of one peak in common (see group bounds are overlapping)
                        //              2.2) -> the intensity of the common scan is strictly greater > 0.0 
                        int sc_num_plus = candidateGroupApexScanNumber + sc_step;
                        int sc_num_minus = candidateGroupApexScanNumber - sc_step;
                        //
                        if (this.workingDataFile.getScan(sc_num_plus) != null && this.workingDataFile.getScan(sc_num_minus) != null) {
                        
                            double rt = this.workingDataFile.getScan(candidateGroupApexScanNumber).getRetentionTime();
                            
                            double rt_plus_1 = this.workingDataFile.getScan(sc_num_plus).getRetentionTime();
                            double rt_minus_1 = this.workingDataFile.getScan(sc_num_minus).getRetentionTime();
                            // 1)
                            if (Math.abs(curGroupApexRT - rt_minus_1) < EPSILON || Math.abs(curGroupApexRT - rt_plus_1) < EPSILON)
                            {
                                
                                // 2) An intersection exists and is not empty
                                if (groupBounds.get(g_i).isConnected(groupBounds.get(cg_i))
                                        && !groupBounds.get(g_i).intersection(groupBounds.get(cg_i)).isEmpty()) {
                                    // Merge group into current
                                    curGroup.addAll(candidateGroup);
                                    // Clear candidate group once merged
                                    mergedGroups.add(candidateGroup);
                                }
                            }
                        }
                        
                    }
                    
                }
                
                
                // Update apex and bounds for all groups!
                for (int g_i=0; g_i < baseGroups.size(); g_i++) {
                    
                    List<Feature> curGroup = baseGroups.get(g_i);
                    
                    int curGroupApexScanNumber = apexScans.get(g_i);
                    double curGroupApexIntensity = this.workingDataFile.getScan(curGroupApexScanNumber).getTIC();
                    //-
                    Range<Double> bounds = curGroup.get(0).getRawDataPointsRTRange();//Range.openClosed(Double.MAX_VALUE, Double.MIN_VALUE);
                    
                    
                    // Update apex scan index
                    int newApexScanNumber = curGroupApexScanNumber;
                    Feature bestPeak = null;
                    for (Feature peak: curGroup) {

                        // Best peak
                        if (bestPeak == null || peak.getHeight() > bestPeak.getHeight())
                            bestPeak = peak;
                        
                        // Best bounds overall
                        bounds.span(peak.getRawDataPointsRTRange());
                    }
                    
                    // Apex
                    if (bestPeak != null) {
                        newApexScanNumber = bestPeak.getRepresentativeScanNumber();
                        apexScans.set(g_i, newApexScanNumber);
                        
                        if (newApexScanNumber != curGroupApexScanNumber)
                            changes_occurred = true;
                    }
                    
                    // Bounds
                    groupBounds.set(g_i, bounds);
                }
                
                
                if (!changes_occurred) {
                    sc_step++;
                } else {
                    sc_step = 1;
                    changes_occurred = false;
                    
                    processedIter = 0;
                }

            }
            
            
            if (DEBUG)
            	logger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> !!!  STARTING MERGE  !!! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            
            // <a/> Merging peaks inside each group
            
            // Progress
            processedGroups = 0;
            totalGroups = baseGroups.size();
            
            List<List<DataPoint>> spectrumOfInterestPerMergedPeak = new ArrayList<>();

//            for (int ind = 0; ind < totalPeaks; ind++) {
            for (int ind = 0; ind < baseGroups.size(); ind++) {
                
                // Task canceled by user
                if (isCanceled())
                    return;
                
                List<Feature> curGroup = baseGroups.get(ind);
                
                //##processedPeaks += curGroup.size();
                processedGroups++;
                
                // Skip already merged group
                if (mergedGroups.contains(curGroup)) { continue; }
                
    
                // Build RT group
                ///////////////ArrayList<Feature> groupedPeaks = this.getPeaksGroupByRT(aPeak, sortedPeaks);
                ArrayList<Feature> groupedPeaks = (ArrayList<Feature>) curGroup;
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
                // Maximized bounds (min/max scan numbers)
                
                for (int i = 0; i < groupedPeaks.size(); i++) {
                    int[] scanNums = groupedPeaks.get(i).getScanNumbers();
                    if (scanNums[0] < minScanNumber) {
                        minScanNumber = scanNums[0];
                    }
                    if (scanNums[scanNums.length - 1] > maxScanNumber) {
                        maxScanNumber = scanNums[scanNums.length - 1];
                    }
                }
    
                // Apex of the most representative (highest) peak of the group.
                int originScanNumber = oldPeak.getRepresentativeScanNumber();
                MergedPeak newPeak = new MergedPeak(this.workingDataFile, FeatureStatus.DETECTED);
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
    
                    if (DEBUG)
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
    
                
                
                //- Set DETECTED only spectrum
                newPeak.setSpectrumOfInterest(/*spectrumOfInterest*/groupedPeaks);
                //-
                
                
                
                // Get peak list row to be updated
                PeakListRow oldRow = peakList.getPeakRow(oldPeak);
    
                // TODO: Might be VIOLENT (quick and dirty => to be verified)
                if (newPeak.getScanNumbers().length == 0) {
                    ++nb_empty_peaks;
                    if (DEBUG)
                    	logger.log(Level.WARNING, "0 scans peak found !");
                    break;
                }
    
                // Add new merged peak to "mergedPeakList", keeping the original ID.
                updateMergedPeakListRow(oldRow, newPeak);
                
                if (DEBUG)
	                for (int s_i: newPeak.getScanNumbers()) { 
	                    if(newPeak.getDataPoint(s_i).getIntensity() <= EPSILON) {
	                        logger.info("!!! Found merged peak with zero intensity scan: " + newPeak);
	                        break;
	                    }
	                }

                
                // Clear already treated peaks
                for (int g_i = 0; g_i < groupedPeaks.size(); g_i++) {
                    
                    sortedPeaks.set(sortedPeaks.indexOf(groupedPeaks.get(g_i)), null);
                }
    
            }
        }

        if (nb_empty_peaks > 0 && DEBUG)
            logger.log(Level.WARNING, "Skipped \"" + nb_empty_peaks  + "\" empty peaks !");
        
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
                
                //- Set DETECTED only spectrum
                //new_p.setSpectrumOfInterest(mergedPeaks);
                new_p.setIsotopePattern(p.getIsotopePattern());
                //-
                
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
        return EPSILON;
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
            
            // !!!!!!!!!!!!!!!!!!
            // TODO: Shouldn't the correct sequence contain the apex of the candidate peak as well !!??
            
        	if (DEBUG)
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

            if (DEBUG)
	            logger.log(Level.INFO,
	                    "Valid sequence is: " + scan_nums_ok.toString());

            double rt_min = this.workingDataFile.getScan(scan_nums_ok.get(0))
                    .getRetentionTime();
            double rt_max = this.workingDataFile.getScan(
                    scan_nums_ok.get(scan_nums_ok.size() - 1))
                    .getRetentionTime();

            Range<Double> rt_range_ok = Range.closed(rt_min - EPSILON, rt_max
                    + EPSILON);

            // List bad candidates
            Vector<Feature> badCandidates = new Vector<Feature>();
            for (Feature p : groupedPeaks) {
                if (!rt_range_ok.encloses(p.getRawDataPointsRTRange())) {
                    badCandidates.add(p);
                    
                    if (DEBUG) {
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
        
        // ---
        // *[1] : Since we sorted by RT at the very beginning of this task:
        //              Make sure to sort back by ID (Ascending) (The default in MZmine)
        Collections.sort(this.mergedPeakListRows, new PeakListRowSorter(
                SortingProperty.ID, SortingDirection.Ascending));
        // ---
        
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


}
