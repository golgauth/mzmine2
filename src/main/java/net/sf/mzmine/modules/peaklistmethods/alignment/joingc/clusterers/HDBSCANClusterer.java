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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.Constraint;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.hdbscanstar.Constraint.CONSTRAINT_TYPE;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.ClusteringProgression;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import weka.core.Instances;
import java.io.IOException;



public class HDBSCANClusterer /*implements ClusteringAlgorithm*/ {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String MODULE_NAME = "DBSCAN clusterer";

	private double[][] distMtx;
	Instances dataSet;

//	//private DBSCANWithProgress clusterer;
//	HDBSCANLinearMemory clusterer;

	private List<Cluster> clusters;
	
	
	private ClusteringProgression clustProgress;


	public static String NEWICK_LEAF_NAME_PREFIX = "n"; //"node_" // "TreeParser" need a String as name (cannot go with only number Id) 

	////private HashMap<Instance, Integer> instanceRowIndexMap;

	//    @Override
	//    public @Nonnull String getName() {
	//	return MODULE_NAME;
	//    }

	public HDBSCANClusterer(ClusteringProgression clustProgress, double[][] rawData) {

		this.distMtx = rawData;
		//		this.dataSet = createSampleWekaDataset(rawData);

		this.clustProgress = clustProgress;
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
			constraints = new ArrayList<>();

			for (int i = 0; i < numPoints; i++) {			
				
				for (int j = i; j < numPoints; j++) {
					
					if (distMtx[i][j] >= 1.0) { // || i == j /* Should never happen */) {
						
						constraints.add(new Constraint(i, j, CONSTRAINT_TYPE.CANNOT_LINK));
					}
				}

			}
			System.out.println("NB contraints: " + constraints.size());
			
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: constraints!");
			
			// Compute core distances:
			long startTime = System.currentTimeMillis();
//			double[] coreDistances = HDBSCANStar.calculateCoreDistances(dataSet, parameters.minPoints, parameters.distanceFunction);
			DistanceCalculator distanceFunction = new HDBSCANStarDistanceFunction();
			double[] coreDistances = HDBSCANStar.calculateCoreDistances(dataSet, 1, distanceFunction);
			System.out.println("Time to compute core distances (ms): " + (System.currentTimeMillis() - startTime));
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: core distances!");
			
			// Calculate minimum spanning tree:
			startTime = System.currentTimeMillis();
//			UndirectedGraph mst = HDBSCANStar.constructMST(dataSet, coreDistances, true, parameters.distanceFunction);
			UndirectedGraph mst = HDBSCANStar.constructMST(dataSet, coreDistances, true, distanceFunction);
			mst.quicksortByEdgeWeight();
			System.out.println("Time to calculate MST (ms): " + (System.currentTimeMillis() - startTime));
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: min spanning tree!");

			//Remove references to unneeded objects:
			dataSet = null;
			
			double[] pointNoiseLevels = new double[numPoints];
			int[] pointLastClusters = new int[numPoints];

			String hierarchyFilename = "hierarchy.out";
			
			//Compute hierarchy and cluster tree:
			ArrayList<Cluster> clusters = null;
			try {
				startTime = System.currentTimeMillis();
//				clusters = HDBSCANStar.computeHierarchyAndClusterTree(mst, parameters.minClusterSize, 
//						parameters.compactHierarchy, constraints, parameters.hierarchyFile, 
//						parameters.clusterTreeFile, ",", pointNoiseLevels, pointLastClusters, parameters.visualizationFile);
				clusters = HDBSCANStar.computeHierarchyAndClusterTree(mst, 2, 
						false /* for performance */, constraints, hierarchyFilename, 
						"tree.out", ",", pointNoiseLevels, pointLastClusters, "visualization.out");
				System.out.println("Time to compute hierarchy and cluster tree (ms): " + (System.currentTimeMillis() - startTime));
				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: hierarchy!");
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
			try {
				startTime = System.currentTimeMillis();
				HDBSCANStar.findProminentClusters(clusters, hierarchyFilename, "partition.out", 
						",", numPoints, infiniteStability);
				System.out.println("Time to find flat result (ms): " + (System.currentTimeMillis() - startTime));
				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: partition!");
			}
			catch (IOException ioe) {
				System.err.println("Error writing to partitioning file.");
				System.exit(-1);
			}
			
			//Compute outlier scores for each point:
			try {
				startTime = System.currentTimeMillis();
				HDBSCANStar.calculateOutlierScores(clusters, pointNoiseLevels, pointLastClusters, 
						coreDistances, "outlier_score.out", ",", infiniteStability);
				System.out.println("Time to compute outlier scores (ms): " + (System.currentTimeMillis() - startTime));
				JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: outlier scores!");
			}
			catch (IOException ioe) {
				System.err.println("Error writing to outlier score file.");
				System.exit(-1);
			}
			
			//System.out.println("Overall runtime (ms): " + (System.currentTimeMillis() - overallStartTime));

			ClusteringResult result = new ClusteringResult(
					null, //clusters, //null,
					null, //clusterer.toString(), 
					clusters.size(), //numberOfClusters(), 
					null //VisualizationType.PCA //null
					);

			
			// Result =>
			this.clusters = clusters;
			
			return result;
			
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.log(Level.SEVERE, null, ex);
			return null;
		}
	}


//	public HDBSCANLinearMemory getClusterer() {
//		return clusterer;
//	}

	public List<Cluster> getResultingClusters() {
		return clusters;
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


}
