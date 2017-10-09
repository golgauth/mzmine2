package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid;

import java.util.Hashtable;
import java.util.List;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RowVsRowScoreGC;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationSingleTask;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.util.RangeUtils;

public class RowVsRowDistanceCatcher {
	
	MZmineProject project;
	boolean useOldestRDFancestor;
	Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping;
	List<PeakListRow> full_rows_list;
	double mzWeight;
	double rtWeight;
	boolean useApex;
	boolean useKnownCompoundsAsRef; 
	boolean useDetectedMzOnly;
	RTTolerance rtToleranceAfter;
	
	double maximumScore;
	
	

	public RowVsRowDistanceCatcher(
			MZmineProject project, boolean useOldestRDFancestor,
			Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping,
			List<PeakListRow> full_rows_list,
			double mzWeight, double rtWeight,
			boolean useApex, boolean useKnownCompoundsAsRef, 
			boolean useDetectedMzOnly, RTTolerance rtToleranceAfter,
			double maximumScore
			) {

		this.project = project;
		this.useOldestRDFancestor = useOldestRDFancestor;
		this.rtAdjustementMapping = rtAdjustementMapping;
		this.full_rows_list = full_rows_list;
		this.mzWeight = mzWeight;
		this.rtWeight = rtWeight;
		this.useApex = useApex;
		this.useKnownCompoundsAsRef = useKnownCompoundsAsRef;
		this.rtToleranceAfter = rtToleranceAfter;
		
		this.maximumScore = maximumScore;
		
	}

	public RowVsRowScoreGC getScore(
			int row_id, int aligned_row_id,
			double mzMaxDiff, double rtMaxDiff
			) {

		PeakListRow peakListRow = full_rows_list.get(row_id);
		PeakListRow alignedRow = full_rows_list.get(aligned_row_id);

		RowVsRowScoreGC score = new RowVsRowScoreGC(
				project, useOldestRDFancestor,
				rtAdjustementMapping,
				peakListRow, alignedRow,
				mzMaxDiff, mzWeight,
				rtMaxDiff, rtWeight,
				0.0d,
				useApex, useKnownCompoundsAsRef, 
				useDetectedMzOnly,
				rtToleranceAfter);

		return score;
	}

//	public double getMaximumScore() {
//		return this.maximumScore;
//	}

	public double getRankedDistance(int row_id, int aligned_row_id, double mzMaxDiff, double rtMaxDiff, double minScore) {
		
		PeakListRow row = full_rows_list.get(row_id);
		PeakListRow k_row = full_rows_list.get(aligned_row_id);
		
		
//		// Same list
//		if ((row_id < 45 && aligned_row_id < 45) 
//				|| (row_id >= 45 && aligned_row_id >= 45 && row_id < 102 && aligned_row_id < 102)
//				|| (row_id >= 102 && aligned_row_id >= 102)) {
////		if (row.getRawDataFiles()[0] == k_row.getRawDataFiles()[0]) {
//			return 1000.0d;
//		} 
//		// Not candidate
//		else if ((Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) >= rtMaxDiff 
//				|| Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ()) >= mzMaxDiff)) {
//			return 100.0d;
//		}
//		
		double score = this.getScore(row_id, aligned_row_id, mzMaxDiff, rtMaxDiff).getScore();
		// Score too low
		if (score <= Math.max(JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE, minScore)) {
			return 10.0d;
		}

		// Score OK
		return this.maximumScore - score;
	}
	
}
