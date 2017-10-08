package org.gnf.clustering.hybrid;

import java.util.*;
import org.gnf.clustering.*;

/**
 * The <code>HybridClustering</code> class provides implementation for the Hybrid Clustering 
 * Algorithm.
 * The algorithm consists of two main phases:
 *  
 * (1) a partitioning phase, where the data objects are clustered by a k-means clustering
 * algorithm into coarse neighborhoods. Each of the k clusters is represented by a data bubble 
 * structure, defined by the following triplet: 
 * 	- centroid of the cluster;
 *  - radius of the cluster;
 *  - kNN distance within the cluster;
 *  
 * (2) a hierarchical clustering phase, the k centroids are hierarchically clustered into 
 * a coarse tree, as well as to the objects within each of the k clusters into k detailed 
 * trees. By replacing the k centroids in the coarse tree by the corresponding detailed trees, 
 * this two-step hybrid algorithm assembles a complete tree of all the data objects.
 * 
 * The data bubble structure is used to compute distances between the centroids. Rather
 * than simply computing Euclidean distance between the centroids, a more 
 * sophisticated approach is used that takes into account properties of the clusters.
 *
 */
public final class HybridClustering implements HierarchicalClustering
{
	/**
	 * Constructs a new <code>HybridClustering</code> object that is initialized with a hierarchical 
	 * clustering service object and a centroids calculator service object.
	 * @param clustering hierarchical clustering service used as a sub-routine of the Hybrid algorithm,
	 * used in the second phase of the Hybrid algorithm, to cluster the centroids of the partitions
	 * and data object within each partitioning.
	 * 
	 * @param calculatorCtr the centroids calculator service used by the k-means algorithm in the
	 * first phase of the Hybrid algorithm.
	 */
	public HybridClustering(final HierarchicalClustering clustering, final CentroidsCalculator calculatorCtr)
	{
		if(clustering == null)
			throw new IllegalArgumentException("Hierarchical clustering object cannot be null.");

		if(calculatorCtr == null)
			throw new IllegalArgumentException("Centroids calculator object cannot be null.");

		m_clustering = clustering;
		m_calculatorCtr = calculatorCtr;
		m_numPartitions = -1;
	}
	static int depth = 0;

	public void dispose()
	{
		m_clustering.dispose();
		m_clustering = null;
		m_calculatorCtr = null;
	}

	/**
	 * Sets the number of partitions for the first phase of the Hybrid algorithm. 
	 * @param numPartitions the number of partitions for the first phase of the Hybrid algorithm.
	 */

	public void setK(int numPartitions)
	{
		m_numPartitions = numPartitions;
	}


	/**
	 * Returns the user defined number of partitions for the first phase of the Hybrid algorithm.
	 * @return the user defined number of partitions for the first phase of the Hybrid algorithm.
	 */

	public int getK()
	{
		return m_numPartitions;
	}


	/**
	 * Returns true if the user has provided the number of partitions to be used for the first phase of the Hybrid algorithm.
	 * @return true if the user has defined the number of partitions to be used in the first phase of 
	 * the Hybrid algorithm, false otherwise.
	 */

	public boolean isUserDefK()
	{
		if (m_numPartitions <= 0) return false;
		else return true;
	}


	/**
	 * Returns the hierarchical clustering service object used for the second phase of the 
	 * Hybrid algorithm.
	 * @return hierarchical clustering service object used by the Hybrid clustering algorithm.
	 */

	public HierarchicalClustering getHierarchicalClustering()	{return m_clustering;}

	/**
	 * Returns the centroids calculator object used to calculate centroids of the clusters in the 
	 * first phase of the Hybrid algorithm.
	 * @return centroids calculator object.
	 */

	public CentroidsCalculator getCentroidsCalculator() {return m_calculatorCtr;}

	public DistanceCalculator getDistanceCalculator()
	{
		return m_clustering.getDistanceCalculator();
	}


	public void setDistanceCalculator(final DistanceCalculator calculator)
	{
		m_clustering.setDistanceCalculator(calculator);
	}

	public LinkageMode getLinkageMode()
	{
		return m_clustering.getLinkageMode();
	}


	public void setLinkageMode(final LinkageMode mode)
	{
		m_clustering.setLinkageMode(mode);
	}


