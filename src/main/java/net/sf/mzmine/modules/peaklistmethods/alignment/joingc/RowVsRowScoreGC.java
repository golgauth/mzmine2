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
import java.util.logging.Logger;

import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationSingleTask;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.SimilarityMethodType;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataFileUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * This class represents a score between peak list row and aligned peak list row
 */
public class RowVsRowScoreGC implements Comparable<RowVsRowScoreGC> {
    
    // Logger.
    private static final Logger LOG = Logger
                    .getLogger(RowVsRowScoreGC.class.getName());

    private PeakListRow peakListRow, alignedRow;
    double score;

    RowVsRowScoreGC(PeakListRow peakListRow, PeakListRow alignedRow,
	    double mzMaxDiff, double mzWeight, double rtMaxDiff, double rtWeight) {

	this.peakListRow = peakListRow;
	this.alignedRow = alignedRow;

	// Calculate differences between m/z and RT values
	double mzDiff = Math.abs(peakListRow.getAverageMZ()
		- alignedRow.getAverageMZ());

	double rtDiff = Math.abs(peakListRow.getAverageRT()
		- alignedRow.getAverageRT());

	score = ((1 - mzDiff / mzMaxDiff) * mzWeight)
		+ ((1 - rtDiff / rtMaxDiff) * rtWeight);

    }
    
