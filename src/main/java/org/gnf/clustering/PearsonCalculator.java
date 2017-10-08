package org.gnf.clustering;


/**
 * The <code>PearsonCalculator</code> class provides implementation for a distance metrics calculator
 * based on the Pearson distance.
 * @author Dmitri Petrov
 * @version 1.0
 */
public class PearsonCalculator implements DistanceCalculator 
{
	public PearsonCalculator(final boolean bAbs) 
	{
		m_bAbs = bAbs;
	} 
	
	
	/**
	 * Returns true if absolute Pearson calculator is used.
	 * @return true if absolute Pearson calculator is used, false otherwise.
	 */
	
	public boolean isAbsolutePearson()
	{
		return m_bAbs;
	}
		
	
	public void dispose() {}

	
	public float calculate(final DataSource sourceOne,	final DataSource sourceTwo, final int nIndexOne, final int nIndexTwo)
	{
		if(sourceOne instanceof ByteSource && sourceTwo instanceof ByteSource)
		{
			final byte [] arData1 = ((ByteSource)sourceOne).getByteArray();
			final byte [] arData2 = ((ByteSource)sourceTwo).getByteArray();
			final int nColCount = sourceOne.getColCount(); 
			return PearsonCalculator.pearson(m_bAbs, nColCount, arData1, arData2, nIndexOne, nIndexTwo);
		}
				
		final int nColCount = sourceOne.getColCount();

		int N = 0;
		double fSumX = 0.0;
		double fSumY = 0.0;
		double fSumXY = 0.0;
		double fSumXX = 0.0;
		double fSumYY = 0.0;
			 
		double fX = 0.0;
		double fY = 0.0;		
		for(int nCol = 0; nCol < nColCount; nCol++)
		{
			
			fX = sourceOne.getValue(nIndexOne, nCol);
			fY = sourceTwo.getValue(nIndexTwo, nCol);

			if(Double.isNaN(fX) || Double.isNaN(fY))
				continue;
					
			fSumX += fX;
			fSumY += fY;
			fSumXX += (fX*fX);
			fSumYY += (fY*fY);
			fSumXY += (fX*fY);
			++N;
		}


		double fCorr =((N*fSumXY - fSumX*fSumY)/Math.sqrt((N*fSumXX - fSumX*fSumX)*(N*fSumYY - fSumY*fSumY)));
		if (m_bAbs) fCorr = Math.abs(fCorr);
		double fR = 1.0f - fCorr;
		if(Double.isNaN(fR))
			throw new RuntimeException("NaN");
		
		return (float)fR;
	}
	
	private static	float pearson(boolean isAbsolute, final int nColCount, final byte[] arData1, final byte[] arData2,	int nIndex1, int nIndex2)
	{
		final int nPart1 = nColCount*nIndex1;
		final int nPart2 = nColCount*nIndex2;

		int nIdx1 = 0;
		int nIdx2 = 0;
		int N = 0;
		double nSumX = 0L;
		double nSumY = 0L;
		double nSumXY = 0L;
		double nSumXX = 0L;
		double nSumYY = 0L;
		
		byte fX = 0;
		byte fY = 0;


		for(int nCol = 0; nCol < nColCount; nCol++)
		{
			nIdx1 = nPart1 + nCol;
			nIdx2 = nPart2 + nCol;

			fX = arData1[nIdx1];
			if(Byte.MAX_VALUE == fX)
			  	continue;
						  
			fY =  arData1[nIdx2];
			if(Byte.MAX_VALUE == fY)
			  	continue;					  	  
			  
			  nSumX += fX;
			  nSumY += fY;
			  nSumXX += (fX*fX);
			  nSumYY += (fY*fY);
			  nSumXY += (fX*fY);

			  ++N;
		}

		nSumXY = nSumXY*N - nSumX * nSumY;
		nSumXX = nSumXX*N - nSumX * nSumX;
		nSumYY = nSumYY*N - nSumY * nSumY;
  
		if(nSumYY == 0 && nSumXY == 0 && nSumXX == 0)
			return 0.0f;
  	  
		if(nSumXX != 0 && nSumXY == 0 && nSumYY == 0)
			return 1.0f - (1.0f/(float)Math.sqrt(nSumXX));
  	  
		if(nSumYY != 0 && nSumXY == 0 && nSumXX == 0)
			return 1.0f - (1.0f/(float)Math.sqrt(nSumYY));
  
		if(nSumXY != 0 && (nSumXX == 0 || nSumYY == 0))
			throw new IllegalStateException();
  
  
		float fCorr = (float)(nSumXY / Math.sqrt(nSumXX*nSumYY));
		fCorr *= (((double)N)/nColCount);
		if (isAbsolute) 
			fCorr = Math.abs(fCorr);
		
		final float fDist = 1.0f - fCorr;
  
		return fDist;
	}
	private final boolean m_bAbs;

}

