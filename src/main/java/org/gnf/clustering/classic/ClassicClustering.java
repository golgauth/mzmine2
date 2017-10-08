package org.gnf.clustering.classic;

import org.gnf.clustering.*;

/**
 * The <code>ClassicClustering</code> class provides standard implementation of hierarchical clustering. 
 * @author DD
 *
 */
public class ClassicClustering extends AbstractHierarchicalClustering
{
//Construction
	public ClassicClustering(final DistanceCalculator calculator, final LinkageMode mode)
	{
		super(calculator, mode);
	}
	
	public Node[] cluster(final DataSource data, final ProgressCounter counter)	throws Exception
	{
		return ClassicClustering.clusterImpl(data, getDistanceCalculator(),	getLinkageMode(), counter);
	}
	
	private static Node[] clusterImpl(final DataSource data,	final DistanceCalculator calculator,
																																			final LinkageMode modeLnk, final ProgressCounter counter)

	{
		final int nRowCount = data.getRowCount();
		final int nColCount = data.getColCount();

		Node[] arTreeNodes = null;
		final int nElemCount = nRowCount;

		if(nElemCount < 2)
			return null;
		

		//Calculate the distance matrix 
			final DistanceMatrix mtx = Utils.distancematrix(nRowCount, nColCount, data, calculator, counter);
			
			if(counter != null && counter.isCancelled())
				return null;

			if(mtx == null)
				return null; // Insufficient memory

			if(counter != null)
			{
				counter.setOperationName("Creating Clusters...");
				//counter.setMessage("Please Wait...");
			}

			arTreeNodes = ClassicClustering.pmlcluster(nElemCount, mtx);

			if(counter != null && counter.isCancelled())
				return null;

			return arTreeNodes;
	}

/**
* Performs clustering using pairwise maximum- (complete-) linking on the given distance matrix.
* @param nElemCount The number of elements to be clustered.
* @param mtxDist 	The distance matrix, with nelements rows, each row being filled up to the
* diagonal. The elements on the diagonal are not used, as they are assumed to be
* zero. The distance matrix will be modified by this routine.
* @return A pointer to a newly allocated array of Node structs, describing the
* hierarchical clustering solution consisting of nelements-1 nodes. Depending on
* whether genes (rows) or microarrays (columns) were clustered, nelements is
* equal to nrows or ncolumns. See src/cluster.h for a description of the Node
* structure.
*/
	public static Node[] pmlcluster(final int nElemCount, final DistanceMatrix mtxDst)
	{
		float[] arMtxDist = null;
		if(mtxDst instanceof DistanceMatrix1D)
		{
			final DistanceMatrix1D mtx1D = (DistanceMatrix1D)mtxDst;
			arMtxDist = mtx1D.getDstMtxData();
		}
		else
		{
			throw new RuntimeException("To be implemented");
		}


		int j;
		int n;
		final int[] arClusterIds = new int[nElemCount];//malloc(nelements*sizeof(int));
		final Node[] arNodes = new Node[nElemCount-1];
		for(int nNode = 0; nNode < arNodes.length; ++nNode)
				{
					arNodes[nNode] = new Node(null);
				}

		//Setup a list specifying to which cluster a gene belongs
		for(j = 0; j < nElemCount; j++) {arClusterIds[j] = j;}

		final int [] arIs = {1};
		final int [] arJs = {0};
		final int [] arIdxs = {0};
		int is = 0;
		int js = 0;

		int nIdx = -1;
		int nIdxTmp = -1;

		long nTotalFindClosest = 0;
		long nTotalRest = 0;
		float fDistClosest = Float.NaN;

		for(n = nElemCount; n > 1; n--)
				{
					fDistClosest = ClassicClustering.find_closest_pair(n, arMtxDist, arIs, arJs, arIdxs, nElemCount);
					is = arIs[0];
					js = arJs[0];

					arNodes[nElemCount -n].m_fDistance = fDistClosest; 

					if(arNodes[nElemCount-n].m_fDistance < 0.0)
						throw new RuntimeException();

//Fix the distances
					for(j = 0; j < js; j++)
								{
									nIdx 		= Utils.MTX_LENGTHS[js -1] + j;
									nIdxTmp= Utils.MTX_LENGTHS[is -1] + j;
									arMtxDist[nIdx] = Math.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
									//arMtxDist[js*nElemCount+j] = Math.max(arMtxDist[is*nElemCount+j], arMtxDist[js*nElemCount+j]);
								}

					for(j = js+1; j < is; j++)
							{
								nIdx	= Utils.MTX_LENGTHS[j -1] + js;
								nIdxTmp= Utils.MTX_LENGTHS[is-1] + j;
								arMtxDist[nIdx] = Math.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
							}

					for(j = is+1; j < n; j++)
							{
								nIdx 		= Utils.MTX_LENGTHS[j -1] + js;
								nIdxTmp= Utils.MTX_LENGTHS[j -1] + is;
								arMtxDist[nIdx] = Math.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
								//arMtxDist[j*nElemCount+js] = Math.max(arMtxDist[j*nElemCount+is], arMtxDist[j*nElemCount+js]);
							}

					System.arraycopy(arMtxDist, Utils.MTX_LENGTHS[n-1-1], arMtxDist, Utils.MTX_LENGTHS[is -1], is);

					for(j = is+1; j < n-1; j++)
							{
								nIdx 		= Utils.MTX_LENGTHS[j  -1] + is;
								nIdxTmp= Utils.MTX_LENGTHS[n-1-1] + j;
								arMtxDist[nIdx] = arMtxDist[nIdxTmp];
								//arMtxDist[j*nElemCount+is] = arMtxDist[(n-1)*nElemCount+j];
							}

					//Update clusterids
					arNodes[nElemCount-n].m_nLeft = arClusterIds[is];
					arNodes[nElemCount-n].m_nRight= arClusterIds[js];
					arClusterIds[js] = n-nElemCount-1;
					arClusterIds[is] = arClusterIds[n-1];
				}

		nTotalFindClosest/=1000;
		nTotalRest /=1000;

		return arNodes;
	}

/**
* This function searches the distance matrix to find the pair with the shortest
* distance between them. The indices of the pair are returned in ip and jp; the
* distance itself is returned by the function.
* @param nElemCount The number of elements in the distance matrix.
* @param arMtxDist 	A ragged array containing the distance matrix. The number of columns in each
* row is one less than the row index.
* @param ip 	A pointer to the integer that is to receive the first index of the pair with	the shortest distance.
* @param jp A pointer to the integer that is to receive the second index of the pair with	the shortest distance.
* @param N
* @return
*/
	private static	float find_closest_pair(final int nElemCount, float [] arMtxDist, final int[] ip, final int[] jp, final int[] idx, final int N)
	{
		int nIdxMax = 0;	 
		float fTemp = 0;//0.0f;
		float fDistance = arMtxDist[nIdxMax];

		final int nLength = Utils.MTX_LENGTHS[nElemCount-1];
		for(int nIdx = 1; nIdx < nLength; ++nIdx)//1 is ok since above we accounted for 0
				{
					fTemp = arMtxDist[nIdx];
					if(fTemp < fDistance)
					{
						fDistance = fTemp;
						nIdxMax = nIdx;
					}
				}

		final long nTmp = 1L + (8L*(long)nIdxMax);
		ip[0] = 1 + (int)Math.floor((-1 + Math.sqrt(nTmp))/2.0);
		jp[0] = nIdxMax - Utils.MTX_LENGTHS[ip[0]-1];
		idx[0] = nIdxMax;

		return fDistance;
	}
}