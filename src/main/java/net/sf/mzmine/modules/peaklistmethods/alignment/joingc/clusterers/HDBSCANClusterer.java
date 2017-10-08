/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.Constraint;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.OutlierScore;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.hdbscanstar.Constraint.CONSTRAINT_TYPE;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.ClusteringProgression;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;



public class HDBSCANClusterer /*implements ClusteringAlgorithm*/ {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String MODULE_NAME = "HDBSCAN* clusterer";

	private double[][] distMtx;
	private Instances dataSet;

//	//private DBSCANWithProgress clusterer;
//	HDBSCANLinearMemory clusterer;

//	private List<Cluster> clusters;
	
	
	private ClusteringProgression clustProgress;


	public static String NEWICK_LEAF_NAME_PREFIX = "n"; //"node_" // "TreeParser" need a String as name (cannot go with only number Id) 

	////private HashMap<Instance, Integer> instanceRowIndexMap;

	//    @Override
	//    public @Nonnull String getName() {
	//	return MODULE_NAME;
	//    }
	
	
	private int k;
	private int minClusterSize;
	private boolean use_constraints;
	private boolean self_edges;
	
	private int clusterSizeThreshold;
	private final static boolean VERBOSE = true;
	
	private int[] pointLastClusterIndexes;
	private int[] flatPartitioning;
	//private int[] flatPartitioning2;
	Map<Integer, List<Integer>> flatPartitioningMap;
	
	
	public HDBSCANClusterer(ClusteringProgression clustProgress, double[][] rawData, 
			int k, int minClusterSize, boolean use_constraints, boolean self_edges, int clusterSizeThreshold) {

		this.distMtx = rawData;
		//		this.dataSet = createSampleWekaDataset(rawData);

		this.clustProgress = clustProgress;
		
		this.k = k;
		this.minClusterSize = minClusterSize;
		
		this.use_constraints = use_constraints;
		this.self_edges = self_edges;
		
		this.clusterSizeThreshold = clusterSizeThreshold;
	}


