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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.camera;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.R.RLocationDetection;
import net.sf.mzmine.util.R.RSessionWrapper;
import net.sf.mzmine.util.R.RSessionWrapperException;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCode;
import com.github.rcaller.util.Globals;
import com.google.common.collect.Range;

/**
 * A task to perform a CAMERA search.
 *
 */
public class CameraSearchTask extends AbstractTask {

	// Logger.
	private static final Logger LOG = Logger.getLogger(CameraSearchTask.class
			.getName());

	// Required version of CAMERA.
	private static final String CAMERA_VERSION = "1.32.0";

	// Minutes to seconds conversion factor.
	private static final double SECONDS_PER_MINUTE = 60.0;

	// The MS-level processed by this module.
	private static final int MS_LEVEL = 1;

	// Isotope regular expression.
	private static final Pattern ISOTOPE_PATTERN = Pattern
			.compile("\\[\\d+\\](.*)");

	// Peak signal to noise ratio.
	private static final double SIGNAL_TO_NOISE = 10.0;

	// Data point sorter.
	private static final DataPointSorter ASCENDING_MASS_SORTER = new DataPointSorter(
			SortingProperty.MZ, SortingDirection.Ascending);

	// Peak list to process.
	private final PeakList peakList;

	// Task progress.
	private double progress;

	// R session.
	private RSessionWrapper rSession;
	private String errorMsg;
	private boolean userCanceled;

	// Parameters.
	private final Double fwhmSigma;
	private final Double fwhmPercentage;
	private final Integer isoMaxCharge;
	private final Integer isoMaxCount;
	private final MZTolerance isoMassTolerance;
	private final Double corrThreshold;
	private final Double corrPValue;

	public CameraSearchTask(final ParameterSet parameters, final PeakList list) {

		// Initialize.
		peakList = list;
		progress = 0.0;

		// Parameters.
		fwhmSigma = parameters.getParameter(CameraSearchParameters.FWHM_SIGMA)
				.getValue();
		fwhmPercentage = parameters.getParameter(
				CameraSearchParameters.FWHM_PERCENTAGE).getValue();
		isoMaxCharge = parameters.getParameter(
				CameraSearchParameters.ISOTOPES_MAX_CHARGE).getValue();
		isoMaxCount = parameters.getParameter(
				CameraSearchParameters.ISOTOPES_MAXIMUM).getValue();
		isoMassTolerance = parameters.getParameter(
				CameraSearchParameters.ISOTOPES_MZ_TOLERANCE).getValue();
		corrThreshold = parameters.getParameter(
				CameraSearchParameters.CORRELATION_THRESHOLD).getValue();
		corrPValue = parameters.getParameter(
				CameraSearchParameters.CORRELATION_P_VALUE).getValue();
		this.userCanceled = false;
	}

	@Override
	public String getTaskDescription() {

		return "Identification of pseudo-spectra in " + peakList;
	}

	@Override
	public double getFinishedPercentage() {

		return progress;
	}

	@Override
	public void run() {

		try {

			setStatus(TaskStatus.PROCESSING);

			// Check number of raw data files.
			if (peakList.getNumberOfRawDataFiles() != 1) {

				throw new IllegalStateException(
						"CAMERA can only process peak lists for a single raw data file, i.e. non-aligned peak lists.");
			}

			// Run the search.
			cameraSearch(peakList.getRawDataFile(0));

			if (!isCanceled()) {

				// Finished.
				setStatus(TaskStatus.FINISHED);
				LOG.info("CAMERA Search completed");
			}

	                // Repaint the window to reflect the change in the peak list
	                Desktop desktop = MZmineCore.getDesktop();
	                if (!(desktop instanceof HeadLessDesktop))
	                    desktop.getMainWindow().repaint();

		} catch (Throwable t) {

			LOG.log(Level.SEVERE, "CAMERA Search error", t);
			setErrorMessage(t.getMessage());
			setStatus(TaskStatus.ERROR);
		}
	}