	public Node [] cluster(final DataSource data, final ProgressCounter counter) throws Exception
	{
		final int nRowCount = data.getRowCount();
		int nKClusterCount = 0;
		if (isUserDefK() && getK() <= nRowCount)
			nKClusterCount = getK();
		else 
			nKClusterCount = (int)(0.25*Math.sqrt(nRowCount));
		if(nKClusterCount == 0)
			nKClusterCount = 1;

		return HybridClustering.clusterImpl(m_clustering,	m_calculatorCtr,	nKClusterCount, 1,	data, counter);
	}

	private static Node[] clusterImpl(final HierarchicalClustering clustering,	final CentroidsCalculator calculatorCtr,
			final int nParamOne, final int nParamTwo,
			final DataSource data, final ProgressCounter counter) throws Exception
	{

		final int MAX_HIER_DATA = 5000;
		final DistanceCalculator calculatorDst = clustering.getDistanceCalculator();		
		final int nRowCount = data.getRowCount();
		final int nColCount = data.getColCount();
		//for (int z = 0; z < depth; z++)
		//	System.out.print(" ");
		//System.out.print("--depth " + depth + " (n: " + nRowCount + ")--\n");

		Node[] arTreeNodes = new Node[nRowCount - 1];
		if(nRowCount < 2)
			return null;

		if(counter != null)
			counter.setOperationName("Creating Clusters...");

		int nCurrNode = 0;
		int nKmeanCount = nParamOne;
		int nKmeanCountFinal = nKmeanCount;

		final int[] arPreCounts = new int[nKmeanCount];

		final DataBubble [] arBubbles = new DataBubble[nKmeanCount];
		for(int nBubble = 0; nBubble < arBubbles.length; nBubble ++)
		{
			arBubbles[nBubble] = new DataBubble();
		}

		final FloatSource1D dataCentroidPre = new FloatSource1D(nKmeanCount, nColCount);								    		
		final int [] arClusterIds = HybridClustering.kmeansClustering(calculatorDst, calculatorCtr, nKmeanCount, nRowCount, nColCount, 	data, 1, arPreCounts, dataCentroidPre, counter);


		final DistanceMatrix mtxSubArDist0 = Utils.distancematrix(nKmeanCount, nColCount, dataCentroidPre, calculatorDst, null);
		final float[] arMinDisat0 = new float[nKmeanCount];//Min distances to a closest cluster

		for(int nKMean = 0; nKMean < nKmeanCount; nKMean++)
		{
			int nElements = arPreCounts[nKMean];//the number of elements in a individual cluster
			if(nElements > MAX_HIER_DATA) 
				nElements = MAX_HIER_DATA;
			final int [] arRows = new int[nElements];
			int rowCounter = 0;
			int j = 0;
			while(j < nRowCount && rowCounter < nElements)
			{
				if(arClusterIds[j] == nKMean)
				{
					arRows[rowCounter] = j; 
					rowCounter ++;
				}
				j++;		
			}		  	
			final DataSource dataSubAr = data.subSource(arRows);
			final DistanceMatrix mtxSubArDist = Utils.distancematrix(nElements, nColCount, dataSubAr, calculatorDst, null);			   			    

			float d = 0.0f;
			float fAvg = 0.0f;
			for(int h = 0; h < nElements; h ++)
			{
				d = calculatorDst.calculate(dataSubAr, dataCentroidPre, h, nKMean);
				fAvg += d;
			}

			float fDistAvg = 0.0f;	
			for(j = 0; j < nElements; j++)
			{
				for(int h = 0; h < j; h++)
				{
					fDistAvg += mtxSubArDist.getValue(j,h);
				}
			}

			arBubbles[nKMean].m_fKNN = nElements == 1 ? 0.0f : (float)( fDistAvg / (nElements * (nElements - 1) / 2.0)); //subArMtxDist.length;// * (nElements - 1));		    					    
			arBubbles[nKMean].m_fRadius	= fAvg / nElements;
			arBubbles[nKMean].m_nBubbleSize = arPreCounts[nKMean];

		}

		HybridClustering.bubbleDistance(nKmeanCount, arBubbles, mtxSubArDist0);

		for(int nKMeanI = 0; nKMeanI < nKmeanCount; nKMeanI++)
		{	
			arMinDisat0[nKMeanI] = Float.MAX_VALUE;
			for(int nKMeanJ = 0; nKMeanJ < nKmeanCount; nKMeanJ++)
			{
				if(nKMeanJ != nKMeanI)
				{
					float d =mtxSubArDist0.getValue(nKMeanI, nKMeanJ);
					if( d < arMinDisat0[nKMeanI])
						arMinDisat0[nKMeanI] = d;
				}
			}
		}

		float d = 0.0f;
		for(int nRow = 0; nRow < nRowCount; nRow++)
		{
			d = calculatorDst.calculate(data, dataCentroidPre, nRow, arClusterIds[nRow]);
			if (d > arMinDisat0[arClusterIds[nRow]] && arPreCounts[arClusterIds[nRow]] > 1) 
			{
				arPreCounts[arClusterIds[nRow]]--;
				arClusterIds[nRow] = nKmeanCountFinal;
				nKmeanCountFinal++;  
			}
		}

		//System.out.println("ctr_total: " + ctr_total);
		final DataSource dataCentroid = data.toEmptySource(nKmeanCountFinal, nColCount);// new MCBNDimFloatSource(nKmeanCountFinal, nColCount);
		dataCentroidPre.copy(0, dataCentroid, 0, nKmeanCount);

		for(int nRow = 0; nRow < nRowCount; nRow++)
		{
			if(arClusterIds[nRow] >= nKmeanCount)
				data.copy(nRow, dataCentroid, arClusterIds[nRow], 1);
		}

		final int[] arCounts = new int[nKmeanCountFinal];
		for(int i = 0; i < nKmeanCount; i++)
		{
			arCounts[i] = arPreCounts[i];
		}

		Arrays.fill(arCounts, nKmeanCount, nKmeanCountFinal, 1);

		int nKmeanCountOriginal = nKmeanCount;

		nKmeanCount = nKmeanCountFinal;


		final int[] arRootIds = new int [nKmeanCount];	

		if(counter != null)
		{
			counter.setOperationName("Creating Clusters...");
			counter.setTotal(nKmeanCount);
		}

		//Hierarchical Clustering for every k-cluster
		int nElements = -1;
		for(int nKM = 0; nKM < nKmeanCount; nKM++)
		{
			long nStart0 = System.currentTimeMillis();
			nElements = arCounts[nKM];

			// Creating a subset of the initial data set, which corresponds to the ith K-Means cluster
			final int[] arOriginalRowIds = new int [nElements];
			final int [] arRows = new int[nElements];
			int rowCounter = 0;
			for(int j = 0; j < nRowCount; j++)
			{
				if(arClusterIds[j] == nKM)
				{
					arRows[rowCounter] = j; 
					arOriginalRowIds[rowCounter] = j;	    	    		
					rowCounter++;
				}
			}


			final DataSource dataSubAr = data.subSource(arRows);
			if(nElements > 1 )
			{
				Node[] subArTreeNodes = null;
				if(nElements <= MAX_HIER_DATA )
				{
					try	{subArTreeNodes = clustering.cluster(dataSubAr, null);}
					catch(Exception ex){throw new RuntimeException("", ex);}
				}
				else
				{
					//depth++;
					int K = Math.min(nKmeanCountOriginal, (int)  Math.sqrt(nElements / 4));    		    		
					subArTreeNodes = HybridClustering.clusterImpl(clustering, calculatorCtr,	K,	nParamTwo, dataSubAr,	null);
					//depth--;
				}

				nStart0 = System.currentTimeMillis();
				int nFirstNode = nCurrNode;			    
				arRootIds[nKM] = -subArTreeNodes.length - nCurrNode;
				for( int nNode = 0; nNode < subArTreeNodes.length; ++nNode)
				{					
					arTreeNodes[nCurrNode] = subArTreeNodes[nNode];
					if(arTreeNodes[nCurrNode].m_nLeft >= 0) 
						arTreeNodes[nCurrNode].m_nLeft = arOriginalRowIds[arTreeNodes[nCurrNode].m_nLeft];
					else
						arTreeNodes[nCurrNode].m_nLeft -=  nFirstNode;
					if(arTreeNodes[nCurrNode].m_nRight >= 0) 
						arTreeNodes[nCurrNode].m_nRight = arOriginalRowIds[arTreeNodes[nCurrNode].m_nRight];
					else
						arTreeNodes[nCurrNode].m_nRight -=  nFirstNode;
					nCurrNode ++;					
				}	
			}			    
			else if(nElements == 1)		    			    
				arRootIds[nKM] = arOriginalRowIds[0];

			if(counter != null)
				counter.increment(1, true);
		}				

		//Centroids Clustering (II level)

		//	for (int z = 0; z < depth; z++)
		//		System.out.print(" ");
		//	System.out.print("--depth " + depth + " over--\n");

		Node[] subArTreeNodes = null; 

		//	for (int z = 0; z < depth; z++)
		//		System.out.print(" ");
		//	System.out.print("centroids\n");
		if(nKmeanCount < MAX_HIER_DATA)
		{
			try	{subArTreeNodes = clustering.cluster(dataCentroid, null);}
			catch(Exception ex){throw new RuntimeException("", ex);}
		}
		else
		{
			int K = (int) Math.sqrt(nKmeanCount / 4); 

			//depth ++;

			subArTreeNodes = HybridClustering.clusterImpl(clustering, calculatorCtr,	K,	nParamTwo, dataCentroid,	counter); 						
			// depth--;
		}


		// Write hierarchical clustering results on K-Means Clusters into the combined hierarchical tree
		double a = 0;
		double b = 0;
		double c = 0;

		int nFirstNode = nCurrNode;		    		    
		for(int nNode = 0; nNode < (subArTreeNodes == null ? 0 : subArTreeNodes.length); ++nNode)
		{	
			arTreeNodes[nCurrNode] = subArTreeNodes[nNode];

			if(arTreeNodes[nCurrNode].m_nLeft >= 0) 
				arTreeNodes[nCurrNode].m_nLeft = arRootIds[arTreeNodes[nCurrNode].m_nLeft];
			else
				arTreeNodes[nCurrNode].m_nLeft -=  nFirstNode;
			if(arTreeNodes[nCurrNode].m_nRight >= 0) 
				arTreeNodes[nCurrNode].m_nRight = arRootIds[arTreeNodes[nCurrNode].m_nRight];
			else
				arTreeNodes[nCurrNode].m_nRight -=  nFirstNode;

			a = 0;
			b = 0;
			c = 0;
			if(arTreeNodes[nCurrNode].m_nLeft < 0) a = arTreeNodes[-arTreeNodes[nCurrNode].m_nLeft - 1].m_fDistance;
			if(arTreeNodes[nCurrNode].m_nRight < 0) b = arTreeNodes[-arTreeNodes[nCurrNode].m_nRight-1].m_fDistance;
			c = arTreeNodes[nCurrNode].m_fDistance;

			arTreeNodes[nCurrNode].m_fDistance = Math.max(Math.max(a,b), c);
			nCurrNode++;				
		}	

		if(counter != null && counter.isCancelled())
			return null;

		return arTreeNodes;	   		
	}

