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

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.CLINK;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerDensityHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.PointerHierarchyRepresentationResult;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.ExtractFlatClusteringFromHierarchy;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.ClusteringProgression;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.VisualizationType;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical.DistanceType;
import net.sf.mzmine.parameters.ParameterSet;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;



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
	//Instances dataSet;

	//private DBSCANWithProgress clusterer;
	HDBSCANLinearMemory clusterer;

	//private List<Cluster> clusters;
	
	
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




	//@Override
	@SuppressWarnings("unchecked")
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

//		    // Setup algorithm
//		    ListParameterization params = new ListParameterization();
//		    params.addParameter(CutDendrogramByNumberOfClusters.Parameterizer.MINCLUSTERS_ID, 3);
//		    params.addParameter(AbstractAlgorithm.ALGORITHM_ID, HDBSCANLinearMemory.class);
//		    params.addParameter(HDBSCANLinearMemory.Parameterizer.MIN_PTS_ID, 20);
//		    CutDendrogramByNumberOfClusters c = ClassGenericsUtil.parameterizeOrAbort(CutDendrogramByNumberOfClusters.class, params);
//		    testParameterizationOk(params);

//			// Setup parameters:
//			ListParameterization params = new ListParameterization();
//			params.addParameter(FileBasedDatabaseConnection.Parameterizer.INPUT_ID, filename);
//			// Add other parameters for the database here!
//
//			// Instantiate the database:
//			Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);
//			// Don't forget this, it will load the actual data (otherwise you get null values below)
//			db.initialize();

			// Adapter to load data from an existing array.
			DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(this.distMtx);
			// Create a database (which may contain multiple relations!)
			Database db = new StaticArrayDatabase(dbc, null);
			// Load the data into the database (do NOT forget to initialize...)
			db.initialize();
			
			//Relation<NumberVector> vectors = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);//NUMBER_VECTOR_FIELD);
			//Relation<LabelList> labels = db.getRelation(TypeUtil.LABELLIST);
			
			PointerDensityHierarchyRepresentationResult res = (PointerDensityHierarchyRepresentationResult) clusterer.run(db/*, labels*/);
		    //List<Cluster<? extends Model>> clusterresults = new Clustering("", "").getAllClusters();
		    //Clustering<? extends Model> clustering = clusterresults.get(0);

			
			
		    System.out.println("ELKI res 1: " + res.getLongName());
		    System.out.println("ELKI res 2: " + res.getHierarchy());
			
			
			ClusteringResult result = new ClusteringResult(
					null, //clusters, //null,
					clusterer.toString(), 
					0, //clusters.size(), //numberOfClusters(), 
					null //VisualizationType.PCA //null
					);

			
			// Result =>
			//this.clusters = clusters;
			
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

//	public List<Cluster> getResultingClusters() {
//		return clusters;
//	}
	
	class HDBSCANDistanceFunction extends de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction<LabelList> {

		@Override
		public SimpleTypeInformation<? super LabelList> getInputTypeRestriction() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double distance(LabelList o1, LabelList o2) {
			
			
			
			int i, j;
			// Integer ID
			i = Integer.valueOf(o1.get(0).substring(NEWICK_LEAF_NAME_PREFIX.length())); // turn "n125" to '125'
			j = Integer.valueOf(o2.get(0).substring(NEWICK_LEAF_NAME_PREFIX.length()));

			return distMtx[i][j];
		}

//		@Override
//		public double distance(Instance inst1, Instance inst2) {
//
//			int attr_index = inst1.numAttributes() - 1;
//			int i, j;
//			// Integer ID
//			i = (int) Math.round(inst1.value(attr_index));
//			j = (int) Math.round(inst2.value(attr_index));
//
//			return distMtx[i][j];
//		}



	}
	


}
