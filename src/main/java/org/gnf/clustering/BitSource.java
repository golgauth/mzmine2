package org.gnf.clustering;

/**
 * The <code>BitSource</code> class provides implementation for a data source whose values are expected
 * to be <code>bit</code>s (e.g. have values 0 or 1). Usage examples may include fingerprints or simple (yes/no) flags.
 * This implementation provides the most conservative memory usage.
 * @version 1.0
 */
public class BitSource implements DataSource
{
	/**
	 * Constructs a new <code>ByteSource</code> object that is initialized with the data to be contained in. 
	 * @param arBytes and array of <code>byte</code>s whose individual bits represent the values in the spreadsheet.
	 * @param nRowCount the number of rows.
	 * @param nUnitCount the number of <code>byte</code> units whose individual bits represent the cell values.
	 */
	public BitSource(final byte[] arBytes, final int nRowCount, final int nUnitCount)
	{
		if(arBytes == null)
			throw new IllegalArgumentException("Array containing bit values cannot be null.");
 	
		if(nRowCount < 0)
			throw new IllegalArgumentException("The number of rows cannot be negative " + nRowCount);
		
		if(nUnitCount < 0)
			throw new IllegalArgumentException("The number of byte units cannot be negative " + nUnitCount);
 	
		m_arBytes = arBytes;
		m_nRowCount = nRowCount;
		m_nUnitCount = nUnitCount;
	}
 
	/**
	 * Constructs a new uninitialized <code>BitSource</code> object with the specifies row and column dimensions.
	 * @param nRowCount the number of rows.
	 * @param nUnitCount the number of <code>byte</code> units whose individual bits represent the cell values.
	 */
	public BitSource(final int nRowCount, final int nUnitCount)
	{
		if(nRowCount < 0)
			throw new IllegalArgumentException("The number of rows cannot be negative " + nRowCount);
		
		if(nUnitCount < 0)
			throw new IllegalArgumentException("The number of byte units cannot be negative " + nUnitCount);
		
		m_arBytes = new byte[nRowCount*nUnitCount];
		m_nRowCount = nRowCount;
		m_nUnitCount = nUnitCount;
	}
 
 
	public int getRowCount()
	{
		return m_nRowCount;
	}
	
	public int getColCount()//fp length
	{
		return m_nUnitCount*8;
	}

	public boolean getBoolValue(final int nRow, final int nCol)
	{
		final int nUnit = nCol/8;
		final int nBitInUnit = nCol - 8*nUnit;
		final int nIdx = nRow*m_nUnitCount+nUnit;
		
		final int nValue = m_arBytes[nIdx];
		//System.out.println(nCol + " " + nIdx + " " + nValue + " " +  MASKS[nBitInUnit] + " " + (nValue & MASKS[nBitInUnit]));	
		return (nValue & MASKS[nBitInUnit]) == MASKS[nBitInUnit];
	}
	
	public void setBoolValue(final int nRow, final int nCol, final boolean b)
	{
		final int nUnit = nCol/8;
		final int nBitInUnit = nCol - 8*nUnit;
		final int nIdx = nRow*m_nUnitCount+nUnit;
		final int nValue = m_arBytes[nIdx];
				
		m_arBytes[nIdx] = b ? (byte)(nValue | MASKS[nBitInUnit]) : (byte)(nValue & MASKS_INV[nBitInUnit]);
	}
	
	
	public float getValue(int nRow, int nCol)
	{
		return getBoolValue(nRow, nCol) ? 1 : 0;
	}
			
	
	public void setValue(int nRow, int nCol, float fValue)
	{
		if(fValue == 0.0f)
			setBoolValue(nRow, nCol, false);
		else if(fValue == 1.0f)
			setBoolValue(nRow, nCol, true);
		else	throw new RuntimeException("Unsupported value " + fValue);
	}
		
	public DataSource toEmptySource(int nRowCount, int nColCount)
	{
		return new BitSource(nRowCount, nColCount);
	}
			
	public DataSource subSource(int [] nRowIndices)
	{
		final int nRowCount = nRowIndices.length;
		final byte[] arBytes = new byte[nRowCount*m_nUnitCount];
		
		int nRow = -1;
		int nIdxFr = -1;
		int nIdxTo = -1;
		int nLength =-1;
		for(int n = 0; n < nRowIndices.length; ++n)
		{
			  nRow = nRowIndices[n];
			  nIdxFr = nRow*m_nUnitCount;
			  nIdxTo = (nRow+1)*m_nUnitCount-1;
			  nLength = nIdxTo - nIdxFr +1;
			  System.arraycopy(m_arBytes, nIdxFr, arBytes, n*m_nUnitCount, nLength);
		}
		return new BitSource(arBytes, nRowCount, m_nUnitCount);
	}
	
 
	public void copy(final int nFrSrc, final DataSource sourceDst, final int nFrDst, final int nLength)
	{
		final int nIdxSrcFr = nFrSrc*m_nUnitCount;
		final int nIdxDstFr = nFrDst*m_nUnitCount;
 	
		if(sourceDst instanceof BitSource)
			System.arraycopy(m_arBytes, nIdxSrcFr, ((BitSource)sourceDst).m_arBytes, nIdxDstFr, nLength*m_nUnitCount);
		else
		{
			throw new RuntimeException("To be implemented");		
		}
	}
	
	private final int m_nRowCount;
	private final int m_nUnitCount;
	private final byte[] m_arBytes;
	
	private static final byte [] MASKS = new byte[8];
	private static final byte [] MASKS_INV = new byte[8];
	static
	{
		for(int n = 0; n < MASKS.length; ++n)
		{
	 		MASKS[n] = (byte)(1 << n);
	 		MASKS_INV[n] = (byte)(255-MASKS[n]);
	 			//System.out.println(MASKS[n]);
		}
	}
}
