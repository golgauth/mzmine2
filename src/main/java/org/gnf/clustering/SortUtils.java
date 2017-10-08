package org.gnf.clustering;

/**
 * The <code>SortUtils</code> class provides tools for sorting to be used internally by the packfge.
 * @author Dmitri Petrov
 *	@version 1.0
 */
class SortUtils
{
	//Construction
	private SortUtils() {}
  
	// Like public version, but without range checks.
	private static int binarySearch0(final double[] ar, final int nIndexFrom, final int nIndexTo, final double fKey)
	{
		int nLow = nIndexFrom;
		int nHigh= nIndexTo -1;

		while(nLow <= nHigh)
		{
			int mid = (nLow + nHigh) >>> 1;
			double midVal = ar[mid];

			int cmp;
			if(midVal < fKey)
				cmp = -1;   // Neither val is NaN, thisVal is smaller
			else if(midVal > fKey)
				cmp = 1;    // Neither val is NaN, thisVal is larger
			else
			{
				long midBits = Double.doubleToLongBits(midVal);
				long keyBits = Double.doubleToLongBits(fKey);
				cmp = (midBits == keyBits ?  0 : // Values are equal
                    (midBits < keyBits ? -1 : 1));// (-0.0, 0.0) or (!NaN, NaN)
                                          // (0.0, -0.0) or (NaN, !NaN)
			}

			if(cmp < 0)
				nLow = mid + 1;
			else if (cmp > 0)
				nHigh = mid - 1;
			else
				return mid; // key found
		}
		return -(nLow + 1);  // key not found.
	}

 
 
 
 
	static void sort3(final double ar[], final int nIndexFrom, final int nIndexToExcl, final int [] arIndices)
	{
		final long NEG_ZERO_BITS = Double.doubleToLongBits(-0.0d);
		/*
		 * The sort is done in three phases to avoid the expense of using
		 * NaN and -0.0 aware comparisons during the main sort.
		 */
		/*
		 * Preprocessing phase:  Move any NaN's to end of array, count the
		 * number of -0.0's, and turn them into 0.0's.
		 */
		int nNumNegZeros = 0;
		int i = nIndexToExcl-1;//nIndexFrom;
		int n = nIndexFrom -1;//nLength;
		while(i > n)//(i < n)
		{
			if(ar[i] != ar[i])//is NAN
			{
				final double fswap = ar[i];
				ar[i] = ar[++n];//ar[--n];
				ar[n] = fswap;
    
				final int nswap = arIndices[i];
				arIndices[i] = arIndices[n];
				arIndices[n] = nswap;
			}
			else
			{
				if(ar[i]==0 && Double.doubleToLongBits(ar[i])==NEG_ZERO_BITS)
				{
					ar[i] = 0.0d;
					nNumNegZeros++;
				}
				i--;////i++;
			}
		}

		// Main sort phase: quicksort everything but the NaN's
		sort1(ar, n+1, /*n-nIndexFrom*/nIndexToExcl-(n+1), arIndices);

		// Postprocessing phase: change 0.0's to -0.0's as required
		if(nNumNegZeros != 0)
		{
			int j = SortUtils.binarySearch0(ar, /*nIndexFrom*/n+1, /*n*/nIndexToExcl, 0.0d); // posn of ANY zero
			do
			{
				j--;
			} while(j>=0 && ar[j]==0.0d);

			// j is now one less than the index of the FIRST zero
			for(int k=0; k<nNumNegZeros; k++)
			{
				ar[++j] = -0.0d;
			}
		}
	}
  
 
 
	/**
	 * Sorts the specified sub-array of doubles into ascending order.
	 */
	private static void sort1(final double ar[], final int nOff, final int nLen, final int [] arIndices)
	{
		// Insertion sort on smallest arrays
		if(nLen < 7)
		{
			for(int i=nOff; i<nLen+nOff; i++)
			{
				for(int j=i; j>nOff && ar[j-1]>ar[j]; j--)
   				{
   					SortUtils.swap(ar, j, j-1, arIndices);
   				}
			}
			return;
		}

		// Choose a partition element, v
		int m = nOff + (nLen >> 1);       // Small arrays, middle element
		if(nLen > 7)
		{
			int l = nOff;
			int n = nOff + nLen - 1;
			if(nLen > 40)
			{        // Big arrays, pseudomedian of 9
				int s = nLen/8;
				l = SortUtils.med3(ar, l,     l+s, l+2*s);
				m = SortUtils.med3(ar, m-s,   m,   m+s);
				n = SortUtils.med3(ar, n-2*s, n-s, n);
			}
			m = SortUtils.med3(ar, l, m, n); // Mid-size, med of 3
		}
		final double v = ar[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = nOff, b = a, c = nOff + nLen - 1, d = c;
		while(true)
		{
			while (b <= c && ar[b] <= v)
			{
				if(ar[b] == v)
					SortUtils.swap(ar, a++, b, arIndices);
	 
				b++;
			}
	
			while (c >= b && ar[c] >= v)
			{
				if(ar[c] == v)
					SortUtils.swap(ar, c, d--, arIndices);
	
				c--;
			}
	
			if(b > c)
				break;
	
			SortUtils.swap(ar, b++, c--, arIndices);
		}

		// Swap partition elements back to middle
		int s, n = nOff + nLen;
		s = Math.min(a-nOff, b-a  );
		SortUtils.vecswap(ar, nOff, b-s, s, arIndices);
		s = Math.min(d-c,   n-d-1);
		SortUtils.vecswap(ar, b,   n-s, s, arIndices);

		// Recursively sort non-partition-elements
		if((s = b-a) > 1)
			SortUtils.sort1(ar, nOff, s, arIndices);
		if((s = d-c) > 1)
			SortUtils.sort1(ar, n-s, s, arIndices);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static void swap(final double ar[], final int a, final int b, final int [] arIndices)
	{
		final double ft = ar[a];
		ar[a] = ar[b];
		ar[b] = ft;
  
		final int nt = arIndices[a];
		arIndices[a] = arIndices[b];
		arIndices[b] = nt;
	}
 
	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(final double ar[], int a, int b, final int n, final int [] arIndices)
	{
		for(int i=0; i<n; i++, a++, b++)
		{
			swap(ar, a, b, arIndices);
		}
	}
 
	/**
	 * Returns the index of the median of the three indexed doubles.
	 */
	private static int med3(double ar[], int a, int b, int c)
	{
		return (ar[a] < ar[b] ?
		 (ar[b] < ar[c] ? b : ar[a] < ar[c] ? c : a) :
		 (ar[b] > ar[c] ? b : ar[a] > ar[c] ? c : a));
	}

}
