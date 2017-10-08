package org.gnf.clustering.sequentialcache;

import java.util.*;
import org.gnf.clustering.*;

class Cluster
{
	private MDCache cache;

	private static float max(final float a, final float b)
	{
		return b > a ? b : a;
	}

	private static float min(final float a, final float b)
	{
		return b < a ? b : a;
	}

	private static int max(final int a, final int b)
	{
		return b > a ? b : a;
	}

	private static int min(final int a, final int b)
	{
		return b < a ? b : a;
	}
    
    
	private static float avg(final int aSize, final float aDis, final int bSize, final float bDis)
	{
		return (aSize * aDis + bSize * bDis) / (aSize + bSize);
	}

	private static float avg(final float aDis, final float bDis)
	{
		return (aDis + bDis) / 2;
	}

	void buildCache(final DistanceMatrix mtx,	int n)
	{
		cache = new MDCache(n);
		MinDistanceBean mb = null;
		for(int i = 0; i < n; i++)
		{
			mb = new MinDistanceBean(i);
			Cluster.recalcMin(mtx, mb, n);
			cache.insert(mb);
			//System.out.println("# Cached: " + mb.distance);
		}
//		System.out.println("############ Cache <0>: " + cache.toString());
	}
  
   
	/**
	 * Performs clustering using pairwise maximum- (complete-) linking on the given distance matrix.
	 * 
	 * @param nElemCount
	 *            The number of elements to be clustered.
	 * @param arMtxDist
	 *            The distance matrix, with nelements rows, each row being filled up to the diagonal. The elements on the diagonal are not used, as they are
	 *            assumed to be zero. The distance matrix will be modified by this routine.
     * @return A pointer to a newly allocated array of Node structs, describing the hierarchical clustering solution consisting of nelements-1 nodes. Depending
     *         on whether genes (rows) or microarrays (columns) were clustered, nelements is equal to nrows or ncolumns. See src/cluster.h for a description of
     *         the Node structure.
     */
	public Node[] pmlcluster(final int nElemCount, final DistanceMatrix mtx,	final LinkageMode modeLnk, final ProgressCounter counter)
	{
		if(counter != null)
 		{
 			counter.setOperationName("Creating Clusters...");
 			counter.setTotal(Utils.MTX_LENGTHS[nElemCount - 1]);
 		}
    	
		int j;
      
		final int[] arClusterIds = new int[nElemCount];// malloc(nelements*sizeof(int));
		final int[] arClusterSize = new int[nElemCount];
		Arrays.fill(arClusterSize, 1);
        
		final Node[] arNodes = new Node[nElemCount - 1];
		for(int nNode = 0; nNode < arNodes.length; ++nNode)
   		{
			arNodes[nNode] = new Node(null);
   		}

        // Setup a list specifying to which cluster a gene belongs
        for(j = 0; j < nElemCount; j++)
        {
        	arClusterIds[j] = j;
        }
        
        int is = 0;
        int js = 0;

        //int nIdx = -1;
        //int nIdxTmp = -1;

        //long nTotalFindClosest = 0;
        //long nTotalRest = 0;
        

        for(int n = nElemCount; n > 1; n--)
        {
        	
        	
            //long nStart = System.currentTimeMillis();
            // if (n < 20) {
            // System.out.println("n=" + n);
            // Cluster.printMatrix(arMtxDist, n);
            // }
            // arNodes[nElemCount - n].m_fDistance = this.find_closest_pair(n, arMtxDist, arIs, arJs, nElemCount);
            MinDistanceBean popped = cache.pop();
            
//            System.out.println("!? => popped: " + popped);
            
            arNodes[nElemCount - n].m_fDistance = popped.distance;
            //nTotalFindClosest += System.currentTimeMillis() - nStart;

            // WARN: !!! WTF!!!
            if (arNodes[nElemCount - n].m_fDistance < 0.0) {
                /** throw new RuntimeException(); */
            	System.out.println("!!!! WTF m_fDistance for node: " + arNodes[nElemCount - n]);
            	arNodes[nElemCount - n].m_fDistance = 0.0;
            }
            
            
            //nStart = System.currentTimeMillis();
            is = Cluster.max(popped.principal, popped.remote);
            js = Cluster.min(popped.principal, popped.remote);
            // is = arIs[0];
            // js = arJs[0];

            // System.out.println("diff=" + Cluster.format.format(popped.distance - arNodes[nElemCount - n].m_fDistance) + "is=" + is + " js=" + js + " old = "
            // + arNodes[nElemCount - n].m_fDistance + " new= " + popped);
            // System.out.println(cache);
            // Afx.TRACE();

            // Fix the distances
            float fValue = 0.0f;
            for (j = 0; j < js; j++)
            {
                //nIdx = MCBClstrUtilsNew.MTX_LENGTHS[js - 1] + j;
                //nIdxTmp = MCBClstrUtilsNew.MTX_LENGTHS[is - 1] + j;
                                                
                if(modeLnk == LinkageMode.MAX)
                {
                	fValue= Cluster.max(mtx.getValue(is, j), mtx.getValue(js, j));//   arMtxDist[nIdxTmp], arMtxDist[nIdx]);
                 //arMtxDist[nIdx] = Cluster.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
                }
                 else if(modeLnk == LinkageMode.MIN)
                 {
                 	fValue= Cluster.min(mtx.getValue(is, j), mtx.getValue(js, j));
                  //arMtxDist[nIdx] = Cluster.min(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
                 }
                else if(modeLnk == LinkageMode.AVG)
                	fValue= Cluster.avg(arClusterSize[is], mtx.getValue(is, j), arClusterSize[js], mtx.getValue(js, j));
                 //arMtxDist[nIdx] = Cluster.avg(arClusterSize[is], arMtxDist[nIdxTmp], arClusterSize[js], arMtxDist[nIdx]);
                 
                mtx.setValue(js, j, fValue);
                // arMtxDist[js*nElemCount+j] = Math.max(arMtxDist[is*nElemCount+j], arMtxDist[js*nElemCount+j]);
                
//                System.out.println("[mode link:" + modeLnk + "] " 
//                		+ "avg(" 
//                		+ arClusterSize[is] + ", " + mtx.getValue(is, j) + ", "
//                		+ arClusterSize[js] + ", " + mtx.getValue(js, j)
//                		+ ")=" + Cluster.avg(arClusterSize[is], mtx.getValue(is, j), arClusterSize[js], mtx.getValue(js, j)));
//                System.out.println("(0) !? => set mtx: " + js + "," + j + " = " + fValue);
            }

            for (j = js + 1; j < is; j++)
            {
                //nIdx = MCBClstrUtilsNew.MTX_LENGTHS[j - 1] + js;
                //nIdxTmp = MCBClstrUtilsNew.MTX_LENGTHS[is - 1] + j;
                
                if(modeLnk == LinkageMode.MAX)
                	fValue= Cluster.max(mtx.getValue(is, j), mtx.getValue(j, js));//arMtxDist[nIdx] = Cluster.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
                else if(modeLnk == LinkageMode.MIN)
                 fValue = Cluster.min(mtx.getValue(is, j), mtx.getValue(j, js));
                else if(modeLnk == LinkageMode.AVG)
                	fValue = Cluster.avg(arClusterSize[is], mtx.getValue(is, j), arClusterSize[js],mtx.getValue(j, js));
                
                mtx.setValue(j, js, fValue);
                
//                System.out.println("(1) !? => set mtx: " + js + "," + j + " = " + fValue);
            }

            for (j = is + 1; j < n; j++)
            {
                //nIdx = MCBClstrUtilsNew.MTX_LENGTHS[j - 1] + js;
                //nIdxTmp = MCBClstrUtilsNew.MTX_LENGTHS[j - 1] + is;
                
                if(modeLnk == LinkageMode.MAX)
                 fValue = Cluster.max(mtx.getValue(j, is), mtx.getValue(j, js));//Cluster.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
                else if(modeLnk == LinkageMode.MIN)
                	fValue = Cluster.min(mtx.getValue(j, is), mtx.getValue(j, js));
                else if(modeLnk == LinkageMode.AVG)
                	fValue = Cluster.avg(arClusterSize[is], mtx.getValue(j, is), arClusterSize[js], mtx.getValue(j, js));
                
                mtx.setValue(j, js, fValue);
                //my changes arMtxDist[nIdx] = Cluster.max(arMtxDist[nIdxTmp], arMtxDist[nIdx]);
                // arMtxDist[j*nElemCount+js] = Math.max(arMtxDist[j*nElemCount+is], arMtxDist[j*nElemCount+js]);
                
//                System.out.println("(2) !? => set mtx: " + js + "," + j + " = " + fValue);

            }

            for (j = 0; j < is; j++)
            {
                //nIdx = MCBClstrUtilsNew.MTX_LENGTHS[is - 1] + j;
                //nIdxTmp = MCBClstrUtilsNew.MTX_LENGTHS[n - 1 - 1] + j;
                
                mtx.setValue(is, j, mtx.getValue(n-1, j));
                //////////arMtxDist[nIdx] = arMtxDist[nIdxTmp];
                // arMtxDist[is*nElemCount+j] = arMtxDist[(n-1)*nElemCount+j];
                
//                System.out.println("(3) !? => set mtx: " + js + "," + j + " = " + fValue);

            }

            for (j = is + 1; j < n - 1; j++) {
                //nIdx = MCBClstrUtilsNew.MTX_LENGTHS[j - 1] + is;
                //nIdxTmp = MCBClstrUtilsNew.MTX_LENGTHS[n - 1 - 1] + j;
                //arMtxDist[nIdx] = arMtxDist[nIdxTmp];
                mtx.setValue(j, is, mtx.getValue(n-1, j));
                // arMtxDist[j*nElemCount+is] = arMtxDist[(n-1)*nElemCount+j];
                
//                System.out.println("(4) !? => set mtx: " + js + "," + j + " = " + fValue);

            }

            
//            System.out.println(mtx.toString());
//            
//       	 	System.out.println("############ Cache <1>: " + cache.toString() + " (Elmt: " + nElemCount + ")");
            this.updateCache(n, is, js, mtx, popped);
//            System.out.println("############ Cache <2>: " + cache.toString() + " (Elmt: " + nElemCount + ")");

            // Update clusterids
            arNodes[nElemCount - n].m_nLeft = arClusterIds[is];
            arNodes[nElemCount - n].m_nRight = arClusterIds[js];
            arClusterIds[js] = n - nElemCount - 1;
            arClusterIds[is] = arClusterIds[n - 1];
            arClusterSize[js] += arClusterSize[is];
            
            //nTotalRest += System.currentTimeMillis() - nStart;
            
            if(counter != null)
            {
            	if(counter.isCancelled())
            		return null;
         		
            	counter.increment(n-1, true);
            }
        }
        // free(clusterid);

        //nTotalFindClosest /= 1000;
        //nTotalRest /= 1000;

        return arNodes;
    }

    
    
