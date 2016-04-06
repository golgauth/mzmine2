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

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.AlignedRowProps;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RawDataFileSorter;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import com.google.common.collect.Range;

class PeakFinderGCTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final MZmineProject project;
    private PeakList peakList, processedPeakList;
    private String suffix;
    private double intTolerance;
    private MZTolerance mzTolerance;
    ////**private RTTolerance rtTolerance;
    private double rtColumnMargin;
    private double minChemSimScore;
    private boolean rtCorrection;
    private ParameterSet parameters;
    private int processedScans, totalScans;
    private boolean MASTERLIST = true, removeOriginal;
    private int masterSample = 0;

    PeakFinderGCTask(MZmineProject project, PeakList peakList,
	    ParameterSet parameters) {

	this.project = project;
	this.peakList = peakList;
	this.parameters = parameters;

	suffix = parameters.getParameter(PeakFinderGCParameters.suffix)
		.getValue();
	intTolerance = parameters.getParameter(
		PeakFinderGCParameters.intTolerance).getValue();
	mzTolerance = parameters.getParameter(PeakFinderGCParameters.MZTolerance)
		.getValue();
//	rtTolerance = parameters.getParameter(PeakFinderGCParameters.RTTolerance)
//		.getValue();
	rtColumnMargin = parameters.getParameter(PeakFinderGCParameters.RTColumnTolerance)
              .getValue();
	minChemSimScore = parameters.getParameter(PeakFinderGCParameters.minSimScore)
	              .getValue();
	rtCorrection = parameters.getParameter(
		PeakFinderGCParameters.RTCorrection).getValue();
	removeOriginal = parameters.getParameter(
		PeakFinderGCParameters.autoRemove).getValue();
    }

    public void run() {

	setStatus(TaskStatus.PROCESSING);
	logger.info("Running gap filler on " + peakList);

	// Calculate total number of scans in all files
	for (RawDataFile dataFile : peakList.getRawDataFiles()) {
	    totalScans += dataFile.getNumOfScans(1);
	}

	// Create new peak list
	processedPeakList = new SimplePeakList(peakList + " " + suffix,
		peakList.getRawDataFiles());

        // Sort rows by ascending RT
        final PeakListRow[] peakListRows = peakList.getRows().clone();
        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.RT,
                SortingDirection.Ascending));

	// Fill new peak list with empty rows
	for (int row = 0; row < peakListRows.length/*peakList.getNumberOfRows()*/; row++) {
	    PeakListRow sourceRow = peakListRows[row]; ////**peakList.getRow(row);
	    PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());
	    newRow.setComment(sourceRow.getComment());
	    for (PeakIdentity ident : sourceRow.getPeakIdentities()) {
		newRow.addPeakIdentity(ident, false);
	    }
	    if (sourceRow.getPreferredPeakIdentity() != null) {
		newRow.setPreferredPeakIdentity(sourceRow
			.getPreferredPeakIdentity());
	    }
	    processedPeakList.addRow(newRow);
	}