	//@Override
	public ClusteringResult performClustering() {


		try {

			// MEMORY STUFF
			Runtime run_time = Runtime.getRuntime();
			Long prevTotal = 0l;
			Long prevFree = run_time.freeMemory();

			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (before...)");
			
			// Read in input file:
			double[][] dataSet = new double[distMtx.length][1];
			for (int i = 0; i < distMtx.length; i++) {					
				dataSet[i] = new double[] { i };
			}
			int numPoints = dataSet.length;
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: data set!");
			
			// Read in constraints:
			ArrayList<Constraint> constraints = null;
			// TODO: Do this in same pass than building dataset above
			if (this.use_constraints) {
				
				constraints = new ArrayList<>();
				
				for (int i = 0; i < numPoints; i++) {			
					
					for (int j = i; j < numPoints; j++) {
						
						if (distMtx[i][j] >= 1.0 || i == j) { // || i == j /* Should never happen */) {
							
							constraints.add(new Constraint(i, j, CONSTRAINT_TYPE.CANNOT_LINK));
						}
					}
	
				}
				
				System.out.println("NB contraints: " + constraints.size());
			}
			
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: constraints!");
			
			// Compute core distances:
			long startTime = System.currentTimeMillis();
//			double[] coreDistances = HDBSCANStar.calculateCoreDistances(dataSet, parameters.minPoints, parameters.distanceFunction);
			DistanceCalculator distanceFunction = new HDBSCANStarDistanceFunction();
			double[] coreDistances = HDBSCANStar.calculateCoreDistances(dataSet, this.k, distanceFunction);
			System.out.println("Time to compute core distances (ms): " + (System.currentTimeMillis() - startTime));
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: core distances!");
			
			// Calculate minimum spanning tree:
			
			startTime = System.currentTimeMillis();
//			UndirectedGraph mst = HDBSCANStar.constructMST(dataSet, coreDistances, true, parameters.distanceFunction);
			UndirectedGraph mst = HDBSCANStar.constructMST(dataSet, coreDistances, this.self_edges, distanceFunction);
			mst.quicksortByEdgeWeight();
			System.out.println("Time to calculate MST (ms): " + (System.currentTimeMillis() - startTime));
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: min spanning tree!");

			//Remove references to unneeded objects:
			dataSet = null;
			
			double[] pointNoiseLevels = new double[numPoints];
			int[] pointLastClusters = new int[numPoints];

			String hierarchyFilename = "_hierarchy.csv";
			
			//Compute hierarchy and cluster tree:
			ArrayList<Cluster> clusters = null;
			try {
				startTime = System.currentTimeMillis();
//				clusters = HDBSCANStar.computeHierarchyAndClusterTree(mst, parameters.minClusterSize, 
//						parameters.compactHierarchy, constraints, parameters.hierarchyFile, 
//						parameters.clusterTreeFile, ",", pointNoiseLevels, pointLastClusters, parameters.visualizationFile);
				clusters = HDBSCANStar.computeHierarchyAndClusterTreeGLG(mst, this.minClusterSize, 
						true /* for performance */, constraints, hierarchyFilename, 
						"_tree.csv", ",", pointNoiseLevels, pointLastClusters, "_visulization.vis");
				System.out.println("Time to compute hierarchy and cluster tree (ms): " + (System.currentTimeMillis() - startTime));
				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: hierarchy!");
				
				this.pointLastClusterIndexes = pointLastClusters;
			}
			catch (IOException ioe) {
				System.err.println("Error writing to hierarchy file or cluster tree file.");
				System.exit(-1);
			}

			//Remove references to unneeded objects:
			mst = null;
			
			//Propagate clusters:
			boolean infiniteStability = HDBSCANStar.propagateTree(clusters);

			//Compute final flat partitioning:
			int[] flatPartitioning = null;
			try {
				startTime = System.currentTimeMillis();
				flatPartitioning = HDBSCANStar.findProminentClusters(clusters, hierarchyFilename, "_partition.csv", 
						",", numPoints, infiniteStability);
				System.out.println("Time to find flat result (ms): " + (System.currentTimeMillis() - startTime));
				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: partition!");
				
				this.flatPartitioning = flatPartitioning;
			}
			catch (IOException ioe) {
				System.err.println("Error writing to partitioning file.");
				System.exit(-1);
			}
//			//Compute final flat partitioning:
//			int[] flatPartitioning2 = null;
//			try {
//				startTime = System.currentTimeMillis();
//				flatPartitioning2 = HDBSCANStar.findProminentClusters(clusters, hierarchyFilename, "_partition2.csv", 
//						",", numPoints, infiniteStability);
//				System.out.println("Time to find flat '2' result (ms): " + (System.currentTimeMillis() - startTime));
//				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: partition2!");
//				
//				this.flatPartitioning2 = flatPartitioning2;
//			}
//			catch (IOException ioe) {
//				System.err.println("Error writing to partitioning file.");
//				System.exit(-1);
//			}
			
			//Compute outlier scores for each point:
			List<OutlierScore> outliers = null;
			try {
				startTime = System.currentTimeMillis();
				outliers = HDBSCANStar.calculateOutlierScores(clusters, pointNoiseLevels, pointLastClusters, 
						coreDistances, "_outlier_scores.csv", ",", infiniteStability);
				System.out.println("Time to compute outlier scores (ms): " + (System.currentTimeMillis() - startTime));
				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: outlier scores!");
			}
			catch (IOException ioe) {
				System.err.println("Error writing to outlier score file.");
				System.exit(-1);
			}
			
			//System.out.println("Overall runtime (ms): " + (System.currentTimeMillis() - overallStartTime));

    		// Few stats
    		System.out.println("######### STATS #########");
    		System.out.println("###### > Found '" + clusters.size() + "' clusters:");
    		
    		
    		List<Cluster> real_clusters = selectClustersFromHierarchy(clusters, clusterSizeThreshold, true, VERBOSE, false);
    		int num_c = real_clusters.size();
    		
//    		List<Integer> leaf_clust_ids = new ArrayList<>();
//    		for (Cluster cl: real_clusters) {
//    			for (Cluster cl0: cl.getPropagatedDescendants())
//    				leaf_clust_ids.add(cl0.getLabel());
//    		}
//    		
//    		//--
//    		// Split incompatible ones
//    		System.out.println("###### > ASS KICKING:");
//    		List<Cluster> to_removed = new ArrayList<>();
//    		List<Cluster> splitted = new ArrayList<>();
//    		for (Cluster cl: real_clusters) {
//				
//    			
//    			System.out.println("\t" + cl.getLabel() + " -> " 
//    					+ cl.getBirthLevel() + " | " + cl.getDeathLevel() + " | " 
//    					+ cl.getStability() + " | " + cl.getNumConstraintsSatisfied());
//    			
//				float max_dist = Float.MIN_VALUE;
//    			
//    			// Search for false cluster
//				if (cl.getStability() < 1.0d) {
//
//					to_removed.add(cl);
//					
//					
//					int parent_id = cl.getPropagatedDescendants().get(0).getParent().getLabel();
//					
//					for (int k = 1; k < cl.getPropagatedDescendants().size(); k++) {
//
//						int cur_parent_id = cl.getPropagatedDescendants().get(k).getParent().getLabel();
//						
//	//    				cl0.
//
//	//    				int i = getDataSetIndexFromClusterIndex(cl0);
//	//    				int j = ;
//	//    				
//	//    				float dist = this.distMtx[i][j];
//	//    				
//	//    				if (dist > max_dist) {
//	//    					max_dist = dist;
//	//    				}
//
//						if (parent_id == cur_parent_id) {
//							
//							// Outsider
//							splitted.add(cl.getPropagatedDescendants().get(k).getParent());
//						} else {
//							
//							splitted.add(cl.getPropagatedDescendants().get(k));
//						}
//
//					}
//				}
//				
////    			if (max_dist > 1.0d) {
////    				
////    			}
//    		}
//    		
////    		// FIX ALL
////    		for (Cluster cl: to_removed) {
////    			
////    			real_clusters.remove(cl);
////    		}
////    		for (Cluster cl: splitted) {
////    			
////    			real_clusters.add(cl);
////    		}
//    		
//    		
//    		
//    		
//    		//--
//    		// Recover singles
//    		Set<Integer> leaves_label_set = new HashSet<>();
//    		int num_l = 0;
//    		for (Cluster cl: clusters) {
//
//				if (cl == null) { continue; }
//
//    			for (Cluster cl0: cl.getPropagatedDescendants()) {
//
//	    			if (!cl0.hasChildren() && leaf_clust_ids.indexOf(cl0.getLabel()) == -1) {
//	    				
//	    				if (VERBOSE) { System.out.println("# Recovered single: " + cl0.getLabel()); }
//	    				
//	    				leaf_clust_ids.add(cl0.getLabel());
//	    				
//	    				real_clusters.add(cl0);
//	    				num_l++;
//	    			}
//	    			
//	    			if (VERBOSE) { leaves_label_set.add(cl0.getLabel()); }
//    			}
//    		}
//    		
//    		// Stats
//    		System.out.println("FOUND '" + (num_c + num_l) + "' REAL VALID clusters (" + num_c + " 'composed' + " + num_l + " 'singles').");
//    		System.out.println("Treated '" + leaf_clust_ids.size() 
//    			+ ((VERBOSE) ? " / " + leaves_label_set.size() : "") 
//    			+ " / " + numPoints + " leaves.");
////    		// Checks
////    		if (leaf_clust_ids.size() != numPoints || (VERBOSE && leaves_label_set.size() != numPoints)) {
////    			throw new IllegalStateException(this.getClass().getSimpleName() + ": Clustering failed - with improper number of points in cluster.");
////    		}
    		
			ClusteringResult<Cluster> result = new ClusteringResult<>(
					real_clusters, //clusters, //null,
					null, //clusterer.toString(), 
					clusters.size(), //numberOfClusters(), 
					null //VisualizationType.PCA //null
					);


			if (VERBOSE) {
				// Mapping Cluster leaf <=> dataSet entry
				System.out.println("\n## FINAL MAPPING:");
//				//System.out.println(Arrays.toString(pointLastClusters));
//	    		Set<Integer> dataSet_ids_set = new HashSet<>();
//	    		int cunt = 0;
//				for (Cluster cl: real_clusters) {
//
//					String str = "";
//					String str_stab = "";
//
//					// If cluster is a leaf, store it as is
//					if (HDBSCANClusterer.isLeaf(cl)) {
//						
//						int dataSet_id = this.getDataSetIndexFromClusterIndex(cl.getLabel());
//						if (dataSet_id != -1) {
//							str += dataSet_id + " ";
//							str_stab += cl.getStability();
//							
//							dataSet_ids_set.add(dataSet_id);
//							cunt++;
//						}
//					} 
//					// Otherwise, find child leaves
//					else {
//						for (Cluster cl0: cl.getPropagatedDescendants()) {
//	
//							int dataSet_id = this.getDataSetIndexFromClusterIndex(cl0.getLabel());
//							if (dataSet_id != -1) {
//								str += dataSet_id + " ";
//								str_stab += cl0.getParent().getStability() + " ";
//								
//								dataSet_ids_set.add(dataSet_id);
//								cunt++;
//							}
//							
//							if (dataSet_id == -1)
//								System.out.println("Couldn't find a mapping for cluster: " + cl0.getLabel() );
//						}
//					}
//
//					String info =  " -> " 
//	    					+ cl.getBirthLevel() + " | " + cl.getDeathLevel() + " | " 
//	    					+ cl.getStability() + " | " + cl.getNumConstraintsSatisfied();
//					String info2 = " -> " 
//	    					+ ((cl.getStability() <= 1.0d) ? str_stab : "");
//					System.out.println("[ " + str + "]" + info + info2);
//				}
//				
//				System.out.println(">>>> Mapped " + cunt + " | " + dataSet_ids_set.size() + ":\n" + dataSet_ids_set.toString());
				
				
//				//-------------
//				System.out.println(">>>> Flat array: " + Arrays.toString(flatPartitioning));
//				for (int i = 0; i < flatPartitioning.length; i++) {
//					
//					// Recover cluster of interest
//					int cl_lbl = flatPartitioning[i];
//					//System.out.println("Looking for cl_lbl: " + cl_lbl);
//					//Take the list of propagated clusters from the root cluster:
////					ArrayList<Cluster> solution = clusters.get(1).getPropagatedDescendants();
//					for (Cluster cl: clusters) {
//						//for (Cluster cl: cluster.getPropagatedDescendants()) {
//
//						if (cl == null) { continue; }
//						
//							//System.out.println("Looking for cl_lbl: " + cl_lbl + " - " + cl.getLabel());
//							if (cl.getLabel() == cl_lbl) {
//
//								String str = "";
//
//								if (isLeaf(cl)) {
//									str += getDataSetIndexFromClusterIndex(cl.getLabel()) + " ";
//								} else
//									for (Cluster cl00: cl.getPropagatedDescendants()) {
//										str += getDataSetIndexFromClusterIndex(cl00.getLabel()) + " ";
//									}
//
//								System.out.println("Hmmm... Interesting >> [ " + str + "]");
//							}
//						//}
//					}
//				}
				
				
				
				boolean fix_outliers = false;
				String outlierChar = "^";
				double outlierThreshold = JoinAlignerGCTask.EPSILON;//0.5d;
				int maxClusterSize = 3;
				
				List<OutlierScore> outliersSorted = new ArrayList<>(outliers);
				Collections.sort(outliersSorted, new IdComparatorForOutlierScore());
				
				Map<Integer, List<Integer>> map = new HashMap<>();
				List<Integer> outliers_3 = new ArrayList<>();
				for (int i = 0; i < flatPartitioning.length; i++) {
					
					int cl_lbl = flatPartitioning[i];
					
					//if (cl_lbl == 0) { continue; } // Skip noise!
					
					if (map.containsKey(cl_lbl)) {
						map.get(cl_lbl).add(i);
					} else {
						List<Integer> list = new ArrayList<>();
						list.add(i);
						map.put(cl_lbl, list);
					}
					
					//int peak_id = getDataSetIndexFromClusterIndex(cl_lbl);
				}
				for (Entry<Integer, List<Integer>> entry: map.entrySet()) {
					
					int key = entry.getKey();
					List<Integer> lst = entry.getValue();
					
					int lst_size = lst.size();
					
					String str = "";
					
					List<Integer> outliers_2 = new ArrayList<>();
					
					boolean has_outliers = false;
					for (Integer i: lst) {
						
						boolean is_outlier = (outliersSorted.get(i).getScore() > outlierThreshold && lst.size() <= maxClusterSize);
						
						str += i + ((is_outlier) ? outlierChar : "") + " "; // Staring suspected outliers
						if (is_outlier && fix_outliers) {
							outliers_2.add(i);
							outliers_3.add(i);
						}
						
						if (is_outlier)
							has_outliers = true;
					}
					if (fix_outliers) {
						// Exclude outliers
						for (int i = outliers_2.size()-1; i >= 0; i--) {
							lst.remove(outliers_2.get(i));
						}
					}
					
					if (key == 0)
						System.out.println("#" + lst_size + " singles: [ " + str + "] - (" + key + ")");
					else
						System.out.println("!! [ " + str + "] - (" + key + ")");

					
					
					has_outliers = (has_outliers || lst.size() > maxClusterSize);
					// If cluster's content ain't reliable, display its local hierarchy
					if (has_outliers) {
						
						ArrayList<Cluster> solution = clusters.get(1).getPropagatedDescendants();
						
						//Cluster weird_clust = clusters.get(key);
						String indent = "    ";
						String str_hierarchy = "";
						//					for (Cluster cl: weird_clust.getPropagatedDescendants()) {
						//						str_hierarchy += cl.getLabel();
						//					}
						BufferedReader reader = new BufferedReader(new FileReader("_hierarchy.csv"));
						//int[] flatPartitioning2 = new int[numPoints];
						String str_hier = "";
						long currentOffset = 0;
						
						//Store all the file offsets at which to find the birth points for the flat clustering:
						TreeMap<Long, ArrayList<Integer>> significantFileOffsets = new TreeMap<Long, ArrayList<Integer>>();
						
						for (Cluster cluster: solution) {
							ArrayList<Integer> clusterList = significantFileOffsets.get(cluster.getFileOffset());
	
							if (clusterList == null) {
								clusterList = new ArrayList<Integer>();
								significantFileOffsets.put(cluster.getFileOffset(), clusterList);
							}
	
							if (cluster.getLabel() == key) {
								
								clusterList.add(cluster.getLabel());
								break;
							}
						}
						
						//Go through the hierarchy file, setting labels for the flat clustering:
						while (!significantFileOffsets.isEmpty()) {
							Map.Entry<Long, ArrayList<Integer>> entry2 = significantFileOffsets.pollFirstEntry();
							ArrayList<Integer> clusterList2 = entry2.getValue();
							Long offset = entry2.getKey();

							reader.skip(offset - currentOffset);
							String line = reader.readLine();

							currentOffset = offset + line.length() + 1;
							String[] lineContents = line.split(",");
							
							for (int i = 1; i < lineContents.length; i++) {
								int label = Integer.parseInt(lineContents[i]);

								if (clusterList2.contains(label)) {
									//flatPartitioning2[i-1] = label;
									str_hier += "[id: " + (i-1) + ", noiseLvl:" + pointNoiseLevels[i-1] + ", lastLbl:" + pointLastClusters[i-1] + ", lvl:" + lineContents[0] + "]" + " ";
								}
							}
						}
//						System.out.println("#FUCK_UP clust: " + Arrays.toString(flatPartitioning2));
						System.out.println("#FUCK_UP!    " + str_hier);
					}
					
					
				}
				System.out.println("#" + (map.size() - 1) + " composites.");
				//-
//				if (fix_outliers) {
//					// Re-map outliers as singles (put them at key 'O', i.e. consider them as noise!)
//					map.get(0).addAll(outliers_3);
//					List<Integer> lst0 = map.get(0);
//					String str0 = "";
//					for (Integer i: lst0) {
//						str0 += i + ((outliersSorted.get(i).getScore() > 0.5d) ? outlierChar : "") + " "; // Staring suspected outliers
//					}
//					// Print again
//					for (Entry<Integer, List<Integer>> entry: map.entrySet()) {
//						
//						int key = entry.getKey();
//						List<Integer> lst = entry.getValue();
//						
//						int lst_size = lst.size();
//						
//						String str = "";					
//						for (Integer i: lst) {
//							str += i + ((outliersSorted.get(i).getScore() > outlierThreshold) ? "^" : "") + " "; // Staring suspected outliers
//						}
//						
//						if (key == 0)
//							System.out.println("#" + lst_size + " singles: [ " + str + "] - (" + key + ")");
//						else
//							System.out.println("!! [ " + str + "] - (" + key + ")");
//					}
//				}
				if (map.get(0) != null)
					System.out.println("#" + (map.size() - 1 + map.get(0).size()) 
							+ " clusters in total (#" + (map.size() - 1) + " composites + #" + map.get(0).size() + " singles).");			
				else
					System.out.println("#" + (map.size() - 1) + " clusters in total");
				
				this.flatPartitioningMap = map;
				
			}

			
			// Result =>
//			this.clusters = clusters;
			
			return result;
			
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.log(Level.SEVERE, null, ex);
			return null;
		}
	}

