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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.collections.CollectionUtils;
import org.gnf.clustering.DataSource;
import org.gnf.clustering.DistanceCalculator;
import org.gnf.clustering.DistanceMatrix;
import org.gnf.clustering.DistanceMatrix1D;
import org.gnf.clustering.DistanceMatrix2D;
import org.gnf.clustering.FloatSource1D;
import org.gnf.clustering.FloatSource2D;
import org.gnf.clustering.HierarchicalClustering;
import org.gnf.clustering.LinkageMode;
import org.gnf.clustering.hybrid.CentroidsCalculator;
import org.gnf.clustering.hybrid.DefaultCentroidsCalculator;
import org.gnf.clustering.hybrid.HybridClustering;
import org.gnf.clustering.hybrid.TanimotoCentroidsCalculator;
import org.gnf.clustering.sequentialcache.SequentialCacheClustering;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.ClustererType;
//import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.DBSCANClusterer;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.HDBSCANClusterer;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.HDBSCANClustererELKI;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.HierarClusterer;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.LinkType;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.Tree;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.TreeNode;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.TreeParser;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid.HybridDistanceCalculator;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.hybrid.RowVsRowDistanceProvider;
//import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.weka.XMeansClusterer;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering.ClusteringResult;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationSingleTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataFileUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import weka.gui.hierarchyvisualizer.HierarchyVisualizer;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusterPair;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.CompleteLinkageStrategy;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.apporiented.algorithm.clustering.Distance;
import com.apporiented.algorithm.clustering.DistanceMap;
import com.apporiented.algorithm.clustering.HierarchyBuilder;
import com.apporiented.algorithm.clustering.LinkageStrategy;
import com.apporiented.algorithm.clustering.SingleLinkageStrategy;
import com.apporiented.algorithm.clustering.WeightedLinkageStrategy;
import com.apporiented.algorithm.clustering.visualization.DendrogramPanel;
import com.carrotsearch.sizeof.RamUsageEstimator;
//import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Range;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.HDBSCANLinearMemory;
import jebl.evolution.graphs.Node;
import jebl.evolution.io.ImportException;
import jebl.evolution.io.NewickExporter;
import jebl.evolution.io.NewickImporter;
import jebl.evolution.trees.RootedTree;



//import de.lmu.ifi.dbs.elki.algorithm.DistanceBasedAlgorithm;
//import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
//import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
//import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
//import de.lmu.ifi.dbs.elki.logging.Logging;
//import de.lmu.ifi.dbs.elki.result.Result;




