package org.gnf.clustering;

import java.text.*;

/**
 * The <code>DistanceMatrix1D</code> class provides implementation for the distance matrix based on a single
 * array of <code>float</code> values representing the lower triangle under the main diagonal.
 * This implementation provides conservative memory usage and fast read/write operations on the data,
 * but has a limitation on the maximum number of values that can be stored on the matrix.  
 * @author Dmitri Petrov
 * @version 1.0
 */
public class DistanceMatrix1D implements DistanceMatrix
{
	/**
	 * Constructs a new empty <code>DistanceMatrix1D</code> object of the specified size. 
	 * @param nRowCount the number of rows in the matrix.
	 */
	public DistanceMatrix1D(final int nRowCount)
	{
		if(nRowCount < 0)
			throw new IllegalArgumentException("Matrix dimension cannot be negative " + nRowCount);
		
		final int nLength = Utils.MTX_LENGTHS[nRowCount - 1];
		m_arMtx = new float[nLength];
		m_nRowCount = nRowCount;		
	}
	
	/**
	 * Constructs a new <code>DistanceMatrix1D</code> object that is initialized with the specified data.
	 * @param arDstMxt an array containing the distance metrics values in the lower triangle under the main diagonal.
	 * @param nRowCount the number of rows.
	 */
	public DistanceMatrix1D(final float [] arDstMxt, final int nRowCount)
	{
		m_arMtx = arDstMxt;
		m_nRowCount = nRowCount;		
	}
	
	public float [] getDstMtxData()
	{
		return m_arMtx; 
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
		
		final int nIdx = Utils.MTX_LENGTHS[nRow - 1] + nCol;
		return m_arMtx[nIdx];
	}
	
	public void setValue(int nRow, int nCol, final float fVal)
	{
		if(nRow < nCol)
		{
			final int nTmp = nRow;
			nRow = nCol;
			nCol = nTmp;
		}
		
		final int nIdx = Utils.MTX_LENGTHS[nRow - 1] + nCol;
		m_arMtx[nIdx] = fVal;
	}
	
	public String toString()
	{
		final StringBuffer buf = new StringBuffer(1000);
		buf.append("\t");
		for(int nCol=0; nCol<m_nRowCount; ++nCol)
		{
			buf.append(nCol);
			buf.append("\t");
		}
		
		final DecimalFormat format = new DecimalFormat("0.00000"); 
		
		int nIdx = -1;
		buf.append("\n");
		for(int nRow=0; nRow<m_nRowCount; ++nRow)
		{
			buf.append(nRow);
			buf.append("\t");
			for(int nCol = 0; nCol < m_nRowCount; ++nCol)
			{
				if(nCol == nRow)
				{
					buf.append("x");
				}
				else if(nCol < nRow)
				{
					nIdx = Utils.MTX_LENGTHS[nRow-1] + nCol;
									
					buf.append(format.format(m_arMtx[nIdx]));
					buf.append("\t");
				}
				else
					break;
			}
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	
	private final float[] m_arMtx;
	private final int m_nRowCount;
}
