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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.ClusteringProgression;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.Pair;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.TriangularMatrix;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.VisualizationType;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical.DistanceType;
import net.sf.mzmine.parameters.ParameterSet;
import weka.clusterers.AbstractClusterer;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.BinarySparseInstance;
import weka.core.DistanceFunction;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.NormalizableDistance;
import weka.core.Option;
import weka.core.SparseInstance;
import weka.core.neighboursearch.PerformanceStats;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddID;

// ---
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Vector;
//
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.DenseInstance;
//import weka.core.DenseInstance;
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




public class HierarClusterer /*implements ClusteringAlgorithm*/ {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String MODULE_NAME = "Hierarchical clusterer";

	private double[][] distMtx;
	//private Map<Pair<Integer, Integer>, Float> distancesMap;
//	private double[] distVect;
//	private float[] distFloatVect;
	private TriangularMatrix distancesMtx;
//	int numInstances;
	double distThreshold;
	
	Instances dataSet;

	private HierarchicalClustererWithProgress clusterer;
	
	private ClusteringProgression clustProgress;
	
	
	public static String NEWICK_LEAF_NAME_PREFIX = "n"; //"node_" // "TreeParser" need a String as name (cannot go with only number Id) 
	
	//    @Override
	//    public @Nonnull String getName() {
	//	return MODULE_NAME;
	//    }
	
	private HashMap<Integer, Integer> instanceRowIndexMap;
	

	public HierarClusterer(ClusteringProgression clustProgress, double[][] rawData) {
		
		this.distMtx = rawData;
		this.dataSet = createSampleWekaDataset(rawData);
		
		this.clustProgress = clustProgress;
	}
//	public HierarClusterer(ClusteringProgression clustProgress, Map<Pair<Integer, Integer>, Float> distancesMap, int numInstances) {
//		
//		this.distancesMap = distancesMap;
//		this.dataSet = createSampleWekaDataset(distancesMap, numInstances);
//		
//		this.clustProgress = clustProgress;
//	}
//	public HierarClusterer(ClusteringProgression clustProgress, double[] distancesVector, int numInstances, double distThreshold) {
//		
//		//this.distancesMap = distancesMap;
//		this.distVect = distancesVector;
//		this.numInstances = numInstances;
//		this.distThreshold = distThreshold;
//		this.dataSet = createSampleWekaDataset(numInstances);
//		
//		this.clustProgress = clustProgress;
//	}
//	public HierarClusterer(ClusteringProgression clustProgress, float[] distancesVector, int numInstances, double distThreshold) {
//		
//		//this.distancesMap = distancesMap;
//		this.distFloatVect = distancesVector;
//		this.numInstances = numInstances;
//		this.distThreshold = distThreshold;
//		this.dataSet = createSampleWekaDataset(numInstances);
//		
//		this.clustProgress = clustProgress;
//	}
	
	public HierarClusterer(ClusteringProgression clustProgress, TriangularMatrix distancesMtx, double distThreshold) {

//		this.numInstances = distancesMtx.getDimension();
		
		this.distancesMtx = distancesMtx;
		
		this.distThreshold = distThreshold;
		this.dataSet = createSampleWekaDataset(distancesMtx.getDimension());//numInstances);
		
		this.clustProgress = clustProgress;
	}
	