//	if (rtCorrection) {
//	    totalScans *= 2;
//	    // Fill the gaps of a random sample using all the other samples and
//	    // take it as master list
//	    // to fill the gaps of the other samples
//	    masterSample = (int) Math.floor(Math.random()
//		    * peakList.getNumberOfRawDataFiles());
//	    fillList(MASTERLIST);
//
//	    // Process all raw data files
//	    fillList(!MASTERLIST);
//
//	} else {
        
        // Build reference RDFs index
        RawDataFile[] rdf_sorted = peakList.getRawDataFiles().clone();
        Arrays.sort(rdf_sorted, new RawDataFileSorter(SortingDirection.Ascending));

	    // Process all raw data files
	    for (RawDataFile dataFile : peakList.getRawDataFiles()) {

		// Canceled?
		if (isCanceled()) {
		    return;
		}
		
		
		
		//if (!dataFile.getName().contains("AC_71")) continue;
		
		

		Vector<GapGC> gaps = new Vector<GapGC>();
	        
		// Fill each row of this raw data file column, create new empty gaps if necessary
		for (int row = 0; row < peakListRows.length/*peakList.getNumberOfRows()*/; row++) {
		    PeakListRow sourceRow = peakListRows[row]; ////**peakList.getRow(row);
		    PeakListRow newRow = processedPeakList.getRow(row);

		    Feature sourcePeak = sourceRow.getPeak(dataFile);

		    if (sourcePeak == null) {

			// Create a new gap

			Range<Double> mzRange = mzTolerance
				.getToleranceRange(sourceRow.getAverageMZ());
//			Range<Double> rtRange = rtTolerance
//				.getToleranceRange(sourceRow.getAverageRT());
			
			// Find column bounds automatically (from adjacent columns bounds)
			double rowLower = Double.MAX_VALUE, rowUpper = Double.MIN_VALUE;
			for (RawDataFile df : peakList.getRawDataFiles()) {
			    Feature p = sourceRow.getPeak(df);
			    if (p != null) {
                                if (p.getRawDataPointsRTRange().lowerEndpoint() < rowLower)
                                    rowLower = p.getRawDataPointsRTRange().lowerEndpoint();
                                if (p.getRawDataPointsRTRange().upperEndpoint() > rowUpper)
                                    rowUpper = p.getRawDataPointsRTRange().upperEndpoint();
			    }
			}
			//-
			double lower = ((row > 0) ? 
			        (sourceRow.getAverageRT() + peakListRows[row-1]/*peakList.getRow(row-1)*/.getAverageRT()) / 2.0 : 
			            rowLower);
			double upper = ((row < peakListRows.length/*peakList.getNumberOfRows()*/-1) ? 
			        (sourceRow.getAverageRT() + peakListRows[row+1]/*peakList.getRow(row+1)*/.getAverageRT()) / 2.0 : 
			            rowUpper);
			Range<Double> rtRange = Range.closed(lower - rtColumnMargin, upper + rtColumnMargin);

			
			
                        // Adjust rtRange search window if requested
		        System.out.println("> Run deconvolution in range: " + rtRange);
		        int rdf_idx = Arrays.asList(rdf_sorted).indexOf(dataFile);
                        if (rtCorrection) {
                            
                            //String[] arrOffsets = null, arrScales = null;
                            String strOffsets = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_OFFSETS);
                            String strScales = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_SCALES);
                            
                            double b_offset = 0.0, a_scale = 0.0;
                            if (strOffsets != null)
                                b_offset = Double.valueOf(strOffsets.split(AlignedRowProps.PROP_SEP, -1)[rdf_idx]);
                            if (strScales != null)
                                a_scale = Double.valueOf(strScales.split(AlignedRowProps.PROP_SEP, -1)[rdf_idx]);
                            
                            // Reverse adjust
                            double searchWindowCenterRT = (rtRange.lowerEndpoint() + rtRange.upperEndpoint()) / 2.0;
                            double searchWindowHalfWidthRT = (rtRange.upperEndpoint() - rtRange.lowerEndpoint()) / 2.0;
                            double adjustedSrcRT = JoinAlignerGCTask.getReverseAdjustedRT(searchWindowCenterRT, b_offset, a_scale);

                            // Update adjusted RT info
                            String strAdjustedRTs = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_RTS);
                            String[] arrAdjustedRTs = strAdjustedRTs.split(AlignedRowProps.PROP_SEP, -1);
                            arrAdjustedRTs[rdf_idx] = String.valueOf(adjustedSrcRT);
                            strAdjustedRTs = AlignedRowProps.implode(AlignedRowProps.PROP_SEP, false, arrAdjustedRTs);
                            ((SimplePeakIdentity) sourceRow.getPreferredPeakIdentity()).setPropertyValue(AlignedRowProps.PROPERTY_RTS, strAdjustedRTs);
                            
                            ////**rtRange = rtTolerance.getToleranceRange(adjustedSrcRT);
                            rtRange = Range.closed(adjustedSrcRT - searchWindowHalfWidthRT, adjustedSrcRT + searchWindowHalfWidthRT);
                            
                            System.out.println(">2 Run deconv in range: " + rtRange + " - recomputed with " + b_offset + "/" + a_scale);
                        
                        } else {
                            
                            // Reset adjusted RT info to nothing!
                            String strAdjustedRTs = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_RTS);
                            String[] arrAdjustedRTs = strAdjustedRTs.split(AlignedRowProps.PROP_SEP, -1);
                            arrAdjustedRTs[rdf_idx] = "";
                            strAdjustedRTs = AlignedRowProps.implode(AlignedRowProps.PROP_SEP, false, arrAdjustedRTs);
                            ((SimplePeakIdentity) sourceRow.getPreferredPeakIdentity()).setPropertyValue(AlignedRowProps.PROPERTY_RTS, strAdjustedRTs);
                            
                        }
                        
                        
                        
                        
                        // TODO: Compute rtRange for peak length in some smart way: ".getRawDataPointsRTRange()"...
                        //        For now, just keep it as is
                        Range<Double> rtDurationRange =  parameters.getParameter(PeakFinderGCParameters.PEAK_DURATION).getValue();                        
                        parameters.getParameter(PeakFinderGCParameters.PEAK_DURATION).setValue(rtDurationRange);
			GapGC newGap = new GapGC(parameters, peakList, row, newRow, dataFile, mzRange,
				rtRange, intTolerance, minChemSimScore);

			gaps.add(newGap);

		    } else {
			newRow.addPeak(dataFile, sourcePeak);
		    }

		}

		// Stop processing this file if there are no gaps
		if (gaps.size() == 0) {
		    processedScans += dataFile.getNumOfScans();
		    continue;
		}

		// Get all scans of this data file
		int scanNumbers[] = dataFile.getScanNumbers(1);

		System.out.println(">>>>>>>>>>>>>>>>>>>>> RDF : " + dataFile.getName());
		
