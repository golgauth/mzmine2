package org.gnf.clustering.hybrid;

/**
 * The <code>DataBubble</code> class provides implementation for a Data Bubble data
 * structure that is used to represent k-means clusters after first phase (k-means clustering)
 * of the Hybrid algorithm. Each data bubble is defined by the following triplet:
 *  - centroid of the cluster;
 *  - radius of the cluster;
 *  - kNN distance within the cluster;
 *  
 * @version 1.0
 */

class DataBubble
{
	float m_fRadius;
	float m_fKNN;
	int m_nBubbleSize;
}
