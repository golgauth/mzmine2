package org.gnf.clustering;

/**
 * The <code>DataSource</code> interface defines a generic spreadsheet-like data source to be used with clustering modules.
 * @author Dmitri Petrov
 * @version 1.0
 */
public interface DataSource
{
	/**
	 * Returns the number of rows in this data source.
	 * @return the number of rows in this data source.
	 */
	public int getRowCount();
	
	/**
	 * Returns the number of columns in this data source.
	 * @return the number of columns in this data source.
	 */
	public int getColCount();
		
	/**
	 * Returns the value of a specific cell.
	 * @param nRow the cell's row
	 * @param nCol the cell's column
	 * @return the cell's value in float precision.
	 */
	public float getValue(int nRow, int nCol);
 
	/**
	 * Sets the value of a specific cell.
	 * @param nRow the cell's row.
	 * @param nCol the cell's column.
	 * @param fValue the value to be set.
	 */
	public void setValue(int nRow, int nCol, float fValue);
 
	/**
	 * Returns a new empty data source of the same type as this data source.
	 * @param nRowCount the number of rows in the new source.
	 * @param nColCount the number of columns in the new data source. 
	 * @return a reference to the new empty <code>DataSource</code> object.
	 */
	public DataSource toEmptySource(int nRowCount, int nColCount);
 
	/**
	 * Returns a new data source of the same type containing a sub-set of data from this data source.
	 * @param nRowIndices and array of row indices whose cells values are to be contained in the new data source.
	 * @return a reference to the new <code>DataSource</code> object.
	 */
	public DataSource subSource(int [] nRowIndices);
 
	/**
	 * Copies a continuous range of cells' values from this data source to the specified data source.
	 * @param nFrSrc the index of the first cell in this data source to copy data from (zero-based).
	 * @param sourceDst the destination data source to contain the copied data.
	 * @param nFrDst the index of the first cell in the destination data source to copy data to (zero-based).
	 * @param nLength the number of cells to copy.
	 */
	public void copy(int nFrSrc, DataSource sourceDst, int nFrDst, int nLength);
}
