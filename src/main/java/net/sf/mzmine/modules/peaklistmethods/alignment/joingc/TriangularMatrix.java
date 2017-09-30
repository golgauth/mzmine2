package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

public abstract class TriangularMatrix {

//	private double[] list;
	private int dimension;
	
    public abstract double set(int row, int column, double value);

    public abstract double get(int row, int column);
    
    //public abstract int getSize();   
    public int getDimension() {
    	return this.dimension;
    }
    protected void setDimension(int dimension) {
    	this.dimension = dimension;
    }
    

    public void validateArguments(int row, int column) {
        if (row > column) {
            throw new IllegalArgumentException("Row (" + row + " given) has to be smaller or equal than column (" + column + " given)!");
        }
    }

//    /*static*/ public int getListIndex(int row, int column) {
//        return sumFormula(column) /*- 1*/ + row;
//    }
    /*static*/ public long getListIndex(int row, int column) {  // Symmetrical
    	
    	if (row > column)
    		return sumFormula(row) /*- 1*/ + (long) column;
    	else
    		return sumFormula(column) /*- 1*/ + (long) row;
    }

//    public int getListIndex(int r, int c) {
//
//    	if (r < c)
//    		return (this.getSize() * r) + c - ((r * (r+1)) / 2);
//    	else
//    		return (this.getSize() * c) + r - ((c * (c+1)) / 2);
//    }

    /*static*/ public long sumFormula(long i) {
        return (i*i + i) / 2;
    }

    
//	public abstract double[] getVector(); 
//	{
//		return this.list;
//	}

    public void print() {
    	
    	for (int i = 0; i < getDimension(); i++) {
    		
    		System.out.println("\n");
    		
    		for (int j = 0; j < getDimension(); j++) {
    			
    			System.out.println(" " + this.get(i,  j));
    		}
    		
    	}
    }
    
    public abstract void printVector();
    
}
