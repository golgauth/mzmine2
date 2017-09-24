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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.weka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.ClusteringProgression;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.VisualizationType;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical.DistanceType;
import net.sf.mzmine.parameters.ParameterSet;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclideanDataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Attribute;
import weka.core.BinarySparseInstance;
import weka.core.DistanceFunction;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.ManhattanDistance;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.SparseInstance;
import weka.core.neighboursearch.PerformanceStats;



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
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.DistanceFunction;
import weka.core.Drawable;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;




public class OPTICSClusterer /*implements ClusteringAlgorithm*/ {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String MODULE_NAME = "OPTICS clusterer";

	private double[][] distMtx;
	Instances dataSet;

	private OPTICSWithProgress clusterer;

	private ClusteringProgression clustProgress;


	public static String NEWICK_LEAF_NAME_PREFIX = "n"; //"node_" // "TreeParser" need a String as name (cannot go with only number Id) 

	////private HashMap<Instance, Integer> instanceRowIndexMap;

	//    @Override
	//    public @Nonnull String getName() {
	//	return MODULE_NAME;
	//    }

	public OPTICSClusterer(ClusteringProgression clustProgress, double[][] rawData) {

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

		////this.instanceRowIndexMap = new HashMap<>();

		//		FastVector attributes = new FastVector();
		//
		//		for (int i = 0; i < rawData[0].length; i++) {
		//			String varName = "dist" + i;
		//			Attribute var = new Attribute(varName);
		//			attributes.addElement(var);
		//		}
		//
		////        Attribute name = new Attribute("name", (FastVector) null);
		////        attributes.addElement(name);
		//
		//		Instances data = new Instances("Dataset", attributes, 0);
		//
		//		for (int i = 0; i < rawData.length; i++) {
		//			
		//			// Store distances
		//			double[] values = new double[data.numAttributes()];
		//			System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);
		//
		//			// Store Id
		////            values[data.numAttributes() - 1] = data.attribute("name").addStringValue(/*this.selectedRawDataFiles[i].getName()*/NEWICK_LEAF_NAME_PREFIX + i);
		//			
		//			Instance inst = new SparseInstance(1.0, values); //BinarySparseInstance(1.0, values); // DenseInstance(1.0, values);
		//			inst.setDataset(data);
		//			data.add(inst);
		//			
		//			instanceRowIndexMap.put(inst, i);
		//		}
		//		
		//		return data;

//		List<SparseInstance> instances = new ArrayList<>();
//		FastVector atts = new FastVector();
//		int numInstances = rawData.length;
//		int numDimensions = rawData[0].length;
//
//
//		// Store distances 
//		for(int dim = 0; dim < numDimensions; dim++)
//		{
//			Attribute current = new Attribute("Attribute" + dim , dim);
//
//			if(dim == 0)
//			{
//				for(int obj = 0; obj < numInstances; obj++)
//				{
//					instances.add(new SparseInstance(numDimensions));
//					// instances.add(new DenseInstance(numDimensions) );
//				}
//			}
//
//			for(int obj = 0; obj < numInstances; obj++)
//			{
//				//		    	if (dim == numDimensions) 
//				//		    	{
//				//		    		instances.get(obj).setValue(current, (double)obj);
//				//		    	} else 
//				//		    	{
//				instances.get(obj).setValue(current, (Double)rawData[dim][obj]);
//				//		    	}
//			}
//
//			atts.addElement(current);
//		}
//
//		//		// At last: Store ID attribute too, for each instance!
//		//		Attribute current = new Attribute("Id", numDimensions);
//		//		for(int obj = 0; obj < numInstances; obj++)
//		//		{
//		//        	instances.get(obj).setValue(current, (double)obj);
//		//        } 
//		//	    atts.add(current);
//
//
//
//		Instances newDataset = new Instances("Dataset" , atts, instances.size());
//
//		for(Instance inst : instances) {
//			//instanceRowIndexMap.put(inst, instances.indexOf(inst));
//			newDataset.add(inst);
//		}
//
//		// output on stdout
//		//System.out.println(data);
//
//		return newDataset;

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

			////instanceRowIndexMap.put(inst, i);
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

		this.dataSet = createSampleWekaDataset(this.distMtx);


		List<Integer> clusters = new ArrayList<Integer>();
		/*HierarchicalClusterer*/ clusterer = new OPTICSWithProgress();

		//System.out.println(java.util.Arrays.asList(clusterer.listOptions()..values()));
		Enumeration e = clusterer.listOptions();
		while (e.hasMoreElements()) {
			Option o = ((Option)e.nextElement());
			System.out.println(o.name() + " | " + o.synopsis() + " | " + o.description());
		}
		System.out.println(Arrays.toString(clusterer.getOptions()));

		//String[] options = new String[5];

		//	LinkType link = parameters.getParameter(
		//		HierarClustererParameters.linkType).getValue();
		//	DistanceType distanceType = parameters.getParameter(
		//		HierarClustererParameters.distanceType).getValue();

		//LinkType link = LinkType.AVERAGE;
		DistanceType distanceType = DistanceType.EUCLIDIAN;

		//		options[0] = "-L";
		//		options[1] = link.name();
		//		options[2] = "-A";
		//		switch (distanceType) {
		//		case EUCLIDIAN:
		//			options[3] = "weka.core.EuclideanDistance";
		//			break;
		//		case CHEBYSHEV:
		//			options[3] = "weka.core.ChebyshevDistance";
		//			break;
		//		case MANHATTAN:
		//			options[3] = "weka.core.ManhattanDistance";
		//			break;
		//		case MINKOWSKI:
		//			options[3] = "weka.core.MinkowskiDistance";
		//			break;
		//		}
		//
		//		
		//		options[4] = "-N";
		//		options[2
		//		options[5] = "1";
		//		
		//		options[6] = "-P";
		////		options[7] = "-D";
		//		
		////		options[8] = "-B";
		//		
		////		options[8] = "-c";
		////		options[9] = "last";


		//		options[0] = "-output-debug-info";
		////		options[1] = "-init";
		////		options[2] = "2"; // CANOPY
		//		options[1] = "-L"; // minimum number of clusters
		//		options[2] = "1";
		//		options[3] = "-H"; // maximum number of clusters
		//		options[4] = "158";

//		String[] options = new String[] { "-no-gui", "-M", "1", 
//				"-D", "net.sf.mzmine.modules.peaklistmethods.alignment.joingc.weka."
//						+ "OPTICSClusterer$CustomDistanceDataObject" };
		String[] options = new String[] { /*"-no-gui",*/ "-M", "3", "-E", "2000.0",
				"-D", "net.sf.mzmine.modules.peaklistmethods.alignment.joingc.weka."
						+ "TestDistClass" };
		

		try {
			clusterer.setOptions(options);
			System.out.println(Arrays.toString(clusterer.getOptions()));

			// MEMORY STUFF
			Runtime run_time = Runtime.getRuntime();
			Long prevTotal = 0l;
			Long prevFree = run_time.freeMemory();

			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER START");
			System.out.println(Arrays.toString(clusterer.getOptions()));

			//			DistanceFunction distanceFunction = new XMeansDistanceFunction();
			//			distanceFunction.setInstances(dataSet);
			//			clusterer.setDistanceF(distanceFunction);

			//clusterer.setNumClusters(2);
			//            clusterer.setInitializationMethod(new SelectedTag(SimpleKMeans.CANOPY, SimpleKMeans.TAGS_SELECTION));


			System.out.println(Arrays.toString(clusterer.getOptions()));
			//clusterer.setPrintNewick(true);

			/*clustProgress.setProgress(0d);*/

			//System.out.println("Trying to bulid clusterer from:" + this.dataSet.numAttributes());
			clusterer.setClusteringProgression(clustProgress);
			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (before...)");
			clusterer.setDistanceMatrix(this.distMtx);
			clusterer.buildClusterer(this.dataSet);
			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (after!!)");
			//			//
			//			Enumeration<?> e2 = dataSet.enumerateInstances();
			//			while (e2.hasMoreElements()) {
			//				clusters.add(clusterer.clusterInstance((Instance) e2.nextElement()));
			//				//System.out.println("\t-> " + clusters.get(clusters.size()-1));
			//			}
			//	        //
			//            JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER LISTED");

			ClusteringResult result = new ClusteringResult(
					null, //clusters, //null,
					clusterer.toString(), 
					clusterer.numberOfClusters(), 
					null //VisualizationType.PCA //null
					);
			//
			JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER RESULTED");

			//System.out.println("###>>> " + clusterer.graph());
			//System.out.println("###>>> " + clusterer.getNumClusters());
			return result;
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.log(Level.SEVERE, null, ex);
			return null;
		}
	}


