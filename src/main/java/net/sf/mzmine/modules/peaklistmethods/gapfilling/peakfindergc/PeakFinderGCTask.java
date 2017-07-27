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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

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

import com.google.common.base.Strings;
import com.google.common.collect.Range;

class PeakFinderGCTask extends AbstractTask {

	
	private static boolean DEBUG_0 = false;
	
	// For comparing small differences.
	private static final double EPSILON = 0.0000001d;

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
	private boolean useRegression;
	private ParameterSet parameters;
	
	private int processedScans, totalScans;
	private int processedGaps, totalGaps;
	List<Feature> recoveredPeaks;
	
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
				PeakFinderGCParameters.rtCorrection).getValue();
		useRegression = parameters.getParameter(
				PeakFinderGCParameters.useRegression).getValue();
		removeOriginal = parameters.getParameter(
				PeakFinderGCParameters.autoRemove).getValue();
	}

	public void run() {

		
		// Check options compatibility
		if (useRegression && !rtCorrection) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Cannot run gapfilling using \"Regression\", option \"RT correction\" must be checked first!");
            return;
		}
		
		
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



		// 4 cases:
		//             1/ No RT correction
		//             2/ RT correction & No Regression
		//                     2A/ "offset/scale" info available from previous "JoinAlignerGC" task
		//                     2B/ "offset/scale" info not available => Switch to regression
		//             3/ RT correction & Regression


		// Build reference RDFs index
		RawDataFile[] rdf_sorted = peakList.getRawDataFiles().clone();
		Arrays.sort(rdf_sorted, new RawDataFileSorter(SortingDirection.Ascending));



		// Progress: Count overall number of gaps
		totalGaps = 0;
		for (RawDataFile dataFile0 : peakList.getRawDataFiles()) {
			for (int row = 0; row < peakListRows.length; row++) {

				PeakListRow sourceRow = peakListRows[row];
				Feature sourcePeak = sourceRow.getPeak(dataFile0);
				if (sourcePeak == null) {
					totalGaps ++;
				}
			}
		}

		recoveredPeaks = new ArrayList<>(); 
		
		boolean switchCorrectionMode = (useRegression);
		//-
		// Cases "No RT correction" or "RT correction using existing offset/scale"
		if (!switchCorrectionMode) {


			// Process
			// Process all raw data files
			for (RawDataFile dataFile : peakList.getRawDataFiles()) {

				// Canceled?
				if (isCanceled()) {
					return;
				}

				// Interrupt if "RT correction using existing offset/scale" ain't possible
				if (switchCorrectionMode) { break; }


				Vector<GapGC> gaps = new Vector<GapGC>();
				// Fill each row of this raw data file column, create new empty gaps if necessary
				for (int row = 0; row < peakListRows.length/*peakList.getNumberOfRows()*/; row++) {

					// Interrupt if "RT correction using existing offset/scale" ain't possible
					if (switchCorrectionMode) { break; }

					PeakListRow sourceRow = peakListRows[row]; ////**peakList.getRow(row);
					PeakListRow newRow = processedPeakList.getRow(row);

					Feature sourcePeak = sourceRow.getPeak(dataFile);

					if (sourcePeak == null) {

						// Create a new gap

						Range<Double> mzRange = mzTolerance
								.getToleranceRange(sourceRow.getAverageMZ());
						//Range<Double> rtRange = rtTolerance
						//	.getToleranceRange(sourceRow.getAverageRT());

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
						//System.out.println("> Run deconvolution in range: " + rtRange);
						int rdf_idx = Arrays.asList(rdf_sorted).indexOf(dataFile);
						if (rtCorrection) {

							String strOffsets = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_OFFSETS);
							String strScales = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_SCALES);

							double b_offset = 0.0, a_scale = 0.0;
							if (strOffsets != null)
								b_offset = Double.valueOf(strOffsets.split(AlignedRowProps.PROP_SEP, -1)[rdf_idx]);
							if (strScales != null)
								a_scale = Double.valueOf(strScales.split(AlignedRowProps.PROP_SEP, -1)[rdf_idx]);

							// 2A)...
							// Case offsets & scales can be recovered from previous "JoinAlignerGC":
							// use them as-is.
							if (strOffsets != null && strScales != null) {

								//logger.info("RT correction: Found 'offset/scale' info from previous 'Join Aligner GC' task!");
								// Reverse adjust
								double searchWindowCenterRT = (rtRange.lowerEndpoint() + rtRange.upperEndpoint()) / 2.0;
								double searchWindowHalfWidthRT = (rtRange.upperEndpoint() - rtRange.lowerEndpoint()) / 2.0;
								double adjustedSrcRT = JoinAlignerGCTask.getReverseAdjustedRT(searchWindowCenterRT, b_offset, a_scale);

								// Update adjusted RT info
								String strAdjustedRTs = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_RTS);
								String[] arrAdjustedRTs = strAdjustedRTs.split(AlignedRowProps.PROP_SEP, -1);
								arrAdjustedRTs[rdf_idx] = String.valueOf(adjustedSrcRT);//String.valueOf(searchWindowCenterRT);//
								strAdjustedRTs = AlignedRowProps.implode(AlignedRowProps.PROP_SEP, false, arrAdjustedRTs);
								((SimplePeakIdentity) sourceRow.getPreferredPeakIdentity()).setPropertyValue(AlignedRowProps.PROPERTY_RTS, strAdjustedRTs);

								// Adjust search range!
								////**rtRange = rtTolerance.getToleranceRange(adjustedSrcRT);
								rtRange = Range.closed(adjustedSrcRT - searchWindowHalfWidthRT, adjustedSrcRT + searchWindowHalfWidthRT);

								System.out.println(">2 Run deconv in range: " + rtRange + " - recomputed with " + b_offset + "/" + a_scale);

							}
							// 2B)...
							// If offset/scale info couldn't be found, switch RT correction mode to "regression approach"
							else {
								logger.info("RT correction: Couldn't find 'offset/scale' info from previous 'Join Aligner GC' task! \n\t" + 
										"Trying to use the 'Regression approach' instead...");
								switchCorrectionMode = true;
								//break;
							}

							//                        // Otherwise apply RT correction using regular regression (See regular MZmine "PeakFinderTask")
							//                        else {
							//
							//                            // Fill the gaps of a random sample using all the other samples and
							//                            // take it as master list
							//                            // to fill the gaps of the other samples
							//                            //masterSample = (int) Math.floor(Math.random() * peakList.getNumberOfRawDataFiles());
							//                            fillList(MASTERLIST);
							//
							//                            // Process all raw data files
							//                            fillList(!MASTERLIST);
							//
							//                            // TODO: !!!
							//
							//                        }

						} 
						// 1)...
						// Adjust nothing!
						else {

							// Cleanup: Reset adjusted RT info to nothing! 
							// In case last use of "PeakFinderGC" was done with option "rtCorrection=true"
							String strAdjustedRTs = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_RTS);
							if (strAdjustedRTs != null) {
								String[] arrAdjustedRTs = strAdjustedRTs.split(AlignedRowProps.PROP_SEP, -1);
								arrAdjustedRTs[rdf_idx] = "";
								strAdjustedRTs = AlignedRowProps.implode(AlignedRowProps.PROP_SEP, false, arrAdjustedRTs);
								((SimplePeakIdentity) sourceRow.getPreferredPeakIdentity()).setPropertyValue(AlignedRowProps.PROPERTY_RTS, strAdjustedRTs);
							}

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

				// Do fill the gap with proper peak
				for (GapGC gap : gaps) {

					Feature peak = gap.fillTheGap();

					if (peak != null) {
						recoveredPeaks.add(peak);
					}

					processedGaps++;
				}
			}

			// List filled gaps (recovered peaks)
			logger.info(">>> RECOVERED '" + recoveredPeaks.size() + "' PEAKS (over all '" + peakList.getName() + "' peak list):");
			for (final Feature peak : recoveredPeaks) {

				boolean alreadyKnown = checkPeak(peak, peakList);

				String status = (alreadyKnown) ? "DUPLICATE" : "REAL NEW";
				logger.info("\t + [" + status + "] Peak: " + peak);

			}
			
		}

		// 3) and 2B)
		if (switchCorrectionMode) {

			//totalScans *= 2;

			// Fill the gaps of a random sample using all the other samples and
			// take it as master list to fill the gaps of the other samples
			masterSample = (int) Math.floor(Math.random() * peakList.getNumberOfRawDataFiles());
			fillTheGapsInTheLists(MASTERLIST, peakListRows);

			// Process all raw data files
			fillTheGapsInTheLists(!MASTERLIST, peakListRows);
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


	public static boolean checkPeak(Feature peak, PeakList peakList) {

		boolean peak_is_known = false;

		for (PeakListRow row : peakList.getRows()) {

			Feature a_p = row.getPeak(peak.getDataFile());
			if (a_p != null && Math.abs(a_p.getRT() - peak.getRT()) < EPSILON) {
				peak_is_known = true;
				break;
			}

		}

		return peak_is_known;
	}
	//-
	private void fixDuplicatePeak(Feature duplicatePeak, PeakList processedPeakList) {

		for (PeakListRow row : processedPeakList.getRows()) {
			for (Feature a_p : row.getPeaks()) {

				if (Math.abs(a_p.getRT() - duplicatePeak.getRT()) < EPSILON) {

					processedPeakList.removeRow(row);
					PeakListRow newRow = new SimplePeakListRow(row.getID());

					for (Feature a_p2 : row.getPeaks()) {

						// Copy all peaks but the duplicate one
						if (a_p != a_p2) {
							newRow.addPeak(a_p.getDataFile(), a_p);
						}
					}
					return;
				}
			}
		}

	}



	public void fillTheGapsInTheLists(boolean masterList, PeakListRow[] peakListRows) {

		// Build reference RDFs index
		RawDataFile[] rdf_sorted = peakList.getRawDataFiles().clone();
		Arrays.sort(rdf_sorted, new RawDataFileSorter(SortingDirection.Ascending));
		
		if (DEBUG_0)
			System.out.println("Doing master list: '" + masterList + "' (masterSample:" + masterSample + ").");

		// Process all raw data files
		for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
			//for (RawDataFile dataFile : peakList.getRawDataFiles()) {
			if (i != masterSample) {
				
				if (DEBUG_0)
					System.out.println("Master sample: " + masterSample + " (i:" + i + ").");
				
				RawDataFile datafile1;
				RawDataFile datafile2;

				if (masterList) {
					datafile1 = peakList.getRawDataFile(masterSample);
					datafile2 = peakList.getRawDataFile(i);
				} else {
					datafile1 = peakList.getRawDataFile(i);
					datafile2 = peakList.getRawDataFile(masterSample);
				}
				RegressionInfo info = new RegressionInfo();
				
				if (DEBUG_0) {	
					System.out.println("datafile1: " + datafile1);
					System.out.println("datafile2: " + datafile2);
				}
				
				for (PeakListRow row : peakList.getRows()) {
					Feature peaki = row.getPeak(datafile1);
					Feature peake = row.getPeak(datafile2);
					if (peaki != null && peake != null) {
						info.addData(peake.getRT(), peaki.getRT());
					}
				}

				info.setFunction();

				// Canceled?
				if (isCanceled()) {
					return;
				}

				Vector<GapGC> gaps = new Vector<GapGC>();

				// Fill each row of this raw data file column, create new empty gaps if necessary
				for (int row = 0; row < peakListRows.length/*peakList.getNumberOfRows()*/; row++) {
					
					PeakListRow sourceRow = peakListRows[row]; ////**peakList.getRow(row);
					PeakListRow newRow = processedPeakList.getRow(row);

					if (DEBUG_0)
						System.out.println("### row: " + row + ", row_id" + sourceRow.getID() 
									+ ", n_row: " + Arrays.asList(peakList.getRows()).indexOf(sourceRow) + ", n_row_id: " + newRow.getID());
					
					Feature sourcePeak = sourceRow.getPeak(datafile1);

					if (sourcePeak == null) {

						// Create a new gap

						double mz = sourceRow.getAverageMZ();
						double rt2 = -1;
						if (!masterList) {
							if (newRow.getPeak(datafile2) != null) {
								rt2 = newRow.getPeak(datafile2).getRT();
							}
						} else {
							if (sourceRow.getPeak(datafile2) != null) {
								rt2 = sourceRow.getPeak(datafile2).getRT();
							}
						}

						if (rt2 > -1) {

							double rt = info.predict(rt2);

							if (rt != -1) {

								Range<Double> mzRange = mzTolerance
										.getToleranceRange(mz);
								//                                Range<Double> rtRange = rtTolerance
								//                                        .getToleranceRange(rt);


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

								double searchWindowCenterRT = (rtRange.lowerEndpoint() + rtRange.upperEndpoint()) / 2.0;
								double searchWindowHalfWidthRT = (rtRange.upperEndpoint() - rtRange.lowerEndpoint()) / 2.0;
								double adjustedSrcRT = rt;

								// Update adjusted RT info
								int rdf_idx = Arrays.asList(rdf_sorted).indexOf(datafile1);

								String strAdjustedRTs = sourceRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_RTS);
								// If "corrected RTs" weren't inherited from previous "JoinAlignerGCTask", then recreate one
								String[] arrAdjustedRTs;
								if (Strings.isNullOrEmpty(strAdjustedRTs)) {
									arrAdjustedRTs = new String[peakList.getNumberOfRawDataFiles()];
									Arrays.fill(arrAdjustedRTs, "");
								} else {
									arrAdjustedRTs = strAdjustedRTs.split(AlignedRowProps.PROP_SEP, -1);
								}
								arrAdjustedRTs[rdf_idx] = String.valueOf(adjustedSrcRT);//String.valueOf(searchWindowCenterRT);//
								strAdjustedRTs = AlignedRowProps.implode(AlignedRowProps.PROP_SEP, false, arrAdjustedRTs);
								((SimplePeakIdentity) sourceRow.getPreferredPeakIdentity()).setPropertyValue(AlignedRowProps.PROPERTY_RTS, strAdjustedRTs);

								// Adjust search range!
								rtRange = Range.closed(adjustedSrcRT - searchWindowHalfWidthRT, adjustedSrcRT + searchWindowHalfWidthRT);

								// TODO: Compute rtRange for peak length in some smart way: ".getRawDataPointsRTRange()"...
								//        For now, just keep it as is
								Range<Double> rtDurationRange =  parameters.getParameter(PeakFinderGCParameters.PEAK_DURATION).getValue();                        
								parameters.getParameter(PeakFinderGCParameters.PEAK_DURATION).setValue(rtDurationRange);
								GapGC newGap = new GapGC(parameters, peakList, row, newRow, datafile1, mzRange,
										rtRange, intTolerance, minChemSimScore);

								gaps.add(newGap);
								
								if (DEBUG_0) {
									System.out.println(">> rt2=" + rt2 + ", rt(adjustedSrcRT)=" + rt + ", searchWindowHalfWidthRT=" + searchWindowHalfWidthRT 
											+ " => Range[" + (adjustedSrcRT - searchWindowHalfWidthRT) + "-" + (adjustedSrcRT + searchWindowHalfWidthRT) + "].");
									System.out.println("New gap GC (for row: " + newRow + " on range " + rtRange + ", datafile: " + datafile1.getName() + ")."
											+ " => Source row was [" + sourceRow + "].");
								}
							}
						}


					} else {
						newRow.addPeak(datafile1, sourcePeak);
					}

				}

				// Stop processing this file if there are no gaps
				if (gaps.size() == 0) {
					processedScans += datafile1.getNumOfScans();
					continue;
				}

				// Do fill the gap with proper peak
				for (GapGC gap : gaps) {
					
					Feature peak = gap.fillTheGap();

					if (peak != null) {
						recoveredPeaks.add(peak);
					}

					processedGaps++;
				}
			}

			// List filled gaps (recovered peaks)
			logger.info(">>> RECOVERED '" + recoveredPeaks.size() + "' PEAKS (over all '" + peakList.getName() + "' peak list):");
			for (final Feature peak : recoveredPeaks) {

				boolean alreadyKnown = checkPeak(peak, peakList);

				String status = (alreadyKnown) ? "DUPLICATE" : "REAL NEW";
				logger.info("\t + [" + status + "] Peak: " + peak);

			}

		}
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

		//		if (totalScans == 0) {
		//			return 0;
		//		}
		//		return (double) processedScans / (double) totalScans;
		if (totalGaps == 0) {
			return 0;
		}
		return (double) processedGaps / (double) totalGaps;

	}

	public String getTaskDescription() {
		return "Gap filling " + peakList;
	}

	PeakList getPeakList() {
		return peakList;
	}

}
