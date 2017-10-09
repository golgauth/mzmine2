package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import org.gnf.clustering.DataSource;
import org.gnf.clustering.DistanceCalculator;

import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid.RowVsRowDistanceCatcher;


public class HybridDistanceCalculator implements DistanceCalculator 
{
	
	RowVsRowDistanceCatcher distanceCatcher;
	double mzMaxDiff;
	double rtMaxDiff;
	
	public HybridDistanceCalculator() {} 

	public void dispose() {}

	
//	public void setDistanceCatcher(RowVsRowDistanceCatcher distanceCatcher) {
//		
//		this.distanceCatcher = distanceCatcher;
//	}
	public void setDistanceCatcher(RowVsRowDistanceCatcher distanceCatcher, double mzMaxDiff, double rtMaxDiff) {
		
		this.distanceCatcher = distanceCatcher;
		this.mzMaxDiff = mzMaxDiff;
		this.rtMaxDiff = rtMaxDiff;
	}
	
	
	public float calculate(final DataSource sourceOne,	final DataSource sourceTwo, final int nIndexOne, final int nIndexTwo)
	{
		
		float val = Float.MAX_VALUE;
		
		if (this.distanceCatcher != null)
		{

			RowVsRowScoreGC score = this.distanceCatcher.getScore(nIndexOne, nIndexTwo, mzMaxDiff, rtMaxDiff);
			val = (float) score.getScore();
		}
		
		return val; 
	}

}
