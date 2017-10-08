package org.gnf.clustering;

/**
 * The <code>DistanceMatrix1D</code> class provides implementation for the distance matrix based on a two-dimensional
 * array of <code>float</code> values representing the lower triangle under the main diagonal.
 * This implementation provides less conservative memory usage and a slower read/write operations on the data
 * in comparison with the {@link  org.gnf.clustering.DistanceMatrix1D DistanceMatrix1D} class, but substantially
 * increases the maximum number of values that can be stored on the matrix.  
 * @author Dmitri Petrov
 * @version 1.0
 */
public class DistanceMatrix2D implements DistanceMatrix
{
//Construction
	/**
	 * Constructs a new empty <code>DistanceMatrix2D</code> object of the specified size. 
	 * @param nRowCount the number of rows in the matrix.
	 */
	public DistanceMatrix2D(final int nRowCount)
	{
		if(nRowCount < 0)
			throw new IllegalArgumentException("Matrix dimension cannot be negative " + nRowCount);
		
		m_arrMtx = new float[nRowCount][];
		for(int nRow = 0; nRow < nRowCount; ++nRow)
		{
			m_arrMtx[nRow] = new float[nRow];
		}
		m_nRowCount = nRowCount;		
	}
	
	public int getRowCount() {return m_nRowCount;}
	public int getColCount()	{return m_nRowCount;}
	
	public float getValue(int nRow, int nCol)
	{
		if(nRow == nCol)
			return 0.0f;
		
		if(nRow < nCol)
		{
			final int nTmp = nRow;
			nRow = nCol;
			nCol = nTmp;
		} 
		
		return m_arrMtx[nRow][nCol];
	}
	
	public void setValue(int nRow, int nCol, float fVal)
	{
		if(nRow < nCol)
		{
			final int nTmp = nRow;
			nRow = nCol;
			nCol = nTmp;
		} 
		m_arrMtx[nRow][nCol] = fVal;
	}
	
	
	public String toString()
	{
		final StringBuffer buf = new StringBuffer(1000);
		buf.append("\t");
		for(int nRow=0; nRow<m_nRowCount; ++nRow)
		{
			buf.append(nRow);
			buf.append("\t");
		}
		
		for(int nRow=0; nRow<m_nRowCount; ++nRow)
		{
			buf.append(nRow);
			buf.append("\t");
			for(int nCol = 0; nCol < m_nRowCount; ++nCol)
			{
				if(nCol == nRow)
					buf.append("x");
				else if(nCol < nRow)
					buf.append(m_arrMtx[nRow][nCol]);
				else
					break;
			}
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	
//DATA SECTION
	private final float[][] m_arrMtx;
	private final int m_nRowCount;
}