	private Instances createSampleWekaDataset(int numInstances) {
		
		ArrayList<Attribute> attrs = new ArrayList<>();

		Instances data = new Instances("Dataset", attrs, 0);
		
		for (int i = 0; i < numInstances; i++) {
		
			ConcreteInstance inst = new ConcreteInstance(i);
			data.add(inst);
		}
		

		return data;

	}
	
//	private Instances createSampleWekaDataset(Map<Pair<Integer, Integer>, Float> distancesMap, int numInstances) {
//		
//		ArrayList<Attribute> attrs = new ArrayList<>();
//
//		Instances data = new Instances("Dataset", attrs, 0);
//		
//		for (int i = 0; i < numInstances; i++) {
//		
//			ConcreteInstance inst = new ConcreteInstance(i);
//			data.add(inst);
//		}
//		
//
//		return data;
//
//	}

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
		
//		instanceRowIndexMap = new HashMap<>();

		
//		FastVector attributes = new FastVector();
//
////		for (int i = 0; i < rawData[0].length; i++) {
////			String varName = "dist" + i;
////			Attribute var = new Attribute(varName);
////			attributes.addElement(var);
////		}
//
//        Attribute name = new Attribute("name", (FastVector) null);
//        attributes.addElement(name);
////        Attribute id = new Attribute("id", (FastVector) null);
////        attributes.addElement(id);
////		Attribute var = new Attribute("id");
////		attributes.addElement(var);
//
//		Instances data = new Instances("Dataset", attributes, 0);
//
//		for (int i = 0; i < rawData.length; i++) {
//			
////			// Store distances
////			double[] values = new double[data.numAttributes()];
////			System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);
//			double[] values = new double[1];
//
//			// Store Id
//            values[data.numAttributes() - 1] = data.attribute("name").addStringValue(/*this.selectedRawDataFiles[i].getName()*/NEWICK_LEAF_NAME_PREFIX + i);
////            values[data.numAttributes() - 1] = data.attribute("id").addStringValue("" + i);
////            values[data.numAttributes() - 1] = i;
//
////			Instance inst = new SparseInstance(data.numAttributes());//new DenseInstance(1.0, values);//new SparseInstance(1.0, values); //new BinarySparseInstance(1.0, values); 
//			Instance inst = new SparseInstance(1.0, values);//new DenseInstance(1.0, values);//new SparseInstance(1.0, values); //new BinarySparseInstance(1.0, values); 
////			inst.setDataset(data);
//			data.add(inst);
//			
////			instanceRowIndexMap.put(data.get(i), i);
//			
//			//System.out.println("i:" + i + " at: " + data.indexOf(inst));
//		}
//		
////		for (Instance k: instanceRowIndexMap.keySet()) {
////			System.out.println("Pair> k:" + 
////					k.getClass().getName() + "@" + 
////			        Integer.toHexString(System.identityHashCode(k)) + 
////			        " | v:" + instanceRowIndexMap.get(k));
////		}
//		
//		
//		
////		Instances newData = new Instances(new Instances("Dataset2", new FastVector(), 0)/*data*/);
////        // add new attributes
////        // 1. nominal
////        FastVector values = new FastVector(); /* FastVector is now deprecated. Users can use any java.util.List */
////        values.addElement("A");               /* implementation now */
////        values.addElement("B");
////        values.addElement("C");
////        values.addElement("D");
////        newData.insertAttributeAt(new Attribute("NewNominal", values), newData.numAttributes());
////        // 2. numeric
////        newData.insertAttributeAt(new Attribute("NewNumeric"), newData.numAttributes());
////
////        for (int i = 0; i < 5; i++) {
////			Instance inst = new SparseInstance(newData.numAttributes());
////			//inst.setDataset(data);
////			newData.add(inst);
////        }
////		
////        // random values
////        Random rand = new Random(1);
////        for (int i = 0; i < newData.numInstances(); i++) {
////          // 1. nominal
////          // index of labels A:0,B:1,C:2,D:3
////          newData.instance(i).setValue(newData.numAttributes() - 2, rand.nextInt(4));
////          // 2. numeric
////          newData.instance(i).setValue(newData.numAttributes() - 1, rand.nextDouble());
////        }
////   
////        // output on stdout
////        System.out.println(newData);
//		
//		
//		
//      // output on stdout
//      //System.out.println(data);
//		
//		return data;
//		
//		
//		
//		
////		List<DenseInstance> instances = new ArrayList<>();
////		ArrayList<Attribute> attrs = new ArrayList<>();
////		int numInstances = rawData.length;
////		int numDimensions = rawData[0].length;
////		
////		
////		for(int dim = 0; dim < numDimensions; dim++)
////		{
////		    Attribute current = new Attribute("A" + dim , dim);//"Attribute" + dim , dim);
////
////		    if(dim == 0)
////		    {
////		        for(int obj = 0; obj < numInstances; obj++)
////		        {
////		            // instances.add(new SparseInstance(numDimensions));
////		            instances.add(new DenseInstance(numDimensions) );
////		        }
////		    }
////
////		    for(int obj = 0; obj < numInstances; obj++)
////		    {
////		        instances.get(obj).setValue(current, (Double)rawData[dim][obj]);
////		    }
////
////		    attrs.add(current);
////		}
////
////		Instances newDataset = new Instances("Dataset" , attrs, instances.size());
////
////		for(Instance inst : instances) {
////		    instanceRowIndexMap.put(inst, instances.indexOf(inst));
////		    newDataset.add(inst);
////		}
////		
////      // output on stdout
////      //System.out.println(data);
////		
////		return newDataset;

//		FastVector attributes = new FastVector();
//
//		for (int i = 0; i < rawData[0].length; i++) {
//			String varName = "dist" + i;
//			Attribute var = new Attribute(varName);
//			attributes.addElement(var);
//		}
//
//        Attribute id = new Attribute("id"); //(FastVector) null);
//        attributes.addElement(id);
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
//			values[rawData[0].length] = i;
//			
//			Instance inst = new SparseInstance(1.0, values); //BinarySparseInstance(1.0, values); // DenseInstance(1.0, values);
//			inst.setDataset(data);
//			data.add(inst);
//			
//			////instanceRowIndexMap.put(inst, i);
//		}
//		
//		return data;