public class JoinAlignerGCTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static String TASK_NAME = "Join aligner GC";

	private final MZmineProject project;
	private PeakList peakLists[];
	private PeakList alignedPeakList;

	// Processed rows counter
	private int processedRows, totalRows;

	private String peakListName;
	//private RowVsRowOrderType comparisonOrder;
	//private ClusteringLinkageStrategyType linkageStartegyType_0;
	private LinkType linkageStartegyType_12;
	private LinkageMode linkageStartegyType_20;
	
	//private boolean use_hybrid_K;
	private int hybrid_K_value;

	
	private boolean saveRAMratherThanCPU_1;
	private boolean saveRAMratherThanCPU_2;
	//
	private boolean useOldestRDFAncestor;
	private MZTolerance mzTolerance;
	private RTTolerance rtTolerance;
	private double mzWeight, rtWeight;
	private double minScore;
	private double idWeight;
	//
	private boolean useApex, useKnownCompoundsAsRef;
	private boolean useDetectedMzOnly;
	private RTTolerance rtToleranceAfter;
	//
	private boolean exportDendrogramAsPng;
	private File dendrogramPngFilename;
	private boolean exportDendrogramAsTxt;
	private File dendrogramTxtFilename;
	DendrogramFormatType dendrogramFormatType;
	private boolean exportDendrogramNewickTxt;
	private File dendrogramNewickTxtFilename;


	/** GLG HACK: temporary removed for clarity
    private boolean sameIDRequired, sameChargeRequired, compareIsotopePattern;
	 **/
	private ParameterSet parameters;

	// ID counter for the new peaklist
	private int newRowID = 1;

	//
	private Format rtFormat = MZmineCore.getConfiguration().getRTFormat();

	//
	private final double maximumScore; // = 1.0d;
	private final float veryLongDistance; // = 1.0d;//50.0d;//Double.MAX_VALUE;
	// For comparing small differences.
	public static final double EPSILON = 0.0000001;


	private static final boolean DEBUG = false;
	private static final boolean DEBUG_2 = false;
	List<PeakListRow> full_rows_list;


	private ClusteringProgression clustProgress; 

	//private int clustering_method;

	private ClustererType CLUSTERER_TYPE; // = 5; //0:Hierar, 1:KMeans, 2:XMeans, 3:Cobweb, 4:OPTICS/DBSCAN, 5:HDBSCAN (Star/ELKI)
	//    private static int K;
	//    private static int MIN_CLUSTER_SIZE;
	//    
	//    private static boolean USE_CONSTRAINTS;
	//    private static boolean SELF_EGDES;



	public static final boolean USE_DOUBLE_PRECISION_FOR_DIST = false;



	JoinAlignerGCTask(MZmineProject project, ParameterSet parameters) {

		this.project = project;
		this.parameters = parameters;

		peakLists = parameters.getParameter(JoinAlignerGCParameters.peakLists)
				.getValue().getMatchingPeakLists();

		peakListName = parameters.getParameter(
				JoinAlignerGCParameters.peakListName).getValue();

		// Since clustering is now order independent, option removed!
		/*
        comparisonOrder = parameters.getParameter(
                JoinAlignerGCParameters.comparisonOrder).getValue();
		 */

		//        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
		//        	linkageStartegyType_0 = parameters.getParameter(
		//        			JoinAlignerGCParameters.linkageStartegyType_0).getValue();
		//        } else {
		//        	linkageStartegyType_12 = parameters.getParameter(
		//        			JoinAlignerGCParameters.linkageStartegyType_12).getValue();
		//        }

		saveRAMratherThanCPU_1 = parameters.getParameter(
				JoinAlignerGCParameters.saveRAMratherThanCPU_1).getValue();
		saveRAMratherThanCPU_2 = parameters.getParameter(
				JoinAlignerGCParameters.saveRAMratherThanCPU_2).getValue();
		
		useOldestRDFAncestor = parameters.getParameter(
				JoinAlignerGCParameters.useOldestRDFAncestor).getValue();

		mzTolerance = parameters
				.getParameter(JoinAlignerGCParameters.MZTolerance).getValue();
		rtTolerance = parameters
				.getParameter(JoinAlignerGCParameters.RTTolerance).getValue();

		mzWeight = parameters.getParameter(JoinAlignerGCParameters.MZWeight)
				.getValue();

		rtWeight = parameters.getParameter(JoinAlignerGCParameters.RTWeight)
				.getValue();

		minScore = parameters.getParameter(JoinAlignerGCParameters.minScore)
				.getValue();

		//        idWeight = parameters.getParameter(JoinAlignerParameters.IDWeight)
		//                .getValue();
		idWeight = 0.0;


		//***
		useApex = parameters.getParameter(
				JoinAlignerGCParameters.useApex).getValue();
		useKnownCompoundsAsRef = parameters.getParameter(
				JoinAlignerGCParameters.useKnownCompoundsAsRef).getValue();
		useDetectedMzOnly = parameters.getParameter(
				JoinAlignerGCParameters.useDetectedMzOnly).getValue();
		rtToleranceAfter = parameters.getParameter(
				JoinAlignerGCParameters.RTToleranceAfter).getValue();
		//***

		/** GLG HACK: temporarily removed for clarity
        sameChargeRequired = parameters.getParameter(
                JoinAlignerParameters.SameChargeRequired).getValue();

        sameIDRequired = parameters.getParameter(
                JoinAlignerParameters.SameIDRequired).getValue();

        compareIsotopePattern = parameters.getParameter(
                JoinAlignerParameters.compareIsotopePattern).getValue();
		 **/

		//        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
		//	        exportDendrogramAsPng = parameters.getParameter(
		//	                JoinAlignerGCParameters.exportDendrogramPng).getValue();
		//	        dendrogramPngFilename = parameters.getParameter(
		//	                JoinAlignerGCParameters.dendrogramPngFilename).getValue();
		////	        exportDendrogramAsTxt = parameters.getParameter(
		////	                JoinAlignerGCParameters.exportDendrogramTxt).getValue();
		////	        dendrogramTxtFilename = parameters.getParameter(
		////	                JoinAlignerGCParameters.dendrogramTxtFilename).getValue();
		//        } else {
		////        	exportDendrogramNewickTxt = parameters.getParameter(
		////        			JoinAlignerGCParameters.exportDendrogramNewickTxt).getValue();
		////        	dendrogramNewickTxtFilename = parameters.getParameter(
		////        			JoinAlignerGCParameters.dendrogramNewickTxtFilename).getValue();
		//        }
		exportDendrogramAsTxt = parameters.getParameter(
				JoinAlignerGCParameters.exportDendrogramTxt).getValue();
		dendrogramTxtFilename = parameters.getParameter(
				JoinAlignerGCParameters.dendrogramTxtFilename).getValue();
		//
		if (JoinAlignerGCParameters.CLUST_METHOD >= 1) {
			dendrogramFormatType = parameters.getParameter(
					JoinAlignerGCParameters.dendrogramFormatType).getValue();
		}

		//

		CLUSTERER_TYPE = parameters.getParameter(
				JoinAlignerGCParameters.clusterer_type).getValue();//.ordinal();
		//        K = parameters.getParameter(
		//                JoinAlignerGCParameters.clusterer_k).getValue();
		//        MIN_CLUSTER_SIZE = parameters.getParameter(
		//                JoinAlignerGCParameters.clusterer_minClusterSize).getValue();
		//        
		//        USE_CONSTRAINTS = parameters.getParameter(
		//                JoinAlignerGCParameters.clusterer_useConstraints).getValue();
		//        SELF_EGDES = parameters.getParameter(
		//                JoinAlignerGCParameters.clusterer_selfEdges).getValue();

		// 
		ClusteringLinkageStrategyType linkageStartegyType_0 = parameters.getParameter(
				JoinAlignerGCParameters.linkageStartegyType_0).getValue();
		if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD) {
			switch (linkageStartegyType_0) {
			case SINGLE:
				linkageStartegyType_12 = LinkType.SINGLE;
				break;
			case AVERAGE:
				linkageStartegyType_12 = LinkType.AVERAGE;
				break;
			case COMPLETE:
				linkageStartegyType_12 = LinkType.COMPLETE;
				break;
			default:
				break;
			}
		} else { //if (CLUSTERER_TYPE == 1 || CLUSTERER_TYPE == 2) {
			switch (linkageStartegyType_0) {
			case SINGLE:
				linkageStartegyType_20 = LinkageMode.MIN;
				break;
			case AVERAGE:
				linkageStartegyType_20 = LinkageMode.AVG;
				break;
			case COMPLETE:
				linkageStartegyType_20 = LinkageMode.MAX;
				break;
			default:
				break;
			}
		}
		
		this.hybrid_K_value = parameters.getParameter(
				JoinAlignerGCParameters.hybrid_K_value).getValue();

		//
		maximumScore = mzWeight + rtWeight;
		veryLongDistance = (float) (10.0 * maximumScore);


		//
		clustProgress = new ClusteringProgression();
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Join aligner GC, " + peakListName + " (" + peakLists.length
				+ " peak lists)";
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalRows == 0)
			return 0f;
		//return (double) processedRows / (double) totalRows;
		double progress = (double) (processedRows + (clustProgress.getProgress() * (double) totalRows / 3.0d)) / (double) totalRows;
		//        System.out.println(">> THE progress: " + progress);
		//        System.out.println("Caught progress: " + clustProgress.getProgress());
		return progress;
	}

	/**
	 * @see Runnable#run()
	 */
	@SuppressWarnings("unchecked")
	public void run() {

		
//		try {
//			for (int x =0 ; x < 100; x++) {
//				//ConsoleProgress.updateProgress((double) x / 100d);
//				try {
//					ConsoleProgress.updateProgressAnimated((double) x / 100d);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				Thread.sleep(20);
//			}
//        } catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        
		// Check options validity
		if ((Math.abs(mzWeight) < EPSILON) && (Math.abs(rtWeight) < EPSILON)) {
			setStatus(TaskStatus.ERROR);
			setErrorMessage("Cannot run alignment, all the weight parameters are zero!");
			return;
		}

		setStatus(TaskStatus.PROCESSING);
		logger.info("Running join aligner");

		// TIME STUFF
		long startTime, endTime;
		float ms;
		//
		startTime = System.currentTimeMillis();

		// MEMORY STUFF
		Runtime run_time = Runtime.getRuntime();
		Long prevTotal = 0l;
		Long prevFree = run_time.freeMemory();
		printMemoryUsage(run_time, prevTotal, prevFree, "START TASK...");


		// Remember how many rows we need to process. Each row will be processed
		// /*twice*/ three times:
		//      - first for score calculation
		//      - second for creating linkages
		//      - third for actual alignment
		for (int i = 0; i < peakLists.length; i++) {
			totalRows += peakLists[i].getNumberOfRows() * 3;
		}

		// Collect all data files
		Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
		for (PeakList peakList : peakLists) {

			for (RawDataFile dataFile : peakList.getRawDataFiles()) {

				// Each data file can only have one column in aligned peak list
				if (allDataFiles.contains(dataFile)) {
					setStatus(TaskStatus.ERROR);
					setErrorMessage("Cannot run alignment, because file "
							+ dataFile + " is present in multiple peak lists");
					return;
				}

				allDataFiles.add(dataFile);
			}
		}

		// Create a new aligned peak list
		alignedPeakList = new SimplePeakList(peakListName,
				allDataFiles.toArray(new RawDataFile[0]));


		/** RTAdjustement mapping **/
		boolean recalibrateRT = useKnownCompoundsAsRef;

		//Hashtable<RawDataFile, double[]> rtAdjustementMapping = new Hashtable<>();
		Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping = new Hashtable<>();

		if (recalibrateRT) {

			boolean rtAdjustOk = true;
			// Iterate source peak lists
			// RT mapping based on the first PeakList
			//            double rt1 = -1.0;
			//            double rt2 = -1.0;

			// (0): Need all list to have AT LEAST all identities of reference list (first list in selection = peakLists[0])
			// TODO: Smarter way (1): Allow having less ref identities in other lists and fill the gap!
			// TODO: Smarter way (2): Or, at least use compounds found in all selected lists!
			ArrayList<String> allIdentified_0 = new ArrayList<>();

			// (0) => Removed
			//---------------------------------------------------  
			// Going on with (2)
			/** Find ref compound names 'over all' lists */
			//List<PeakListRow> allIdentified_all_rows = new ArrayList<>();
			List<String> allIdentified_all_ident = new ArrayList<>();
			//Set<String> allIdentified_unique_ident = new HashSet<>();
			Map<String, ArrayList<PeakListRow>> identRowsMap = new HashMap<>();
			//List<PeakListRow> allIdentified = new ArrayList<>();
			for (int i=0; i < peakLists.length; ++i) {
				for (PeakListRow a_pl_row : peakLists[i].getRows()) {

					// If row actually was identified AND is a "reference compound"
					if (JDXCompound.isKnownIdentity(a_pl_row.getPreferredPeakIdentity())) {
						String isRefCompound = a_pl_row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF);
						if (isRefCompound != null && isRefCompound.equals(AlignedRowProps.TRUE)) {
							//allIdentified_all_ident.add(a_pl_row.getPreferredPeakIdentity().getName());
							//allIdentified_unique_ident.add(a_pl_row.getPreferredPeakIdentity().getName());
							//allIdentified_all_rows.add(a_pl_row);

							String key = a_pl_row.getPreferredPeakIdentity().getName();

							if (!identRowsMap.containsKey(key)) {
								identRowsMap.put(key, new ArrayList<PeakListRow>());
							}
							identRowsMap.get(key).add(a_pl_row);
							//allIdentified_all_rows.add(a_pl_row);
							allIdentified_all_ident.add(key);

							//System.out.println("Found ident: " + key + " for row: " + a_pl_row + "(PL: " + peakLists[i].getRawDataFile(0).getName() + ")");
						}
					}
				}
			}
			/** Find ref compound names 'common' to all lists */
			//Collections.frequency(animals, "bat");
			////for (Map.Entry<String, ArrayList<PeakListRow>> entry : identRowsMap.entrySet()) {
			Iterator< Map.Entry<String, ArrayList<PeakListRow>> > iter = identRowsMap.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, ArrayList<PeakListRow>> entry = iter.next();

				String name = entry.getKey();

				// This identity wasn't found in all lists
				if (Collections.frequency(allIdentified_all_ident, name) != peakLists.length) {
					// Remove all row entries related to this missing identity
					//            		for (PeakListRow a_pl_row : entry.getValue()) {
					//            			allIdentified_all_rows.remove(a_pl_row);
					//            		}
					////identRowsMap.remove(name);
					iter.remove();

					//System.out.println("Removed ident: " + name + "(found only '" + Collections.frequency(allIdentified_all_ident, name) + "' / " + peakLists.length + ")");
				}
				//System.out.println(Arrays.toString(allIdentified_all_rows.toArray()));
			}
			//System.out.println("Final left ident: \n\t => " + Arrays.toString(allIdentified_all_rows.toArray()));

			//            // 
			//            for (ArrayList<PeakListRow> identified_rows : identRowsMap.values()) {
			//            	
			//            	for (PeakListRow a_pl_row : identified_rows) {
			//            		
			//            	}
			//            }

			//
			for (int i=0; i < peakLists.length; ++i) {
				//double offset, scale;
				PeakList a_pl = peakLists[i];

				logger.info("# Search identities for list: " + a_pl.getRawDataFile(0) + " (nb peakLists = " + peakLists.length + ")");

				// Get ref RT1 and RT2
				// Sort peaks by ascending RT
				PeakListRow a_pl_rows[] = a_pl.getRows().clone(); 
				Arrays.sort(a_pl_rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
				//
				ArrayList<PeakListRow> allIdentified = new ArrayList<>();
				//ArrayList<String> allIdentified_i = new ArrayList<>();
				List<Double> rtList = new ArrayList<>();
				//
				for (int j=0; j < a_pl_rows.length; ++j) {

					PeakListRow row = a_pl_rows[j];

					// If row actually was identified AND is a "reference compound"

					//                    if (JDXCompound.isKnownIdentity(row.getPreferredPeakIdentity())) {
					//                        
					//                        logger.info("\t* Trying with identity: " + row.getPreferredPeakIdentity().getName());
					//                        
					//                        // Is a "reference compound"
					//                        String isRefCompound = row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF);
					//                        if (isRefCompound != null && isRefCompound.equals(AlignedRowProps.TRUE)) {
					//                            
					//                            logger.info("\t\t !! OK with identity: " + row.getPreferredPeakIdentity().getName());
					//                            
					//                            if (i == 0) {
					//                                allIdentified.add(row);
					//                                allIdentified_0.add(row.getPreferredPeakIdentity().getName());
					//                            } else if (allIdentified_0.contains(row.getPreferredPeakIdentity().getName())) {
					//                                allIdentified.add(row);
					//                                allIdentified_i.add(row.getPreferredPeakIdentity().getName());
					//                            }
					//                                
					//                        }
					//                    } else {
					////                            logger.info("aFailed 0: " + (row.getPreferredPeakIdentity().getName() != JDXCompound.UNKNOWN_JDX_COMP.getName()));
					////                            logger.info("aFailed 1: " + row.getPreferredPeakIdentity());
					////                            logger.info("aFailed 2: " + row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF));                       
					//                    }

					/**
                    if (allIdentified_all_rows.contains(row)) {
                    	allIdentified.add(row);
                    	System.out.println("## Found identified for row: " + row);
                    }
					 */
					//                    for (Map.Entry<String, ArrayList<PeakListRow>> entry : identRowsMap.entrySet()) {
					for (ArrayList<PeakListRow> rows : identRowsMap.values()) {
						if (rows.contains(row)) {
							allIdentified.add(row);
							//System.out.println("## Found identified for row: " + row);
						}
					}


				}

				//                // Free mem
				//                allIdentified_all_ident = null;
				//                identRowsMap = null;


				//                //
				//                logger.info("> allIdentified: NB found compounds: " + allIdentified.size());
				//                boolean allRefsFound = true;
				//                for (String name : allIdentified_0) {
				//                    
				//                    if (i > 0 && !allIdentified_i.contains(name)) {
				//                        allRefsFound = false;
				//                        logger.info("\t> Ref compound '" + name + "' not found for list '" + peakLists[i].getRawDataFile(0) + "'!");
				//                    }
				//                }
				//                //
				//                if (!allRefsFound) {
				//                    //logger.info("> Compound(s) missing for list '" + peakLists[i].getRawDataFile(0) + "' => ABORT!!");
				//                    setStatus(TaskStatus.ERROR);
				//                    setErrorMessage("> Ref compound(s) missing for list '" + peakLists[i].getRawDataFile(0) + "' => ABORT!!");
				//                    return;
				//                }


				if (allIdentified.size() > 0) {

					// Deal with ref compounds intervals 
					//          - 1 pair [offset,scale] for each interval
					//          - special treatment for peaks popping BEFORE the FIRST ref compound
					//          - special treatment for peaks popping AFTER the LAST ref compound

					// Storing RTs (one per ref compound)
					for (PeakListRow r : allIdentified) {
						rtList.add(r.getAverageRT());
					}

				} else {

					// Do nothing
					logger.info("! Warn: Couldn't identify at least 1 ref compound for peaklist: " + a_pl
							+ " => No RT recalibration (not gonna apply any offset, nor scale) !");
					rtAdjustOk = false;
					continue;

				}

				boolean offset_only = (rtList.size() == 1);
				// Think of "y = ax+b" (line equation)
				double b_offset, a_scale;

				RawDataFile refPL_RDF = peakLists[0].getRawDataFile(0);

				// We have ref(s) to work with
				if (rtList.size() > 1) {

					for (int i_rt = 0; i_rt < (rtList.size() - 1); i_rt++) {

						double rt1 = rtList.get(i_rt);
						//                        double rt2;
						double rt2 = rtList.get(i_rt + 1);

						//                        // If not "Offset only" case, set 'rt2'
						//                        if (!offset_only)
						//                            rt2 = rtList.get(i_rt + 1);
						//                        // Unset otherwise
						//                        else
						//                            rt2 = -1.0d;


						// First list as ref, so:
						if (i == 0) {

							b_offset = 0.0d;                      
							a_scale = 0.0d;

							//                            // If "Offset only" case, unset scale
							//                            if (offset_only)
							//                                a_scale = -1.0d;

							//
							////rtAdjustementMapping.put(a_pl.getRawDataFile(0), new double[]{ b_offset, a_scale, rt1, rt2 });
							if (rtAdjustementMapping.get(a_pl.getRawDataFile(0)) == null) {
								rtAdjustementMapping.put(a_pl.getRawDataFile(0), new ArrayList<double[]>());
							}
							rtAdjustementMapping.get(a_pl.getRawDataFile(0)).add(new double[]{ b_offset, a_scale, rt1, rt2 });

						} else {

							//double rt1_ref = rtAdjustementMapping.get(refPL_RDF)[2];
							//double rt2_ref = rtAdjustementMapping.get(refPL_RDF)[3];
							double rt1_ref = rtAdjustementMapping.get(refPL_RDF).get(i_rt)[2];
							double rt2_ref = rtAdjustementMapping.get(refPL_RDF).get(i_rt)[3];
							//
							a_scale = ((rt2_ref - rt2) - (rt1_ref - rt1)) / (rt2 - rt1);
							b_offset = (rt1_ref - rt1) - (a_scale * rt1);

							//                         // If "Offset only" case, unset scale
							//                            if (offset_only)
							//                                a_scale = -1.0d;

							//
							////rtAdjustementMapping.put(a_pl.getRawDataFile(0), new double[]{ b_offset, a_scale, rt1, rt2 });
							if (rtAdjustementMapping.get(a_pl.getRawDataFile(0)) == null) {
								rtAdjustementMapping.put(a_pl.getRawDataFile(0), new ArrayList<double[]>());
							}
							rtAdjustementMapping.get(a_pl.getRawDataFile(0)).add(new double[]{ b_offset, a_scale, rt1, rt2 });

							logger.info(">> peakLists[0]/peakLists[i]:" + peakLists[0] + "/" + peakLists[i]);
							logger.info(">> rt1_ref/rt1:" + rt1_ref + "/" + rt1);
							logger.info(">> rt2_ref/rt2:" + rt2_ref + "/" + rt2);
							logger.info(">> offset/scale: " + b_offset + "/" + a_scale);
						}


						//                        // If not "Offset only" case, skip last iteration
						//                        if (!offset_only && (i_rt == rtList.size() - 2)) {
						//                            break;
						//                        }
					}

				} else if (offset_only) {

					double rt1 = rtList.get(0);
					double rt2 = -1.0d;

					if (i == 0) {

						b_offset = 0.0d;                      
						a_scale = 0.0d;

						if (rtAdjustementMapping.get(a_pl.getRawDataFile(0)) == null) {
							rtAdjustementMapping.put(a_pl.getRawDataFile(0), new ArrayList<double[]>());
						}
						rtAdjustementMapping.get(a_pl.getRawDataFile(0)).add(new double[]{ b_offset, a_scale, rt1, rt2 });

					} else {

						double rt1_ref = rtAdjustementMapping.get(refPL_RDF).get(0)[2];
						a_scale = 0.0d;
						b_offset = (rt1_ref - rt1); // - (a_scale * rt1);

						if (rtAdjustementMapping.get(a_pl.getRawDataFile(0)) == null) {
							rtAdjustementMapping.put(a_pl.getRawDataFile(0), new ArrayList<double[]>());
						}
						rtAdjustementMapping.get(a_pl.getRawDataFile(0)).add(new double[]{ b_offset, a_scale, rt1, rt2 });

					}

				}

			}
			//
			if (!rtAdjustOk) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Cannot run alignment, because ref compounds detection was incomplete");
				return;
			}
		}

		printMemoryUsage(run_time, prevTotal, prevFree, "COMPOUND DETECTED");

		/** Alignment mapping **/ 
		// Iterate source peak lists
		Hashtable<SimpleFeature, Double> rtPeaksBackup = new Hashtable<SimpleFeature, Double>();
		Hashtable<PeakListRow, Object[]> infoRowsBackup = new Hashtable<PeakListRow, Object[]>();

		// Since clustering is now order independent, option removed!
		// Build comparison order
		ArrayList<Integer> orderIds = new ArrayList<Integer>();
		for (int i=0; i < peakLists.length; ++i) { orderIds.add(i); }
		/*
        logger.info("ORDER: " + comparisonOrder);
        if (comparisonOrder == RowVsRowOrderType.RANDOM) {
            Collections.shuffle(orderIds);
        } else if (comparisonOrder == RowVsRowOrderType.REVERSE_SEL) {
            Collections.reverse(orderIds);
        }
		 */
		Integer[] newIds = orderIds.toArray(new Integer[orderIds.size()]);
		//


		//        double[][] distances = null;
		TriangularMatrix distances = null;
		DistanceMatrix distancesGNF_Tri = null;
		DistanceMatrix distancesGNF_Tri_Bkp = null;
		

		String[] short_names;
		int nbPeaks = 0;
		for (int i = 0; i < newIds.length; ++i) {
			PeakList peakList = peakLists[newIds[i]];
			nbPeaks += peakList.getNumberOfRows();
			logger.info("> Peaklist '" + peakList.getName() + "' [" + newIds[i] + "] has '" + peakList.getNumberOfRows() + "' rows.");
		}
		
		// If 'Hybrid' or no distance matrix: no need for a matrix
		if (CLUSTERER_TYPE == ClustererType.HYBRID || !saveRAMratherThanCPU_1) {
			//distances = new double[nbPeaks][nbPeaks];
			if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD) {
				if (USE_DOUBLE_PRECISION_FOR_DIST) {
					distances = new TriangularMatrixDouble(nbPeaks);
				} else {
					distances = new TriangularMatrixFloat(nbPeaks);
				}
			} else { //if (CLUSTERER_TYPE == 1 || CLUSTERER_TYPE == 2) {
				
				int nRowCount = nbPeaks;
				//distancesGNF_Tri = nRowCount < 20000 ? new DistanceMatrix1D(nRowCount) : new DistanceMatrixTriangular1D2D(nRowCount);
				distancesGNF_Tri = new DistanceMatrixTriangular1D2D(nRowCount);
			}
		}

		short_names = new String[nbPeaks];

		Map<String, PeakListRow> row_names_dict = new HashMap<>();
		full_rows_list = new ArrayList<>();

		
		/**
//		for (int i = 0; i < nRowCount-1; i++)
//		{
//			for (int j = i+1 ; j < nRowCount; j++) 
//			{
//				mtx.setValue(i, j, Float.valueOf(reader.readLine()));
//			}
//		}
		*/
