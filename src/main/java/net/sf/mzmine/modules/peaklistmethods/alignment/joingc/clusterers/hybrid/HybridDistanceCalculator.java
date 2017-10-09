package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid;

import org.gnf.clustering.DataSource;
import org.gnf.clustering.DistanceCalculator;


public class HybridDistanceCalculator implements DistanceCalculator 
{
	
	RowVsRowDistanceCatcher distanceCatcher;
	double mzMaxDiff;
	double rtMaxDiff;
	double minScore;
	
	public HybridDistanceCalculator() {} 

	public void dispose() {}

	
//	public void setDistanceCatcher(RowVsRowDistanceCatcher distanceCatcher) {
//		
//		this.distanceCatcher = distanceCatcher;
//	}
	public void setDistanceCatcher(RowVsRowDistanceCatcher distanceCatcher, double mzMaxDiff, double rtMaxDiff, double minScore) {
		
		this.distanceCatcher = distanceCatcher;
		this.mzMaxDiff = mzMaxDiff;
		this.rtMaxDiff = rtMaxDiff;
		
		this.minScore = minScore;
	}
	
	
	public float calculate(final DataSource sourceOne,	final DataSource sourceTwo, final int nIndexOne, final int nIndexTwo)
	{
		
		float val = Float.MAX_VALUE;
		
		if (this.distanceCatcher != null) {
			val = (float) (this.distanceCatcher.getRankedDistance(nIndexOne, nIndexTwo, mzMaxDiff, rtMaxDiff, 0));
		} else {
			throw new IllegalStateException("Cannot compute distances without a proper 'RowVsRowDistanceCatcher' !" + distanceCatcher);
		}
		
		return val; 
	}

}