		FastVector attributes = new FastVector();
//
////		for (int i = 0; i < rawData[0].length; i++) {
////			String varName = "dist" + i;
////			Attribute var = new Attribute(varName);
////			attributes.addElement(var);
////		}
//
//        Attribute name = new Attribute("name", (FastVector) null);
//        attributes.addElement(name);
////        Attribute id = new Attribute("id", (FastVector) null);
////        attributes.addElement(id);
		Attribute var = new Attribute("id");
		attributes.addElement(var);

		Instances data = new Instances("Dataset", attributes, 0);
		
//		data.setClassIndex(data.numAttributes() - 1);
//		AddID addId = new AddID();
//		try {
//			addId.setInputFormat(data); 
//			data = Filter.useFilter(data, addId);
//			//addId.setIDIndex("ID");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		for (int i = 0; i < rawData.length; i++) {
			
//			// Store distances
//////			double[] values = new double[data.numAttributes()];
//////			System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);
//			double[] values = new double[data.numAttributes()]; // =1 !!
////
////			// Store Id
//////            values[data.numAttributes() - 1] = data.attribute("name").addStringValue(/*this.selectedRawDataFiles[i].getName()*/NEWICK_LEAF_NAME_PREFIX + i);
//////            values[data.numAttributes() - 1] = data.attribute("id").addStringValue("" + i);
////            values[data.numAttributes() - 1] = i;
////
//////			Instance inst = new SparseInstance(data.numAttributes());//new DenseInstance(1.0, values);//new SparseInstance(1.0, values); //new BinarySparseInstance(1.0, values); 
//			Instance inst = new SparseInstance(1.0, values);//new DenseInstance(1.0, values);//new SparseInstance(1.0, values); //new BinarySparseInstance(1.0, values); 
//			//			inst.setDataset(data);
//			data.add(inst);
//			
//			//System.out.println("i:" + i + " at: " + data.indexOf(inst));
			
			ConcreteInstance inst = new ConcreteInstance(i);
			//inst.setUnid(i);
			data.add(inst);
		}
		
