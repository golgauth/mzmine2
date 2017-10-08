package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import org.gnf.clustering.DistanceMatrix;

public class DistanceMatrixTriangular1D2D implements DistanceMatrix {
	
	private int dimension;
	
    private final LargeArrayFloat list;

    public DistanceMatrixTriangular1D2D(int nRowCount) {
    	
    	System.out.println(">>>>>>>>>>>>> (0) Created distance MATRIX, dim: " + nRowCount + "x" + nRowCount);
    	System.out.println(">>>>>>>>>>>>> (1) Created distance VECTOR, dim: " + sumFormula(nRowCount) 
    		+ "(i.e. " + nRowCount + " * (" + nRowCount + " + 1) / 2)");
    	
        list = new LargeArrayFloat(sumFormula(nRowCount));
        
//        this.setDimension(dimension);
        dimension = nRowCount;
    }
//    
//    public int getDimension() {
//    	return this.dimension;
//    }
//    protected void setDimension(int dimension) {
//    	this.dimension = dimension;
//    }

    public DistanceMatrixTriangular1D2D(DistanceMatrix distanceMatrix2) {
    	
    	this.dimension = distanceMatrix2.getRowCount();
    	this.list = new LargeArrayFloat(sumFormula(this.dimension));
    	
    	for (int i = 0; i < this.dimension; ++i) {
    		for (int j = i; j < this.dimension; ++j) {
    			 this.setValue(i, j, distanceMatrix2.getValue(i, j));
    		}
    	}
    }
    
    static public long getListIndex(int row, int column) {  // Symmetrical
    	
    	if (row > column)
    		return sumFormula(row) /*- 1*/ + (long) column;
    	else
    		return sumFormula(column) /*- 1*/ + (long) row;
    }

    static public long sumFormula(long i) {
        return (i*i + i) / 2;
    }


	@Override
	public int getRowCount() {
		return dimension;
	}

	@Override
	public int getColCount() {
		return dimension;
	}

	@Override
	public float getValue(int nRow, int nCol) {
		
		return list.get(getListIndex(nRow, nCol));
	}

	@Override
	public void setValue(int nRow, int nCol, float fVal) {
 
        list.set(getListIndex(nRow, nCol), fVal);
	}
	
	
	
	
	// ---------------------------------------

	public void printVector() {   	
		//System.out.println(Arrays.toString(this.getVector()));
		System.out.println(list.toString());
	}
	//-
	public void print() {

		for (int i = 0; i < this.dimension; i++) {

    		System.out.println("\n");
    		
    		for (int j = 0; j < this.dimension; j++) {
    			
    			System.out.println(" " + this.getValue(i,  j));
    		}
    		
    	}
    }
	//-
	public double[][] toTwoDimArray() {
		
		double[][] arr = new double[this.dimension][this.dimension];
		
    	for (int i = 0; i < this.dimension; i++) {
    		
    		for (int j = 0; j < this.dimension; j++) {
    			
    			arr[i][j] = this.getValue(i,  j);
    		}
    	}
		
		return arr;
	}

}