	/**
	 * Perform CAMERA search.
	 *
	 * @param rawFile
	 *            raw data file of peak list to process.
	 */
	private void cameraSearch(final RawDataFile rawFile) {

		LOG.finest("Detecting peaks.");

		errorMsg = null;
		try {

//			String[] reqPackages = { "CAMERA" };
//			String[] reqPackagesVersions = { CAMERA_VERSION };
//			this.rSession = new RSessionWrapper("Camera search feature", reqPackages, reqPackagesVersions);
//			this.rSession.open();	
//
//
//			// Create empty peaks matrix.
//			this.rSession.eval("columnHeadings <- c('mz','mzmin','mzmax','rt','rtmin','rtmax','into','intb','maxo','sn')");
//			this.rSession.eval("peaks <- matrix(nrow=0, ncol=length(columnHeadings))");
//			this.rSession.eval("colnames(peaks) <- columnHeadings");

			//---------------------------------
			
//			Globals.R_Linux = "/media/golgauth/DATA-MINT/R-3.4.0/bin/R";
//			Globals.RScript_Linux = "/media/golgauth/DATA-MINT/R-3.4.0/bin/Rscript";
			Globals.R_Linux = RLocationDetection.getRExecutablePath();
			Globals.RScript_Linux = RLocationDetection.getRScriptExecutablePath();
			RCaller caller = RCaller.create();
			RCode code = RCode.create();
			String loadCode = "library(" + "CAMERA" + ")"; //+ ", logical.return = TRUE)";
			code.addRCode(loadCode);
			
//			double[] arr = new double[] { 1.0, 2.0, 3.0 };
//			code.addDoubleArray("myarr", arr);
//			code.addRCode("avg <- mean(myarr)");
//			caller.setRCode(code);
//			caller.runAndReturnResult("avg");
//			double[] result = caller.getParser().getAsDoubleArray("avg");
//			System.out.println(result[0]);

			
			code.addRCode("columnHeadings <- c('mz','mzmin','mzmax','rt','rtmin','rtmax','into','intb','maxo','sn')");
			code.addRCode("peaks <- matrix(nrow=0, ncol=length(columnHeadings))");
			code.addRCode("colnames(peaks) <- columnHeadings");
			
			
			//---------------------------------
			
			
			// Initialize.
			final Feature[] peaks = peakList.getPeaks(rawFile);
			progress = 0.0;

			// Initialize scan map.
			final Map<Scan, Set<DataPoint>> peakDataPointsByScan = new HashMap<Scan, Set<DataPoint>>(
					rawFile.getNumOfScans(MS_LEVEL));
			int dataPointCount = 0;
			for (final int scanNumber : rawFile.getScanNumbers(MS_LEVEL)) {

				// Create a set to hold data points (sorted by m/z).
				final Set<DataPoint> dataPoints = new TreeSet<DataPoint>(
						ASCENDING_MASS_SORTER);

				// Add a dummy data point.
				dataPoints.add(new SimpleDataPoint(0.0, 0.0));
				dataPointCount++;

				// Map the set.
				peakDataPointsByScan.put(rawFile.getScan(scanNumber),
						dataPoints);
			}

			// Add peaks.
			// 80 percents for building peaks list.
			double progressInc = 0.8 / (double) peaks.length;
			for (final Feature peak : peaks) {

				// Get peak data.
				Range<Double> rtRange = null;
				Range<Double> intRange = null;
				final double mz = peak.getMZ();

				// Get the peak's data points per scan.
				for (final int scanNumber : peak.getScanNumbers()) {

					final Scan scan = rawFile.getScan(scanNumber);
					if (scan.getMSLevel() != MS_LEVEL) {

						throw new IllegalStateException(
								"CAMERA can only process peak lists from MS-level "
										+ MS_LEVEL);
					}

					// Copy the data point.
					final DataPoint dataPoint = peak.getDataPoint(scanNumber);
					if (dataPoint != null) {

						final double intensity = dataPoint.getIntensity();
						peakDataPointsByScan.get(scan).add(
								new SimpleDataPoint(mz, intensity));
						dataPointCount++;

						// Update RT & intensity range.
						final double rt = scan.getRetentionTime();
						if (rtRange == null) {
							rtRange = Range.singleton(rt);
							intRange = Range.singleton(intensity);
						} else {
							rtRange = rtRange.span(Range.singleton(rt));
							intRange = intRange
									.span(Range.singleton(intensity));
						}

					}
				}

				// Set peak values.
				final double area = peak.getArea();
				final double maxo = intRange == null ? peak.getHeight()
						: intRange.upperEndpoint();
				final double rtMin = (rtRange == null ? peak
						.getRawDataPointsRTRange() : rtRange).lowerEndpoint();
				final double rtMax = (rtRange == null ? peak
						.getRawDataPointsRTRange() : rtRange).upperEndpoint();

				// Add peak row.
//				this.rSession.eval("peaks <- rbind(peaks, c(" + mz + ", " // mz
//						+ mz + ", " // mzmin: use the same as mz.
//						+ mz + ", " // mzmax: use the same as mz.
//						+ peak.getRT() + ", " // rt
//						+ rtMin + ", " // rtmin
//						+ rtMax + ", " // rtmax
//						+ area + ", " // into: peak area.
//						+ area + ", " // intb: doesn't affect result, use area.
//						+ maxo + ", " // maxo
//						+ SIGNAL_TO_NOISE + "))", 
//						false);

				String code0 = "peaks <- rbind(peaks, c(" + mz + ", " // mz
						+ mz + ", " // mzmin: use the same as mz.
						+ mz + ", " // mzmax: use the same as mz.
						+ peak.getRT() + ", " // rt
						+ rtMin + ", " // rtmin
						+ rtMax + ", " // rtmax
						+ area + ", " // into: peak area.
						+ area + ", " // intb: doesn't affect result, use area.
						+ maxo + ", " // maxo
						+ SIGNAL_TO_NOISE + "))";
				
				code.addRCode(code0);

				
				progress += progressInc;
			}

			// 20 percents (5*4) for building pseudo-isotopes groups.
			progressInc = 0.05;

			// Create R vectors.
			final int scanCount = peakDataPointsByScan.size();
			final double[] scanTimes = new double[scanCount];
			final int[] scanIndices = new int[scanCount];
			final double[] masses = new double[dataPointCount];
			final double[] intensities = new double[dataPointCount];

			// Fill vectors.
			int scanIndex = 0;
			int pointIndex = 0;
			for (final int scanNumber : rawFile.getScanNumbers(MS_LEVEL)) {

				final Scan scan = rawFile.getScan(scanNumber);
				scanTimes[scanIndex] = scan.getRetentionTime();
				scanIndices[scanIndex] = pointIndex + 1;
				scanIndex++;

				for (final DataPoint dataPoint : peakDataPointsByScan.get(scan)) {

					masses[pointIndex] = dataPoint.getMZ();
					intensities[pointIndex] = dataPoint.getIntensity();
					pointIndex++;
				}
			}

			// Set vectors.
//			this.rSession.assign("scantime", scanTimes);
//			this.rSession.assign("scanindex", scanIndices);
//			this.rSession.assign("mass", masses);
//			this.rSession.assign("intensity", intensities);
			
			code.addDoubleArray("scantime", scanTimes);
			code.addIntArray("scanindex", scanIndices);
			code.addDoubleArray("mass", masses);
			code.addDoubleArray("intensity", intensities);

			
			// Construct xcmsRaw object
//			this.rSession.eval("xRaw <- new(\"xcmsRaw\")");
//			this.rSession.eval("xRaw@tic <- intensity");
//			this.rSession.eval("xRaw@scantime <- scantime * " + SECONDS_PER_MINUTE);
//			this.rSession.eval("xRaw@scanindex <- scanindex");
//			this.rSession.eval("xRaw@env$mz <- mass");
//			this.rSession.eval("xRaw@env$intensity <- intensity");
			
			code.addRCode("xRaw <- new(\"xcmsRaw\")");
			code.addRCode("xRaw@tic <- intensity");
			code.addRCode("xRaw@scantime <- scantime * " + SECONDS_PER_MINUTE);
			code.addRCode("xRaw@scanindex <- as.integer(scanindex)");
			code.addRCode("xRaw@env$mz <- mass");
			code.addRCode("xRaw@env$intensity <- intensity");
			

			// Create the xcmsSet object.
//			this.rSession.eval("xs <- new(\"xcmsSet\")");

			code.addRCode("xs <- new(\"xcmsSet\")");

			// Set peaks.
//			this.rSession.eval("xs@peaks <- peaks");
			
			code.addRCode("xs@peaks <- peaks");

			// Set file (dummy) file path.
//			this.rSession.eval("xs@filepaths  <- ''");

			code.addRCode("xs@filepaths  <- ''");

			// Set sample name.
//			this.rSession.assign("sampleName", peakList.getName());
//			this.rSession.eval("sampnames(xs) <- sampleName");

			code.addString("sampleName", peakList.getName());
			code.addRCode("sampnames(xs) <- sampleName");
			
			
			// Create an empty xsAnnotate.
//			this.rSession.eval("an <- xsAnnotate(xs, sample=1)");
			
			code.addRCode("an <- xsAnnotate(xs, sample=1)");
			

			// Group by RT.
//			this.rSession.eval("an <- groupFWHM(an, sigma=" + fwhmSigma
//					+ ", perfwhm=" + fwhmPercentage + ')');
			
			code.addRCode("an <- groupFWHM(an, sigma=" + fwhmSigma
					+ ", perfwhm=" + fwhmPercentage + ')');

			progress += progressInc;

			// Identify isotopes.
//			this.rSession.eval(
//					"an <- findIsotopes(an, maxcharge=" + isoMaxCharge
//					+ ", maxiso=" + isoMaxCount + ", ppm="
//					+ isoMassTolerance.getPpmTolerance() + ", mzabs="
//					+ isoMassTolerance.getMzTolerance() + ')');
			
			code.addRCode(
					"an <- findIsotopes(an, maxcharge=" + isoMaxCharge
					+ ", maxiso=" + isoMaxCount + ", ppm="
					+ isoMassTolerance.getPpmTolerance() + ", mzabs="
					+ isoMassTolerance.getMzTolerance() + ')');
			
			progress += progressInc;

			// Split groups by correlating peak shape (need to set xraw to raw
			// data).
//			this.rSession.eval(
//					"an <- groupCorr(an, calcIso=TRUE, xraw=xRaw, cor_eic_th="
//							+ corrThreshold + ", pval=" + corrPValue + ')');

			code.addRCode(
					"an <- groupCorr(an, calcIso=TRUE, xraw=xRaw, cor_eic_th="
							+ corrThreshold + ", pval=" + corrPValue + ')');
			
			progress += progressInc;

			// Get the peak list.
//			this.rSession.eval("peakList <- getPeaklist(an)");

			code.addRCode("peakList <- getPeaklist(an)");

			// Extract the pseudo-spectra and isotope annotations from the peak
			// list.
//			rSession.eval("pcgroup <- as.integer(peakList$pcgroup)");
//			rSession.eval("isotopes <- peakList$isotopes");

			code.addRCode("pcgroup <- as.integer(peakList$pcgroup)");
			code.addRCode("isotopes <- peakList$isotopes");
			
			
//			final int[] spectra = (int[]) rSession.collect("pcgroup");
//			final String[] isotopes = (String[]) rSession.collect("isotopes");

			
			code.addRCode("result <- list(pcgroup=pcgroup, isotopes=isotopes)");
			caller.setRCode ( code ) ;
			caller.runAndReturnResult ( "result" ) ;
			
			final int[] spectra = caller.getParser().getAsIntArray("pcgroup");
			//caller. ( "pcgroup" ) ;isotopes
			final String[] isotopes = caller.getParser().getAsStringArray("isotopes");

			
			
			// Add identities.
			if (spectra != null) {

				addPseudoSpectraIdentities(peaks, spectra, isotopes);
			}
			progress += progressInc;
			// Turn off R instance, once task ended gracefully.
//			if (!this.userCanceled) this.rSession.close(false);

		} 
//		catch (RSessionWrapperException e) {
//			if (!this.userCanceled) {
//				errorMsg = "'R computing error' during CAMERA search. \n" + e.getMessage();
//				e.printStackTrace();
//			}
//		}
		catch (Exception e) {
			if (!this.userCanceled) {
				errorMsg = "'Unknown error' during CAMERA search. \n" + e.getMessage();
				e.printStackTrace();
			}
		}

		// Turn off R instance, once task ended UNgracefully.
//		try {
//			if (!this.userCanceled) this.rSession.close(this.userCanceled);
//		}
//		catch (RSessionWrapperException e) {
//			if (!this.userCanceled) {
//				// Do not override potential previous error message.
//				if (errorMsg == null) {
//					errorMsg = e.getMessage();
//				}
//			} else {
//				// User canceled: Silent.
//			}
//		}


		// Report error.
		if (errorMsg != null) {
			setErrorMessage(errorMsg);
			setStatus(TaskStatus.ERROR);				
		}
	}

