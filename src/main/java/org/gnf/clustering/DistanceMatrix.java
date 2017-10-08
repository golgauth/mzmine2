package org.gnf.clustering;

/**
 * The <code>DistanceMatrix</code> class defines a template for a distance matrix container
 * to be used by clustering services.
 * @author Dmitri Petrov
 * @version 1.0
 */
public interface DistanceMatrix
{
	/**
	 * Returns the number of rows in the matrix.
	 * @return the number of rows in the matrix.
	 */
	public int getRowCount();
	
	/**
	 * Returns the number of columns in the matrix.
	 * @return the number of columns in the matrix.
	 */
	public int getColCount();
	
	/**
	 * Returns the value of an individual cell in the matrix.
	 * @param nRow the cell's row
	 * @param nCol the cell's column
	 * @return the cell's value in <code>float</code> precision.
	 */
	public float getValue(final int nRow, final int nCol);
 
	/**
	 * Sets the value of an individual cell in the matrix.
	 * @param nRow the cell's row
	 * @param nCol the cell's column
	 * @param fVal the value to be set.
	 */
	public void setValue(final int nRow, final int nCol, final float fVal);
}
