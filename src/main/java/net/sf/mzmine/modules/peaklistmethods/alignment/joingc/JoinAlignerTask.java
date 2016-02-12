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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import com.google.common.collect.Range;

class JoinAlignerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private PeakList peakLists[], alignedPeakList;

    // Processed rows counter
    private int processedRows, totalRows;

    private String peakListName;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private double mzWeight, rtWeight;
    
    private boolean useApex, useKnownCompoundsAsRef;
    private RTTolerance rtToleranceAfter;
    
    /** GLG HACK: temporary removed for clarity
    private boolean sameIDRequired, sameChargeRequired, compareIsotopePattern;
    **/
    private ParameterSet parameters;

    // ID counter for the new peaklist
    private int newRowID = 1;

    JoinAlignerTask(MZmineProject project, ParameterSet parameters) {

        this.project = project;
        this.parameters = parameters;

        peakLists = parameters.getParameter(JoinAlignerParameters.peakLists)
                .getValue().getMatchingPeakLists();

        peakListName = parameters.getParameter(
                JoinAlignerParameters.peakListName).getValue();

        mzTolerance = parameters
                .getParameter(JoinAlignerParameters.MZTolerance).getValue();
        rtTolerance = parameters
                .getParameter(JoinAlignerParameters.RTTolerance).getValue();

        mzWeight = parameters.getParameter(JoinAlignerParameters.MZWeight)
                .getValue();

        rtWeight = parameters.getParameter(JoinAlignerParameters.RTWeight)
                .getValue();
        
        //***
        useApex = parameters.getParameter(
                JoinAlignerParameters.useApex).getValue();
        useKnownCompoundsAsRef = parameters.getParameter(
                JoinAlignerParameters.useKnownCompoundsAsRef).getValue();
        rtToleranceAfter = parameters.getParameter(
                JoinAlignerParameters.RTToleranceAfter).getValue();
        //***
        
        /** GLG HACK: temporarily removed for clarity
        sameChargeRequired = parameters.getParameter(
                JoinAlignerParameters.SameChargeRequired).getValue();

        sameIDRequired = parameters.getParameter(
                JoinAlignerParameters.SameIDRequired).getValue();

        compareIsotopePattern = parameters.getParameter(
                JoinAlignerParameters.compareIsotopePattern).getValue();
        **/
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Join aligner, " + peakListName + " (" + peakLists.length
                + " peak lists)";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0f;
        return (double) processedRows / (double) totalRows;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        if ((mzWeight == 0) && (rtWeight == 0)) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Cannot run alignment, all the weight parameters are zero");
            return;
        }

        setStatus(TaskStatus.PROCESSING);
        logger.info("Running join aligner");

        // Remember how many rows we need to process. Each row will be processed
        // twice, first for score calculation, second for actual alignment.
        for (int i = 0; i < peakLists.length; i++) {
            totalRows += peakLists[i].getNumberOfRows() * 2;
        }

        // Collect all data files
        Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
        for (PeakList peakList : peakLists) {

            for (RawDataFile dataFile : peakList.getRawDataFiles()) {

                // Each data file can only have one column in aligned peak list
                if (allDataFiles.contains(dataFile)) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("Cannot run alignment, because file "
                            + dataFile + " is present in multiple peak lists");
                    return;
                }

                allDataFiles.add(dataFile);
            }
        }

        // Create a new aligned peak list
        alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[0]));

        /** RTAdjustement mapping **/
        boolean recalibrateRT = useKnownCompoundsAsRef;
        Hashtable<PeakList, double[]> rtAdjustementMapping = new Hashtable<PeakList, double[]>();
        if (recalibrateRT) {
            
            boolean rtAdjustOk = true;
            // Iterate source peak lists
            // RT mapping based on the first PeakList
            double rt1 = -1.0;
            double rt2 = -1.0;
            for (int i=0; i < peakLists.length; ++i) {
                //double offset, scale;
                PeakList a_pl = peakLists[i];
                
                // Get ref RT1 and RT2
                // Sort peaks by ascending RT
                PeakListRow a_pl_rows[] = a_pl.getRows(); 
                Arrays.sort(a_pl_rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
                ArrayList<PeakListRow> allIdentified = new ArrayList<PeakListRow>();
                for (int j=0; j < a_pl_rows.length; ++j) {
                    PeakListRow row = a_pl_rows[j];
                    // If row actually was identified
                    if (row.getPreferredPeakIdentity() != null 
                            && !row.getPreferredPeakIdentity().getName().equals(JDXCompound.UNKNOWN_JDX_COMP.getName())) {
                        allIdentified.add(row);
                    }

//                    logger.info("ID = " + row.getPreferredPeakIdentity());
//                    logger.info("ID Unknown = " + JDXCompound.UNKNOWN_JDX_COMP);
//                    logger.info("ID Equals = " + row.getPreferredPeakIdentity().equals(JDXCompound.UNKNOWN_JDX_COMP));
                }
                logger.info("allIdentified: NB found compounds: " + allIdentified.size());
                for (PeakListRow r : allIdentified) {
                    logger.info(r.getPreferredPeakIdentity().toString());
                }
                if (allIdentified.size() == 2) {
        //            RT1 = allIdentified.get(0).getPeaks()[0].getRT();
        //            RT2 = allIdentified.get(0).getPeaks()[0].getRT();
                    // TODO: Or even better: duplicate the Peaklists involved and update the peaks RT
                    //          using "Feature.setRT()", as planned some time ago via button "Apply & Adjust RT"
                    //          from RTAdjuster module's result table !!!
                    rt1 = allIdentified.get(0).getAverageRT();
                    rt2 = allIdentified.get(1).getAverageRT();
                } else {
                    logger.info("Error => Couldn't identify the 2 required ref compounds for peaklist: " + a_pl);
                    rtAdjustOk = false;
                    continue;
                }
                
                // First list as ref, so:
//                double offset, scale;
                // Think of "y = ax+b" (line equation)
                double b_offset, a_scale;
                if (i == 0) {
                    // ********
//                    offset = 0.0;
//                    scale = 1.0;
                    // ********
                    b_offset = 0.0;
                    a_scale = 0.0;
                    // ********
                    rtAdjustementMapping.put(a_pl, new double[]{ b_offset, a_scale, rt1, rt2 });
                } else {
                    double rt1_ref = rtAdjustementMapping.get(peakLists[0])[2];
                    double rt2_ref = rtAdjustementMapping.get(peakLists[0])[3];
//                    offset =  rt1 - rt1_ref;
//                    scale = (rt2 - rt1) / (rt2_ref - rt1_ref);
                    // ********
//                    offset =  rt1_ref - rt1;
//                    scale = (rt2_ref - rt1_ref) / (rt2 - rt1);
                    // ********
                    a_scale = ((rt2_ref - rt2) - (rt1_ref - rt1)) / (rt2 - rt1);
                    b_offset = (rt1_ref - rt1) - (a_scale * rt1);
                    // ********
                    rtAdjustementMapping.put(a_pl, new double[]{ b_offset, a_scale, rt1, rt2 });
                    logger.info(">> peakLists[0]/peakLists[i]:" + peakLists[0] + "/" + peakLists[i]);
                    logger.info(">> rt1_ref/rt1:" + rt1_ref + "/" + rt1);
                    logger.info(">> rt2_ref/rt2:" + rt2_ref + "/" + rt2);
                    logger.info(">> offset/scale: " + b_offset + "/" + a_scale);
                }
    
            }
            //
            if (!rtAdjustOk) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Cannot run alignment, because ref compounds detection was incomplete");
                return;
            }
        }
        
        
        /** Alignment mapping **/ 
        // Iterate source peak lists
        Hashtable<SimpleFeature, Double> rtPeakBackup = new Hashtable<SimpleFeature, Double>();
        //
        for (PeakList peakList : peakLists) {

            // Create a sorted set of scores matching
            TreeSet<RowVsRowScore> scoreSet = new TreeSet<RowVsRowScore>();

            PeakListRow allRows[] = peakList.getRows();
            logger.info("Treating list " + peakList + " / NB rows: " + allRows.length);

            // Calculate scores for all possible alignments of this row
            for (PeakListRow row : allRows) {

                if (isCanceled())
                    return;

                // Calculate limits for a row with which the row can be aligned
//                Range<Double> mzRange = mzTolerance.getToleranceRange(row
//                        .getAverageMZ());
//                Range<Double> rtRange = rtTolerance.getToleranceRange(row
//                        .getAverageRT());
                // GLG HACK: Use best peak rather than average. No sure it is better... ???
                Range<Double> mzRange = mzTolerance.getToleranceRange(row
                        .getBestPeak().getMZ());
                Range<Double> rtRange = rtTolerance.getToleranceRange(row
                        .getBestPeak().getRT());

                // Get all rows of the aligned peaklist within parameter limits
                PeakListRow candidateRows[] = alignedPeakList
                        .getRowsInsideScanAndMZRange(rtRange, mzRange);

                // Calculate scores and store them
                for (PeakListRow candidate : candidateRows) {

                    /** GLG HACK: temporarily removed for clarity
                    if (sameChargeRequired) {
                        if (!PeakUtils.compareChargeState(row, candidate))
                            continue;
                    }
0.1
                    if (sameIDRequired) {
                        if (!PeakUtils.compareIdentities(row, candidate))
                            continue;
                    }

                    if (compareIsotopePattern) {
                        IsotopePattern ip1 = row.getBestIsotopePattern();
                        IsotopePattern ip2 = candidate.getBestIsotopePattern();

                        if ((ip1 != null) && (ip2 != null)) {
                            ParameterSet isotopeParams = parameters
                                    .getParameter(
                                            JoinAlignerParameters.compareIsotopePattern)
                                    .getEmbeddedParameters();

                            if (!IsotopePatternScoreCalculator.checkMatch(ip1,
                                    ip2, isotopeParams)) {
                                continue;
                            }
                        }
                    }
                    **/

//                    RowVsRowScore score = new RowVsRowScore(row, candidate,
//                            RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
//                            RangeUtils.rangeLength(rtRange) / 2.0, rtWeight);
                    // GLG HACK: Use apex rather than average!
                    RowVsRowScore score = new RowVsRowScore(
                            peakList, rtAdjustementMapping,
                            row, candidate,
                            RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
                            RangeUtils.rangeLength(rtRange) / 2.0, rtWeight, 
                            useApex, useKnownCompoundsAsRef, rtToleranceAfter);

                    // If match was not rejected afterwards and score is acceptable
                    if (score.getScore() > 0.0)
                        scoreSet.add(score);

                }

                processedRows++;

            }

            // Create a table of mappings for best scores
            Hashtable<PeakListRow, PeakListRow> alignmentMapping = new Hashtable<PeakListRow, PeakListRow>();

            // Iterate scores by descending order
            Iterator<RowVsRowScore> scoreIterator = scoreSet.iterator();
            while (scoreIterator.hasNext()) {

                RowVsRowScore score = scoreIterator.next();

                // Check if the row is already mapped
                if (alignmentMapping.containsKey(score.getPeakListRow()))
                    continue;

                // Check if the aligned row is already filled
                if (alignmentMapping.containsValue(score.getAlignedRow()))
                    continue;

                alignmentMapping.put(score.getPeakListRow(),
                        score.getAlignedRow());

            }

            // Align all rows using mapping
            for (PeakListRow row : allRows) {

                PeakListRow targetRow = alignmentMapping.get(row);

                // If we have no mapping for this row, add a new one
                if (targetRow == null) {
                    targetRow = new SimplePeakListRow(newRowID);
                    newRowID++;
                    alignedPeakList.addRow(targetRow);
                }

                // Add all peaks from the original row to the aligned row
                for (RawDataFile file : row.getRawDataFiles()) {
                    Feature originalPeak = row.getPeak(file);
                    if (originalPeak != null) {
                        
                        if (recalibrateRT) {
                            // Set adjusted retention time to all peaks in this row
                            // *[Note 1]
                            logger.info("{" + rtAdjustementMapping.get(peakList)[0] + ", " + rtAdjustementMapping.get(peakList)[1] + "}");
                            double b_offset = rtAdjustementMapping.get(peakList)[0];
                            double a_scale = rtAdjustementMapping.get(peakList)[1];
                            //**double rt1 = rtAdjustementMapping.get(peakList)[2];
                            //**double rt2 = rtAdjustementMapping.get(peakList)[3];
                            //double adjustedRT = (originalPeak.getRT() + offset) * scale;
                            //**double adjustedRT = ((originalPeak.getRT() - rt1) * scale) + rt1 + offset;
                            double delta_rt = a_scale * originalPeak.getRT() + b_offset;
                            double adjustedRT = originalPeak.getRT() + delta_rt;
                            
                            SimpleFeature adjustedPeak = new SimpleFeature(originalPeak);
                            PeakUtils.copyPeakProperties(originalPeak, adjustedPeak);
                            adjustedPeak.setRT(adjustedRT);
                            logger.info("adjusted Peak/RT = " + originalPeak + ", " + adjustedPeak + " / " + originalPeak.getRT() + ", " + adjustedPeak.getRT());
                            //logger.info("adjusted RT = " + originalPeak.getRT() + ", " + adjustedPeak.getRT());
                            targetRow.addPeak(file, adjustedPeak);
                            rtPeakBackup.put(adjustedPeak, originalPeak.getRT());
                        } else {
                            targetRow.addPeak(file, originalPeak);
                        }
                        logger.info("targetRow RT=" + targetRow.getPeaks()[targetRow.getPeaks().length-1].getRT() + " / ID: " + newRowID);
                    }
                    else {
                        setStatus(TaskStatus.ERROR);
                        setErrorMessage("Cannot run alignment, no originalPeak");
                        return;
                    }
                }
                
                // Add all non-existing identities from the original row to the
                // aligned row
                //PeakUtils.copyPeakListRowProperties(row, targetRow);
                // Set the preferred identity
                if (targetRow.getPreferredPeakIdentity() == null) {
                    targetRow.setPreferredPeakIdentity(row.getPreferredPeakIdentity());
                }
                // Copy all possible peak identities, if these are not already present
                for (PeakIdentity identity : row.getPeakIdentities()) {
                    if (!PeakUtils.containsIdentity(targetRow, identity))
                        targetRow.addPeakIdentity(identity, false);
                }
                // Notify MZmine about the change in the project
                // TODO: Get the "project" from the instantiator of this class instead.
                // Still necessary ???????
                MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
                project.notifyObjectChanged(targetRow, false);
                //MZmineCore.getDesktop().getMainWindow().repaint();
                
                processedRows++;

            }

        } // Next peak list
        
        //logger.info("alignedPeakList.getNumberOfRawDataFiles(): " + alignedPeakList.getNumberOfRawDataFiles());
        
        // Restore real RT - for the sake of consistency
        // (the adjusted one was only useful during alignment process)
        for (SimpleFeature peak : rtPeakBackup.keySet()) {
            peak.setRT(rtPeakBackup.get(peak));
        }

        // Add new aligned peak list to the project
        project.addPeakList(alignedPeakList);

        // Add task description to peakList
        alignedPeakList
                .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "Join aligner", parameters));

        logger.info("Finished join aligner");
        setStatus(TaskStatus.FINISHED);

    }

}