//		for (Instance k: instanceRowIndexMap.keySet()) {
//			System.out.println("Pair> k:" + 
//					k.getClass().getName() + "@" + 
//			        Integer.toHexString(System.identityHashCode(k)) + 
//			        " | v:" + instanceRowIndexMap.get(k));
//		}

//		for (int i = 0; i < data.size(); i++) {
//			
////			instanceRowIndexMap.put((int) Math.round(data.get(i).), i);
//			System.out.println("Mapped: " + data.get(i).numAttributes());
//		}
//		System.out.println("Mapped: " + instanceRowIndexMap.toString());

		return data;

	}

	
//	private int getInstanceRowIndex(Instance inst) {
//		
//		if (!instanceRowIndexMap.containsKey(inst)) {
//			int i = -1;
//			if (inst != null) {
//				
//				int attr_index = inst.numAttributes() - 1;
//				
//				if (inst.attribute(attr_index).isNominal() || inst.attribute(attr_index).isString()) {
//					
//					int valIndex_1 = (int) inst.value(attr_index);
//					String val_1 = inst.attribute(attr_index).value(valIndex_1);
//					i = Integer.valueOf(val_1.substring(NEWICK_LEAF_NAME_PREFIX.length())); // turn "n125" to '125'
//					//System.out.println("Instance not contained in map, " + inst + " yet i = '" + i + "'.");
//					
//					System.out.println("Key k:" + 
//							inst.getClass().getName() + "@" + 
//					        Integer.toHexString(System.identityHashCode(inst)) + 
//					        " not found!");
//				}
//			}
//			//System.out.println("Instance " + inst + " not contained in map!");
//			return 0;
//		}
//		return instanceRowIndexMap.get(inst);
//	}


	//@Override
	public ClusteringResult performClustering(LinkType link/*Instances dataset, ParameterSet parameters*/, int minNumClusters) {

		List<Integer> clusters = new ArrayList<Integer>();
		/*HierarchicalClusterer*/ clusterer = new HierarchicalClustererWithProgress();
		
		//System.out.println(java.util.Arrays.asList(clusterer.listOptions()..values()));
		Enumeration e = clusterer.listOptions();
		while (e.hasMoreElements()) {
			Option o = ((Option)e.nextElement());
			System.out.println(o.name() + " | " + o.synopsis() + " | " + o.description());
		}
		System.out.println(Arrays.toString(clusterer.getOptions()));
		
		String[] options = new String[7];
		
		//	LinkType link = parameters.getParameter(
		//		HierarClustererParameters.linkType).getValue();
		//	DistanceType distanceType = parameters.getParameter(
		//		HierarClustererParameters.distanceType).getValue();

		//LinkType link = LinkType.AVERAGE;
		DistanceType distanceType = DistanceType.EUCLIDIAN;
		
		options[0] = "-L";
		options[1] = link.name();
		options[2] = "-A";
		switch (distanceType) {
		case EUCLIDIAN:
			options[3] = "weka.core.EuclideanDistance";
			break;
		case CHEBYSHEV:
			options[3] = "weka.core.ChebyshevDistance";
			break;
		case MANHATTAN:
			options[3] = "weka.core.ManhattanDistance";
			break;
		case MINKOWSKI:
			options[3] = "weka.core.MinkowskiDistance";
			break;
		}

		
		options[4] = "-N";
		options[5] = "1"; //"" + minNumClusters; //"1";
		
		options[6] = "-P";
//		options[7] = "-D";
		
//		options[8] = "-B";
		
//		options[8] = "-c";
//		options[9] = "last";

		try {
			clusterer.setOptions(options);
			System.out.println(Arrays.toString(clusterer.getOptions()));
			
	        // MEMORY STUFF
	        Runtime run_time = Runtime.getRuntime();
	        Long prevTotal = 0l;
	        Long prevFree = run_time.freeMemory();
			
	        //
            JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER START");

			DistanceFunction distanceFunction = new HierarDistanceFunction();
			distanceFunction.setInstances(dataSet);
			clusterer.setDistanceFunction(distanceFunction);

			System.out.println(Arrays.toString(clusterer.getOptions()));
			//clusterer.setPrintNewick(true);
			
			/*clustProgress.setProgress(0d);*/
			
			//System.out.println("Trying to bulid clusterer from:" + this.dataSet.numAttributes());
			clusterer.setClusteringProgression(clustProgress);
	        //
            JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (before...)");
            System.out.println("Trying to build clusterer from numInstances: " + this.dataSet.numInstances());
            //clusterer.buildClusterer(this.dataSet, this.distMtx);
//            if (JoinAlignerGCTask.USE_DOUBLE_PRECISION_FOR_DIST)
//            	clusterer.buildClustererGLG(this.dataSet, this.distVect, this.distThreshold);
//            else
//            	clusterer.buildClustererGLG(this.dataSet, this.distFloatVect, this.distThreshold);
            clusterer.buildClustererGLG(this.dataSet, this.distancesMtx, this.distThreshold);
            //
            JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER BUILD (after!!)");
			//
			Enumeration<?> e2 = dataSet.enumerateInstances();
			while (e2.hasMoreElements()) {
				clusters.add(clusterer.clusterInstance((Instance) e2.nextElement()));
				//System.out.println("\t-> " + clusters.get(clusters.size()-1));
			}
	        //
            JoinAlignerGCTask.printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER LISTED");
			
			ClusteringResult<Integer> result = new ClusteringResult<>(
					clusters, //null,
					//clusterer.toString(),
					clusterer.toStringGLG(),
					clusterer.getNumClusters(), 
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

	
	public HierarchicalClustererWithProgress getClusterer() {
		return clusterer;
	}

	class HierarDistanceFunction extends NormalizableDistance /*implements DistanceFunction*/ {

		private static final long serialVersionUID = 1L;

		@Override
		public double distance(Instance inst1, Instance inst2) {

//			int attr_index = inst1.numAttributes() - 1;
//////			
////////			for (int i=0; i<inst1.numAttributes(); i++) {
////////				System.out.println(inst1.attribute(i).name());
////////				for (int j=0; j<inst1.attribute(i).numValues(); j++) {
////////					System.out.println("\t - " + inst1.attribute(i).value(j));
////////				}
////////			}
//////			
////////			System.out.println("Instance: " + arg0);
////////			Enumeration e = arg0.enumerateAttributes();
////////			while (e.hasMoreElements()) {
////////				Attribute a = ((Attribute)e.nextElement());
////////				System.out.println("\tAttribute: " + a.toString());
////////				
////////				Enumeration ee = a.enumerateValues();
////////				while (ee.hasMoreElements()) {
////////					System.out.println("\t\tValue: " + ee.nextElement());
////////				}
////////			}
//////			//System.out.println(Arrays.toString(clusterer.getOptions()));
//////
//////			
////////			int i = Integer.valueOf(arg0.attribute(attr_index).value(0));
////////			int j = Integer.valueOf(arg1.attribute(attr_index).value(0));
////////			double d = arg0.value(attr_index);
////////			int i = Integer.valueOf(arg0.value(attr_index));
////////			int j = Integer.valueOf(arg1.attributes("id").value(0));
////
//			int i, j;
//			
//			// String ID
//			if (inst1.attribute(attr_index).isNominal() || inst1.attribute(attr_index).isString()) {
//				//int valIndex_1 = (int) inst1.value(attr_index);
//				String val_1 = inst1.attribute(attr_index).value((int) inst1.value(attr_index));
//				String val_2 = inst2.attribute(attr_index).value((int) inst2.value(attr_index));
//				if (val_1.startsWith(NEWICK_LEAF_NAME_PREFIX)) {
//					i = Integer.valueOf(val_1.substring(NEWICK_LEAF_NAME_PREFIX.length())); // turn "n125" to '125'
//					j = Integer.valueOf(val_2.substring(NEWICK_LEAF_NAME_PREFIX.length()));
//				} else {
//					i = Integer.valueOf(val_1);
//					j = Integer.valueOf(val_2);
//				}
//			} 
//			// Numerical ID
//			else {
//				double val_1 = inst1.value(attr_index);
//				i = (int) Math.round(val_1);
//				j = (int) Math.round(inst2.value(attr_index));
//			}
//			
//			//System.out.println("Using distMtx[" + i + "][" + j + "] = " + distMtx[i][j]);
//			
////			i = getInstanceRowIndex(inst1);
//////			j = getInstanceRowIndex(inst2);
//			
////			// Integer ID
////			i = (int) Math.round(inst1.value(attr_index));
////			j = (int) Math.round(inst2.value(attr_index));
//
			
			int i, j;
			i = ((ConcreteInstance) inst1).getUnid();
			j = ((ConcreteInstance) inst2).getUnid();
			
//			if (i > j) {
//				int swap = i;
//				i = j;
//				j = swap;
//			}
			
			//return distancesMap.get(new Pair<Integer, Integer>(i, j));
			
//			if (JoinAlignerGCTask.USE_DOUBLE_PRECISION_FOR_DIST)
//				return JoinAlignerGCTask.getValueFromVector(i, j, numInstances, distVect);
//			else
//				return JoinAlignerGCTask.getValueFromVector(i, j, numInstances, distFloatVect);
			return distancesMtx.get(i, j);
			/**
			return distMtx[i][j];
			*/
		}

		@Override
		public String getRevision() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String globalInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected double updateDistance(double currDist, double diff) {
			// TODO Auto-generated method stub
			return 0;
		}


	}
	
	
	public class ConcreteInstance implements Instance, Serializable, RevisionHandler {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		
		private int m_unid;
		
//		public void setUnid(int unid) {	
//			m_unid = unid;
//		}
		public int getUnid() {	
			return m_unid;
		}
		
		public ConcreteInstance(int unid) {
			this.m_unid = unid;
		}

		public ConcreteInstance(ConcreteInstance instance) {

			m_unid = instance.m_unid;
		}

		
		@Override
		public Object copy() {
			
			ConcreteInstance inst = new ConcreteInstance(this);
			return inst;
		}

		@Override
		public String getRevision() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Attribute attribute(int index) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Attribute attributeSparse(int indexOfIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Attribute classAttribute() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int classIndex() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean classIsMissing() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public double classValue() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Instance copy(double[] values) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Instances dataset() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void deleteAttributeAt(int position) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Enumeration<Attribute> enumerateAttributes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean equalHeaders(Instance inst) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public String equalHeadersMsg(Instance inst) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean hasMissingValue() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int index(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void insertAttributeAt(int position) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isMissing(int attIndex) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isMissingSparse(int indexOfIndex) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isMissing(Attribute att) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Instance mergeInstance(Instance inst) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int numAttributes() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int numClasses() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int numValues() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void replaceMissingValues(double[] array) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setClassMissing() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setClassValue(double value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setClassValue(String value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setDataset(Instances instances) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMissing(int attIndex) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setMissing(Attribute att) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setValue(int attIndex, double value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setValueSparse(int indexOfIndex, double value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setValue(int attIndex, String value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setValue(Attribute att, double value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setValue(Attribute att, String value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setWeight(double weight) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Instances relationalValue(int attIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Instances relationalValue(Attribute att) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String stringValue(int attIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String stringValue(Attribute att) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double[] toDoubleArray() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toStringNoWeight(int afterDecimalPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toStringNoWeight() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toStringMaxDecimalDigits(int afterDecimalPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString(int attIndex, int afterDecimalPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString(int attIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString(Attribute att, int afterDecimalPoint) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String toString(Attribute att) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public double value(int attIndex) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double valueSparse(int indexOfIndex) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double value(Attribute att) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double weight() {
			// TODO Auto-generated method stub
			return 0;
		}

		
	}
	
	
	
	

}
