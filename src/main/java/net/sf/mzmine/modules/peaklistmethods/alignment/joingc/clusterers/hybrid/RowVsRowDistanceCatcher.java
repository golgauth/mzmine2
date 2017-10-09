package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid;

import java.util.Hashtable;
import java.util.List;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RowVsRowScoreGC;
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

	public RowVsRowDistanceCatcher(
			MZmineProject project, boolean useOldestRDFancestor,
			Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping,
			List<PeakListRow> full_rows_list,
			double mzWeight, double rtWeight,
			boolean useApex, boolean useKnownCompoundsAsRef, 
			boolean useDetectedMzOnly, RTTolerance rtToleranceAfter
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

}
