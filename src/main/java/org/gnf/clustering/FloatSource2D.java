package org.gnf.clustering;

/**
 * The <code>FloatSource</code> class provides implementation for a data source whose values are expected
 * to have <code>float</code> precision. This implementation is intended for use with the most generic data, but it
 * has a high memory usage and may be not suitable for large data sets. Whenever possible it is advised
 * to consider alternative low memory data sources like {@link  org.gnf.clustering.BitSource BitSource} or {@link  org.gnf.clustering.ByteSource ByteSource}.
 * @author Dmitri Petrov
 * @version 1.0
 */
public class FloatSource2D implements DataSource
{
	/**
	 * Constructs a new <code>FloatSource</code> object that is initialized with the data to be contained in. 
	 * @param arFloats and array of <code>float</code> values representing the data.
	 * @param nRowCount the number of rows in the constructed data source.
	 * @param nColCount the number of columns in the constructed data source.
	 * @see org.gnf.clustering.DataSource.
	 */
	public FloatSource2D(final float [][] arFloats, final int nRowCount, final int nColCount)
	{
		/*if(arFloats == null)
			throw new IllegalArgumentException("Array containing values cannot be null.");
		
		if(nRowCount < 0)
			throw new IllegalArgumentException("The number of rows cannot be negative " + nRowCount);
		
		if(nColCount < 0)
			throw new IllegalArgumentException("The number of columns cannot be negative " + nColCount);
		
		if(nRowCount*nColCount > arFloats.length)
			throw new IllegalArgumentException("The number of specified cells " + (nRowCount*nColCount) +
																																						" exceeds the number of elements " + arFloats.length + "in the specified array");
		*/		
		m_arFloats = arFloats;
		m_nRowCount = nRowCount;
		m_nColCount = nColCount;
	}
	
	/**
	 * Constructs a new uninitialized <code>FloatSource</code> object with the specifies row and column dimensions.
	 * @param nRowCount the number of rows in the constructed data source.
	 * @param nColCount the number of columns in the constructed data source.
	 * @see org.gnf.clustering.DataSource.
	 */
	public FloatSource2D(final int nRowCount, final int nColCount)
	{
		m_arFloats = new float[nRowCount][nColCount];
		m_nRowCount = nRowCount;
		m_nColCount = nColCount;
	}
	
	/**
	 * Returns the internal array containing the actual data.
	 * @return a reference to a <code>float[]</code> array containing the data.
	 */
	public float[][] getFloatArray() {return m_arFloats;}
	
	public int getRowCount()
	{
		return m_nRowCount;
	}
	
	public int getColCount()
	{
		return m_nColCount;
	}
	
	public float getValue(final int nRow, final int nCol)
	{
		final int nIdx = nRow*m_nColCount + nCol;
		try{return m_arFloats[nRow][nCol];}
		catch(Exception ex)
		{
			throw new RuntimeException("",ex);
		}
	}
	
	public void setValue(int nRow, int nCol, float fValue)
	{
		final int nIdx = nRow*m_nColCount + nCol;
		m_arFloats[nRow][nCol] = fValue;
	}

		
	public DataSource toEmptySource(int nRowCount, int nColCount)
	{
		return new FloatSource2D(nRowCount, nColCount);
	}
	
	public DataSource subSource(int nRowFr, int nRowTo)
	{
		final int nRowCount = nRowTo - nRowFr +1;
		final float[][] arFloats = new float[nRowCount][m_nColCount];
		final int nIdxFr = nRowFr*m_nColCount;
		final int nIdxTo = (nRowTo+1)*m_nColCount-1;
		final int nLength = nIdxTo - nIdxFr +1; 
		
		for (int i = 0; i < nRowCount; i++) {
		    System.arraycopy(m_arFloats[i + nRowFr], 0, arFloats[i], 0, m_nColCount);
		}

		
		//System.arraycopy(m_arFloats, nIdxFr, arFloats, 0, nLength);
		return new FloatSource2D(arFloats, /*arFlags,*/ nRowCount, m_nColCount);
	}
	
	public DataSource subSource(int [] nRowIndices)
	{
		final int nRowCount = nRowIndices.length;
		final float[][] arFloats = new float[nRowCount][m_nColCount];
		
		int nRow = -1;
		int nIdxFr = -1;
		int nIdxTo = -1;
		int nLength =-1;
		for(int n = 0; n < nRowIndices.length; ++n)
		{
			  nRow = nRowIndices[n];
			  nIdxFr = nRow*m_nColCount;
			  nIdxTo = (nRow+1)*m_nColCount-1;
			  nLength = nIdxTo - nIdxFr +1;
			  System.arraycopy(m_arFloats[nRow], 0, arFloats[n], 0, m_nColCount);
			 
		}
		return new FloatSource2D(arFloats, nRowCount, m_nColCount);
	}
	
	public void copy(final int nFrSrc, final DataSource sourceDst, final int nFrDst, final int nLength)
	{ 	
 		for(int n = 0; n < nLength; ++n)
		{
 			for(int nCol = 0; nCol < m_nColCount; ++nCol)
 			{
 				sourceDst.setValue(nFrDst+n, nCol, getValue(nFrSrc+n, nCol));
 			}
 		}
 	
	}
 
	
	private float[][] m_arFloats;
	private final int m_nColCount;
	private final int m_nRowCount; 
}
