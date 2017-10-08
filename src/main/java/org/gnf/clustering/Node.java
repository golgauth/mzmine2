package org.gnf.clustering;

/**
 * The <code>Node</code> class defines an output unit for hierarchical clustering.
 * Instances of this class represent node of actual hierarchical binary tree with references to their children.
 * @author DD
 *
 */
public class Node
{
	public Node(final String strId)
	{
		m_strId = strId;
	}
	
	/**
	 * Constructs a new fully initialized <code>Node</code> object. 
	 * @param nLeft the left child ID.
	 * @param nRight the right child ID.
	 * @param fDist the height of the node. 
	 */
	public Node(final int nLeft, final int nRight, float fDist)
	{
		m_nLeft = nLeft;
		m_nRight = nRight;
		m_fDistance = fDist;
	}
	
	public String toString()
	{
		return m_strId + " {" + m_nLeft + "\t" + m_nRight + "\t" + m_fDistance + "}";
	}
		
	/**
	 * The left child ID.
	 */
	public int m_nLeft;
	/**
	 * The right child ID.
	 */
	public int m_nRight;
	
	/**
	 * The height of the node. 
	 */
	public double m_fDistance = Double.NaN;
	
	/**
	 * The ID of the node.
	 */
	public String m_strId;
	
	public int m_nTermLeft = -1;
	public int m_nTermRight= -1;
}