//		// Calculate limits for a row with which the row can be aligned
//		//            Range<Double> mzRange = mzTolerance.getToleranceRange(row
//		//                    .getAverageMZ());
//		//            Range<Double> rtRange = rtTolerance.getToleranceRange(row
//		//                    .getAverageRT());
//		// GLG HACK: Use best peak rather than average. No sure it is better... ???
//		PeakListRow any_row = peakLists[newIds[0]].getRow(0);
//		Range<Double> mzRange = mzTolerance.getToleranceRange(any_row
//				.getBestPeak().getMZ());
//		Range<Double> rtRange = rtTolerance.getToleranceRange(any_row
//				.getBestPeak().getRT());

//		Map<String, String> dendro_names_dict = new HashMap<>();
//		List<String> rows_list = new ArrayList<>();
//		long short_names_unid = 0;
//		String long_name;
		for (int i = 0; i < newIds.length; ++i) {

		    PeakList peakList = peakLists[newIds[i]];

		    PeakListRow allRows[] = peakList.getRows();
		    for (int j = 0; j < allRows.length; ++j) {

		        PeakListRow row = allRows[j];

//		        if (exportDendrogramAsTxt) {
//
//		            int x = full_rows_list.size();
//
//		            // Each name HAS to be unique
//		            //short_names[x] = String.valueOf(short_names_unid); //"x" + short_names_id; //"[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
//		            short_names[x] = "" + short_names_unid; //"x" + short_names_id; //"[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
//
//		            // Produce human readable names
//		            RawDataFile ancestorRDF = DataFileUtils.getAncestorDataFile(project, peakList.getRawDataFile(0), true);
//		            // Avoid exception if ancestor RDFs have been removed...
//		            String suitableRdfName = (ancestorRDF == null) ? peakList.getRawDataFile(0).getName() : ancestorRDF.getName();
//		            long_name = "[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
//		            dendro_names_dict.put(short_names[x], long_name);
//		            //short_names_unid.add(BigInteger.ONE);
//		            short_names_unid++;
//
//		            row_names_dict.put(short_names[x], row);
//		            rows_list.add(row.getBestPeak().getDataFile() + ", @" + row.getBestPeak().getRT());
//		        }

		        full_rows_list.add(row);
		    }
		}
		RowVsRowDistanceProvider distProvider = new RowVsRowDistanceProvider(
		        project, useOldestRDFAncestor,
		        rtAdjustementMapping, full_rows_list, 
		        mzWeight, rtWeight,
		        useApex,
		        useKnownCompoundsAsRef,
		        useDetectedMzOnly,
		        rtToleranceAfter,
		        maximumScore);


//		// Go build matrix if required!
//		int x = 0;
//		for (int i = 0; i < newIds.length; ++i) {
//
//
//			PeakList peakList = peakLists[newIds[i]];
//
//			PeakListRow allRows[] = peakList.getRows();
//			logger.info("Treating list " + peakList + " / NB rows: " + allRows.length);
//
//			// Calculate scores for all possible alignments of this row
//			for (int j = 0; j < allRows.length; ++j) {
//
//				/** if (x >= nbPeaks - 1) { break; } */
//				////int x = (i * allRows.length) + j;
//
//				PeakListRow row = allRows[j];
//
////				// Each name HAS to be unique
////				//short_names[x] = String.valueOf(short_names_unid); //"x" + short_names_id; //"[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
////				short_names[x] = "" + short_names_unid; //"x" + short_names_id; //"[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
////
////				// Produce human readable names
////				RawDataFile ancestorRDF = DataFileUtils.getAncestorDataFile(project, peakList.getRawDataFile(0), true);
////				// Avoid exception if ancestor RDFs have been removed...
////				String suitableRdfName = (ancestorRDF == null) ? peakList.getRawDataFile(0).getName() : ancestorRDF.getName();
////				long_name = "[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
////				dendro_names_dict.put(short_names[x], long_name);
////				//short_names_unid.add(BigInteger.ONE);
////				short_names_unid++;
////
////				row_names_dict.put(short_names[x], row);
////				full_rows_list.add(row);
////				rows_list.add(row.getBestPeak().getDataFile() + ", @" + row.getBestPeak().getRT());
//
//
//				if (isCanceled())
//					return;
//
////				// Calculate limits for a row with which the row can be aligned
////				//            Range<Double> mzRange = mzTolerance.getToleranceRange(row
////				//                    .getAverageMZ());
////				//            Range<Double> rtRange = rtTolerance.getToleranceRange(row
////				//                    .getAverageRT());
//				// GLG HACK: Use best peak rather than average. No sure it is better... ???
//				Range<Double> mzRange = mzTolerance.getToleranceRange(row
//						.getBestPeak().getMZ());
//				Range<Double> rtRange = rtTolerance.getToleranceRange(row
//						.getBestPeak().getRT());
//
//				// Get all rows of the aligned peaklist within parameter limits
//				/*
//            PeakListRow candidateRows[] = alignedPeakList
//                    .getRowsInsideScanAndMZRange(rtRange, mzRange);
//				 */
//				//////List<PeakListRow> candidateRows = new ArrayList<>();
//
//
//
//				int y = 0; // Cover only the upper triangle (half the squared matrix)
//
//
//				for (int k = 0; k < newIds.length; ++k) {
//
//
//					
//					if (isCanceled())
//						return;
//
//					//                    // !!! Skip the diagonal !!!
//					//                    if (x == y) { continue; }
//					//                    
//
//
//					PeakList k_peakList = peakLists[newIds[k]];
//					PeakListRow k_allRows[] = k_peakList.getRows();
//
//					//                  if (k != i) {
//					for (int l = 0; l < k_allRows.length; ++l) {
//
//						// Cover only the upper triangle (half the squared matrix),  Skip the lower triangle
//						if (x > y) { ++y; continue; }
//						/**if (y < x + 1) { ++y; continue; }
//						if (y >= nbPeaks) { break; }*/
//
//
//						////int y = (k * k_allRows.length) + l;
//
//
//						PeakListRow k_row = k_allRows[l];
//
//						double normalized_rt_dist = Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) / ((RangeUtils.rangeLength(rtRange) / 2.0));
//
//
//						if (x == y) {
//							/** Row tested against himself => fill matrix diagonal with zeros */
//							//                            distances[x][y] = 0.0d;
//							if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
//								distances.set(x, y, 0d);
//							else {
//								//distancesGNF.setValue(x, y, 0.0f);
//								// Do nothing on diagonal with this impl of dist matrix!
//							}
//							//continue; 
//						} else {
////							double mzMaxDiff = RangeUtils.rangeLength(mzRange) / 2.0;
////							double rtMaxDiff = RangeUtils.rangeLength(rtRange) / 2.0;
////							System.out.println("(1) CaseRT: " + Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT())
////							+ " >= " + rtMaxDiff + "? " + (Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) >= rtMaxDiff));
////							System.out.println("(1) CaseMZ: " + Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ())
////							+ " >= " + mzMaxDiff + "? " + (Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ()) >= mzMaxDiff));
////							System.out.println("(1) Rows: (" 
////									+ x + "," + y + ") - (" + full_rows_list.indexOf(row) + "," + full_rows_list.indexOf(k_row) + ")" + row + " | " + k_row);
//
//							if (k != i) {
//								
////								System.out.println("(1.1) CaseRT: " + Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT())
////								+ " < " + rtTolerance.getTolerance() / 2.0 + "? " + (Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) < rtTolerance.getTolerance() / 2.0));
////								System.out.println("(1.1) CaseMZ: " + Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ())
////								+ " < " + mzTolerance.getMzTolerance() / 2.0 + "? " + (Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ()) < mzTolerance.getMzTolerance() / 2.0));
////								System.out.println("(1.1) Rows: (" 
////										+ x + "," + y + ") - (" + full_rows_list.indexOf(row) + "," + full_rows_list.indexOf(k_row) + ")" + row + " | " + k_row);
//
//								// Is candidate
//								if (Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) < rtTolerance.getTolerance() / 2.0 
//										&& Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ()) < mzTolerance.getMzTolerance() / 2.0) {
//									//////candidateRows.add(k_row);
//
//									/** Row is candidate => Distance is the score */
//									score = new RowVsRowScoreGC(
//											this.project, useOldestRDFAncestor,
//											/*row.getRawDataFiles()[0],*/ rtAdjustementMapping,
//											row, k_row,
//											RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
//											RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
//											idWeight,
//											useApex, useKnownCompoundsAsRef, 
//											useDetectedMzOnly,
//											rtToleranceAfter);
//
//									//-
//									// If match was not rejected afterwards and score is acceptable
//									// (Acceptable score => higher than absolute min ever and higher than user defined min)
//									// 0.0 is OK for a minimum score only in "Dot Product" method (Not with "Person Correlation")
//									//if (score.getScore() > Jmissing1DXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE)
//									if (score.getScore() > Math.max(JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE, minScore)) {
//										//////scoreSet.add(score);
//										// The higher the score, the lower the distance!
//										//                                        distances[x][y] = maximumScore - score.getScore();//(mzWeight + rtWeight) - score.getScore();
//										if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
//											distances.set(x, y , maximumScore - score.getScore());
//										else
//											distancesGNF_Tri.setValue(x, y, (float) (maximumScore - score.getScore()));
//									} else {
//										/** Score too low => Distance is Infinity */
//										//distances[x][y] = veryLongDistance * (1.0d + normalized_rt_dist); // Need to rank distances for "rejected" cases
//										////distances[x][y] = veryLongDistance; // Need to rank distances for "rejected" cases
//										//////distances[x][y] = Double.MAX_VALUE;
//										//                                        distances[x][y] = veryLongDistance;
//										if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
//											distances.set(x, y , veryLongDistance);
//										else
//											distancesGNF_Tri.setValue(x, y, veryLongDistance);
//									}
//
//								} else {
//									/** Row is not candidate => Distance is Infinity */
//									////distances[x][y] = 10.0d * veryLongDistance * (1.0d + normalized_rt_dist); // Need to rank distances for "rejected" cases
//									//distances[x][y] = veryLongDistance; // Need to rank distances for "rejected" cases
//									//distances[x][y] = Double.MAX_VALUE;//10.0d * veryLongDistance; // Need to rank distances for "rejected" cases
//									//////distances[x][y] = Double.MAX_VALUE;
//									//                                    distances[x][y] = 10.0d * veryLongDistance;
//									if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
//										distances.set(x, y , 10.0d * veryLongDistance);
//									else
//										distancesGNF_Tri.setValue(x, y, 10.0f * veryLongDistance);
//								}
//							} else {
//								/** Both rows belong to same list => Distance is Infinity */
//								////distances[x][y] = 100.0d * veryLongDistance * (1.0d + normalized_rt_dist); // Need to rank distances for "rejected" cases
//								//distances[x][y] = Double.MAX_VALUE;//100.0d * veryLongDistance; // Need to rank distances for "rejected" cases
//								//////distances[x][y] = Double.MAX_VALUE;
//								//                                distances[x][y] = 100.0d * veryLongDistance;
//								if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
//									distances.set(x, y , 100.0d * veryLongDistance);
//								else
//									distancesGNF_Tri.setValue(x, y, 100.0f * veryLongDistance);
//							}
//							
//							System.out.println("(1) Final dist: " + distancesGNF_Tri.getValue(x, y));
//						}
//
//						//                        // 28.499
//						//                        if (row.getBestPeak().getRT() > 28.499d && row.getBestPeak().getRT() < 28.500d) {
//						//                            System.out.println("28.499 scored: " + row.getBestPeak().getRT() + " | " + k_row.getBestPeak().getRT());
//						//                            System.out.println("\t => " + distances[x][y]);
//						//                        }
//
//						//                        if (x == y) {
//						//                            /** Row tested against himself => fill matrix diagonal with zeros */
//						//                            distances[x][y] = 0.0d;
//						//                            //continue; 
//						//                        } else {
//						//
//						//                            if (k != i) {
//						//                                
//						//                                RowVsRowScoreGC score = new RowVsRowScoreGC(
//						//                                        this.project, useOldestRDFAncestor,
//						//                                        /*row.getRawDataFiles()[0],*/ rtAdjustementMapping,
//						//                                        row, k_row,
//						//                                        RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
//						//                                        RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
//						//                                        idWeight,
//						//                                        useApex, useKnownCompoundsAsRef, 
//						//                                        useDetectedMzOnly,
//						//                                        rtToleranceAfter);
//						//                                
//						//                                distances[x][y] = maximumScore - score.getScore();
//						//                                
//						//                            } else {
//						//                                
//						//                                  /** Both rows belong to same list => Distance is Infinity */
//						//                                  distances[x][y] = 5.0d * veryLongDistance; // Need to rank distances for "rejected" cases
//						//                            }
//						//                        }missing1
//						//                        
//						//                        // 28.499
//						//                        if (row.getBestPeak().getRT() > 28.499d && row.getBestPeak().getRT() < 28.500d) {
//						//                            System.out.println("28.499 scored: " + row.getBestPeak().getRT() + " | " + k_row.getBestPeak().getRT());
//						//                            System.out.println("\t => " + distances[x][y]);
//						//                        }
//
//
//						++y;
//					}
//					//                  } else {
//					//                      /** Both rows belong to same list => Distance is Infinity */
//					//                      distances[x][y] = Double.MAX_VALUE;
//					//                  }
//				}
//
//				processedRows++;
//
//				++x;
//			}
//
//		}

		// If 'Hybrid' or no distance matrix: no need for a matrix
		if (CLUSTERER_TYPE == ClustererType.HYBRID || !saveRAMratherThanCPU_1) {
			
			for (int x = 0; x < nbPeaks; ++x) {
			
				for (int y = x; y < nbPeaks; ++y) {
					
					float dist = (float) distProvider.getRankedDistance(x, y, mzTolerance.getMzTolerance(), rtTolerance.getTolerance(), minScore);
	
					if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
						distances.set(x, y , dist);
					else
						distancesGNF_Tri.setValue(x, y, dist);
					
				}
				
				processedRows++;
				logger.info("Treating lists: " + (Math.round(100 * processedRows / (double) nbPeaks)) + " %");

			}
		}

		if (DEBUG) {
			System.out.println("\nDISTANCES MATRIX:\n");
			distances.print();
			System.out.println("\nDISTANCES VECTOR:\n");
			distances.printVector();
		}

		