//		// Process each scan
//		for (int scanNumber : scanNumbers) {
//
//		    // Canceled?
//		    if (isCanceled()) {
//			return;
//		    }
//
//		    // Get the scan
//		    Scan scan = dataFile.getScan(scanNumber);
//
//		    // Feed this scan to all gaps
////		    for (GapGC gap : gaps) {
////			gap.offerNextScan(scan);
////		    }
//		    GapGC gap = gaps.get(0);
//		    gap.offerNextScan(scan);
//
//		    processedScans++;
//		}
//		
//		
//
//		// Finalize gaps
////		for (GapGC gap : gaps) {
////		    gap.noMoreOffers();
////		}
//                GapGC gap = gaps.get(0);
//                gap.noMoreOffers();
		
		for (GapGC gap : gaps) {
		    gap.fillTheGap();
		}

	    }
//	}

	// Append processed peak list to the project
	project.addPeakList(processedPeakList);

        // Add quality parameters to peaks
	QualityParameters.calculateQualityParameters(processedPeakList);

	// Add task description to peakList
	processedPeakList
		.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
			"Gap filling ", parameters));

	// Remove the original peaklist if requested
	if (removeOriginal)
	    project.removePeakList(peakList);

	logger.info("Finished gap-filling on " + peakList);
	setStatus(TaskStatus.FINISHED);

    }

//    public void fillList(boolean masterList) {
//	for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
//	    if (i != masterSample) {
//
//		RawDataFile datafile1;
//		RawDataFile datafile2;
//
//		if (masterList) {
//		    datafile1 = peakList.getRawDataFile(masterSample);
//		    datafile2 = peakList.getRawDataFile(i);
//		} else {
//		    datafile1 = peakList.getRawDataFile(i);
//		    datafile2 = peakList.getRawDataFile(masterSample);
//		}
//		RegressionInfo info = new RegressionInfo();
//
//		for (PeakListRow row : peakList.getRows()) {
//		    Feature peaki = row.getPeak(datafile1);
//		    Feature peake = row.getPeak(datafile2);
//		    if (peaki != null && peake != null) {
//			info.addData(peake.getRT(), peaki.getRT());
//		    }
//		}
//
//		info.setFunction();
//
//		// Canceled?
//		if (isCanceled()) {
//		    return;
//		}
//
//		Vector<GapGC> gaps = new Vector<GapGC>();
//
//		// Fill each row of this raw data file column, create new empty
//		// gaps
//		// if necessary
//		for (int row = 0; row < peakList.getNumberOfRows(); row++) {
//		    PeakListRow sourceRow = peakList.getRow(row);
//		    PeakListRow newRow = processedPeakList.getRow(row);
//
//		    Feature sourcePeak = sourceRow.getPeak(datafile1);
//
//		    if (sourcePeak == null) {
//
//			// Create a new gap
//
//			double mz = sourceRow.getAverageMZ();
//			double rt2 = -1;
//			if (!masterList) {
//			    if (processedPeakList.getRow(row)
//				    .getPeak(datafile2) != null) {
//				rt2 = processedPeakList.getRow(row)
//					.getPeak(datafile2).getRT();
//			    }
//			} else {
//			    if (peakList.getRow(row).getPeak(datafile2) != null) {
//				rt2 = peakList.getRow(row).getPeak(datafile2)
//					.getRT();
//			    }
//			}
//
//			if (rt2 > -1) {
//
//			    double rt = info.predict(rt2);
//
//			    if (rt != -1) {
//
//				Range<Double> mzRange = mzTolerance
//					.getToleranceRange(mz);
//				Range<Double> rtRange = rtTolerance
//					.getToleranceRange(rt);
//
//				GapGC newGap = new GapGC(parameters, peakList, row, newRow, datafile1,
//					mzRange, rtRange, intTolerance);
//
//				gaps.add(newGap);
//			    }
//			}
//
//		    } else {
//			newRow.addPeak(datafile1, sourcePeak);
//		    }
//
//		}
//
//		// Stop processing this file if there are no gaps
//		if (gaps.size() == 0) {
//		    processedScans += datafile1.getNumOfScans();
//		    continue;
//		}
//
//		// Get all scans of this data file
//		int scanNumbers[] = datafile1.getScanNumbers(1);
//
//		// Process each scan
//		for (int scanNumber : scanNumbers) {
//
//		    // Canceled?
//		    if (isCanceled()) {
//			return;
//		    }
//
//		    // Get the scan
//		    Scan scan = datafile1.getScan(scanNumber);
//
//		    // Feed this scan to all gaps
//		    for (GapGC gap : gaps) {
//			gap.offerNextScan(scan);
//		    }
//		    processedScans++;
//		}
//
//		// Finalize gaps
//		for (GapGC gap : gaps) {
//		    gap.noMoreOffers();
//		}
//	    }
//	}
//    }

    public double getFinishedPercentage() {
	if (totalScans == 0) {
	    return 0;
	}
	return (double) processedScans / (double) totalScans;

    }

    public String getTaskDescription() {
	return "Gap filling " + peakList;
    }

    PeakList getPeakList() {
	return peakList;
    }

}
