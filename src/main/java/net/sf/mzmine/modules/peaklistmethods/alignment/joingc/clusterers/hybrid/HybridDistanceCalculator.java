package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid;

import org.gnf.clustering.DataSource;
import org.gnf.clustering.DistanceCalculator;


public class HybridDistanceCalculator implements DistanceCalculator 
{
	
	RowVsRowDistanceProvider distanceProvider;
	double mzMaxDiff;
	double rtMaxDiff;
	double minScore;
	
	public HybridDistanceCalculator() {} 

	public void dispose() {}

	
	public void setDistanceProvider(RowVsRowDistanceProvider distanceProvider, double mzMaxDiff, double rtMaxDiff, double minScore) {
		
		this.distanceProvider = distanceProvider;
		this.mzMaxDiff = mzMaxDiff;
		this.rtMaxDiff = rtMaxDiff;
		
		this.minScore = minScore;
	}
	
	
	public float calculate(final DataSource sourceOne,	final DataSource sourceTwo, final int nIndexOne, final int nIndexTwo)
	{
		
		float val = Float.MAX_VALUE;
		
		if (this.distanceProvider != null) {
			//val = (float) (this.distanceProvider.getRankedDistance(nIndexOne, nIndexTwo, mzMaxDiff, rtMaxDiff, minScore));
			val = (float) (this.distanceProvider.getSimpleDistance(nIndexOne, nIndexTwo, mzMaxDiff, rtMaxDiff, minScore));
		} else {
			throw new IllegalStateException("Cannot compute distances without a proper 'RowVsRowDistanceProvider' !" + distanceProvider);
		}
		
		return val; 
	}

}
