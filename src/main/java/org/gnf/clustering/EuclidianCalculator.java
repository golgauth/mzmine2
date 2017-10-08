package org.gnf.clustering;

/**
 * The <code>EuclidianCalculator</code> class provides implementation for a distance metrics calculator
 * based on the Euclidian distance.
 * @author Dmitri Petrov
 * @version 1.0
 */
public class EuclidianCalculator implements DistanceCalculator 
{
 public EuclidianCalculator() {} 
	
	public void dispose() {}

	
	public float calculate(final DataSource sourceOne,	final DataSource sourceTwo, final int nIndexOne, final int nIndexTwo)
	{
		if(sourceOne instanceof ByteSource && sourceTwo instanceof ByteSource)
		{
			final byte [] arData1 = ((ByteSource)sourceOne).getByteArray();
			final byte [] arData2 = ((ByteSource)sourceTwo).getByteArray();
			final int nColCount = sourceOne.getColCount(); 
			return EuclidianCalculator.eucledian(nColCount, arData1, arData2, nIndexOne, nIndexTwo);
		}
		
		
		final int nColCount = sourceOne.getColCount();
		
		float fSum = 0;

		float f1 = 0.0f;
		float f2 = 0.0f;

		float fDiff = 0.0f;
		float fDiffSq = 0.0f;
		
		for(int nCol = 0; nCol < nColCount; nCol++)
		{
			f1 = sourceOne.getValue(nIndexOne, nCol);
			f2 = sourceTwo.getValue(nIndexTwo, nCol);

			if(Float.isNaN(f1) || Float.isNaN(f2))
				continue;
					

			fDiff = f2 - f1;
			fDiffSq = fDiff*fDiff;

			fSum += fDiffSq;
		} 


		return (float)Math.pow(fSum,0.5); 
	}
	
	static	float eucledian(final int nColCount, final byte[] arData1, final byte[] arData2,	int nIndex1, int nIndex2)
	{
		final int nPart1 = nColCount*nIndex1;
		final int nPart2 = nColCount*nIndex2;

		int nIdx1 = 0;
		int nIdx2 = 0;

		int nDiff = 0;
		int nDiffSq = 0;

		double fSum = 0;

		for(int nCol = 0; nCol < nColCount; nCol++)
		{
			nIdx1 = nPart1 + nCol;
			nIdx2 = nPart2 + nCol;

			nDiff = arData1[nIdx1] - arData2[nIdx2];
			nDiffSq = nDiff*nDiff;

			fSum += nDiffSq;
		}

		return (float)Math.sqrt(fSum); 
	}

}