	private static void bubbleDistance(final int nRowCount, final DataBubble[] arDataBubbles, final DistanceMatrix mtxSubArDist)
	{
		float fDist = 0.0f;
		float fE1 = 0.0f;
		float fE2 = 0.0f;
		float fNNDist1 = 0.0f;
		float fNNDist2 = 0.0f;
		for(int nRow = 1; nRow < nRowCount; ++nRow)
		{	            
			for(int nCol = 0; nCol < nRow; ++nCol) 
			{

				fDist = mtxSubArDist.getValue(nRow,nCol);
				fE1 = arDataBubbles[nRow].m_fRadius; //arDataBubble[i * 3 + 1];
				fE2 = arDataBubbles[nCol].m_fRadius;//arDataBubble[j * 3 + 1];
				fNNDist1 = arDataBubbles[nRow].m_fKNN;//arDataBubble[i * 3 + 2];
				fNNDist2 = arDataBubbles[nCol].m_fKNN;//arDataBubble[j * 3 + 2];
				if(fDist - (fE1 + fE2) >= 0)
					fDist = fDist - (fE1 + fE2) + fNNDist1 + fNNDist2;
				else
					fDist = Math.max(fNNDist1, fNNDist2);

				mtxSubArDist.setValue(nRow,nCol,fDist);
			}	                
		}          
	}

