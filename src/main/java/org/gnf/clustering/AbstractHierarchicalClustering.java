package org.gnf.clustering;

/**
 * The <code>AbstractHierarchicalClustering</code> class provides implementation for some common functionality
 * used by implementing clustering services.
 *	@version 1.0
 */
public abstract class AbstractHierarchicalClustering implements HierarchicalClustering 
{
	//Construction	
	/**
	 * This constructor is called by the implementing class constructor to initialize the object with the specified distance metrics calculator
	 * and linkage mode.
	 * @param calculator the distance metrics calculator 
	 * @param mode the linkage mode.
	 * @see org.gnf.clustering.DistanceCalculator
	 * @see org.gnf.clustering.LinkageMode
	 */
	protected AbstractHierarchicalClustering(final DistanceCalculator calculator, final LinkageMode mode)
	{
		setDistanceCalculator(calculator);
		setLinkageMode(mode);
	}
	
	//IMPLEMENTATION SECTION
	public void dispose()
	{
		m_calculator.dispose();
		m_calculator = null;
		
		m_modeLink = null;
	}
	
	public DistanceCalculator getDistanceCalculator()
	{
		return m_calculator;
	}

	public void setDistanceCalculator(final DistanceCalculator calculator)
	{
		if(calculator == null)
		throw new IllegalArgumentException("Distance calculator cannot be null.");
			
		m_calculator = calculator;
	}

	public LinkageMode getLinkageMode() {return m_modeLink;}
	public void setLinkageMode(final LinkageMode mode)
	{
		if(mode == null)
			throw new IllegalArgumentException("Linkage mode cannot be null.");
		
		m_modeLink = mode;
		
	}
	
	//DATA SECTION
	private DistanceCalculator m_calculator;
	private LinkageMode m_modeLink; 
}
