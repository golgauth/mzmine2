package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.util.Arrays;

public class TriangularMatrixDouble extends TriangularMatrix {

//    private final double[] list;
    private final LargeArrayDouble list;

    public TriangularMatrixDouble(int dimension) {
    	
    	System.out.println(">>>>>>>>>>>>> (0) Created distance MATRIX, dim: " + dimension + "x" + dimension);
    	System.out.println(">>>>>>>>>>>>> (1) Created distance VECTOR, dim: " + sumFormula(dimension) 
    		+ "(i.e. " + dimension + " * (" + dimension + " + 1) / 2)");
    	
//        list = new float[sumFormula(dimension)];
        list = new LargeArrayDouble(sumFormula(dimension));
        this.setDimension(dimension);
    }

    @Override
    public double set(int row, int column, double value) {
        //validateArguments(row, column);

        long listIndex = getListIndex(row, column);
//        float oldValue = list[listIndex];
//        list[listIndex] = (float) value;
        double oldValue = list.get(listIndex);
        list.set(listIndex, value);

        return oldValue;
    }
    
    @Override
    public double get(int row, int column) {
        //validateArguments(row, column);

    	return list.get(getListIndex(row, column));
    }

//	public double[] getVector() {
//		return this.list;
//	}

	@Override
    public void printVector() {   	
//    	System.out.println(Arrays.toString(this.getVector()));
    }

}