//		if (distances != null)
//			printDistanceMatrixToDatFile("mtx5.dat", distances, false);


		printMemoryUsage(run_time, prevTotal, prevFree, "DISTANCES COMPUTED");


		// DEBUG: save distances matrix as CSV
		if (DEBUG) {
			// Open file
			File curFile = new File("/home/golgauth/matrix-aligner.csv");
			FileWriter writer;
			try {
				writer = new FileWriter(curFile);
			} catch (Exception e) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Could not open file " + curFile
						+ " for writing.");
				return;
			}
			// Write things
			try {
				for (int i = 0; i < distances.getDimension(); i++) {

					if (i == 0)
						for (int j = 0; j < distances.getDimension(); j++) {
							writer.write(short_names[j] + "\t");
						}

					for (int j = 0; j < distances.getDimension(); j++) {
						if (j == 0)
							writer.write(short_names[i] + "\t");
						writer.write(rtFormat.format(distances.get(i,  j)) + "\t");
					}
					writer.write("\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Close file
			try {
				writer.close();
			} catch (Exception e) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Could not close file " + curFile);
				return;
			}
		}




		//////
		double max_dist = maximumScore; //Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) / ((RangeUtils.rangeLength(rtRange) / 2.0));

		//---
		List<Cluster> validatedClusters_0 = null;
		List<Integer> validatedClusters_1 = null;
		List<Node> validatedClusters_2 = null;
		// ---
		Cluster clust = null;
		Tree tree_1 = null;
		jebl.evolution.trees.Tree tree_2 = null;
		// ---
		ClusteringResult clusteringResult = null;
		String newickCluster_clean = null;
		// ---
		List<ca.ualberta.cs.hdbscanstar.Cluster> clustersHDBSCAN = null;
		HDBSCANClusterer hdbscanClusterer = null;


		// OLD (RAM killer) way!
		//        if (clustering_method == 0) {    
		//
		//        	ClusteringAlgorithm alg = new DefaultClusteringAlgorithmWithProgress(); //DefaultClusteringAlgorithm();        
		//        	clust = alg.performClustering(distances, short_names, linkageStrategy);
		//
		//        	System.out.println("Done clustering");
		//
		//        	if (isCanceled())
		//        		return;
		//
		//         	List<Cluster> validatedClusters = getValidatedClusters_0(clust, newIds.length, max_dist);
		//        }
		//        // WEKA way! (And more: Mostly HDBSCAN*/ELKI, see 'CLUSTERER_TYPE==5')
		//        else {


		long startTime2, endTime2;
		float ms2;

		String newickCluster;
		List<List<Integer>> gnfClusters = null;

		if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD) {

			// WEKA hierarchical clustering
			HierarClusterer hierarClusterer;
			hierarClusterer = new HierarClusterer(clustProgress, distances, this.minScore);

			printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER CREATED");

			//
			startTime2 = System.currentTimeMillis();
			//
			int minNumClusters = 1;
			for (PeakList pl: this.peakLists) {

				if (pl.getNumberOfRows() > minNumClusters) {
					minNumClusters = pl.getNumberOfRows();
				}
			}
			clusteringResult = hierarClusterer.performClustering(linkageStartegyType_12, minNumClusters);
			//            
			printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER PERFORMED");
			//
			endTime2 = System.currentTimeMillis();
			ms2 = (endTime2 - startTime2);
			System.out.println("Done clustering: " + clusteringResult);

			System.out.println("Done clustering: " + clusteringResult + "[took: " + ms2 + "ms. to build tree]");

			if (isCanceled())
				return;


			// Getting the result of the clustering in Newick format
			newickCluster = clusteringResult.getHierarchicalCluster();
			//            
			printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER NEWICKED");


			//////


			int current_depth = 0;

			// Make string really Newick standard
			String line_sep = "\n";//System.getProperty("line.separator");
			//	        	System.out.println("Line sep = '" + line_sep + "'.");
			//	        	System.out.println("Line sep found at index '" + newickCluster.indexOf(line_sep) + "'.");
			newickCluster_clean = newickCluster.substring(newickCluster.indexOf(line_sep) + line_sep.length()) + ";";

			//		        	PrintWriter out;
			//		        	try {
			//		        		out = new PrintWriter("newick_check.txt");
			//		        		
			//	        			String output_str = newickCluster_clean ;
			//	        			
			////        			for (String short_n : dendro_names_dict.keySet()) {
			////        				String short_nn = HierarClusterer.NEWICK_LEAF_NAME_PREFIX + short_n;
			////        				String long_nn = dendro_names_dict.get(short_n).replaceAll(", ", "_");
			////        				output_str = output_str.replaceAll(short_nn + ":", long_nn + ":");
			////        			}
			//		        		
			//		        		out.println(output_str);
			//		        		
			//		        		out.close();
			//		        	} catch (FileNotFoundException e) {
			//		        		// TODO Auto-generated catch block
			//		        		e.printStackTrace();
			//		        	}
			PrintWriter out;
			try {
				out = new PrintWriter("newick_check_0.txt");
				out.println(newickCluster);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


			BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");

			TreeParser tp = new TreeParser(r);
			tree_1 = tp.tokenize(1, "tree", null);
			int tree_height = tree_1.getHeight();
			System.out.println("# Largest tree height is: " + tree_height);
			//
			if (DEBUG)
				System.out.println("####" + recursive_print(tree_1, 0, 0));



		} else { 
			
			boolean do_verbose = true;
			boolean do_cluster = true;
			boolean do_print = true;
			boolean do_data = false;


			org.gnf.clustering.Node[] arNodes = null;
			//int nRowCount = distancesGNF_Tri.getRowCount();
			int nRowCount = full_rows_list.size();
			
			String[] rowNames = null;
			if (do_print) {
				rowNames = new String [nRowCount];
				for (int i = 0; i < nRowCount; i++) {
					rowNames[i] = "ID_" + i + "_" + full_rows_list.get(i).getID();//full_rows_list.get(i).getAverageRT();
				}
			}
			String outputPrefix = null;
			
			if (CLUSTERER_TYPE == ClustererType.CLASSIC) { // Pure Hierar!
				
				outputPrefix = "hierar_0";
				
				throw new IllegalStateException("'" + ClustererType.CLASSIC.toString() + "' algorithm not yet implemented!");
				
			} else if (CLUSTERER_TYPE == ClustererType.CACHED) { // Pure Hierar!
				
				// TODO: ...!
				if (DEBUG_2)
					System.out.println(distancesGNF_Tri.toString());
				
				if (saveRAMratherThanCPU_2) { // Requires: distances values will be recomputed on demand during "getValidatedClusters_3()"
					distancesGNF_Tri_Bkp = null; // No duplicate backup storage!
				} else { // Otherwise, backing up the distance matrix (matrix being deeply changed during "clusterDM()", then no more exploitable)
					distancesGNF_Tri_Bkp = new DistanceMatrixTriangular1D2D(distancesGNF_Tri);
					printMemoryUsage(run_time, prevTotal, prevFree, "GNF CLUSTERER BACKUP MATRIX");
				}
				
				System.out.println("Clustering...");
				if(distancesGNF_Tri != null)				
					arNodes = org.gnf.clustering.sequentialcache
					.SequentialCacheClustering.clusterDM(distancesGNF_Tri, linkageStartegyType_20, null, nRowCount);
				
//				if (true) {
//					
//					for (int i=0; i<distancesGNF_Tri_Bkp.getRowCount(); i++) {
//						for (int j=0; j<distancesGNF_Tri_Bkp.getRowCount(); j++) {
//							
//							PeakListRow any_row = full_rows_list.get(i);
//							
//							Range<Double> mzRange = mzTolerance.getToleranceRange(any_row
//									.getBestPeak().getMZ());
//							Range<Double> rtRange = rtTolerance.getToleranceRange(any_row
//									.getBestPeak().getRT());
//
//							double mzMaxDiff = RangeUtils.rangeLength(mzRange) / 2.0;
//							double rtMaxDiff = RangeUtils.rangeLength(rtRange) / 2.0;
//							System.out.println(distancesGNF_Tri_Bkp.getValue(i,  j) + " | " 
//									+ distProvider.getRankedDistance(i, j, mzMaxDiff, rtMaxDiff, minScore));
//						}
//					}
//				}
				
				
				distancesGNF_Tri = null;
				System.gc();

				printMemoryUsage(run_time, prevTotal, prevFree, "GNF CLUSTERER DONE");

				if (DEBUG_2)
					System.out.println(distancesGNF_Tri.toString());

				if (DEBUG_2)
					for (int i = 0; i < arNodes.length; i++) {
						System.out.println("Node " + i + ": " + arNodes[i]);
					}
				
				// TODO: Use usual interfacing ...
//				ClusteringResult<org.gnf.clustering.Node> clust_res = new ClusteringResult<>(
//						Arrays.asList(arNodes), null, 0, null);
				
				
				outputPrefix = "hierar_1";


			} else if (CLUSTERER_TYPE == ClustererType.HYBRID) { // Hybrid!

				// TODO: ...!
				int nColCount = 1;

				float[] arFloats = new float[nRowCount*nColCount];
				for (int i=0; i < arFloats.length; i++) {
					arFloats[i] = (float) Math.random(); // i;
				}
				DataSource source = new FloatSource1D(arFloats, nRowCount, nColCount);

				DistanceCalculator calculator = new HybridDistanceCalculator();
				//-
//				PeakListRow any_row = full_rows_list.get(0);
//				Range<Double> mzRange = mzTolerance.getToleranceRange(any_row
//						.getBestPeak().getMZ());
//				Range<Double> rtRange = rtTolerance.getToleranceRange(any_row
//						.getBestPeak().getRT());
//				double mzMaxDiff = RangeUtils.rangeLength(mzRange) / 2.0;
//				double rtMaxDiff = RangeUtils.rangeLength(rtRange) / 2.0;
				double mzMaxDiff = mzTolerance.getMzTolerance();
				double rtMaxDiff = rtTolerance.getTolerance();
				((HybridDistanceCalculator) calculator).setDistanceProvider(distProvider, mzMaxDiff, rtMaxDiff, minScore);
				
				// <A> 1nd pass: use distances as usual... (simply not precomputed this time)
				final HierarchicalClustering clusteringHier = new SequentialCacheClustering(calculator, linkageStartegyType_20);
				System.out.println("Clustering...");

				// <B> 2nd pass: use average on clusters
				CentroidsCalculator calculatorCtr = new DefaultCentroidsCalculator(); 
				//-
				final HybridClustering clusteringHybrid = new HybridClustering(clusteringHier, calculatorCtr);
				if(hybrid_K_value > 0)
					clusteringHybrid.setK(hybrid_K_value);
				//-
				try {
					arNodes = clusteringHybrid.cluster(source, null);

					printMemoryUsage(run_time, prevTotal, prevFree, "GNF CLUSTERER DONE");

					// File output

					outputPrefix = "hierar_2";
				
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
			
			
			// Sort Nodes by correlation score (Required in 'getValidatedClusters_3')
			int[] rowOrder = new int[nRowCount];
			System.out.println("Sorting tree nodes...");
			org.gnf.clustering.Utils.NodeSort(arNodes, nRowCount - 2, 0,  rowOrder);

			
			if (do_cluster) {
				
				gnfClusters = getValidatedClusters_3(arNodes, 0.0f, newIds.length, max_dist, distancesGNF_Tri_Bkp, distProvider);
				
//				for (int i = 0; i < nRowCount; i++) {
//					if (!flatLeaves.contains(i)) {
//						List<Integer> cl_single = new ArrayList<>();
//						cl_single.add(i);
//						gnfClusters.add(cl_single);
//					}
//				}
				
				//-- Print
				if (DEBUG_2 && do_verbose)
					for (int i=0; i < gnfClusters.size(); i++) {
						List<Integer> cl = gnfClusters.get(i);
						String str = "";
						for (int j = 0; j < cl.size(); j++) {
							int r = cl.get(j);
							str += cl.get(j) + "^(" + full_rows_list.get(r).getID() + ", " + full_rows_list.get(r).getAverageRT() + ")" + " ";
						}
						System.out.println(str);
					}
			}
			
			// File output

			String outGtr = outputPrefix + ".gtr";
			String outCdt = outputPrefix + ".cdt";

			
			System.out.println("Writing output to file...");

			int nColCount = 1;
			String[] colNames = new String[nColCount];
			colNames[nColCount-1] = "Id";
			String sep = "\t";

			
			if (do_print) {
				try {
					
					float[] arFloats = new float[nRowCount];
					for (int i=0; i < arFloats.length; i++) {
						arFloats[i] = i / 2.0f;
					}
					DataSource source = (do_data) ? new FloatSource1D(arFloats, nRowCount, nColCount) : null;
					
					/*org.gnf.clustering.Utils.*/JoinAlignerGCTask.GenerateCDT(outCdt, source/*null*/, 
							nRowCount, nColCount, sep, rowNames, colNames, rowOrder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


				org.gnf.clustering.Utils./*JoinAlignerGCTask.*/WriteTreeToFile(outGtr, nRowCount - 1, arNodes, true);
				
				printMemoryUsage(run_time, prevTotal, prevFree, "GNF CLUSTERER FILES PRINTED");

			}


		}



		////// Arrange row clustered list with method 0,1,2
		List<List<PeakListRow>> clustersList = new ArrayList<>();
		//
		int finalNbPeaks = 0;
		Set<String> leaf_names = new HashSet<>();

		if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD) {

			tot = 0;
			tot2 = 0;

			List<Integer> validatedClusters;
			//        	if (USE_DOUBLE_PRECISION_FOR_DIST) {
			//	        	validatedClusters = getValidatedClusters_11(clusteringResult, newIds.length, max_dist, distancesAsVector, numInstances);
			//	        	//List<Integer> validatedClusters = getValidatedClusters_1(clusteringResult, newIds.length, max_dist, distances);
			//        	} else {
			//	        	validatedClusters = getValidatedClusters_11(clusteringResult, newIds.length, max_dist, distancesAsFloatVector, numInstances);
			//        	}
			validatedClusters = getValidatedClusters_2(clusteringResult, newIds.length, max_dist, distances);


			// Free memory ASAP
			distances = null;
			System.gc();

			int nbLeaves = 0;
			for (Integer nodekey: validatedClusters) {
				nbLeaves += tree_1.getNodeByKey(nodekey).numberLeaves;
			}
			System.out.println("NbLeaves = " + nbLeaves);
			System.out.println("tot = " + tot + " | tot2 = " + tot2);
			//            if (nbLeaves != 752)
			//            	return;


			List<String> leaf_names_flat = new ArrayList<>();
			List<Integer> leaf_names_keys = new ArrayList<>();
			int i_cl = 0;
			for (Integer nodekey: validatedClusters) {

				if (isCanceled())
					return;

				TreeNode node = tree_1.getNodeByKey(nodekey);

				List<Integer> leafs_nodekeys = getClusterLeafs_1(node);
				List<PeakListRow> rows_cluster = new ArrayList<>();
				RawDataFile rdf = null;
				for (Integer l_nodekey: leafs_nodekeys) {

					TreeNode leaf = tree_1.getNodeByKey(l_nodekey);

					// Recover related PeakListRow
					//                    rows_cluster.add(row_names_dict.get(leaf.getName()));
					//        			int leaf_id = Integer.valueOf(leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
					//					logger.info("Some node name: '" + leaf.getName() + "'! (" + leaf.getKey() + ")");
					String leaf_name = leaf.getName();

					if (leaf.getName().isEmpty()) {
						logger.info("\t=> Skipped!");
						continue;
					}

					if (leaf.getName().startsWith(HierarClusterer.NEWICK_LEAF_NAME_PREFIX))
						leaf_name = leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length());
					else if (leaf.getName().startsWith(" ")) {
						leaf_name = leaf.getName().substring(1);
					}
					int leaf_id = Integer.valueOf(leaf_name);
					rows_cluster.add(full_rows_list.get(leaf_id));
					leaf_names.add(leaf.getName());

				}
				clustersList.add(rows_cluster);
				finalNbPeaks += rows_cluster.size();
				//
				processedRows += rows_cluster.size();


				//        		if (i_cl == validatedClusters.size()-1) {
				//	        		// Few stats
				//	        		System.out.println("######### STATS #########");
				//	        		System.out.println("###### > Found '" + clustersList.size() + "' clusters:");
				//	        		for (List<PeakListRow> cl: clustersList) {
				//	        			System.out.println("###### \t > " + cl.size());
				//	        		}
				//        		}
				//        		i_cl++;

			}

		} else { //if (CLUSTERER_TYPE == 1 || CLUSTERER_TYPE == 2) {

			// TODO: ...!
			// Build peak list row clusters
//			Set<Integer> row_ids = new HashSet<>(); 
//			List<Integer> row_ids_list = new ArrayList<>();
			for (List<Integer> cl: gnfClusters) {

				List<PeakListRow> rows_cluster = new ArrayList<>();
				for (int i = 0; i < cl.size(); i++) {
					rows_cluster.add(full_rows_list.get(cl.get(i)));
//					row_ids.add(cl.get(i));
//					row_ids_list.add(cl.get(i));
				}
				clustersList.add(rows_cluster);
				finalNbPeaks += rows_cluster.size();
				//
				processedRows += rows_cluster.size();
			}
//			System.out.println("## !!! NB IDS = " + row_ids.size());
//			System.out.println("## !!! IDS = " + row_ids.toString());
//			System.out.println("## !!! NB IDS (1) = " + row_ids_list.size() + ", " + finalNbPeaks);
//			Collections.sort(row_ids_list);
//			System.out.println("## !!! IDS (1) = " + row_ids_list.toString());
			
			printMemoryUsage(run_time, prevTotal, prevFree, "GNF CLUSTERER CLUSTER_LIST");

		}


		// DEBUG stuff: REMOVE !!!
		/** printAlignedPeakList(clustersList); */



		int nbAddedPeaks = 0;
		int nbUniquePeaks = 0;
		int nbAddedRows = 0;
		// Fill alignment table: One row per cluster
		for (List<PeakListRow> cluster: clustersList) {

			if (isCanceled())
				return;


			if (DEBUG) {
				// Check cluster integrity
				Set<String> rdf_names = new HashSet<>();
				List<String> rdf_names_flat = new ArrayList<>();
				for (PeakListRow row: cluster) {
					rdf_names.add(row.getBestPeak().getDataFile().getName());
					rdf_names_flat.add(row.getBestPeak().getDataFile().getName());
				}
				System.out.println("rdf_names.size() = " + rdf_names.size());
				nbUniquePeaks += rdf_names.size();
				if (rdf_names.size() != rdf_names_flat.size()) {
					System.out.println(">> Found RDF duplicate: ");
					System.out.println("\t * " + Arrays.toString(rdf_names_flat.toArray()));
				}
			}


			PeakListRow targetRow = new SimplePeakListRow(newRowID);
			newRowID++;
			alignedPeakList.addRow(targetRow);
			nbAddedRows++;
			//
			infoRowsBackup.put(targetRow, new Object[] { 
					new HashMap<RawDataFile, Double[]>(), 
					new HashMap<RawDataFile, PeakIdentity>(), 
					new HashMap<RawDataFile, Double>() 
			});

			for (PeakListRow row: cluster) {

				// Add all non-existing identities from the original row to the
				// aligned row
				//PeakUtils.copyPeakListRowProperties(row, targetRow);
				// Set the preferred identity
				////if (row.getPreferredPeakIdentity() != null)
				if (JDXCompound.isKnownIdentity(row.getPreferredPeakIdentity())) {
					targetRow.setPreferredPeakIdentity(row.getPreferredPeakIdentity());
					//                //JDXCompound.setPreferredPeakIdentity(targetRow, row.getPreferredPeakIdentity());
					//                SimplePeakIdentity newIdentity = new SimplePeakIdentity(prop);
					//                targetRow.setPreferredPeakIdentity(row.getPreferredPeakIdentity());
				}
				else
					targetRow.setPreferredPeakIdentity(JDXCompound.createUnknownCompound());
				//                JDXCompound.setPreferredPeakIdentity(targetRow, JDXCompound.createUnknownCompound());



				// Add all peaks from the original row to the aligned row
				//for (RawDataFile file : row.getRawDataFiles()) {
				for (RawDataFile file : alignedPeakList.getRawDataFiles()) {

					//                if (recalibrateRT) {
					//                    if (!Arrays.asList(row.getRawDataFiles()).contains(file)) {
					//                        double b_offset = rtAdjustementMapping.get(peakList)[0];
					//                        double a_scale = rtAdjustementMapping.get(peakList)[1];
					//                        ((HashMap<RawDataFile, Double[]>) infoRowsBackup.get(targetRow)[0]).put(file, new Double[] { Double.NaN, b_offset, a_scale });                        
					//                        //continue;
					//                        //break;
					//                    }
					//                }

					if (Arrays.asList(row.getRawDataFiles()).contains(file)) {

						Feature originalPeak = row.getPeak(file);
						if (originalPeak != null) {

							if (recalibrateRT) {
								// Set adjusted retention time to all peaks in this row
								// *[Note 1]
								RawDataFile pl_RDF = row.getRawDataFiles()[0];//////peakList.getRawDataFile(0);
								/** 
                                logger.info("{" + rtAdjustementMapping.get(pl_RDF)[0] + ", " + rtAdjustementMapping.get(pl_RDF)[1] + "}");
                                double b_offset = rtAdjustementMapping.get(pl_RDF)[0];
                                double a_scale = rtAdjustementMapping.get(pl_RDF)[1]; 
								 */
								double[] row_offset_scale = JoinAlignerGCTask.getOffsetScaleForRow(
										row.getRawDataFiles()[0], row.getAverageRT(), rtAdjustementMapping);
								//                                logger.info("{" + row_offset_scale[0] + ", " + row_offset_scale[1] + "}");
								double b_offset = row_offset_scale[0];
								double a_scale = row_offset_scale[1];
								//
								double adjustedRT = JoinAlignerGCTask.getAdjustedRT(originalPeak.getRT(), b_offset, a_scale);

								SimpleFeature adjustedPeak = new SimpleFeature(originalPeak);
								PeakUtils.copyPeakProperties(originalPeak, adjustedPeak);
								adjustedPeak.setRT(adjustedRT);
								//                                logger.info("adjusted Peak/RT = " + originalPeak + ", " + adjustedPeak + " / " + originalPeak.getRT() + ", " + adjustedPeak.getRT());

								targetRow.addPeak(file, adjustedPeak);
								nbAddedPeaks++;
								// Adjusted RT info
								rtPeaksBackup.put(adjustedPeak, originalPeak.getRT());
								((HashMap<RawDataFile, Double[]>) infoRowsBackup.get(targetRow)[0]).put(file, new Double[] { adjustedRT, b_offset, a_scale });//originalPeak.getRT());

								//                                processedRows++;

							} else {

								// HELP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
								////if (!Arrays.asList(targetRow.getPeaks()).contains(originalPeak)) {

								targetRow.addPeak(file, originalPeak);
								nbAddedPeaks++;
								//                                logger.info("Added peak: " + originalPeak);

								//                                processedRows++;

								////}

								if (DEBUG) {
									//                                    if (targetRow.getNumberOfPeaks() > peakLists.length) {
									//                                        bip = true;
									//                                        
									//                                        String str_p = "";
									//                                        for (Feature p: targetRow.getPeaks())
									//                                            str_p += p + ", ";
									//                                        logger.info("----------- >> " + targetRow.getNumberOfPeaks() + " peaks for row: " + targetRow.getID() + "! " + "(" + str_p + ")");
									//                                    }
									//                                    
									//                                    int nbPeaks_0 = 0;
									//                                    for (PeakListRow plr: alignedPeakList.getRows()) {
									//                                        nbPeaks_0 += plr.getNumberOfPeaks();
									//                                        logger.info("R-" + plr.getID() + " > Found peaks" + Arrays.toString(plr.getPeaks()));
									//                                    }
									//                                    logger.info("++++++++++++++ >> " + nbPeaks_0 + " / " + nbAddedPeaks);
								}
							}

							// Identification info
							((HashMap<RawDataFile, PeakIdentity>) infoRowsBackup.get(targetRow)[1]).put(file, targetRow.getPreferredPeakIdentity());
							//
							String strScore = targetRow.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE);
							if (strScore != null)
								((HashMap<RawDataFile, Double>) infoRowsBackup.get(targetRow)[2]).put(file, Double.valueOf(strScore));
							else
								((HashMap<RawDataFile, Double>) infoRowsBackup.get(targetRow)[2]).put(file, 0.0);

							//                            logger.info("targetRow >>> Added Peak @" + originalPeak.getClass().getName() + '@' + Integer.toHexString(originalPeak.hashCode()) 
							//                                    + ",  RT=" + targetRow.getPeaks()[targetRow.getPeaks().length-1].getRT() + " / ID: " + targetRow.getID());
							//                            logger.info(".");

						}
						else {
							setStatus(TaskStatus.ERROR);
							setErrorMessage("Cannot run alignment, no originalPeak");
							return;
						}

					} 
					//                else {
					//                    if (recalibrateRT) {
					//                        double b_offset = rtAdjustementMapping.get(peakList)[0];
					//                        double a_scale = rtAdjustementMapping.get(peakList)[1];
					//                        ((HashMap<RawDataFile, Double[]>) infoRowsBackup.get(targetRow)[0]).put(file, new Double[] { Double.NaN, b_offset, a_scale });                        
					//                        //continue;
					//                    }
					//                }

				}

				// Copy all possible peak identities, if these are not already present
				for (PeakIdentity identity : row.getPeakIdentities()) {
					PeakIdentity clonedIdentity = (PeakIdentity) identity.clone();
					if (!PeakUtils.containsIdentity(targetRow, clonedIdentity))
						targetRow.addPeakIdentity(clonedIdentity, false);
				}

				//            // Notify MZmine about the change in the project
				//            // TODO: Get the "project" from the instantiator of this class instead.
				//            // Still necessary ???????
				//            MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
				//            project.notifyObjectChanged(targetRow, false);
				//            // Repaint the window to reflect the change in the peak list
				//            Desktop desktop = MZmineCore.getDesktop();
				//            if (!(desktop instanceof HeadLessDesktop))
				//                desktop.getMainWindow().repaint();

				//processedRows++;

			}

//			// Notify MZmine about the change in the project, necessary ???
//			MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
//			project.notifyObjectChanged(targetRow, false);
		}

		//        // Free mem
		//        rtAdjustementMapping = null;


		int finalNbPeaks_0 = 0;

		for (PeakListRow plr: alignedPeakList.getRows()) {
			finalNbPeaks_0 += plr.getNumberOfPeaks();
		}


		//----------------------------------------------------------------------







		// Restore real RT - for the sake of consistency
		// (the adjusted one was only useful during alignment process)
		// WARN: Must be done before "Post processing" part to take advantage
		//       of the "targetRow.update()" used down there
		for (SimpleFeature peak : rtPeaksBackup.keySet()) {
			peak.setRT((double) rtPeaksBackup.get(peak));
		}


		/** Post-processing... **/
		// Build reference RDFs index: We need an ordered reference here, to be able to parse
		// correctly while reading back stored info
		RawDataFile[] rdf_sorted = alignedPeakList.getRawDataFiles().clone();
		Arrays.sort(rdf_sorted, new RawDataFileSorter(SortingDirection.Ascending));

		// Process
		for (PeakListRow targetRow: infoRowsBackup.keySet()) {

			if (isCanceled())
				return;

			// Refresh averaged RTs...
			((SimplePeakListRow) targetRow).update();

			HashMap<RawDataFile, Double[]> rowRTinfo = ((HashMap<RawDataFile, Double[]>) infoRowsBackup.get(targetRow)[0]);
			HashMap<RawDataFile, PeakIdentity> rowIDs = ((HashMap<RawDataFile, PeakIdentity>) infoRowsBackup.get(targetRow)[1]);
			HashMap<RawDataFile, Double> rowIDsScores = ((HashMap<RawDataFile, Double>) infoRowsBackup.get(targetRow)[2]);

			String[] rowIDsNames = new String[rowIDs.values().size()];
			int i = 0;
			for (PeakIdentity id: rowIDs.values()) {
				rowIDsNames[i] = (id != null) ? id.getName() : "";
				++i;
			}

			//            // Save preferred (most frequent) identity
			//            // Open question: Shouldn't we be set as "most frequent", the most 
			//            //                  frequent excluding UNKNOWN??? (See *[Note2])
			int mainIdentityCard = 0;
			double mainIdentitySum = 0.0;
			PeakIdentity mainIdentity = null;

			// Save original RTs and Identities
			String strAdjustedRTs = "", strOffsets = "", strScales = "";
			String strIdentities = "";
			String strScores = "";
			// Object[] = { sum, cardinality }
			HashMap<String, Object[]> scoreQuantMapping = new HashMap<String, Object[]>();

			/** Tricky: using preferred row identity to store information **/
			for (RawDataFile rdf: rdf_sorted) {

				////logger.info(">>>> RDF_write: " + rdf.getName());

				if (Arrays.asList(targetRow.getRawDataFiles()).contains(rdf)) {

					// Adjusted RTs of source aligned rows used to compute target row
					if (recalibrateRT) {
						double rt = rowRTinfo.get(rdf)[0];
						//                        double offset = rowRTinfo.get(rdf)[1];
						//                        double scale = rowRTinfo.get(rdf)[2];
						strAdjustedRTs += rtFormat.format(rt) + AlignedRowProps.PROP_SEP;
						//                        strOffsets += rtFormat.format(offset) + AlignedRowIdentity.IDENTITY_SEP;
						//                        strScales += rtFormat.format(scale) + AlignedRowIdentity.IDENTITY_SEP;
					}

					//
					PeakIdentity id = rowIDs.get(rdf);
					double d_score = rowIDsScores.get(rdf);

					strIdentities += id.getName() + AlignedRowProps.PROP_SEP;
					strScores += d_score + AlignedRowProps.PROP_SEP;


					//                    int cardinality = CollectionUtils.cardinality(id.getName(), Arrays.asList(rowIDsNames));
					//                    if (cardinality > mainIdentityCard) {// && !id.getName().equals(JDXCompound.UNKNOWN_JDX_COMP.getName()) /* *[Note2] */) {
					//                        mainIdentity = id;
					//                        mainIdentityCard = cardinality;
					//                    }

					int cardinality = 0;
					double sum = 0.0;
					for (RawDataFile rdf2: rdf_sorted) {
						PeakIdentity id2 = rowIDs.get(rdf2);
						if (id2 != null && id2.getName().equals(id.getName())) {
							cardinality++;
							sum += rowIDsScores.get(rdf2);
						}
					}
					// If overall score is zero 'cardinality' prevails, Otherwise 'sum' prevails
					// (With sum only check 'null' and 'Unknown' identities would be in concurrency)
					if ((sum == 0.0 && cardinality > mainIdentityCard) || sum > mainIdentitySum) {
						mainIdentity = id;
						mainIdentityCard = cardinality;
						////logger.info(">> found max for: " + mainIdentity.getName() + " / " + sum + ", " + mainIdentitySum + " / " + cardinality + ", " + mainIdentityCard);
						mainIdentitySum = sum;
					}


					//-

					if (scoreQuantMapping.get(id.getName()) == null) {
						Object[] infos = { d_score, 1 };
						scoreQuantMapping.put(id.getName(), infos);
					} else {
						Object[] infos = scoreQuantMapping.get(id.getName());
						infos[0] = d_score + (double) infos[0];
						infos[1] = 1 + (Integer) infos[1];
					}


				} else {
					if (recalibrateRT) {

						strAdjustedRTs += AlignedRowProps.PROP_SEP;
						//                      strOffsets += AlignedRowIdentity.IDENTITY_SEP;
						//                      strScales += AlignedRowIdentity.IDENTITY_SEP;
					}
					strIdentities += AlignedRowProps.PROP_SEP;
					strScores += AlignedRowProps.PROP_SEP;
				}
				if (recalibrateRT) {
					// Gaps must have recalibration info as well, so do it whether or not
					/** 
                    double offset = rtAdjustementMapping.get(rdf)[0];
                    double scale = rtAdjustementMapping.get(rdf)[1];
					 */
					double[] row_offset_scale = JoinAlignerGCTask.getOffsetScaleForRow(rdf, targetRow.getAverageRT(), rtAdjustementMapping);
					double offset = row_offset_scale[0];
					double scale = row_offset_scale[1];
					strOffsets += rtFormat.format(offset) + AlignedRowProps.PROP_SEP;
					strScales += rtFormat.format(scale) + AlignedRowProps.PROP_SEP;
				}                
			}
			if (recalibrateRT) {
				strAdjustedRTs = strAdjustedRTs.substring(0, strAdjustedRTs.length()-1);
				strOffsets = strOffsets.substring(0, strOffsets.length()-1);
				strScales = strScales.substring(0, strScales.length()-1);
			}
			strIdentities = strIdentities.substring(0, strIdentities.length()-1);
			strScores = strScores.substring(0, strScores.length()-1);

			String strQuant = "";
			double mainIdentityQuant = Double.MIN_VALUE;
			// Calculate normalized quantification & deduce most present identity
			for (String idName : scoreQuantMapping.keySet()) {
				Object[] infos = scoreQuantMapping.get(idName);
				infos[0] = (double) infos[0] / (double) rdf_sorted.length;
				strQuant += idName + AlignedRowProps.KEYVAL_SEP + infos[0] + AlignedRowProps.PROP_SEP;
				if ((double) infos[0] > mainIdentityQuant) {
					mainIdentityQuant = (double) infos[0];
				}
			}
			strQuant = strQuant.substring(0, strQuant.length()-1);
			////strQuant = String.valueOf(mainIdentityQuant);

			//
			if (recalibrateRT) {
				//logger.info(">> found max for: " + mainIdentity);
				((SimplePeakIdentity) mainIdentity).setPropertyValue(AlignedRowProps.PROPERTY_RTS, strAdjustedRTs);
				((SimplePeakIdentity) mainIdentity).setPropertyValue(AlignedRowProps.PROPERTY_OFFSETS, strOffsets);
				((SimplePeakIdentity) mainIdentity).setPropertyValue(AlignedRowProps.PROPERTY_SCALES, strScales);
			}
			((SimplePeakIdentity) mainIdentity).setPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_NAMES, strIdentities);
			((SimplePeakIdentity) mainIdentity).setPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_SCORES, strScores);
			((SimplePeakIdentity) mainIdentity).setPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_QUANT, strQuant);
			// Copy the original preferred identity's properties into the targetRow's preferred one
			// and update the mainIdentity properties
			for (PeakIdentity p: targetRow.getPeakIdentities()) {
				if (p.getName().equals(mainIdentity.getName())) {
					///if (p.getName().equals(mainIdentityName)) {
					PeakIdentity targetMainIdentity = p;
					targetRow.setPreferredPeakIdentity(targetMainIdentity);
					//////JDXCompound.setPreferredPeakIdentity(targetRow, targetMainIdentity);
					// Copy props
					for (String key: mainIdentity.getAllProperties().keySet()) {
						String value = mainIdentity.getAllProperties().get(key);
						((SimplePeakIdentity) targetMainIdentity).setPropertyValue(key, value);
					}

					break;
				}
			}