    RowVsRowScoreGC(RawDataFile rawDF, Hashtable<RawDataFile, double[]> rtAdjustementMapping,
            PeakListRow peakListRow, PeakListRow alignedRow,
            double mzMaxDiff, double mzWeight, double rtMaxDiff, double rtWeight,
            double idWeight,
            boolean useApex, boolean useKnownCompoundsAsRef, RTTolerance rtToleranceAfter
            //, PeakListRow[] allRows, PeakListRow[] candidateRows
            ) 
    {

        this.peakListRow = peakListRow;
        this.alignedRow = alignedRow;

        // Calculate differences between m/z and RT values
//        double mzDiff = Math.abs(peakListRow.getAverageMZ()
//                - alignedRow.getAverageMZ());

//        double rtDiff = Math.abs(peakListRow.getAverageRT()
//                - alignedRow.getAverageRT());
        
        // * Use true chemical similarity rather than average
        // * Use apex RT rather than average (and do the same in "JoinAlignerTask")
        // TODO: Do it in RANSAC Aligner as well !!!
        double rtDiff = 0.0;
        double[] vec1 = new double[JDXCompound.MAX_MZ];
        Arrays.fill(vec1, 0.0);
        double[] vec2 = new double[JDXCompound.MAX_MZ];
        Arrays.fill(vec2, 0.0);
        
        // RT at apex
        boolean recalibrateRT = useKnownCompoundsAsRef;
        double adjustedRT = 0.0;
        if (useApex) {
            // RT at apex

            if (recalibrateRT) {
                
                double b_offset = rtAdjustementMapping.get(rawDF)[0];
                double a_scale = rtAdjustementMapping.get(rawDF)[1];
                //**double rt1 = rtAdjustementMapping.get(peakList)[2];
                //**double rt2 = rtAdjustementMapping.get(peakList)[3];
                //adjustedRT = (peakListRow.getAverageRT() + offset) * scale;
                //**adjustedRT = ((peakListRow.getAverageRT() - rt1) * scale) + rt1 + offset;
//                double delta_rt = a_scale * peakListRow.getAverageRT() + b_offset;
//                adjustedRT = peakListRow.getAverageRT() + delta_rt;
                adjustedRT = JoinAlignerGCTask.getAdjustedRT(peakListRow.getAverageRT(), b_offset, a_scale);


                
                // Compare what is comparable: "alignedRow.getAverageRT()" returns
                // a comparable value because RT are adjusted on the fly in the
                // "JoinAlignerTask" (See *[Note 1]).
                rtDiff = Math.abs(adjustedRT
                        - alignedRow.getAverageRT());
                
//                // DEBUG
//                LOG.info("RT (adjusted / avg): " + adjustedRT + " / " + peakListRow.getAverageRT());
//                double rtDiff000 = Math.abs(peakListRow.getAverageRT()
//                        - alignedRow.getAverageRT());
//                LOG.info("RTdiff (row / alignRow): " + peakListRow + " / " + alignedRow);
//                LOG.info("RTdiff (offset / scale): " + b_offset + " / " + a_scale);
//                LOG.info("RTdiff (adjusted / avg): " + rtDiff + " / " + rtDiff000);
                
                
//                switch (commonIdentified.size()) {
//                case 0: // Do not recalibrate
//                    rtDiff = Math.abs(peakListRow.getBestPeak().getRT()
//                            - alignedRow.getBestPeak().getRT());                    
//                    break;
//                case 1: // Recalibrate by offset
//                    // ...
//                    double offsetRT = commonIdentified.get(0)[1].getBestPeak().getRT() - commonIdentified.get(0)[0].getBestPeak().getRT();
//                    break;
//                case 2: // Recalibrate by scale
//                    // ...
//                    break;
//                default: // Recalibrate by scales along segments
//                    // ... Complicated: We do not want to implement this yet ...
//                    break;
//                }
                
                
            } else {
                
                rtDiff = Math.abs(peakListRow.getAverageRT()
                      - alignedRow.getAverageRT());
                
            }
            
        } else {
            // Compute mean RTs
            // TODO: !!!!
            // RT at apex (just keep apex for RT for now...)
            rtDiff = Math.abs(peakListRow.getBestPeak().getRT()
                  - alignedRow.getBestPeak().getRT());
//            rtDiff = Math.abs(peakListRow.getAverageRT()
//                    - alignedRow.getAverageRT());
        }
        
        // MZ at apex
        if (useApex) {
            // MZ at apex
            RawDataFile refRDF = peakListRow.getRawDataFiles()[0];
            Scan apexScan = refRDF.getScan(peakListRow.getBestPeak().getRepresentativeScanNumber());
            // Get scan m/z vector.
            //LOG.info("DPs MZ Range: " + apexScan.getMZRange());
            DataPoint[] dataPoints = apexScan.getDataPoints();
            for (int j=0; j < dataPoints.length; ++j) {
                DataPoint dp = dataPoints[j];
                vec1[(int) Math.round(dp.getMZ())] = dp.getIntensity();
            }
//            //
//            refRDF = alignedRow.getRawDataFiles()[0];
//            apexScan = refRDF.getScan(alignedRow.getBestPeak().getRepresentativeScanNumber());
//            dataPoints = apexScan.getDataPoints();
//            for (int j=0; j < dataPoints.length; ++j) {
//                DataPoint dp = dataPoints[j];
//                vec2[(int) Math.round(dp.getMZ())] = dp.getIntensity();
//            }
            // Nop! We want to compare similarity with the "synthetic sample" resulting from
            // all the peaks already grouped into the aligned row. This "synthetic sample" is
            // continuously evolving.
            // Average (MZ profile) of all the already aligned peaks at apex:
            int nbPeaks = alignedRow.getRawDataFiles().length;
            for (RawDataFile rdf : alignedRow.getRawDataFiles()) {
                apexScan = rdf.getScan(alignedRow.getPeak(rdf).getRepresentativeScanNumber());
                dataPoints = apexScan.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec2[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
            }
            for (int j=0; j < vec2.length; ++j) {
                vec2[j] /= nbPeaks;
            }
        } else {
            // Compute mean MZ profiles
            
            // MZ at apex and 5% around
            RawDataFile refRDF = peakListRow.getRawDataFiles()[0];
            Scan apexScan = refRDF.getScan(peakListRow.getBestPeak().getRepresentativeScanNumber());
            // 
            // vec1
            // Scan numbers to be averaged
            int apexScanNumber = apexScan.getScanNumber();
            int[] peakScanNumbers = peakListRow.getBestPeak().getScanNumbers();
            int nbSideScans = (int) Math.round(peakScanNumbers.length * 5.0 / 100.0);
            int firstScanNum = Math.max(apexScanNumber - nbSideScans, peakScanNumbers[0]);
            int lastScanNum = Math.min(apexScanNumber + nbSideScans, peakScanNumbers[peakScanNumbers.length-1]);
            int nbScans = lastScanNum - firstScanNum;
            //
            // Compute average
            for (int i=firstScanNum; i<lastScanNum; ++i) {
                Scan curScan = refRDF.getScan(i);
                DataPoint[] dataPoints = curScan.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
            }
            for (int j=0; j < vec1.length; ++j) {
                vec1[j] /= nbScans;
            }
            
            // vec2
            refRDF = alignedRow.getRawDataFiles()[0];
            apexScan = refRDF.getScan(alignedRow.getBestPeak().getRepresentativeScanNumber());
            // Scan numbers to be averaged
            apexScanNumber = apexScan.getScanNumber();
            peakScanNumbers = alignedRow.getBestPeak().getScanNumbers();
            firstScanNum = Math.max(apexScanNumber - nbSideScans, peakScanNumbers[0]);
            lastScanNum = Math.min(apexScanNumber + nbSideScans, peakScanNumbers[peakScanNumbers.length-1]);
            nbScans = lastScanNum - firstScanNum;
            //
            // Compute average
            for (int i=firstScanNum; i<lastScanNum; ++i) {
                Scan curScan = refRDF.getScan(i);
                DataPoint[] dataPoints = curScan.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec2[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
            }
            for (int j=0; j < vec2.length; ++j) {
                vec2[j] /= nbScans;
            }
        }
        

        double chemSimScore = computeSimilarityScore(vec1, vec2);

        // Check if RT, once recalibrated, still allows to join the new row
        if (recalibrateRT) {
//            Range<Double> rtRangeAfter = rtToleranceAfter.getToleranceRange(adjustedRT);
//            if (!rtRangeAfter.contains(alignedRow.getAverageRT())) {
//                score = -1.0;
//                return;
//            }
            if (rtDiff > rtToleranceAfter.getTolerance() / 2.0) {
                // Finally reject the peaks of this row
                score = JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE;
                return;
            }
        }
        
        // Scoring       
//        score = ((1 - mzDiff / mzMaxDiff) * mzWeight)
//                + ((1 - rtDiff / rtMaxDiff) * rtWeight);
        
        double idSimScore = (JDXCompound.isKnownIdentity(peakListRow.getPreferredPeakIdentity())
                && JDXCompound.isKnownIdentity(alignedRow.getPreferredPeakIdentity())
                && peakListRow.getPreferredPeakIdentity().getName() == alignedRow.getPreferredPeakIdentity().getName()) ? 1.0 : 0.0;
        
        score = (chemSimScore * mzWeight)
                + ((1 - rtDiff / rtMaxDiff) * rtWeight);
                //+ idSimScore * idWeight;

    }
    
    // Compute chemical similarity score using dot product method
    private double computeSimilarityScore(double[] vec1, double[] vec2) {

        double simScore = 0.0;

        try {
            simScore = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
        } catch (IllegalArgumentException e ) {
            LOG.severe(e.getMessage());
        } 

        return simScore;
    }

    public static double computeSimilarityScore(double[] vec1, double[] vec2, SimilarityMethodType simMethodType) throws IllegalArgumentException {

        double simScore = 0.0;

        try {

            if (simMethodType == SimilarityMethodType.DOT) {
                double[] vec1_norm = new double[vec1.length];
                double[] vec2_norm = new double[vec2.length];

                double div1 = 0.0, div2 = 0.0;
                for (int i=0; i < vec1.length; ++i) {
                    div1 += vec1[i] * vec1[i];
                    div2 += vec2[i] * vec2[i];
                }
                for (int i=0; i < vec1.length; ++i) {
                    vec1_norm[i] = vec1[i] / Math.sqrt(div1);
                    vec2_norm[i] = vec2[i] / Math.sqrt(div2);
                }
                simScore = (new ArrayRealVector(vec1_norm)).dotProduct(vec2_norm);
            } else if (simMethodType == SimilarityMethodType.PEARSON) {
                simScore = new PearsonsCorrelation().correlation(vec1, vec2);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to compute similarity score for vec1.length=" + vec1.length + " and vec2.length=" + vec2.length);
        } 

        return simScore;
    }

    /**
     * This method returns the peak list row which is being aligned
     */
    PeakListRow getPeakListRow() {
	return peakListRow;
    }

    /**
     * This method returns the row of aligned peak list
     */
    PeakListRow getAlignedRow() {
	return alignedRow;
    }

    /**
     * This method returns score between the these two peaks (the lower score,
     * the better match)
     */
    double getScore() {
	return score;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RowVsRowScoreGC object) {

	// We must never return 0, because the TreeSet in JoinAlignerTask would
	// treat such elements as equal
	if (score < object.getScore())
	    return 1;
	else
	    return -1;

    }

}
