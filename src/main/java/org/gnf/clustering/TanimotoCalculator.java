package org.gnf.clustering;

/**
 * The <code>TanimotoCalculator</code> class provides implementation for a distance metrics calculator
 * based on the Tanimoto algorithm.
 * @author Dmitri Petrov
 * @version 1.0
 */
public class TanimotoCalculator implements DistanceCalculator
{
	public void dispose() {}
	public float calculate(final DataSource data1, final DataSource data2,	int nIndex1, int nIndex2)
	{
		if(data1 instanceof BitSource && data2 instanceof BitSource)
			return TanimotoCalculator.tanimotoSource((BitSource)data1, (BitSource)data2, nIndex1, nIndex2);
		
		return TanimotoCalculator.tanimoto(data1, data2, nIndex1, nIndex2);
	}
		
	
	static	float tanimotoSource(final BitSource data1, final BitSource data2, int nIndex1, int nIndex2) 
	{
		int Nab = 0;
		int Na = 0;
		int Nb = 0;
 	
		boolean b1 = false;
		boolean b2 = false;
				
		final int nFPLength = data1.getColCount();
		
		//System.out.println(nFPLength);
				
		for(int nBit = 0; nBit < nFPLength; ++nBit)
		{     
			b1 = data1.getBoolValue(nIndex1, nBit);
			b2 = data2.getBoolValue(nIndex2, nBit);
			//	System.out.println(nBit + " " + b1 + " " + b2);	
			if(b1) Na++;
			if(b2) Nb++;
			if(b1 && b2) Nab++;
		}
		
		if (Na == 0 && Nb == 0)
			return 0.0f;
		
		final int nSum = Na+Nb-Nab;
		if(nSum == 0)
			return 1;
		//System.out.println(Na+ " " + Nb + " " + Nab + " " + nSum);
		final float ddd = 1 - (nSum == 0 ? 0 : ((float) Nab)/((float)nSum));
        
		return ddd;
	}
	
	static	float tanimoto(final DataSource data1, final DataSource data2, int nIndex1, int nIndex2) 
	{
		int Nab = 0;
		int Na = 0;
		int Nb = 0;
 	
		float f1 = Float.NaN;
		float f2 = Float.NaN;
		
		boolean b1 = false;
		boolean b2 = false;
				
		final int nFPLength = data1.getColCount();
					
		for(int nBit = 0; nBit < nFPLength; ++nBit)
		{
			f1 = data1.getValue(nIndex1, nBit);
			f2 = data2.getValue(nIndex2, nBit);
					
			if(f1 != 1.0f && f1 != 0.0f)
				throw new IllegalArgumentException("Value must be either 0.0 or 1.0, but it is " + f1);
					
			if(f2 != 1.0f && f2 != 0.0f)
				throw new IllegalArgumentException("Value must be either 0.0 or 1.0, but it is " + f2);
					
			b1 = f1 == 1.0f;// data1.getBoolValue(nIndex1, nBit);
			b2 = f2 == 1.0f;//data2.getBoolValue(nIndex2, nBit);
							
			if(b1) Na++;
			if(b2) Nb++;
			if(b1 && b2) Nab++;
		}
		
		if (Na == 0 && Nb == 0)
			return 0.0f;
		
		final int nSum = Na+Nb-Nab;
		if(nSum == 0)
			return 1;
		final float ddd = 1 - (nSum == 0 ? 0 : ((float) Nab)/((float)nSum));
        
		return ddd;
	}
	
}
