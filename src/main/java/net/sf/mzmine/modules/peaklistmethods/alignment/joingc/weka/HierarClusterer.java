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
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.VisualizationType;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.hierarchical.DistanceType;
import net.sf.mzmine.parameters.ParameterSet;
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

public class HierarClusterer /*implements ClusteringAlgorithm*/ {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private static final String MODULE_NAME = "Hierarchical clusterer";

	private double[][] distMtx;
	Instances dataSet;

	private HierarchicalClusterer clusterer;
	
	
	public static String NEWICK_LEAF_NAME_PREFIX = "n"; //"node_" // "TreeParser" need a String as name (cannot go with only number Id) 
	
	//    @Override
	//    public @Nonnull String getName() {
	//	return MODULE_NAME;
	//    }

	public HierarClusterer(double[][] rawData) {
		
		distMtx = rawData;
		dataSet = createSampleWekaDataset(rawData);
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

        Attribute name = new Attribute("name", (FastVector) null);
        attributes.addElement(name);
//        Attribute id = new Attribute("id", (FastVector) null);
//        attributes.addElement(id);
//		Attribute var = new Attribute("id");
//		attributes.addElement(var);

		Instances data = new Instances("Dataset", attributes, 0);

		for (int i = 0; i < rawData.length; i++) {
			
			// Store distances
			double[] values = new double[data.numAttributes()];
			System.arraycopy(rawData[i], 0, values, 0, rawData[0].length);

			// Store Id
            values[data.numAttributes() - 1] = data.attribute("name").addStringValue(/*this.selectedRawDataFiles[i].getName()*/NEWICK_LEAF_NAME_PREFIX + i);
//            values[data.numAttributes() - 1] = data.attribute("id").addStringValue("" + i);
//            values[data.numAttributes() - 1] = i;

			Instance inst = new SparseInstance(1.0, values); //BinarySparseInstance(1.0, values); // DenseInstance(1.0, values);
			inst.setDataset(data);
			data.add(inst);
		}
		
		
		
		
		
//		Instances newData = new Instances(new Instances("Dataset2", new FastVector(), 0)/*data*/);
//        // add new attributes
//        // 1. nominal
//        FastVector values = new FastVector(); /* FastVector is now deprecated. Users can use any java.util.List */
//        values.addElement("A");               /* implementation now */
//        values.addElement("B");
//        values.addElement("C");
//        values.addElement("D");
//        newData.insertAttributeAt(new Attribute("NewNominal", values), newData.numAttributes());
//        // 2. numeric
//        newData.insertAttributeAt(new Attribute("NewNumeric"), newData.numAttributes());
//
//        for (int i = 0; i < 5; i++) {
//			Instance inst = new SparseInstance(newData.numAttributes());
//			//inst.setDataset(data);
//			newData.add(inst);
//        }
//		
//        // random values
//        Random rand = new Random(1);
//        for (int i = 0; i < newData.numInstances(); i++) {
//          // 1. nominal
//          // index of labels A:0,B:1,C:2,D:3
//          newData.instance(i).setValue(newData.numAttributes() - 2, rand.nextInt(4));
//          // 2. numeric
//          newData.instance(i).setValue(newData.numAttributes() - 1, rand.nextDouble());
//        }
//   
//        // output on stdout
//        System.out.println(newData);
		
		
		
      // output on stdout
      //System.out.println(data);
		
		return data;
	}


	//@Override
	public ClusteringResult performClustering(LinkType link/*Instances dataset, ParameterSet parameters*/) {

		List<Integer> clusters = new ArrayList<Integer>();
		/*HierarchicalClusterer*/ clusterer = new HierarchicalClusterer();
		
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
		options[5] = "1";
		
		options[6] = "-P";
//		options[7] = "-D";
		
//		options[8] = "-B";
		
//		options[8] = "-c";
//		options[9] = "last";

		try {
			clusterer.setOptions(options);
			System.out.println(Arrays.toString(clusterer.getOptions()));

			DistanceFunction distanceFunction = new HierarDistanceFunction();
			distanceFunction.setInstances(dataSet);
			clusterer.setDistanceFunction(distanceFunction);

			System.out.println(Arrays.toString(clusterer.getOptions()));
			//clusterer.setPrintNewick(true);
			
			//System.out.println("Trying to bulid clusterer from:" + this.dataSet.numAttributes());
			clusterer.buildClusterer(this.dataSet);//dataset);
			// clusterer.graph() gives only the first cluster and in the case
			// there
			// are more than one cluster the variables in the second cluster are
			// missing.
			// I'm using clusterer.toString() which contains all the clusters in
			// Newick format.
			Enumeration<?> e2 = dataSet.enumerateInstances();
			while (e2.hasMoreElements()) {
				clusters.add(clusterer.clusterInstance((Instance) e2.nextElement()));
				System.out.println("\t-> " + clusters.get(clusters.size()-1));
			}
			
			ClusteringResult result = new ClusteringResult(
					null, //clusters, //null,
					clusterer.toString(), 
					clusterer.getNumClusters(), 
					null //VisualizationType.PCA //null
					);
			//System.out.println("###>>> " + clusterer.graph());
			//System.out.println("###>>> " + clusterer.getNumClusters());
			return result;
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.log(Level.SEVERE, null, ex);
			return null;
		}
	}

	//    @Override
	//    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	//	return HierarClustererParameters.class;
	//    }

	public HierarchicalClusterer getClusterer() {
		return clusterer;
	}

	class HierarDistanceFunction extends NormalizableDistance /*implements DistanceFunction*/ {


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
			
			// String ID
			if (inst1.attribute(attr_index).isNominal() || inst1.attribute(attr_index).isString()) {
				int valIndex_1 = (int) inst1.value(attr_index);
				String val_1 = inst1.attribute(attr_index).value(valIndex_1);
				i = Integer.valueOf(val_1.substring(NEWICK_LEAF_NAME_PREFIX.length())); // turn "n_125" to "125"
				j = Integer.valueOf(inst2.attribute(attr_index).value((int) inst2.value(attr_index)).substring(NEWICK_LEAF_NAME_PREFIX.length()));
			} 
			// Numerical ID
			else {
				double val_1 = inst1.value(attr_index);
				i = (int) Math.round(val_1);
				j = (int) Math.round(inst2.value(attr_index));
			}
			
			//System.out.println("Using distMtx[" + i + "][" + j + "] = " + distMtx[i][j]);
			
			return distMtx[i][j];
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


}
