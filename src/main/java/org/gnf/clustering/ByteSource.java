package org.gnf.clustering;

/**
 * The <code>ByteSource</code> class provides implementation for a data source whose values are expected
 * to be in <code>byte</code> precision (e.g. be in the range [-128,127]. This implementation provides
 * a low memory usage.
 * @author Dmitri Petrov
 * @version 1.0
 */
public class ByteSource implements DataSource
{
	/**
	 * Constructs a new <code>ByteSource</code> object that is initialized with the data to be contained in. 
	 * @param arBytes and array of <code>bytes</code> values representing the data in the spreadsheet.
	 * @param nRowCount the number of rows in the constructed data source.
	 * @param nColCount the number of columns in the constructed data source.
	 */
	public ByteSource(final byte [] arBytes, final int nRowCount, final int nColCount)
	{
		if(arBytes == null)
			throw new IllegalArgumentException("Array cannot be null.");
		
		if(nRowCount < 0)
			throw new IllegalArgumentException("The number of rows cannot be negative " + nRowCount);
		
		if(nColCount < 0)
			throw new IllegalArgumentException("The number of columns cannot be negative " + nColCount);
		
		if(nRowCount*nColCount > arBytes.length)
			throw new IllegalArgumentException("The number of specified cells " + (nRowCount*nColCount) +
																																						" exceeds the number of elements " + arBytes.length + "in the specified array");
			
		m_arBytes = arBytes;
		m_nRowCount = nRowCount;
		m_nColCount = nColCount;
	}
	
	/**
	 * Constructs a new uninitialized <code>ByteSource</code> object with the specifies row and column dimensions.
	 * @param nRowCount the number of rows in the constructed data source.
	 * @param nColCount the number of columns in the constructed data source.
	 */
	public ByteSource(final int nRowCount, final int nColCount)
	{
		if(nRowCount < 0)
			throw new IllegalArgumentException("The number of rows cannot be negative " + nRowCount);
		
		if(nColCount < 0)
			throw new IllegalArgumentException("The number of columns cannot be negative " + nColCount);
		
		m_arBytes = new byte[nRowCount*nColCount];
		m_nRowCount = nRowCount;
		m_nColCount = nColCount;
	}
		
	
	public int getRowCount()
	{
		return m_nRowCount;
	}
	
	public int getColCount()
	{
		return m_nColCount;
	}
	
	
	/**
	 * Returns the internal array containing the actual data.
	 * @return a reference to a <code>byte[]</code> array containing the data.
	 */
	public byte [] getByteArray() {return m_arBytes;}
	
	public float getValue(final int nRow, final int nCol)
	{
	 final int nIdx = nRow*m_nColCount + nCol;
		return m_arBytes[nIdx];
	}
	
	public void setValue(int nRow, int nCol, float fValue)
	{
		if(fValue < Byte.MIN_VALUE || fValue > Byte.MAX_VALUE)
			throw new IllegalArgumentException("Float value (" + fValue + ") is out of bounds [" + Byte.MIN_VALUE + ", " + Byte.MAX_VALUE + "]");  
		
		if(((float)((byte)fValue)) != fValue)
			throw new IllegalArgumentException("Float value (" + fValue + ") must have a discrete nature " + fValue);
		
		final int nIdx = nRow*m_nColCount + nCol;
		m_arBytes[nIdx] = (byte)fValue;
	}
		
	public DataSource toEmptySource(int nRowCount, int nColCount)
	{
		return new ByteSource(nRowCount, nColCount);
	}
		
	
	public DataSource subSource(final int [] nRowIndices)
	{
		final int nRowCount = nRowIndices.length;
		final byte[] arBytes = new byte[nRowCount*m_nColCount];
			
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
			  System.arraycopy(m_arBytes, nIdxFr, arBytes, n*m_nColCount, nLength);
		}
		return new ByteSource(arBytes, nRowCount, m_nColCount);
	}
	

	public void copy(final int nFrSrc, final DataSource sourceDst, final int nFrDst, final int nLength)
	{
		if(m_nColCount != sourceDst.getColCount())
			throw new IllegalArgumentException("The number of columns must be the same");
 		
		final int nIdxSrcFr = nFrSrc*m_nColCount;
		final int nIdxDstFr = nFrDst*m_nColCount;
 	
		if(sourceDst instanceof ByteSource)
			System.arraycopy(m_arBytes, nIdxSrcFr, ((ByteSource)sourceDst).m_arBytes, nIdxDstFr, nLength*m_nColCount);
		else
		{
			for(int n = 0; n < nLength; ++n)
			{
 				for(int nCol = 0; nCol < m_nColCount; ++nCol)
 				{
 					sourceDst.setValue(nFrDst+n, nCol, getValue(nFrSrc+n, nCol));
 				}
			}
		}
	}
   
	public String toString()
	{
		final StringBuffer buf = new StringBuffer(10000);
		for(int nRow=0; nRow<m_nRowCount; ++nRow)
 		{
 			for(int nCol = 0; nCol < m_nColCount; ++nCol)
			{
 				int nIdx = nRow*m_nColCount + nCol;
				buf.append(m_arBytes[nIdx]);
				if(nCol < m_nColCount -1)
					buf.append(", ");
			}
 			buf.append("\n");
 		}
		return buf.toString();
	}
 
 
	private byte[] m_arBytes;
	private final int m_nColCount;
	private final int m_nRowCount; 
}