//			// Notify MZmine about the change in the project, necessary ???
//			MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
//			this.project.notifyObjectChanged(targetRow, false);
		}



//		//        if (DEBUG) {
//		int finalNbPeaks2 = 0;
//
//		for (PeakListRow plr: alignedPeakList.getRows()) {
//			finalNbPeaks2 += plr.getNumberOfPeaks();
//
//			for (int i=0; i <  plr.getNumberOfPeaks(); i++) {
//
//				Feature peak = plr.getPeaks()[i];
//
//				String str = peak.getDataFile() + ", @" + peak.getRT();
//
//				//if (!rows_list.contains(str))
//				//logger.info("# MISSING Peak: " + str);
//				rows_list.remove(str);
//				//                else
//				//                    logger.info("> OK Peak: " + str);
//			}
//		}
//		for (String str: rows_list)
//			logger.info("# MISSING Peak: " + str);

		logger.info("Nb peaks ...: " + finalNbPeaks_0 + " / " + leaf_names.size() + " | " + nbAddedRows + " / " + alignedPeakList.getNumberOfRows());
		//logger.info("Nb peaks ...: bip = " + bip);
		logger.info("Nb peaks treated: " + short_names.length + " | " + full_rows_list.size() + " | " + row_names_dict.size() + " | " + finalNbPeaks + " | " + nbAddedPeaks + /*" | " + finalNbPeaks2 +*/ " | >>> " + nbUniquePeaks);

		if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD) {
			
			logger.info("Nb peaks treated - suite: " + leaf_names.size() + " | " + tree_1.getLeafCount() + " | " + full_rows_list.size());                    
		
		} else { //if (CLUSTERER_TYPE == 1 || CLUSTERER_TYPE == 2) {
			//logger.info("Nb peaks treated - suite: " + leaf_names.size() + " | " + getClusterLeafs_2((RootedTree) tree_2, ((RootedTree) tree_2).getRootNode()) + " | " + full_rows_list.size());                    
			
			// TODO: ...!
			
		}


		//
		endTime = System.currentTimeMillis();
		ms = (endTime - startTime);
		logger.info("## >> Whole JoinAlignerGCTask processing took " + Float.toString(ms) + " ms.");


		//System.out.println("globalInfo: " + hierarClusterer.getClusterer().globalInfo());

		//----------------------------------------------------------------------

		if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD) { // Newick => FigTree!

			// Use long names instead of short default ones
			boolean USE_EXPLICIT_NAMES = true;
			// Prefix explicit names with short names (debugging purpose)
			boolean SHOW_NODE_KEY = (USE_EXPLICIT_NAMES && false);


			if (exportDendrogramAsTxt && dendrogramTxtFilename != null) {
				PrintWriter out;
				try {
					out = new PrintWriter(dendrogramTxtFilename);

					String output_str = (dendrogramFormatType == DendrogramFormatType.NEWICK) 
							? newickCluster_clean : recursive_print(tree_1, 0, 0);

//					if (USE_EXPLICIT_NAMES) {
//						for (String short_n : dendro_names_dict.keySet()) {
//							String short_nn = HierarClusterer.NEWICK_LEAF_NAME_PREFIX + short_n;
//							String long_nn = dendro_names_dict.get(short_n).replaceAll(", ", "_");
//							output_str = output_str.replaceAll(short_nn + ":", ((SHOW_NODE_KEY) ? short_nn + "-" : "") + long_nn + ":");
//						}
//					}

					out.println(output_str);

					out.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else { //if (CLUSTERER_TYPE == 1 || CLUSTERER_TYPE == 2) { // CDT + GTR => TreeView!
			
			// TODO: ...!
			
		}

		//----------------------------------------------------------------------

		// Add new aligned peak list to the project
		this.project.addPeakList(alignedPeakList);
		
		for (RawDataFile rdf: alignedPeakList.getRawDataFiles())
			System.out.println("RDF: " + rdf);

		// Add task description to peakList
		alignedPeakList
			.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
				JoinAlignerGCTask.TASK_NAME, parameters));

		logger.info("Finished join aligner GC");
		setStatus(TaskStatus.FINISHED);



	}


	private void printDistanceMatrixToDatFile(String datFileName, TriangularMatrix matrix, boolean binary) {


		try /*(Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(datFileName), "utf-8")))*/ {

			if (binary) {

				FileOutputStream fstream = new FileOutputStream(datFileName);
				DataOutputStream outputFile = new DataOutputStream(new BufferedOutputStream(fstream));


				for (int i = 0; i < matrix.getDimension(); i++) {

					for (int j = i; j < matrix.getDimension(); j++) {

						//writer.write(i + " "+ j + " " + matrix.get(i, j) + "\n");
						//            		outputFile.writeInt(i);
						//            		outputFile.writeInt(j);
						//            		outputFile.writeDouble(matrix.get(i, j));

						outputFile.writeUTF(i + " " + j + " " + matrix.get(i, j) + " ");
					}
					//outputFile.writeUTF(" ");
				}
				//outputFile.writeChar('\n');

				//writer.close();
				outputFile.close();

			} else {

				Writer writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(datFileName), "utf-8"));

				for (int i = 0; i < matrix.getDimension(); i++) {

					for (int j = i; j < matrix.getDimension(); j++) {
						writer.write(i + " " + j + " " + matrix.get(i, j) + " " /*+ "\n"*/);
					}
					//writer.write(" ");
				}

				writer.close();
			}

		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}

	private void printAlignedPeakList(List<List<PeakListRow>> clustersList) {

		// Print result to file (one line per cluster)
		System.out.println("--------------- IN FINE ----------------");
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream("clusters_meth-" + CLUSTERER_TYPE), "utf-8"))) {

			for (List<PeakListRow> cl: clustersList) {

				for (PeakListRow plr: cl) {

					writer.write(full_rows_list.indexOf(plr) + " ");//(plr.getID() + " ");
				}
				writer.write("\n");
			}

			writer.close();

		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}

	//    private static int encodeIndexes8(int index1, int index2) {
	//        return (index1 << 8) | index2;
	//    }
	//
	//    private static int[] decodeIndexes8(int combinedIds) {
	//        return new int[] { combinedIds >> 8, combinedIds & 0x00FF };
	//    }



	String recursive_print(Tree tree, int currkey, int currdepth) {

		TreeNode currNode = tree.getNodeByKey(currkey);
		int numChildren = currNode.numberChildren();


		String str = "";
		for (int i = 0; i < currdepth; i++)
		{
			str += "  ";
		}
		str += currNode.getName() + "[" + currkey + "], depth: " + currdepth + " > " + currNode.height + " > " + currNode.weight + " > " + currNode.getBcnScore() + "\n";

		for (int i = 0; i < numChildren; i++) {

			int childkey = currNode.getChild(i).key;
			TreeNode childnode = tree.getNodeByKey(childkey);
			//System.out.println("child name is: " + childnode.getName() + " depth is: " + currdepth);
			str += recursive_print(tree, childkey, currdepth + 1);
		}

		return str;
	}

	//    public String toConsole(Cluster clust, int indent)
	//    {
	//        String str = "";
	//
	//        for (int i = 0; i < indent; i++)
	//        {
	//            str += "  ";
	//
	//        }
	//        String name = clust.getName() 
	//                + (clust.isLeaf() ? " (leaf)" : "") 
	//                + (clust.getDistanceValue() != null ? "  distance: " + clust.getDistanceValue() : "")
	//                + (clust.getDistanceValue() != null ? "  t-distance: " + clust.getTotalDistance() : "")
	//                ;
	//        str += name + "\n";
	//        for (Cluster child : clust.getChildren())
	//        {
	//            str += toConsole(child, indent + 1);
	//        }
	//
	//        return str;
	//    }
	//


	/**
	 * Two clusters can be merged if and only if:
	 *  - The resulting merged cluster: (their parent) doesn't exceed 'level' leaves
	 *  - The distance between them two is acceptable (close enough)
	 */
	private List<Integer> getValidatedClusters_2(ClusteringResult clusteringResult, int level, double max_dist, TriangularMatrix distMtx) {

		List<Cluster> validatedClusters = new ArrayList<>();

		String newickCluster = clusteringResult.getHierarchicalCluster();
		// Make string really Newick standard
		//String newickCluster_clean = newickCluster.substring(newickCluster.indexOf(System.getProperty("line.separator"))+1) + ";";
		String newickCluster_clean = newickCluster.substring(newickCluster.indexOf("\n")+1) + ";";

		// Parse Newick formatted string
		BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
		TreeParser tp = new TreeParser(r);
		Tree tree = tp.tokenize(1, "tree", null);

		return recursive_validate_clusters_2(tree, level, max_dist, 0, 0, distMtx);
	}


	static int tot = 0;
	static int tot2 = 0;
	//-
	List<Integer> recursive_validate_clusters_2(Tree tree, int level, double max_dist, int currkey, int currdepth, TriangularMatrix distMtx) {

		List<Integer> validatedClusters = new ArrayList<>();

		TreeNode currNode = tree.getNodeByKey(currkey);

		if (!currNode.isLeaf()) {

			int numChildren = currNode.numberChildren();

			for (int i = 0; i < numChildren; i++) {

				TreeNode child = currNode.getChild(i);

				if (child.isLeaf()) {
					validatedClusters.add(child.key);
					tot++;
					tot2++;
					continue;
				}


				boolean node_is_cluster = true;

				double max_dist_2 = -1d;
				if (child.numberLeaves <= level /*&& !child.isLeaf()*/) {

					List<Integer> leafs_nodekeys = getClusterLeafs_1(child);

					/** Leaves must all be an acceptable distance from each other for node to be considered a cluster! */
					// For each leaf
					for (int k = 0; k < leafs_nodekeys.size(); k++) {

						TreeNode left_leaf = tree.getNodeByKey(leafs_nodekeys.get(k));
						//System.out.println("left_leaf.getName() = " + left_leaf.getName());
						//	        			int left_leaf_id = Integer.valueOf(left_leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
						//						logger.info("Left node name: '" + left_leaf.getName() + "'! (" + left_leaf.getKey() + ")");
						String left_leaf_name = left_leaf.getName();

						if (left_leaf.getName().isEmpty()) {
							logger.info("\t=> Skipped!");
							continue;
						}

						if (left_leaf.getName().startsWith(HierarClusterer.NEWICK_LEAF_NAME_PREFIX))
							left_leaf_name = left_leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length());
						else if (left_leaf.getName().startsWith(" ")) {
							left_leaf_name = left_leaf.getName().substring(1);
						}
						int left_leaf_id = Integer.valueOf(left_leaf_name);

						// For each leaf
						for (int l = k+1; l < leafs_nodekeys.size(); l++) {

							//if (l == k) { continue; }

							TreeNode right_leaf = tree.getNodeByKey(leafs_nodekeys.get(l));
							//System.out.println("right_leaf.getName() = " + right_leaf);
							//		        			int right_leaf_id = Integer.valueOf(right_leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
							//							logger.info("Right node name: '" + right_leaf.getName() + "'! (" + right_leaf.getKey() + ")");
							String right_leaf_name = right_leaf.getName();

							if (right_leaf.getName().isEmpty()) {
								logger.info("\t=> Skipped!");
								continue;
							}

							if (right_leaf.getName().startsWith(HierarClusterer.NEWICK_LEAF_NAME_PREFIX))
								right_leaf_name = right_leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length());
							else if (right_leaf.getName().startsWith(" ")) {
								right_leaf_name = right_leaf.getName().substring(1);
							}
							int right_leaf_id = Integer.valueOf(right_leaf_name);

							// Get distance between left and right leafs
							//		    				double dist = getValueFromVector(left_leaf_id, right_leaf_id, dim, distVect);//distMtx[left_leaf_id][right_leaf_id];
							double dist = distMtx.get(left_leaf_id, right_leaf_id);
							if (max_dist_2 < dist) {
								max_dist_2 = dist;
							}
						}
					}

				}
				node_is_cluster = (child.numberLeaves <= level && (max_dist_2 >= 0d && max_dist_2 < max_dist + EPSILON));

				if (node_is_cluster) {
					validatedClusters.add(child.key);
					tot++;
					tot2 += child.numberLeaves;
				} else {
					validatedClusters.addAll(recursive_validate_clusters_2(tree, level, max_dist, child.key, currdepth + 1, distMtx));
				}
			}
		} else {
			validatedClusters.add(currNode.key);
			tot++;
			tot2++;
		}

		//    	for (int i = 0; i < numChildren; i++) {
		//    	
		//    		int childkey = currNode.getChild(i).key;
		//    		TreeNode childnode = tree.getNodeByKey(childkey);
		//    		System.out.println("child name is: " + childnode.getName() + " depth is: " + currdepth);
		//    		recursive_print(tree, childkey, currdepth+1);
		//    	}

		if (DEBUG) {
			// Check integrity
			Set<Integer> leafs = new HashSet<>();
			for (int clust_key : validatedClusters) {

				TreeNode clust = tree.getNodeByKey(clust_key);
				leafs.addAll(getClusterLeafs_1(clust));
			}
			System.out.println("Leafs are (count:" + leafs.size() + "):");
			System.out.println(Arrays.toString(leafs.toArray()));
		}

		return validatedClusters;	
	}


	private List<Integer> getClusterLeafs_1(TreeNode node) {

		List<Integer> leafs_nodekeys = new ArrayList<>();

		if (node.isLeaf()) {
			//leafs_nodekeys = new ArrayList<>();
			leafs_nodekeys.add(node.getKey());
		} else {


			for (int i = 0; i < node.numberChildren(); i++) {

				TreeNode child = node.getChild(i);

				if (child.isLeaf()) {
					leafs_nodekeys.add(child.getKey());
				} else {
					leafs_nodekeys.addAll(getClusterLeafs_1(child));
				}
			}
		}

		return leafs_nodekeys;
	}


	/**
	 * Two clusters can be merged if and only if:
	 *  - The resulting merged cluster: (their parent) doesn't exceed 'level' leaves
	 *  - The distance between them two is acceptable (close enough)
	 */
