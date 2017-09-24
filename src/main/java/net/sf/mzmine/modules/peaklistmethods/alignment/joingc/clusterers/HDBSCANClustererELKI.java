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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.Constraint;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.hdbscanstar.Constraint.CONSTRAINT_TYPE;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.CLINK;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerDensityHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.ExtractFlatClusteringFromHierarchy;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.ClusteringProgression;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.VisualizationType;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical.DistanceType;
import net.sf.mzmine.parameters.ParameterSet;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import java.io.IOException;
// ---XMeans
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Vector;
//




public class HDBSCANClustererELKI /*implements ClusteringAlgorithm*/ {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String MODULE_NAME = "DBSCAN clusterer";

	private double[][] distMtx;
	Instances dataSet;

	//private DBSCANWithProgress clusterer;
	HDBSCANLinearMemory clusterer;

	private List<Cluster> clusters;
	
	
	private ClusteringProgression clustProgress;


	public static String NEWICK_LEAF_NAME_PREFIX = "n"; //"node_" // "TreeParser" need a String as name (cannot go with only number Id) 

	////private HashMap<Instance, Integer> instanceRowIndexMap;

	//    @Override
	//    public @Nonnull String getName() {
	//	return MODULE_NAME;
	//    }

	public HDBSCANClustererELKI(ClusteringProgression clustProgress, double[][] rawData) {

		this.distMtx = rawData;
		//		this.dataSet = createSampleWekaDataset(rawData);

		this.clustProgress = clustProgress;
	}

	//    public void setDistancesMatrix(double[][] distMtx) {
	//        this.distMtx = distMtx;
	//    }
	/**
	 * Creates the weka data set for clustering of samples
	 *
	 * @param rawData
	 *            Data extracted from selected Raw data files and rows.
	 * @return Weka library data set
	 */
	private Instances createSampleWekaDataset(double[][] rawData) {

		FastVector attributes = new FastVector();

		for (int i = 0; i < rawData[0].length; i++) {
			String varName = "dist" + i;
			Attribute var = new Attribute(varName);
			attributes.addElement(var);
		}

		Attribute id = new Attribute("id"); //(FastVector) null);
		attributes.addElement(id);

		Instances data = new Instances("Dataset", attributes, 0);

		for (int i = 0; i < rawData.length; i++) {

			// Store distances
			double[] values = new double[data.numAttributes()];
			System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);

			// Store Id
			//            values[data.numAttributes() - 1] = data.attribute("name").addStringValue(/*this.selectedRawDataFiles[i].getName()*/NEWICK_LEAF_NAME_PREFIX + i);
			values[rawData[0].length] = i;

			Instance inst = new SparseInstance(1.0, values); //BinarySparseInstance(1.0, values); // DenseInstance(1.0, values);
			inst.setDataset(data);
			data.add(inst);

		}