	public OPTICSWithProgress getClusterer() {
		return clusterer;
	}

	public class CustomDistanceDataObject extends EuclideanDataObject {
		
	    /**
		 * 
		 */
		private static final long serialVersionUID = 963552817253622349L;

		public CustomDistanceDataObject(Instance originalInstance, String key, Database database) {
			super(originalInstance, key, database);
			// TODO Auto-generated constructor stub
		}

		/**
	     * Calculates the euclidian-distance between dataObject and this.dataObject
	     * @param dataObject The DataObject, that is used for distance-calculation with this.dataObject;
	     *        now assumed to be of the same type and with the same structure
	     * @return double-value The euclidian-distance between dataObject and this.dataObject
	     */
		@Override
	    public double distance(DataObject dataObject) {

			int numDimensions = distMtx[0].length;
			int attr_index = numDimensions - 1;
			CustomDistanceDataObject ddo = (CustomDistanceDataObject) dataObject;

	    	int i, j;
			// Integer ID
			i = (int) Math.round(this.getInstance().value(attr_index));
			j = (int) Math.round(ddo.getInstance().value(attr_index));

			return distMtx[i][j];

	    	
	    	
//	      double dist = 0.0;
	//
//	      Instance firstInstance = getInstance();
//	      Instance secondInstance = dataObject.getInstance(); 
//	      int firstNumValues = firstInstance.numValues();
//	      int secondNumValues = secondInstance.numValues();
//	      int numAttributes = firstInstance.numAttributes();
	//
//	      int firstI, secondI;
//	      for (int p1 = 0, p2 = 0; p1 < firstNumValues || p2 < secondNumValues;) {
//	        if (p1 >= firstNumValues) {
//	          firstI = numAttributes;
//	        } else {
//	          firstI = firstInstance.index(p1);
//	        }
	//
//	        if (p2 >= secondNumValues) {
//	          secondI = numAttributes;
//	        } else {
//	          secondI = secondInstance.index(p2);
//	        }
	//
//	        double cDistance = 0;
//	        if (firstI == secondI) {
//	          cDistance = computeDistance(firstI, firstInstance.valueSparse(p1),
//	                                      secondInstance.valueSparse(p2));
//	          p1++;
//	          p2++;
//	        } else if (firstI > secondI) {
//	          cDistance = computeDistance(secondI, 0, secondInstance.valueSparse(p2));
//	          p2++;
//	        } else {
//	          cDistance = computeDistance(firstI, firstInstance.valueSparse(p1), 0);
//	          p1++;
//	        }
//	        dist += cDistance * cDistance;
//	      }
//	      return Math.sqrt(dist);
	    }

	}
	
	
	class XMeansDistanceFunction extends ManhattanDistance /*implements DistanceFunction*/ {

