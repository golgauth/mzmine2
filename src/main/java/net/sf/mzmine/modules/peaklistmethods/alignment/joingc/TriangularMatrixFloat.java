package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.util.Arrays;

public class TriangularMatrixFloat extends TriangularMatrix {
	
//    private final float[] list;
    private final LargeArrayFloat list;

    public TriangularMatrixFloat(int dimension) {
    	
//    	System.out.println(">>>>>>>>>>>>> (-1) MAX_INT: " + Integer.MAX_VALUE);
    	System.out.println(">>>>>>>>>>>>> (0) Created distance MATRIX, dim: " + dimension + "x" + dimension);
    	System.out.println(">>>>>>>>>>>>> (1) Created distance VECTOR, dim: " + sumFormula(dimension) 
    		+ "(i.e. " + dimension + " * (" + dimension + " + 1) / 2)");
    	
//        list = new float[sumFormula(dimension)];
        list = new LargeArrayFloat(sumFormula(dimension));
//    	System.out.println(">>>>>>>>>>>>> (2) !!! Created distance VECTOR, dim: " + this.getVector().length);
        this.setDimension(dimension);
    }

    @Override
    public double set(int row, int column, double value) {
        //validateArguments(row, column);

        long listIndex = getListIndex(row, column);
//        float oldValue = list[listIndex];
//        list[listIndex] = (float) value;
        float oldValue = list.get(listIndex);
        list.set(listIndex, (float) value);

        return oldValue;
    }

    @Override
    public double get(int row, int column) {
        //validateArguments(row, column);
    	
    	return list.get(getListIndex(row, column));
    }

//	public float[] getVector() {
//		return this.list;
//	}

	@Override
    public void printVector() {   	
    	//System.out.println(Arrays.toString(this.getVector()));
		System.out.println(list.toString());
    }

}
