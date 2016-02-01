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

package net.sf.mzmine.modules.peaklistmethods.alignment.ransac;

import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.commons.math.linear.ArrayRealVector;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;

/**
 * This class represents a score between peak list row and aligned peak list row
 */
public class RowVsRowScore implements Comparable<RowVsRowScore> {
    
    // Logger.
    private static final Logger LOG = Logger
                    .getLogger(RowVsRowScore.class.getName());

    private PeakListRow peakListRow, alignedRow;
    double score;
    private String errorMessage;

    RowVsRowScore(PeakListRow peakListRow, PeakListRow alignedRow,
	    double mzMaxDiff, double rtMaxDiff, double correctedRT)
	    throws Exception {

	this.alignedRow = alignedRow;
	this.peakListRow = peakListRow;

	// Calculate differences between m/z and RT values
	double mzDiff = Math.abs(peakListRow.getAverageMZ()
		- alignedRow.getAverageMZ());
	double rtDiff = Math.abs(correctedRT - alignedRow.getAverageRT());

	score = ((1 - mzDiff / mzMaxDiff) + (1 - rtDiff / rtMaxDiff));
    }
    
    RowVsRowScore(PeakListRow peakListRow, PeakListRow alignedRow,
            double mzMaxDiff, double rtMaxDiff, double correctedRT, boolean useApex) {

        this.peakListRow = peakListRow;
        this.alignedRow = alignedRow;

        // Calculate differences between m/z and RT values
        double mzDiff = Math.abs(peakListRow.getAverageMZ()
                - alignedRow.getAverageMZ());
        double rtDiff = Math.abs(correctedRT - alignedRow.getAverageRT());

//        score = ((1 - mzDiff / mzMaxDiff) + (1 - rtDiff / rtMaxDiff));
        
        // Use true chemical similarity rather than average
        // TODO: use a weighted approach rather than just the apex MZ profile...
        RawDataFile refRDF = peakListRow.getRawDataFiles()[0];
        Scan apexScan = refRDF.getScan(peakListRow.getBestPeak().getRepresentativeScanNumber());
        // Get scan m/z vector.
        double[] vec1 = new double[JDXCompound.MAX_MZ];
        Arrays.fill(vec1, 0.0);
        //LOG.info("DPs MZ Range: " + apexScan.getMZRange());
        DataPoint[] dataPoints = apexScan.getDataPoints();
        for (int j=0; j < dataPoints.length; ++j) {
            DataPoint dp = dataPoints[j];
            vec1[(int) Math.round(dp.getMZ())] = dp.getIntensity();
        }
        double[] vec2 = new double[JDXCompound.MAX_MZ];
        Arrays.fill(vec2, 0.0);
        //
        refRDF = alignedRow.getRawDataFiles()[0];
        apexScan = refRDF.getScan(alignedRow.getBestPeak().getRepresentativeScanNumber());
        dataPoints = apexScan.getDataPoints();
        for (int j=0; j < dataPoints.length; ++j) {
            DataPoint dp = dataPoints[j];
            vec2[(int) Math.round(dp.getMZ())] = dp.getIntensity();
        }

        double chemSimScore = computeSimilarityScore(vec1, vec2);
//        score = ((1 - mzDiff / mzMaxDiff) + (1 - rtDiff / rtMaxDiff));
        score = (chemSimScore + (1 - rtDiff / rtMaxDiff));
    }
    
    // Compute chemical similarity score using dot product method
    private double computeSimilarityScore(double[] vec1, double[] vec2) {

        double simScore = 0.0;

        try {

            //                if (this.simMethodType == SimilarityMethodType.DOT) {
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
            //                } else if (this.simMethodType == SimilarityMethodType.PEARSON) {
                //                        simScore = new PearsonsCorrelation().correlation(vec1, vec2);
            //                }
        } catch (IllegalArgumentException e ) {
            LOG.severe("Failed to compute similarity score for vec1.length=" + vec1.length + " and vec2.length=" + vec2.length);
        } 

        return simScore;
    }

    /**
     * This method returns the peak list row which is being aligned
     */
    public PeakListRow getPeakListRow() {
	return peakListRow;
    }

    /**
     * This method returns the row of aligned peak list
     */
    public PeakListRow getAlignedRow() {
	return alignedRow;
    }

    /**
     * This method returns score between the these two peaks (the lower score,
     * the better match)
     */
    public double getScore() {
	return score;
    }

    String getErrorMessage() {
	return errorMessage;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RowVsRowScore object) {

	// We must never return 0, because the TreeSet in JoinAlignerTask would
	// treat such elements as equal
	if (score < object.getScore()) {
	    return 1;
	} else {
	    return -1;
	}

    }

}
