package org.gnf.clustering;

/**
 * The <code>ProgressCounter</code> interface defines a template for a progress monitor to be used with clustering services.
 * The implemented methods must provide support for multi-threaded execution.
 * @author Dmitri Petrov
 * @version 1.0
 */
public interface ProgressCounter
{
	public void setMessage(final String strMessage);
	public void setOperationName(final String strOpName);
	public int getTotal();
	public int getProcessed();
	public void setTotal(final int nTotal);
	public void increment(final int nProcessedInc, final boolean bUpdatePrg);
	public void updateProgress();
	public void complete();
	public boolean isCancelled();
	public boolean isPaused();
	public void dispose();
}