	//This is an inoptimal algorithm to generate a binomial random number, it's O(n). 
	//Taken from http://stackoverflow.com/questions/1241555/algorithm-to-generate-poisson-and-binomial-random-numbers
	//Reference:  chapter 10 of Non-Uniform Random Variate Generation (PDF) by Luc Devroye
	// A better algorithm is in the C-code of the cluster library
	private static int getBinomial(int n, double p) 
	{   
		final Random r = new Random(0);
		int x = 0;   
		for(int i = 0; i < n; i++)    
		{
			if(r.nextDouble() < p)       
				x++;
		}
		return x;
	} 

	//A faster algorithm needs to be implemented. For now getBinomial routine is used
	private static int binomial(int n, double p) 
	{
		return HybridClustering.getBinomial(n, p);
	}

	private static void randomAssign(final int nClusterCount, final int nElemCount, final int[] clusterId)
	{
		int i, j;
		int k = 0;
		double p;
		int n = nElemCount - nClusterCount;

		for(i = 0; i < nClusterCount - 1; i++)
		{
			p = 1.0 / (nClusterCount - i);
			j = HybridClustering.binomial(n, p);
			n -= j;
			j += k + 1; 

			for(; k < j; k++) {
				clusterId[k] = i;

			}
		}

		for( ; k < nElemCount; k++) {clusterId[k] = i;}

		for (i = 0; i < nElemCount; i++)
		{ 
			j = (int) (i + (nElemCount - i) * new Random(0).nextDouble());
			k = clusterId[j];
			clusterId[j] = clusterId[i];
			clusterId[i] = k;
		}
	}