    //Olga begin
   
    
	public static void recalcMin(final DistanceMatrix mtx,	final MinDistanceBean mb, final int n)
	{
		float min = Float.MAX_VALUE;
		int i, nIdx, fIdx = mb.remote;
        // float dis = Float.MAX_VALUE;
		//boolean up = true;// Math.random() * 2 > 1;
    	float fValue = 0.0f;
        for (i = mb.principal + 1; i < n; i++) 
        {
            nIdx = Utils.MTX_LENGTHS[i - 1] + mb.principal;
            fValue = mtx.getValue(i, mb.principal);
            if(fValue < min)//arMtxDist[nIdx] < min)
            {
                min = fValue;//arMtxDist[nIdx];
                fIdx = i;

            }
        }
        for (i = 0; i < mb.principal; i++) 
        {
            nIdx = Utils.MTX_LENGTHS[mb.principal - 1] + i;
            fValue = mtx.getValue(i, mb.principal);
            if (fValue < min) {
                min = fValue;
                fIdx = i;
            }
        }

        mb.remote = fIdx;
        mb.distance = min;
        // System.out.println("Recalculated min distance between " + mb.principal + " and " + mb.remote + " is " + mb.distance);
    }

    
    
    
    
	void updateCache(final int n, final int is, final int js, final DistanceMatrix mtx,	final MinDistanceBean popped)
	{
		int last = n - 1;

		ArrayList<MinDistanceBean> matches = new ArrayList<MinDistanceBean>();

        MinDistanceBean mb = cache.first;
        MinDistanceBean previous = cache.first;

        // Cluster.printMatrix(arMtxDist, n - 1);
        while (cache.first != null
                        && (cache.first.principal == last || cache.first.remote == last || cache.first.principal == is || cache.first.remote == is
                                        || cache.first.principal == js || cache.first.remote == js))
            if (mb.principal == last) 
            {
                cache.extract(previous, mb);
                previous = cache.first;
                mb = cache.first;

            } 
            else
            {
                if (mb.remote == last) 
                {
                    mb.remote = is;
                }
                if (mb.principal == is || mb.principal == js || mb.remote == is || mb.remote == js)
                {

                    Cluster.recalcMin(mtx, mb, n - 1);
                    cache.extract(previous, mb);
                    matches.add(mb);
                    previous = cache.first;
                    mb = cache.first;
                }

            }
        	if (previous != null) 
        	{
        		while (previous.getNext() != null) 
        		{
        			mb = previous.getNext();
        			if (mb.principal == last) 
        			{
        				cache.extract(previous, mb);
        				continue;
        			} 
        			else if (mb.remote == last) 
        			{
        				mb.remote = is;

        			}
        			if (mb.principal == is || mb.principal == js || mb.remote == is || mb.remote == js) 
        			{

        				Cluster.recalcMin(mtx, mb, n - 1);
        				cache.extract(previous, mb);
        				matches.add(mb);

        			} 
        			else 
        			{
        				previous = previous.getNext();
        			}
                // mb=mb.getNext();
        		}
        	}
        	if (popped.principal != last) 
        	{
        		Cluster.recalcMin(mtx, popped, n - 1);
        		matches.add(popped);
        	}
        	// System.out.println(" recalculated " + matches.size() + " out of " + n + " rows   (" + Cluster.format.format(matches.size() * 100 / n) + "%)");
        	// Static.printInfo("matches: " + matches.size());
        	for (MinDistanceBean minDistanceBean : matches) 
        	{
        		cache.insert(minDistanceBean);
        	}
    	}

 }