	private int printValidatedClusters(List<Cluster> clusters) {
		
		int nb_real_cl = 0;
		

//		//Take the list of propagated clusters from the root cluster:
//		ArrayList<Cluster> solution = clusters.get(1).getPropagatedDescendants();

		for (Cluster cl: clusters) {
			
			if (cl == null) { continue; }
			
			if (cl.getPropagatedDescendants().size() <= 3) {
				System.out.println("###### \t > " + cl.getLabel() + " [size: " + cl.getPropagatedDescendants().size() + "]");
				String labels_str = "";
				for (Cluster cl0: cl.getPropagatedDescendants()) {
					labels_str += " " + cl0.getLabel();
				}
				System.out.println("\t\t\t -> " + labels_str);
				nb_real_cl++;
			} else {
				
				nb_real_cl += printValidatedClusters(cl.getPropagatedDescendants());
				
//				if (cl != null)
//    				for (Cluster cl2: cl.getPropagatedDescendants()) {
//    					System.out.println("###### \t >< " + ((cl2 != null) ? cl2.getPropagatedDescendants().size() : "null") + "[" + cl2.getLabel() + "]");
//    				}
			}
		}
		
		return nb_real_cl;

	}
	
	public Map<Integer, List<Integer>> getPartitioningMap() {
		
		return this.flatPartitioningMap;
	}
	
	
	