//	private List<List<Integer>> getValidatedClusters_3(
//			/*ClusteringResult clusteringResult*/org.gnf.clustering.Node[] arNodes, 
//			float minCorrValue, int level, double max_dist, DistanceMatrix1D distMtx 
//			/*,Set<Integer> flatLeaves*/) {
	private List<List<Integer>> getValidatedClusters_3(
			org.gnf.clustering.Node[] arNodes, 
			float minCorrValue, int level, 
			double max_dist, DistanceMatrix distMtx,
			RowVsRowDistanceProvider distProvider
			) {

		List<List<Integer>> validatedClusters = new ArrayList<>();

		int nNodes = arNodes.length; //distMtx.getRowCount() - 1;
		System.out.println("nNodes=" + nNodes + " | arNodes.length=" + arNodes.length);
		
		
		boolean do_compute_best = false;
		
		float maxCorrDist = 0;	
		if (do_compute_best) {
			for (int nNode = 0; nNode < nNodes; nNode++) {
	
				if (arNodes[nNode].m_fDistance > maxCorrDist)
					maxCorrDist = (float) arNodes[nNode].m_fDistance;
				
			}
			if (maxCorrDist < 1) maxCorrDist = 1;
		}
		
		// Find nodes that matched with good quality (> minCorrValue)
		// (Assuming the 'arNodes' as already sorted on this very same criteria)
		// @See org.gnf.clustering.Utils.NodeSort()
		List<Integer> bestNodes = new ArrayList<>(); 
		if (do_compute_best) {
			for (int nNode = 0; nNode < nNodes; nNode++) {
				
				org.gnf.clustering.Node node = arNodes[nNode];
				
				float corr_val = (float) ((maxCorrDist - node.m_fDistance) / maxCorrDist);
				// And if matched good enough
				if (corr_val >= minCorrValue) {
					
					bestNodes.add(nNode);
				} else {
					break; // Because 'arNodes' is sorted (do not break otherwise!)
				}
			}
		}
		if (DEBUG_2)
			System.out.println("###BEST NODES (size:" + bestNodes.size() + "): " + bestNodes.toString());

		
		// Find nodes that can be clusters (nb leaves < level)
		//for (int nBest: bestNodes) {
		System.out.println("##START TRACING HIERARCHY... (starting from furthest node)");
		
		// TODO: ... Implement all stuff for cutoff, right now, just browsing the whole tree from very worst
		// correlation scoring node
//		int nBest = bestNodes.get(bestNodes.size() - 1); //112; //
		int nBest = arNodes.length - 1; //112; //
		if (nBest < 0) { nBest = -nBest - 1; }
		
		System.out.println("#TRACING BEST NODE '" + nBest + "' :");
		//**validatedClusters.addAll(recursive_validate_clusters_3(arNodes, nBest, level, max_dist, distMtx));
		validatedClusters.addAll(recursive_validate_clusters_3(arNodes, nBest, level, max_dist, distMtx, distProvider));
		

		if (DEBUG) {
			// Check integrity
			Set<Integer> leaves = new HashSet<>();
			for (List<Integer> clust_leaves: validatedClusters) {

				leaves.addAll(clust_leaves);
			}
			System.out.println("Leafs are (count:" + leaves.size() + "):");
			System.out.println(Arrays.toString(leaves.toArray()));
		}
		//-
		if (DEBUG_2)
			printValidatedClusters_3(validatedClusters);
		
		return validatedClusters;
	}
	//-
	List<List<Integer>> recursive_validate_clusters_3(
			org.gnf.clustering.Node[] arNodes, int nNode, 
			int level, /*float minCorrValue, */
			double max_dist, DistanceMatrix distMtx, RowVsRowDistanceProvider distProvider) {

		List<List<Integer>> validatedClusters = new ArrayList<>();


		//int nNodes = arNodes.length;

		if (nNode < 0) { nNode = -nNode - 1; }

		// WARN: Skip the trees's super parent in any case !!
		if (nNode >= arNodes.length) { /*nNode = 0;*/ return validatedClusters; }

		org.gnf.clustering.Node node = arNodes[nNode];

		
		// Is leaf parent node => no need to go further: this is a cluster!
		boolean is_dual_leaf_node = (node.m_nLeft >= 0 && node.m_nRight >= 0);
		if (DEBUG_2)
			System.out.println("\n# >>>>>>>>>>>>> BEGIN ITERATING NODE #" + nNode + " <<<<<<<<<<<< '" 
					+ node.toString() + "' (Is dual leaf? " + is_dual_leaf_node + ")");
//		if (!is_dual_leaf_node) {

			List<Integer> leaves = getLeafIds(arNodes, nNode);
			
			if (DEBUG_2) {
				
			
				System.out.println("#NB lEAVES " + leaves.size() + " (expected lvl: " + level + ")");
			//if (leaves.size() <= level) {

				System.out.println("#GET INTO NODE lEAVES " + node.toString());

				// If can be a cluster: Is the current node a splitting point?
//				List<Integer> left_leaves = getLeafIds(arNodes, node.m_nLeft);
//				List<Integer> right_leaves = getLeafIds(arNodes, node.m_nRight);
//				//
//				System.out.println("getLeafIds(arNodes, " + node.m_nLeft + "): " + left_leaves);
//				System.out.println("getLeafIds(arNodes, " + node.m_nRight + "): " + right_leaves);
				
				System.out.println("getLeafIds(arNodes, " + nNode + "): " + leaves);
			}
			
				// Check validity
				boolean node_is_cluster = true;
				float max_dist_2 = Float.MIN_VALUE;
				//-
				boolean nb_leaves_ok = (leaves.size() <= level);
				//-
				// Compare distances of each leaf to each other to check cluster's consistency
				if (nb_leaves_ok) {
					for (int i = 0; i < leaves.size(); i++) {
						for (int j = i+1; j < leaves.size(); j++) {
	
							// Get distance between left and right leafs
							float dist = 0.0f;
							if (distMtx != null) {
								dist = distMtx.getValue(leaves.get(i), leaves.get(j));
							} else {
								
//								PeakListRow row = full_rows_list.get(leaves.get(i));
//								Range<Double> mzRange = mzTolerance.getToleranceRange(row
//										.getBestPeak().getMZ());
//								Range<Double> rtRange = rtTolerance.getToleranceRange(row
//										.getBestPeak().getRT());
								
//								RowVsRowScoreGC score = distProvider.getScore(
//										leaves.get(i), leaves.get(j),
//										RangeUtils.rangeLength(mzRange) / 2.0,
//										RangeUtils.rangeLength(rtRange) / 2.0
//										);
//								
//								dist = (float) (maximumScore - score.getScore());
								
								dist = (float) distProvider.getRankedDistance(
										leaves.get(i), leaves.get(j), 
//										RangeUtils.rangeLength(mzRange) / 2.0, 
//										RangeUtils.rangeLength(rtRange) / 2.0,  
										mzTolerance.getMzTolerance(), 
										rtTolerance.getTolerance(),
										minScore
										);
								
							}
							if (max_dist_2 < dist) {
								max_dist_2 = dist;
							}
							if (DEBUG_2)
								System.out.println("dist(" + leaves.get(i) + "," + leaves.get(j) + ") = " + dist);
						}
					}
				}
				node_is_cluster = nb_leaves_ok && (max_dist_2 >= 0f && max_dist_2 < max_dist + EPSILON);
				
				if (DEBUG_2)
					System.out.println("#IS CLUSTER? " + node_is_cluster + " (nb_leaves_ok: " + nb_leaves_ok 
						+ ", max_dist: " + max_dist_2 + " < " + max_dist + ")");
				
				// If valid, keep as is...
				if (node_is_cluster) {
					
					if (DEBUG_2)
						System.out.println("#CLUSTER OK " + node_is_cluster + " (" + max_dist_2 + " / " + max_dist + ")");

					validatedClusters.add(leaves);
					tot++;
					tot2 += leaves.size();
				} 
				// Otherwise, split! (ie. iterate through left and right branches)
				else {

					// Is node: Recurse on left
					if (node.m_nLeft < 0)
						validatedClusters.addAll(recursive_validate_clusters_3(
								arNodes, node.m_nLeft, level, /*minCorrValue,*/
								max_dist, distMtx, 
								distProvider));
					// Is leaf: Append
					else
						validatedClusters.add(Arrays.asList(new Integer[] { node.m_nLeft } ));
					//-
					// Is node: Recurse on right
					if (node.m_nRight < 0)
						validatedClusters.addAll(recursive_validate_clusters_3(
								arNodes, node.m_nRight, level, /*minCorrValue,*/
								max_dist, distMtx, 
								distProvider));
					// Is leaf: Append
					else
						validatedClusters.add(Arrays.asList(new Integer[] { node.m_nRight } ));

				}

			//}

//		} else { // Can the two leaves be a cluster?
//
//			System.out.println("#CLUSTERIZE NODE lEAVES... " + node.toString());
//			
//			boolean node_is_cluster = true;
//			float max_dist_2 = Float.MIN_VALUE;
//			
//			boolean nb_leaves_ok = (2 <= level);
//
//			// Get distance between left and right leafs
//			float dist = distMtx.getValue(node.m_nLeft, node.m_nRight);
//			if (max_dist_2 < dist) {
//				max_dist_2 = dist;
//			}
//			node_is_cluster = nb_leaves_ok && (max_dist_2 >= 0f && max_dist_2 < max_dist + EPSILON);
//
//			System.out.println("#IS CLUSTER? " + node_is_cluster + " (nb_leaves_ok: " + nb_leaves_ok 
//					+ ", max_dist: " + max_dist_2 + " < " + max_dist + ")");
//			
//			if (node_is_cluster) {
//				List<Integer> clust = new ArrayList<>();
//				clust.add(node.m_nLeft);
//				clust.add(node.m_nRight);
//				validatedClusters.add(clust);
//			}
//		}
				if (DEBUG_2)
					System.out.println("# >>>>>>>>>>>>> END ITERATING NODE #" + nNode + " <<<<<<<<<<<< '" 
							+ node.toString() + "' (Is dual leaf? " + is_dual_leaf_node + ")");

		return validatedClusters;	
	}
	//-
	List<Integer> getLeafIds(org.gnf.clustering.Node[] arNodes, int nNode/*org.gnf.clustering.Node parentNode*//*, List<Integer> doneNodes*/) {

		List<Integer> leafIds = new ArrayList<>();

//		System.out.println("(0) nNode: " + nNode); 		
		if (nNode < 0) { nNode = -nNode - 1; }
//		System.out.println("(1) nNode: " + nNode); 
		
		// WARN: Skip the trees's super parent in any case !!
		if (nNode >= arNodes.length) { /*nNode = 0;*/ return leafIds; }
		
		if (arNodes[nNode].m_nLeft < 0) // Is node => recurse ... getLeafIds(arNodes, -arNodes[nNode].m_nLeft - 1)
			leafIds.addAll(getLeafIds(arNodes, arNodes[nNode].m_nLeft));
		else // Is leaf => append
			leafIds.add(arNodes[nNode].m_nLeft);

		if (arNodes[nNode].m_nRight < 0)  // Is node => recurse
			leafIds.addAll(getLeafIds(arNodes, arNodes[nNode].m_nRight));
		else  // Is leaf => append
			leafIds.add(arNodes[nNode].m_nRight);
		
		return leafIds;
	}
	//-
	void printValidatedClusters_3(List<List<Integer>> validatedClusters) {
		
		int i = 0;
		for (List<Integer> cl: validatedClusters) {
			
			System.out.println("CLUS#" + i + ": " + cl.toString());
			i++;
		}
	}
	

