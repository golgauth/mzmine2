package org.gnf.clustering;

import java.util.concurrent.*;

/**
 * The <code>DistMatrixCalculator</code> class provides implementation for a job
 * representing a chunk of the total distance matrix calculation task to be run concurrently on multiple CPUs.
 * @author Dmitri Petrov
 * @version 1.0
 */
class DistMatrixCalculator implements Callable<Boolean>
{
	/**
	 * Constructs a new <code>DistMatrixCalculator</code> object that is initialized with the specified distance metrics calculation service,
	 * the data source. This class is intended to be used internally by the package.
	 * @param calculator the distance metrics calculation service
	* @param data the specified data source.
	* @param nIdxFr the index of the first cell of the calculation task chunk in the matrix triangle under the main diagonal.
	* @param nIdxTo the index of the last  cell of the calculation task chunk in the matrix triangle under the main diagonal.
	* @param mtx the output matrix to contain the calculated distances.
	* @param counter the progress counter.
	*/
	DistMatrixCalculator(final DistanceCalculator calculator,
 																					final DataSource data,	final int nIdxFr, final int nIdxTo,
 																					final DistanceMatrix mtx,	final ProgressCounter counter)
 	{
		m_calculator = calculator;
		m_data = data;
 
		m_nIdxFr = nIdxFr;
		m_nIdxTo = nIdxTo;
		m_mtx = mtx;
		m_counter = counter;
 	}
 
 
	DistanceCalculator getCalculator() {return m_calculator;}
	DataSource getDataSource() {return m_data;}
	DistanceMatrix getDistanceMatrix() {return m_mtx;}
	int getFromIndex() {return m_nIdxFr;}
	int getToIndex() {return m_nIdxTo;}
 
 
	public Boolean call() throws Exception
	{
		final int nIterCount = m_nIdxTo - m_nIdxFr +1; 
		int nPct = 0;
		int nPctOld = -1;
		int nIncrement = 0;
  	
		int nIndex1 = -1;
		int nIndex2 = -1;
		long nTmp = 0L;
		float fMtx = 0.0f;
		for(int nIdx = m_nIdxFr; nIdx <= m_nIdxTo; ++nIdx)
		{
			if(m_counter != null)
			{
				if(m_counter.isCancelled())
				{
					m_counter = null;
					return false;//CancellationException will be thrown first in the PropertyChangeListener
				}
 		  	
 				if(m_counter.isPaused())
 				{
 					Thread.sleep(100);
 					if(m_counter.isCancelled())
 					{
 						m_counter = null;
 						return false;//CancellationException will be thrown first in the PropertyChangeListener
 					}
 						
 					--nIdx;
 					continue;
 				}
			}
 		 					
			nTmp = 1L + (8L*(long)nIdx);
			nIndex1 = 1 + (int)Math.floor((-1 + Math.sqrt(nTmp))/2.0);
			nIndex2 = nIdx - Utils.MTX_LENGTHS[nIndex1-1];
					
			fMtx = m_calculator.calculate(m_data, m_data, nIndex1, nIndex2);
			if(Float.isNaN(fMtx))
			{
				fMtx = m_calculator.calculate(m_data, m_data, nIndex1, nIndex2);
				throw new IllegalStateException("Distance Matrix value cannot be NaN.");
			}
					
			m_mtx.setValue(nIndex1, nIndex2, fMtx);
 		  					
			if(m_counter != null)
			{
				if(m_counter.isCancelled())
				return false;
					
				++nIncrement;
				nPct = (int)(100*((nIdx - m_nIdxFr +1)/((float)nIterCount)));
				if(nPct != nPctOld)
				{
					m_counter.increment(nIncrement, true);
					nIncrement = 0;
				}
						
				nPctOld = nPct;
			}
		}
 	 	
		m_counter = null;
		return true; 	
	}
 
	//DATA SECTION
	private final DistanceCalculator m_calculator;
	private final DataSource m_data;
	private final int m_nIdxFr;
	private final int m_nIdxTo;
	private final DistanceMatrix m_mtx;
	private ProgressCounter m_counter;
}