		return data;

	}

	//	private int getInstanceRowIndex(Instance inst) {
	//		
	////		if (!instanceRowIndexMap.containsKey(inst))
	////			return 0;
	//		System.out.println(this.dataSet);
	//		return instanceRowIndexMap.get(inst);
	//	}



	//@Override
	public ClusteringResult performClustering() {

//		this.dataSet = createSampleWekaDataset(this.distMtx);


//		List<Integer> clusters = new ArrayList<Integer>();
		/*HierarchicalClusterer*/ clusterer = new HDBSCANLinearMemory(EuclideanDistanceFunction.STATIC, 1);//new HDBSCANDistanceFunction(), 1);


		String[] options = new String[] { /*"-no-gui",*/ "-M", "3", "-E", "0.9",
				"-D", "net.sf.mzmine.modules.peaklistmethods.alignment.joingc.weka."
						+ "TestDistClass" };
		

		try {
//			clusterer.setOptions(options);
//			System.out.println(Arrays.toString(clusterer.getOptions()));

			// MEMORY STUFF
			Runtime run_time = Runtime.getRuntime();
			Long prevTotal = 0l;
			Long prevFree = run_time.freeMemory();

			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER START");
//			System.out.println(Arrays.toString(clusterer.getOptions()));

			//			DistanceFunction distanceFunction = new XMeansDistanceFunction();
			//			distanceFunction.setInstances(dataSet);
			//			clusterer.setDistanceF(distanceFunction);

			//clusterer.setNumClusters(2);
			//            clusterer.setInitializationMethod(new SelectedTag(SimpleKMeans.CANOPY, SimpleKMeans.TAGS_SELECTION));


//			System.out.println(Arrays.toString(clusterer.getOptions()));
			//clusterer.setPrintNewick(true);

			/*clustProgress.setProgress(0d);*/

			//System.out.println("Trying to bulid clusterer from:" + this.dataSet.numAttributes());
//			clusterer.setClusteringProgression(clustProgress);
			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (before...)");
////			clusterer.setDistanceMatrix(this.distMtx);
//			/** clusterer.buildClusterer(this.dataSet); */
//			DatabaseConnection dbc1 = new ArrayAdapterDatabaseConnection(this.distMtx);
//			
////			Collection<IndexFactory<?, ?>> indexFactories = new ArrayList<>();
////			ObjectListParameter<IndexFactory<?, ?>> indexFactoryP = new ObjectListParameter<>(INDEX_ID, IndexFactory.class, true);
////			indexFactories.addAll(indexFactoryP.instantiateClasses(spatparams));
//
//			Database db = new StaticArrayDatabase(dbc1, null);
//			db.initialize();
//			PointerDensityHierarchyRepresentationResult pdhr_result = clusterer.run(db, db.getRelation(TypeUtil.LABELLIST));
//			//de.lmu.ifi.dbs.elki.result.Result pdhr_result = clusterer.run(db);
//			/** 
//			ExtractFlatClusteringFromHierarchy(HierarchicalClusteringAlgorithm algorithm, double threshold, ExtractFlatClusteringFromHierarchy.OutputMode outputmode, boolean singletons)
//			Constructor.
//			ExtractFlatClusteringFromHierarchy(HierarchicalClusteringAlgorithm algorithm, int minclusters, ExtractFlatClusteringFromHierarchy.OutputMode outputmode, boolean singletons)
//			Constructor.
//			HDBSCANHierarchyExtraction(HierarchicalClusteringAlgorithm algorithm, int minClSize, boolean hierarchical)
//			Constructor.
//			SimplifiedHierarchyExtraction(HierarchicalClusteringAlgorithm algorithm, int minClSize)
//			Constructor.
//			*/
////			ExtractFlatClusteringFromHierarchy extraction = new ExtractFlatClusteringFromHierarchy(new CLINK(EuclideanDistanceFunction.STATIC), 1d, true, false);
////			System.out.println("RESULT 0: " + pdhr_result.getHierarchy() + extraction.);
//			
//			
//			//
//			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (after!!)");
//			//			//
//			//			Enumeration<?> e2 = dataSet.enumerateInstances();
//			//			while (e2.hasMoreElements()) {
//			//				clusters.add(clusterer.clusterInstance((Instance) e2.nextElement()));
//			//				//System.out.println("\t-> " + clusters.get(clusters.size()-1));
//			//			}
//			//	        //
//			//            JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER LISTED");
//
//			ClusteringResult result = new ClusteringResult(
//					null, //clusters, //null,
//					clusterer.toString(), 
//					0, //clusterer.numberOfClusters(), 
//					null //VisualizationType.PCA //null
//					);
//					
//			//
//			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER RESULTED");
//
//			//System.out.println("###>>> " + clusterer.graph());
//			//System.out.println("###>>> " + clusterer.getNumClusters());
//			return result;
			
			
			// Read in input file:
//			double[][] dataSet; // = new double[distMtx.length * distMtx.length][2];
//			double[][] dataSet = new double[(distMtx.length * distMtx.length / 2) - distMtx.length][1];
			double[][] dataSet = new double[distMtx.length][1];
//			try {
//				dataSet = HDBSCANStar.readInDataSet(parameters.inputFile, ",");
//			}
//			catch (IOException ioe) {
//				System.err.println("Error reading input data set file.");
//				System.exit(-1);
//			}
			for (int i = 0; i < distMtx.length; i++) {					
				//for (int j = i + 1; j < distMtx.length; j++) {
//				for (int j = i; j < distMtx.length; j++) {
//					
////					dataSet[(i * distMtx.length) + j][0] = i;
////					dataSet[(i * distMtx.length) + j][1] = j;
//					
//				}
////				dataSetList.add(new double[] { i });

				dataSet[i] = new double[] { i };

			}
//			dataSet = (double[][]) dataSetList.toArray();
//			dataSetList = null;
			int numPoints = dataSet.length;
//			System.gc();
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: data set!");
			
			// Read in constraints:
			ArrayList<Constraint> constraints = null;
//			if (parameters.constraintsFile != null) {
//				try {
//					constraints = HDBSCANStar.readInConstraints(parameters.constraintsFile, ",");
//				}
//				catch (IOException e) {
//					System.err.println("Error reading constraints file.");
//					System.exit(-1);
//				}
//			}
			// TODO: Do this in same pass than building dataset above
			constraints = new ArrayList<>();
//			for (int k = 0; k < numPoints; k++) {
//				
//				int i = (int) Math.round(dataSet[k][0]);
//				int j = (int) Math.round(dataSet[k][1]);
//				
//				// Explicitly forbid too distant pairs!
//				// "ml" must-link, "cl" cannot-link 
//				if (distMtx[i][j] >= 1.0 || i == j /* Should never happen */) {
//	
//					int pointA = (i * distMtx.length) + j;
//					int pointB = (j * distMtx.length) + i;
//					constraints.add(new Constraint(pointA, pointB, CONSTRAINT_TYPE.CANNOT_LINK));
//				}
//			}
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
					clusterer.toString(), 
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


	public HDBSCANLinearMemory getClusterer() {
		return clusterer;
	}

	public List<Cluster> getResultingClusters() {
		return clusters;
	}
	
//	class HDBSCANDistanceFunction extends de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction<LabelList> {
//
//		@Override
//		public SimpleTypeInformation<? super LabelList> getInputTypeRestriction() {
//			// TODO Auto-generated method stub
//			return null;
//		}
//
//		@Override
//		public double distance(LabelList o1, LabelList o2) {
//			
//			
//			
//			int i, j;
//			// Integer ID
//			i = Integer.valueOf(o1.get(0).substring(NEWICK_LEAF_NAME_PREFIX.length())); // turn "n125" to '125'
//			j = Integer.valueOf(o2.get(0).substring(NEWICK_LEAF_NAME_PREFIX.length()));
//
//			return distMtx[i][j];
//		}
//
////		@Override
////		public double distance(Instance inst1, Instance inst2) {
////
////			int attr_index = inst1.numAttributes() - 1;
////			int i, j;
////			// Integer ID
////			i = (int) Math.round(inst1.value(attr_index));
////			j = (int) Math.round(inst2.value(attr_index));
////
////			return distMtx[i][j];
////		}
//
//
//
//	}
	
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