//	List<List<Integer>> recursive_validate_clusters_3(org.gnf.clustering.Node[] arNodes, double min_correl_val, 
//			final Set<Integer> flatLeaves) {
//	
//		List<List<Integer>> validatedClusters = new ArrayList<>();
//
//		int nNodes = arNodes.length;
//
//		float maxDist = 0;	
////		if (treeviewFormat)
////		{					
//			for (int nNode = 0; nNode < nNodes; nNode++) {
//				
////				System.out.println("m_fDistance: " + arNodes[nNode].m_fDistance + " | " + maxDist);
//				
//				if (arNodes[nNode].m_fDistance > maxDist)
//					maxDist = (float) arNodes[nNode].m_fDistance;
//				
//				//System.out.println("arNodes[" + i + "] = " + arNodes[nNode].toString());
//			}
//			if (maxDist < 1) maxDist = 1;
////			System.out.println("maxDist: " + maxDist);
////		}
//		
//		//Map<Integer, Integer> doneNodes = new HashMap<>(); // Avoid browsing nodes already integrated nodes
//		for (int nNode = 0; nNode < nNodes; nNode++)
//		{
////			String nodeRecord = "NODE" + (nNode + 1) +  "X";
////			if (arNodes[nNode].m_nLeft < 0) 
////				nodeRecord += "\tNODE" + org.gnf.clustering.Utils.IntToStr(-arNodes[nNode].m_nLeft - 1 + 1) + "X";
////			else nodeRecord += "\tGENE" + org.gnf.clustering.Utils.IntToStr(arNodes[nNode].m_nLeft + 1) + "X";
////
////			if (arNodes[nNode].m_nRight < 0) 
////				nodeRecord += "\tNODE" + org.gnf.clustering.Utils.IntToStr(-arNodes[nNode].m_nRight - 1 + 1) + "X";
////			else nodeRecord += "\tGENE" + org.gnf.clustering.Utils.IntToStr(arNodes[nNode].m_nRight + 1) + "X";
//			
//
//			float val = (float) ((maxDist - arNodes[nNode].m_fDistance) / maxDist);
//
//			if (val > min_correl_val - EPSILON /*&& !doneNodes.contains(nNode)*/) {
//				
//				List<Integer> leaf_ids = getLeafIds(arNodes, nNode/*, doneNodes*/);
////				//doneNodes.addAll(leaf_ids);
////				if (doneNodes.get(nNode) != null)
////					doneNodes.put(nNode, Math.max(leaf_ids.size(), ));
//				
//				// If a subset of this cluster was previously registered, replace!
//				int subset_exists_index = -1; // -1=false
//				for (int i = 0; i < validatedClusters.size(); i++) {
//					List<Integer> cl = validatedClusters.get(i);
//					if (leaf_ids.containsAll(cl)) {
//						subset_exists_index = i;
//						break;
//					}
//				}
//				if (subset_exists_index != -1) // Absorb
//					validatedClusters.set(subset_exists_index, leaf_ids);
//				else
//					validatedClusters.add(leaf_ids);
//				flatLeaves.addAll(leaf_ids);
//			}
//		}
//
//		return validatedClusters;	
//	}


	public static double getAdjustedRT(double rt, double b_offset, double a_scale) {
		double delta_rt = a_scale * rt + b_offset;
		return (rt + delta_rt);
	}

	public static double getReverseAdjustedRT(double rt, double b_offset, double a_scale) {
		double delta_rt = a_scale * rt + b_offset;
		return (rt - delta_rt);
	}

	public static double[] getOffsetScaleForRow(RawDataFile rdf, double row_rt, Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping) {

		//        double[] ret = null;
		//        double a_scale = -1.0;
		//        double b_offset = -1.0;

		List<double[]> ref_values = rtAdjustementMapping.get(rdf);

		// Single ref => 2 cases:
		//      1/ Single [offset, scale]
		//      2/ Offset only
		if (ref_values.size() == 1) {
			//            // Offset only (scale set to -1.0d)
			//            if (Math.abs(ref_values.get(0)[1] + 1.0d) < EPSILON)
			//                return new double[] { ref_values.get(0)[0], 1.0d };
			//            // Single [offset, scale]
			//            else {
			return new double[] { ref_values.get(0)[0], ref_values.get(0)[1] };
			//            }
		}

		// Scale, but before the first interval
		if (row_rt < ref_values.get(0)[2]) 
			////|| (Math.abs(ref_values.get(0)[2] - row_rt) < EPSILON)) 
		{
			// TODO: Warning! Very approximative!
			return new double[] { ref_values.get(0)[0], ref_values.get(0)[1] };
		}
		// Scale, but after the last interval
		if (row_rt > ref_values.get(ref_values.size()-1)[2]) 
			////|| (Math.abs(ref_values.get(ref_values.size()-1)[2] - row_rt) < EPSILON)) 
		{
			// TODO: Warning! Very approximative!
			return new double[] { ref_values.get(ref_values.size()-1)[0], ref_values.get(ref_values.size()-1)[1] };
		}

		// Scale, inside an interval
		for (int i = 1; i < ref_values.size(); ++i) {

			// Exactly on the current reference RT: Use current ref's [offset/scale] values
			if (Math.abs(ref_values.get(i)[2] - row_rt) < EPSILON) {

				return new double[] { ref_values.get(i)[0], ref_values.get(i)[1] };
			} 
			// Strictly before the current reference RT: Use the previous interval's [offset/scale] values
			else if (ref_values.get(i)[2] > row_rt) {

				return new double[] { ref_values.get(i-1)[0], ref_values.get(i-1)[1] };
			}
		}

		return null;
	}

	public static void GenerateCDT(final String outFileName, DataSource source, 
			int nRowCount, int nColCount, String separator,
			final String[] rowNames, final String[] colNames, int[] rowOrder) throws IOException 
	{	



		FileWriter fstream = new FileWriter(outFileName);
		BufferedWriter writer = new BufferedWriter(fstream);

		int nRow = 0;
		int nCol = 0;			
		String nextLine = null; 

		String outHead = "GID\tDESCR\tNAME" + ((source != null) ? "\t" : "");
		if (source != null) { // Case no additional columns, except info ones
			for (int i = 0; i < nColCount - 1 ; i++)
				outHead += colNames[i] + "\t";
			outHead += colNames[nColCount - 1] + "\n";
		} else {
			outHead += "\n";
		}
		writer.write(outHead);  
		for (int i = 0; i < nRowCount; i++)
		{
			int n = rowOrder[i];
			String outRow = "GENE" + (org.gnf.clustering.Utils.IntToStr(n + 1))  + "X\t";
			outRow += rowNames[n] + "\t" + rowNames[n] + ((source != null) ? "\t" : "");
			if (source != null) {
				for (int j = 0; j < nColCount - 1 ; j++)
					outRow += org.gnf.clustering.Utils.FloatToStr(source.getValue(n, j),-1) + "\t";
				outRow += org.gnf.clustering.Utils.FloatToStr(source.getValue(n, nColCount - 1),-1);
			}
			if (i < nRowCount - 1) outRow += "\n";
			writer.write(outRow);           
		}			
		writer.close();

	}
	//-