	/**
	 * Get flat clustering from tree using a maximum cluster size cutoff value.
	 * 
	 * @param clusters Tree expressed in Clusters as given by 'computeHierarchyAndClusterTree()'
	 * @param clusterSizeThreshold Maximum cluster size cutoff value 
	 * @param skip_native_leaves Store only clusters parenting more than one leaf
	 * @param verbose Do speak
	 * @return
	 */
	public List<Cluster> selectClustersFromHierarchy(List<Cluster> clusters, int clusterSizeThreshold,
			boolean skip_native_leaves, boolean verbose, boolean substitue_indexes) {
		
		List<Integer> leaves = new ArrayList<>();
		
		List<Cluster> real_clusters = new ArrayList<>();
		
//		int nb_clusters = clusters.get(1).getPropagatedDescendants().size();
		
		for (Cluster cl: clusters) {

			if (cl == null) {
				
				if (verbose) { System.out.println("# NULL cluster (=noise)"); }
				
			} else if (!cl.hasChildren()) {//(cl.getPropagatedDescendants().size() == 0) {
				
				if (!skip_native_leaves) {
					if (verbose) { 
						if (substitue_indexes)
							System.out.println("# Native Leaf: " + getDataSetIndexFromClusterIndex(cl.getLabel()));
						else
							System.out.println("# Native Leaf: " + cl.getLabel()); 
					}
				}
				
			} else if (cl.getPropagatedDescendants().size() <= clusterSizeThreshold) {

				boolean skip_clust = false;
				
				String labels_str = "";
				for (Cluster cl0: cl.getPropagatedDescendants()) {

					int pos = leaves.indexOf(cl0.getLabel());
					
					if (pos == -1) {
						
						if (verbose) {
							if (substitue_indexes)
								labels_str += " " + getDataSetIndexFromClusterIndex(cl0.getLabel()) + (!cl0.hasChildren() ? "*" : ""); // Star real leaves;
							else
								labels_str += " " + cl0.getLabel() + (!cl0.hasChildren() ? "*" : ""); // Star real leaves
						}
						
						leaves.add(cl0.getLabel());
					} else {
						
						skip_clust = true;
					}
					
				}

				if (!skip_clust) {
					
					if (verbose) { 
						if (substitue_indexes)
							System.out.println("# Cluster: " + getDataSetIndexFromClusterIndex(cl.getLabel()) + " [" + labels_str + "]");
						else
							System.out.println("# Cluster: " + cl.getLabel() + " [" + labels_str + "]"); 
					}
					
					real_clusters.add(cl);
				}
			} else {
				//System.out.println("# NOT a Leaf: " + cl.getLabel() + " (nb children: " + cl.getPropagatedDescendants().size() + ")");
				selectClustersFromHierarchy(cl.getPropagatedDescendants(), clusterSizeThreshold, skip_native_leaves, verbose, substitue_indexes);
			}
			
		}
		
//		// Recover singles
////		int i = 0;
//		for (Cluster cl: clusters) {
//			
//			if (cl == null) { continue; }
//				
//			if (!cl.hasChildren() && leaves.indexOf(cl.getLabel()) == -1) {
//				
//				System.out.println("# Recovered single: " + cl.getLabel());
//				over_all_clusters_num++;
//			}
//		}
		
		//return over_all_clusters_num;
		return real_clusters;
	}
	