	private static int[] kmeansClustering(final DistanceCalculator calculatorDst,
			final CentroidsCalculator calculatorCtr,
			final int nClusterCount, final int nRowCount, final int nColCount, final DataSource data,
			final int nPass, final int[] arCounts, 
			final FloatSource1D dataCentr, final ProgressCounter counter)
	{
		int[] clusterId = new int[nRowCount];
		double clusterError = 0;
		final int[] tClusterId = new int[nRowCount];

		final int[] mapping = new int[nClusterCount * nClusterCount];

		try{clusterId = HybridClustering.kmeansFast2(calculatorDst, calculatorCtr, nClusterCount, nRowCount, nColCount, data, 
				nPass, dataCentr, clusterId, clusterError, tClusterId, arCounts, mapping, counter);}
		catch(Exception ex)
		{
			throw new RuntimeException("", ex);
		}
		return clusterId;
	}


	private static final int[] kmeansFast2(final DistanceCalculator calculatorDst,	final CentroidsCalculator calculatorCtr,
			final int nClusterCount, final int nRowCount, final int nColCount, final DataSource data,
			final int nPass,  final FloatSource1D dataCentr,
			final int[] arClusterIds, final double fClusterError, final int[] tClusterId, final int[] arCounts, final int[] mapping,
			final ProgressCounter counter) throws Exception
	{
		int i, j, k;
		int iter = 0;
		int iFound = 1;
		int iPass = 0;	
		float shiftSum = 0;
		double[] saved = new double[nRowCount];
		final float[] arUpperCurrent = new float[nRowCount];
		final float[][] arLowerPossible = new float[nRowCount ][ nClusterCount];
		//for (int z = 0; z < depth; z++)
		//	System.out.print(" ");
		// System.out.print(nRowCount + " " + nClusterCount + "\n");
		double error = Double.MAX_VALUE;

		DataSource dataCentrPrev = null;

		do
		{ 
			double total = Double.MAX_VALUE;
			int nCounter = 0;
			int period = 10;

			if(nPass != 0)
				HybridClustering.randomAssign(nClusterCount, nRowCount, tClusterId);

			Arrays.fill(arCounts, 0, nClusterCount, 0);

			for(i = 0; i < nRowCount; i++)
			{
				arCounts[tClusterId[i]]++;
			}		      		    

			int z = 0;
			while(true)	    	 		    	  
			{
				if(counter != null)
				{
					counter.setOperationName("Performing K-Means iteration... " + (nCounter +1));
					counter.setTotal(1);
				}

				long start=System.currentTimeMillis();	
				double previous = total;
				total = 0.0;
				if(nCounter % period == 0) 		          
				{ 
					for(i = 0; i < nRowCount; i++) saved[i] = tClusterId[i];
					if(period < Integer.MAX_VALUE / 2) period *= 2;
				}

				dataCentrPrev = dataCentr.subSource(0, nClusterCount-1);
				calculatorCtr.calculateCentroids(nClusterCount, nRowCount, nColCount, data, tClusterId, dataCentr, arCounts, counter);

				final float centrShift[] = new float [nClusterCount];
				if(iter != 0)
				{
					for(int nCluster = 0; nCluster < nClusterCount; nCluster++)
					{
						centrShift[nCluster] = calculatorDst.calculate(dataCentrPrev, dataCentr, nCluster, nCluster);		
					}
				}
				shiftSum = 0;

				for(int h = 0; h < nClusterCount; h++)
				{
					shiftSum += centrShift[h];
				}

				final DistanceMatrix centrDistMtx = Utils.distancematrix(nClusterCount, nColCount, dataCentr, calculatorDst, null);

				float fDistCentr = 0.0f;
				float fDistance = 0.0f;
				float fMinLowerPossible = 0.0f;
				for(i = 0; i < nRowCount; i++)  
				{
					k = tClusterId[i];
					if(arCounts[k] == 1)
						continue;				              

					fMinLowerPossible = Float.MAX_VALUE;
					if(iter != 0)
					{
						arUpperCurrent[i] += centrShift[k];		              
						for(j = 0; j < nClusterCount; j++)
						{
							arLowerPossible[i][j] = Math.max(0, arLowerPossible[i][j] - centrShift[j]);
							if(arLowerPossible[i][j] < fMinLowerPossible) 
								fMinLowerPossible = arLowerPossible[i][j];		  
						}	
					}

					fDistance = calculatorDst.calculate(data, dataCentr,	i, k);

					arUpperCurrent[i] = fDistance;
					if(iter  == 0 || fMinLowerPossible < arUpperCurrent[i])
					{

						for(j = 0; j < nClusterCount; j++)
						{ 
							float tDistance;
							if(j == k)
								continue;

							if(iter == 0 || arLowerPossible[i][j] < arUpperCurrent[i])
							{
								fDistCentr = centrDistMtx.getValue(Math.max(j, tClusterId[i]), Math.min(j, tClusterId[i])); 
								if(fDistCentr < 2 * fDistance)
								{
									tDistance = calculatorDst.calculate(data, dataCentr, i, j);
									arLowerPossible[i][j] = tDistance;
									if(tDistance <= fDistance)// && counts[j] <= 10000)	            				  //  if ((tDistance == distance && (float)Math.random() > 0.7) || tDistance < distance)	  
									{
										fDistance = tDistance;
										arCounts[tClusterId[i]]--;
										tClusterId[i] = j;
										arUpperCurrent[i] = fDistance;
										arLowerPossible[i][j] = fDistance;
										arCounts[j]++;
									} 
								}
								else
								{
									arLowerPossible[i][j] = Math.max(0, fDistCentr - fDistance);
									z++;
								}  
							} 
							else z++;
						}
					}
					else z += nClusterCount;

					total += arUpperCurrent[i];

					if(counter != null)
					{
						counter.setMessage("Updating Clusters " + (int)((((float)(i+1))/nRowCount*100)) + "%");
					}
				}

				z = 0;
				iter ++;
				if(iter > 1 && shiftSum < 0.0001)
					break;

				for(i = 0; i < nRowCount; i++)
				{
					if(saved[i] != tClusterId[i])
						break;
				}

				if(i == nRowCount)
					break;     

				++nCounter;
				if(counter != null)
				{
					counter.complete();
				}

			}

			if(counter != null)
			{
				counter.setMessage("");
				counter.complete();
			}

			if(nPass <= 1)
			{ 
				error = total;
				return tClusterId;
			}

			for(i = 0; i < nClusterCount; i++)
			{
				mapping[i] = -1;
			}

			for(i = 0; i < nRowCount; i++)
			{
				j = tClusterId[i];
				k = arClusterIds[i];
				if(mapping[k] == -1)
					mapping[k] = j;
				else if (mapping[k] != j)
				{ 
					if(total < error)
					{ 
						iFound = 1;
						error = total;
						for(j = 0; j < nRowCount; j++)
						{
							arClusterIds[j] = tClusterId[j];
						}
					}
					break;
				}
			}
			if(i == nRowCount)
				iFound++; 

		} while(++iPass < nPass);
		return arClusterIds;
	}

	private HierarchicalClustering m_clustering;
	private CentroidsCalculator m_calculatorCtr;
	private int m_numPartitions;
}
