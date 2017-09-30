package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

public class LargeArrayDouble {

	private final long CHUNK_SIZE = 1024 * 1024 * 1024; // 1GiB

	long size;
	double[][] data;

	public LargeArrayDouble(long size) {
		
		
		this.size = size;
		if (size == 0) {
			data = null;
		} else {
			int chunks = (int) (size / CHUNK_SIZE);
			int remainder = (int) (size - ((long) chunks) * CHUNK_SIZE);
		
			System.out.println(this.getClass().getSimpleName() 
					+ " > Created with " + chunks + " chunks (size: " + CHUNK_SIZE + " each) + a remainder of " + remainder 
					+ " => TOTAL: " + size);

			data = new double[chunks + (remainder == 0 ? 0 : 1)][];
			for (int idx = chunks; --idx >= 0;) {
				data[idx] = new double[(int) CHUNK_SIZE];
			}
			if (remainder != 0) {
				data[chunks] = new double[remainder];
			}
			
//			System.out.println(this.getClass().getSimpleName() 
//					+ " > Created with " + chunks + " chunks (size: " + CHUNK_SIZE + " each) + a remainder of " + remainder 
//					+ " => TOTAL: " + size);
		}
	}

	public double get(long index) {
		
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException(
					"Error attempting to access data element " + index + ".  Array is " + size + " elements long.");
		}
		int chunk = (int) (index / CHUNK_SIZE);
		int offset = (int) (index - (((long) chunk) * CHUNK_SIZE));
		return data[chunk][offset];
	}

	public void set( long index, double f ) {
		
		if( index<0 || index>=size ) {
			throw new IndexOutOfBoundsException("Error attempting to access data element "+index+".  Array is "+size+" elements long.");
		}
		int chunk = (int)(index/CHUNK_SIZE);
		int offset = (int)(index - (((long)chunk)*CHUNK_SIZE));
		data[chunk][offset] = f;
	}
	
	public void writeToFile() { // toString won't make sense for large array!
		
//		String str = "";
//		
//		for (int i = 0; i < size; ++i) {
//			str += 
//		}
//		
//		return str;
		
		// ...
	}
	
}