	public int getDataSetIndexFromClusterIndex(int clusterIndex) {
		
		if (clusterIndex == 0)
			return -1;
		
		return indexOf(this.pointLastClusterIndexes, clusterIndex, 0);
	}
	
	private static int INDEX_NOT_FOUND = -1;
	private static int indexOf(int[] array, int valueToFind, int startIndex) {
	      if (array == null) {
	          return INDEX_NOT_FOUND;
	      }
	      if (startIndex < 0) {
	          startIndex = 0;
	      }
	      for (int i = startIndex; i < array.length; i++) {
	          if (valueToFind == array[i]) {
	              return i;
	          }
	      }
	      return INDEX_NOT_FOUND;
	  }

	public int[] getFlatPartitioning() {
		return this.flatPartitioning;
	}
	
//	public HDBSCANLinearMemory getClusterer() {
//		return clusterer;
//	}

//	public List<Cluster> getResultingClusters() {
//	return clusters;
//}

	public static boolean isLeaf(Cluster clust) {
		return (!clust.hasChildren());
	}

	
	class HDBSCANStarDistanceFunction implements DistanceCalculator {

		@Override
		public double computeDistance(double[] attributesOne, double[] attributesTwo) {

			int i, j;
			// Integer ID
			i = (int) Math.round(attributesOne[0]);
			j = (int) Math.round(attributesTwo[0]);

			return distMtx[i][j];
		}

		@Override
		public String getName() {
			return "GLG: From pre-computed distance matrix.";
		}



	}

	
	class IdComparatorForOutlierScore implements Comparator<OutlierScore> {

		@Override
		public int compare(OutlierScore a, OutlierScore b) {
			
			return a.getId() < b.getId() ? -1 : a.getId() == b.getId() ? 0 : 1;
		}
	}

}