	/**
	 * Add pseudo-spectra identities.
	 *
	 * @param peaks
	 *            peaks to annotate with identities.
	 * @param spectraExp
	 *            the pseudo-spectra ids vector.
	 * @param isotopeExp
	 *            the isotopes vector.
	 */
	private void addPseudoSpectraIdentities(final Feature[] peaks,
			final int[] spectra, final String[] isotopes) {

		// Add identities for each peak.
		int peakIndex = 0;
		for (final Feature peak : peaks) {

			// Create pseudo-spectrum identity
			final SimplePeakIdentity identity = new SimplePeakIdentity(
					"Pseudo-spectrum #"
							+ String.format("%03d", spectra[peakIndex]));
			identity.setPropertyValue(PeakIdentity.PROPERTY_METHOD,
					"Bioconductor CAMERA");

			// Add isotope info, if any.
			if (isotopes != null) {

				final String isotope = isotopes[peakIndex].trim();
				if (isotope.length() > 0) {

					// Parse the isotope pattern.
					final Matcher matcher = ISOTOPE_PATTERN.matcher(isotope);
					if (matcher.matches()) {

						identity.setPropertyValue("Isotope", matcher.group(1));

					} else {

						LOG.warning("Irregular isotope value: " + isotope);
					}
				}
			}

			// Add identity to peak's row.
			peakList.getPeakRow(peak).addPeakIdentity(identity, true);
			peakIndex++;
		}
	}


	@Override
	public void cancel() {

		this.userCanceled = true;

		super.cancel();

		// Turn off R instance, if already existing.
		try {
			if (this.rSession != null) this.rSession.close(true);
		}
		catch (RSessionWrapperException e) {
			// Silent, always...
		}
	}
}