		//private static final long serialVersionUID = 1L;

		/**
		 * 
		 */
		private static final long serialVersionUID = 963552817253622349L;

		@Override
		public double distance(Instance inst1, Instance inst2) {

			int attr_index = inst1.numAttributes() - 1;

			//			for (int i=0; i<inst1.numAttributes(); i++) {
			//				System.out.println(inst1.attribute(i).name());
			//				for (int j=0; j<inst1.attribute(i).numValues(); j++) {
			//					System.out.println("\t - " + inst1.attribute(i).value(j));
			//				}
			//			}

			//			System.out.println("Instance: " + arg0);
			//			Enumeration e = arg0.enumerateAttributes();
			//			while (e.hasMoreElements()) {
			//				Attribute a = ((Attribute)e.nextElement());
			//				System.out.println("\tAttribute: " + a.toString());
			//				
			//				Enumeration ee = a.enumerateValues();
			//				while (ee.hasMoreElements()) {
			//					System.out.println("\t\tValue: " + ee.nextElement());
			//				}
			//			}
			//System.out.println(Arrays.toString(clusterer.getOptions()));


			//			int i = Integer.valueOf(arg0.attribute(attr_index).value(0));
			//			int j = Integer.valueOf(arg1.attribute(attr_index).value(0));
			//			double d = arg0.value(attr_index);
			//			int i = Integer.valueOf(arg0.value(attr_index));
			//			int j = Integer.valueOf(arg1.attributes("id").value(0));

			int i, j;

			////			// String ID
			////			if (inst1.attribute(attr_index).isNominal() || inst1.attribute(attr_index).isString()) {
			////				int valIndex_1 = (int) inst1.value(attr_index);
			////				String val_1 = inst1.attribute(attr_index).value(valIndex_1);
			////				i = Integer.valueOf(val_1.substring(NEWICK_LEAF_NAME_PREFIX.length())); // turn "n_125" to "125"
			////				j = Integer.valueOf(inst2.attribute(attr_index).value((int) inst2.value(attr_index)).substring(NEWICK_LEAF_NAME_PREFIX.length()));
			////			} 
			////			// Numerical ID
			////			else {
			////				double val_1 = inst1.value(attr_index);
			////				i = (int) Math.round(val_1);
			////				j = (int) Math.round(inst2.value(attr_index));
			////			}
			////			if (KMeansClusterer.this != null && instanceRowIndexMap != null) {
			//				i = getInstanceRowIndex(inst1);
			//				j = getInstanceRowIndex(inst2);
			////			} else {
			////				i = 0;
			////				j = 0;
			////			}
			//			//System.out.println("Using distMtx[" + i + "][" + j + "] = " + distMtx[i][j]);

			// Integer ID
			i = (int) Math.round(inst1.value(attr_index));
			j = (int) Math.round(inst2.value(attr_index));
			//System.out.println("Using distMtx[" + i + "][" + j + "] = " + distMtx[i][j]);

			return distMtx[i][j];
		}


	}


}
