package org.gnf.clustering;

/**
 * The <code>LinkageMode</code> class defines a template for linkage criteria for hierarchical clustering.
 * @author DD
 *
 */
public class LinkageMode
{
	//Construction
	private LinkageMode(final String strName)
	{
		m_strName = strName;
	}
		
	//IMPLEMENTATION SECTION
	public String toString()
	{
		return m_strName;
	}
	
	//DATA SECTION
	
	/**
	* Mean or average linkage clustering
	*/
	public static final LinkageMode AVG = new LinkageMode("Avg");
		
	/**
	 * Minimum or single linkage clustering.
	 */
	public static final LinkageMode MIN = new LinkageMode("Min");
	
	/**
	 * Maximum or complete linkage clustering.
	 */
	public static final LinkageMode MAX = new LinkageMode("Max");
	private final String m_strName;
}