//	public static void WriteTreeToFile(final String fileName, final int nNodes, 
//			org.gnf.clustering.Node [] arNodes, boolean treeviewFormat) 
//	{	
//		try
//		{
//			float maxDist = 0;	
//			if (treeviewFormat)
//			{					
//				for(int nNode = 0; nNode < nNodes; nNode++) {
//					
//					System.out.println("m_fDistance: " + arNodes[nNode].m_fDistance + " | " + maxDist);
//					
//					if (arNodes[nNode].m_fDistance > maxDist)
//						maxDist = (float) arNodes[nNode].m_fDistance;
//				}
//				if (maxDist < 1) maxDist = 1;
//				System.out.println("maxDist: " + maxDist);
//			}
//
//			FileWriter fstream = new FileWriter(fileName);
//			BufferedWriter writer = new BufferedWriter(fstream);
//
//			for(int nNode = 0; nNode < nNodes; nNode++)
//			{
//				String nodeRecord = "NODE" + (nNode + 1) +  "X";
//				if (arNodes[nNode].m_nLeft < 0) 
//					nodeRecord += "\tNODE" + org.gnf.clustering.Utils.IntToStr(-arNodes[nNode].m_nLeft - 1 + 1) + "X";
//				else nodeRecord += "\tGENE" + org.gnf.clustering.Utils.IntToStr(arNodes[nNode].m_nLeft + 1) + "X";
//
//				if (arNodes[nNode].m_nRight < 0) 
//					nodeRecord += "\tNODE" + org.gnf.clustering.Utils.IntToStr(-arNodes[nNode].m_nRight - 1 + 1) + "X";
//				else nodeRecord += "\tGENE" + org.gnf.clustering.Utils.IntToStr(arNodes[nNode].m_nRight + 1) + "X";
//
//				float val = 0;
//				if (treeviewFormat) 
//					val = (float) ((maxDist - arNodes[nNode].m_fDistance)/maxDist);
//				else 
//					val = (float) arNodes[nNode].m_fDistance;
//
//
//				nodeRecord += "\t" + (org.gnf.clustering.Utils.FloatToStr(val,2));
//
//				writer.write(nodeRecord + '\n');            
//			}
//			writer.close();
//		} catch (Exception e){System.err.println("Error: " + e.getMessage());}
//
//	}
//	
	

	/**
	 * MEMORY check stuffs
	 */
	static final int MB = 1024 * 1024;
	static int toMB(long bytes) {
		return (int) Math.rint(bytes / MB);
	}
	//-
	// Prints in MegaBytes
	public static void printMemoryUsage(Runtime rt, Long prevTotal, Long prevFree, String prefix) {

		long max = rt.maxMemory();
		long total = rt.totalMemory();
		long free = rt.freeMemory();
		if (total != prevTotal || free != prevFree) {
			long used = total - free;
			long prevUsed = (prevTotal - prevFree);
			System.out.println(
					"## [" + prefix + "] MEM USAGE [max: " + toMB(max) + "] ## >>> Total: " + toMB(total) + 
					", Used: " + toMB(used) +
					", ∆Used: " + toMB(used - prevUsed) +
					", Free: " + toMB(free) +
					", ∆Free: " + toMB(free - prevFree)
					);
			prevTotal = total;
			prevFree = free;
		}
	}


}
