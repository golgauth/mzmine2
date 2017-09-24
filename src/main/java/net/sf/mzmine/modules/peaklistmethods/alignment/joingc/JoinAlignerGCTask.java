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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
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
//import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.DBSCANClusterer;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.HDBSCANClusterer;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.HierarClusterer;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.LinkType;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.Tree;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.TreeNode;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.TreeParser;
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
    private ClusteringLinkageStrategyType linkageStartegyType_0;
    private LinkType linkageStartegyType_12;
    private boolean useOldestRDFAncestor;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private double mzWeight, rtWeight;
    private double minScore;
    private double idWeight;

    private boolean useApex, useKnownCompoundsAsRef;
    private boolean useDetectedMzOnly;
    private RTTolerance rtToleranceAfter;

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
    private final double veryLongDistance; // = 1.0d;//50.0d;//Double.MAX_VALUE;
    // For comparing small differences.
    private static final double EPSILON = 0.0000001;


    private static final boolean DEBUG = false;
    List<PeakListRow> full_rows_list;
    
    
    private ClusteringProgression clustProgress; 
    

    private static final int CLUSTERER_TYPE = 0; //0:Hierar, 1:KMeans, 2:XMeans, 3:Cobweb, 4:OPTICS/DBSCAN, 5:HDBSCAN (Star/ELKI)
	public static final boolean USE_DOUBLE_PRECISION_FOR_DIST = true;
	
	
	
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
        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
        	linkageStartegyType_0 = parameters.getParameter(
        			JoinAlignerGCParameters.linkageStartegyType_0).getValue();
        } else {
        	linkageStartegyType_12 = parameters.getParameter(
        			JoinAlignerGCParameters.linkageStartegyType_12).getValue();
        }
        
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

        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
	        exportDendrogramAsPng = parameters.getParameter(
	                JoinAlignerGCParameters.exportDendrogramPng).getValue();
	        dendrogramPngFilename = parameters.getParameter(
	                JoinAlignerGCParameters.dendrogramPngFilename).getValue();
//	        exportDendrogramAsTxt = parameters.getParameter(
//	                JoinAlignerGCParameters.exportDendrogramTxt).getValue();
//	        dendrogramTxtFilename = parameters.getParameter(
//	                JoinAlignerGCParameters.dendrogramTxtFilename).getValue();
        } else {
//        	exportDendrogramNewickTxt = parameters.getParameter(
//        			JoinAlignerGCParameters.exportDendrogramNewickTxt).getValue();
//        	dendrogramNewickTxtFilename = parameters.getParameter(
//        			JoinAlignerGCParameters.dendrogramNewickTxtFilename).getValue();
        }
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
        maximumScore = mzWeight + rtWeight;
        veryLongDistance = 10.0d * maximumScore;
        
        
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
    public void run() {

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
            
            // (0)
//            for (int i=0; i < peakLists.length; ++i) {
//                //double offset, scale;
//                PeakList a_pl = peakLists[i];
//                
//                logger.info("# Search identities for list: " + a_pl.getRawDataFile(0) + " (nb peakLits = " + peakLists.length + ")");
//                
//                // Get ref RT1 and RT2
//                // Sort peaks by ascending RT
//                PeakListRow a_pl_rows[] = a_pl.getRows().clone(); 
//                Arrays.sort(a_pl_rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
//                //
//                ArrayList<PeakListRow> allIdentified = new ArrayList<>();
//                ArrayList<String> allIdentified_i = new ArrayList<>();
//                List<Double> rtList = new ArrayList<>();
//                //
//                for (int j=0; j < a_pl_rows.length; ++j) {
//                	
//                    PeakListRow row = a_pl_rows[j];
//                    // If row actually was identified AND is a "reference compound"
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
////                        logger.info("aFailed 0: " + (row.getPreferredPeakIdentity().getName() != JDXCompound.UNKNOWN_JDX_COMP.getName()));
////                        logger.info("aFailed 1: " + row.getPreferredPeakIdentity());
////                        logger.info("aFailed 2: " + row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF));                       
//                    }
//                }
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

        LinkageStrategy linkageStrategy = null;
        //
        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
        	
        	switch (linkageStartegyType_0) {
        	case AVERAGE:
        		linkageStrategy = new AverageLinkageStrategy();
        		break;
        	case COMPLETE:
        		linkageStrategy = new CompleteLinkageStrategy();
        		break;
        	case SINGLE:
        		linkageStrategy = new SingleLinkageStrategy();
        		break;
        	case WEIGHTED:
        		linkageStrategy = new WeightedLinkageStrategy();
        		break;
        	default:
        		linkageStrategy = new AverageLinkageStrategy();
        		break;
        	}
        }



        double[][] distances = null;
        //Map<Pair<Integer, Integer>, Float> distancesMap = new HashMap<>();
        double[] distancesAsVector = null;
        float[] distancesAsFloatVector = null;
        int numInstances = 0;
        
        
        String[] short_names;
        int nbPeaks = 0;
        for (int i = 0; i < newIds.length; ++i) {
            PeakList peakList = peakLists[newIds[i]];
            nbPeaks += peakList.getNumberOfRows();
            logger.info("> Peaklist '" + peakList.getName() + "' [" + newIds[i] + "] has '" + peakList.getNumberOfRows() + "' rows.");
        }
        distances = new double[nbPeaks][nbPeaks];
        short_names = new String[nbPeaks];

        Map<String, PeakListRow> row_names_dict = new HashMap<>();
        full_rows_list = new ArrayList<>();
        Map<String, String> dendro_names_dict = new HashMap<>();
        List<String> rows_list = new ArrayList<>();

        int x = 0;
        //BigInteger short_names_unid = BigInteger.ZERO;
        long short_names_unid = 0;
        String long_name;
        for (int i = 0; i < newIds.length; ++i) {


            PeakList peakList = peakLists[newIds[i]];

            PeakListRow allRows[] = peakList.getRows();
            logger.info("Treating list " + peakList + " / NB rows: " + allRows.length);

            // Calculate scores for all possible alignments of this row
            for (int j = 0; j < allRows.length; ++j) {

                ////int x = (i * allRows.length) + j;

                PeakListRow row = allRows[j];

                // Each name HAS to be unique
                //short_names[x] = String.valueOf(short_names_unid); //"x" + short_names_id; //"[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
                short_names[x] = "" + short_names_unid; //"x" + short_names_id; //"[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
                
                // Produce human readable names
                RawDataFile ancestorRDF = DataFileUtils.getAncestorDataFile(project, peakList.getRawDataFile(0), true);
                // Avoid exception if ancestor RDFs have been removed...
                String suitableRdfName = (ancestorRDF == null) ? peakList.getRawDataFile(0).getName() : ancestorRDF.getName();
                long_name = "[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
                dendro_names_dict.put(short_names[x], long_name);
                //short_names_unid.add(BigInteger.ONE);
                short_names_unid++;
                
                row_names_dict.put(short_names[x], row);
                full_rows_list.add(row);
                rows_list.add(row.getBestPeak().getDataFile() + ", @" + row.getBestPeak().getRT());


                if (isCanceled())
                    return;

                // Calculate limits for a row with which the row can be aligned
                //            Range<Double> mzRange = mzTolerance.getToleranceRange(row
                //                    .getAverageMZ());
                //            Range<Double> rtRange = rtTolerance.getToleranceRange(row
                //                    .getAverageRT());
                // GLG HACK: Use best peak rather than average. No sure it is better... ???
                Range<Double> mzRange = mzTolerance.getToleranceRange(row
                        .getBestPeak().getMZ());
                Range<Double> rtRange = rtTolerance.getToleranceRange(row
                        .getBestPeak().getRT());

                // Get all rows of the aligned peaklist within parameter limits
                /*
            PeakListRow candidateRows[] = alignedPeakList
                    .getRowsInsideScanAndMZRange(rtRange, mzRange);
                 */
                //////List<PeakListRow> candidateRows = new ArrayList<>();
                int y = 0;
                for (int k = 0; k < newIds.length; ++k) {

                    if (isCanceled())
                        return;

                    PeakList k_peakList = peakLists[newIds[k]];
                    PeakListRow k_allRows[] = k_peakList.getRows();

                    //                  if (k != i) {
                    for (int l = 0; l < k_allRows.length; ++l) {

                        ////int y = (k * k_allRows.length) + l;


                        PeakListRow k_row = k_allRows[l];

                        double normalized_rt_dist = Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) / ((RangeUtils.rangeLength(rtRange) / 2.0));

                        
                        if (x == y) {
                            /** Row tested against himself => fill matrix diagonal with zeros */
                            distances[x][y] = 0.0d;
                            //continue; 
                        } else {
                            
                            if (k != i) {

                                // Is candidate
                                if (Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) < rtTolerance.getTolerance() / 2.0 
                                        && Math.abs(row.getBestPeak().getMZ() - k_row.getBestPeak().getMZ()) < mzTolerance.getMzTolerance() / 2.0) {
                                    //////candidateRows.add(k_row);

                                    /** Row is candidate => Distance is the score */
                                    RowVsRowScoreGC score;
//                                    
//                                    if (newIds[i] == 0) {
//                                        missing1
//                                        score = new RowVsRowScoreGC(
//                                                this.project, useOldestRDFAncestor,
//                                                k_row.getRawDataFiles()[0], rtAdjustementMapping,
//                                                k_row, row,
//                                                RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
//                                                RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
//                                                idWeight,
//                                                useApex, useKnownCompoundsAsRef, 
//                                                useDetectedMzOnly,
//                                                rtToleranceAfter);
//                                    } else {
                                        
                                        score = new RowVsRowScoreGC(
                                                this.project, useOldestRDFAncestor,
                                                /*row.getRawDataFiles()[0],*/ rtAdjustementMapping,
                                                row, k_row,
                                                RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
                                                RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
                                                idWeight,
                                                useApex, useKnownCompoundsAsRef, 
                                                useDetectedMzOnly,
                                                rtToleranceAfter);
                                        
//                                        if (Math.abs(score.getScore() - 1.0d) < EPSILON) {
//                                            System.out.println("Found quite a heavy chem. sim. between: " + row.getBestPeak() + " and " + k_row.getBestPeak());
//                                        }
                                        
//                                    }
                                    
                                    //-
                                    // If match was not rejected afterwards and score is acceptable
                                    // (Acceptable score => higher than absolute min ever and higher than user defined min)
                                    // 0.0 is OK for a minimum score only in "Dot Product" method (Not with "Person Correlation")
                                    //if (score.getScore() > Jmissing1DXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE)
                                    if (score.getScore() > Math.max(JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE, minScore)) {
                                        //////scoreSet.add(score);
                                        // The higher the score, the lower the distance!
                                        distances[x][y] = maximumScore - score.getScore();//(mzWeight + rtWeight) - score.getScore();
                                    } else {
                                        /** Score too low => Distance is Infinity */
                                        //distances[x][y] = veryLongDistance * (1.0d + normalized_rt_dist); // Need to rank distances for "rejected" cases
                                        ////distances[x][y] = veryLongDistance; // Need to rank distances for "rejected" cases
                                        //////distances[x][y] = Double.MAX_VALUE;
                                        distances[x][y] = veryLongDistance;
                                    }

                                } else {
                                    /** Row is not candidate => Distance is Infinity */
                                    ////distances[x][y] = 10.0d * veryLongDistance * (1.0d + normalized_rt_dist); // Need to rank distances for "rejected" cases
                                    //distances[x][y] = veryLongDistance; // Need to rank distances for "rejected" cases
                                    //distances[x][y] = Double.MAX_VALUE;//10.0d * veryLongDistance; // Need to rank distances for "rejected" cases
                                    //////distances[x][y] = Double.MAX_VALUE;
                                    distances[x][y] = 10.0d * veryLongDistance;
                                }
                            } else {
                                /** Both rows belong to same list => Distance is Infinity */
                                ////distances[x][y] = 100.0d * veryLongDistance * (1.0d + normalized_rt_dist); // Need to rank distances for "rejected" cases
                                //distances[x][y] = Double.MAX_VALUE;//100.0d * veryLongDistance; // Need to rank distances for "rejected" cases
                                //////distances[x][y] = Double.MAX_VALUE;
                                distances[x][y] = 100.0d * veryLongDistance;
                            }
                        }
                        
//                        // 28.499
//                        if (row.getBestPeak().getRT() > 28.499d && row.getBestPeak().getRT() < 28.500d) {
//                            System.out.println("28.499 scored: " + row.getBestPeak().getRT() + " | " + k_row.getBestPeak().getRT());
//                            System.out.println("\t => " + distances[x][y]);
//                        }

//                        if (x == y) {
//                            /** Row tested against himself => fill matrix diagonal with zeros */
//                            distances[x][y] = 0.0d;
//                            //continue; 
//                        } else {
//
//                            if (k != i) {
//                                
//                                RowVsRowScoreGC score = new RowVsRowScoreGC(
//                                        this.project, useOldestRDFAncestor,
//                                        /*row.getRawDataFiles()[0],*/ rtAdjustementMapping,
//                                        row, k_row,
//                                        RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
//                                        RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
//                                        idWeight,
//                                        useApex, useKnownCompoundsAsRef, 
//                                        useDetectedMzOnly,
//                                        rtToleranceAfter);
//                                
//                                distances[x][y] = maximumScore - score.getScore();
//                                
//                            } else {
//                                
//                                  /** Both rows belong to same list => Distance is Infinity */
//                                  distances[x][y] = 5.0d * veryLongDistance; // Need to rank distances for "rejected" cases
//                            }
//                        }missing1
//                        
//                        // 28.499
//                        if (row.getBestPeak().getRT() > 28.499d && row.getBestPeak().getRT() < 28.500d) {
//                            System.out.println("28.499 scored: " + row.getBestPeak().getRT() + " | " + k_row.getBestPeak().getRT());
//                            System.out.println("\t => " + distances[x][y]);
//                        }

                        
                        ++y;
                    }
                    //                  } else {
                    //                      /** Both rows belong to same list => Distance is Infinity */
                    //                      distances[x][y] = Double.MAX_VALUE;
                    //                  }
                }

                processedRows++;

                ++x;
            }

        }
        
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
                for (int i = 0; i < distances.length; i++) {

                    if (i == 0)
                        for (int j = 0; j < distances[i].length; j++) {
                            writer.write(short_names[j] + "\t");
                        }

                    for (int j = 0; j < distances[i].length; j++) {
                        if (j == 0)
                            writer.write(short_names[i] + "\t");
                        writer.write(rtFormat.format(distances[i][j]) + "\t");
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
        
        
        // OLD (RAM killer) way!
        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {    

        	ClusteringAlgorithm alg = new DefaultClusteringAlgorithmWithProgress(); //DefaultClusteringAlgorithm();        
        	clust = alg.performClustering(distances, short_names, linkageStrategy);

        	System.out.println("Done clustering");

        	if (isCanceled())
        		return;

         	List<Cluster> validatedClusters = getValidatedClusters_0(clust, newIds.length, max_dist);
        }
        // WEKA way! (And more: Mostly HDBSCAN*/ELKI, see 'CLUSTERER_TYPE==5')
        else {


        	long startTime2, endTime2;
        	float ms2;
        	
        	String newickCluster;
        		
        	if (CLUSTERER_TYPE == 0) {
        		
        		// WEKA hierarchical clustering
        		//HierarClusterer hierarClusterer = new HierarClusterer(clustProgress, distances);
//        		for (int i = 0; i < distances.length; i++) {
//        			for (int j = i; j < distances[0].length; j++) {
//        				
//        				distancesMap.put(new Pair<Integer, Integer>(i, j), (float) distances[i][j]);
//        			}
//        		}
        		
        		numInstances = distances.length;
        		HierarClusterer hierarClusterer;
        		if (USE_DOUBLE_PRECISION_FOR_DIST) {
        			distancesAsVector = symmetricMatrixToVector(distances);
        			hierarClusterer = new HierarClusterer(clustProgress, distancesAsVector, numInstances, this.minScore);
        		} else {
        			distancesAsFloatVector = symmetricMatrixToFloatVector(distances);
        			hierarClusterer = new HierarClusterer(clustProgress, distancesAsFloatVector, numInstances, this.minScore);
        		}
				// Get rid of distances, since we already transmitted it as vector to the clusterer!
				distances = null;
				System.gc();
				/**
				HierarClusterer hierarClusterer = new HierarClusterer(clustProgress, distances);
				*/
				
        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER CREATED");

        		//
        		startTime2 = System.currentTimeMillis();
        		//
        		clusteringResult = hierarClusterer.performClustering(linkageStartegyType_12);
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


        		// 1st parsing method
        		if (JoinAlignerGCParameters.CLUST_METHOD >= 1) {

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


        			//void setup() {
        			BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
        			//      BufferedReader r = null;
        			//		try {
        			//			r = new BufferedReader(new StringReader(hierarClusterer.getClusterer().graph()));
        			//		} catch (Exception e) {
        			//			// TODO Auto-generated catch block
        			//			e.printStUSE_HIERARackTrace();
        			//		}
        			//        BufferedReader r = null;
        			//		try {
        			//			r = new BufferedReader(new FileReader(nwk_ok));
        			//		} catch (FileNotFoundException e) {
        			//			// TODO Auto-generated catch block
        			//			e.printStackTrace();
        			//		}
        			if (JoinAlignerGCParameters.CLUST_METHOD == 1) {

        				TreeParser tp = new TreeParser(r);
        				tree_1 = tp.tokenize(1, "tree", null);
        				int tree_height = tree_1.getHeight();
        				System.out.println("# Largest tree height is: " + tree_height);
        				//
        				if (DEBUG)
        					System.out.println("####" + recursive_print(tree_1, 0, 0));

        				// 2nd parsing method
        			} else if (JoinAlignerGCParameters.CLUST_METHOD == 2) {

        				// Parse Newick formatted string
        				BufferedReader r2 = new BufferedReader(new StringReader(newickCluster_clean));

        				NewickImporter importer = new NewickImporter(r2, true);

        				try {
        					while (importer.hasTree()) {
        						tree_2 = importer.importNextTree();
        					}
        				} catch (IOException | ImportException e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}

        			}

        		}
        	
        	// NOT "HIERAR"!
        	} 
//        	else if (CLUSTERER_TYPE == 1) {
//    			
//        		// WEKA k-means clustering
//        		KMeansClusterer kMeansClusterer = new KMeansClusterer(clustProgress, distances);
////        		public static final int RANDOM = 0;
////        		public static final int KMEANS_PLUS_PLUS = 1;
////        		public static final int CANOPY = 2;
////        		public static final int FARTHEST_FIRST = 3;
//
//
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER CREATED");
//
//        		//
//        		startTime2 = System.currentTimeMillis();
//        		//
//        		clusteringResult = kMeansClusterer.performClustering(/*linkageStartegyType_12*/);
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER PERFORMED");
//        		//
//        		endTime2 = System.currentTimeMillis();
//        		ms2 = (endTime2 - startTime2);
//        		System.out.println("Done clustering: " + clusteringResult);
//
//        		System.out.println("Done clustering: " + clusteringResult + "[took: " + ms2 + "ms. to build tree]");
//
//
//        		//
//        		System.out.println("RESULT 1: " + kMeansClusterer.getClusterer().toString());
//        		System.out.println("RESULT 2: " + kMeansClusterer.getClusterer().getNumClusters());
//        		
//        		
//        		
//        		if (isCanceled())
//        			return;
//
//
//        		// Getting the result of the clustering in Newick format
//        		newickCluster = clusteringResult.getHierarchicalCluster();
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER NEWICKED");
//
//
//        		//////
//
//
//        		// 1st parsing method
//        		if (JoinAlignerGCParameters.CLUST_METHOD >= 1) {
//
//        			int current_depth = 0;
//
//        			// Make string really Newick standard
//        			String line_sep = "\n";//System.getProperty("line.separator");
//        			//	        	System.out.println("Line sep = '" + line_sep + "'.");
//        			//	        	System.out.println("Line sep found at index '" + newickCluster.indexOf(line_sep) + "'.");
//        			newickCluster_clean = newickCluster.substring(newickCluster.indexOf(line_sep) + line_sep.length()) + ";";
//
//        			//	        	PrintWriter out;
//        			//	        	try {
//        			//	        		out = new PrintWriter("newick_check.txt");
//        			//	        		
//        			//        			String output_str = newickCluster_clean ;
//        			//        			
//        			//        			for (String short_n : dendro_names_dict.keySet()) {
//        			//        				String short_nn = HierarClusterer.NEWICK_LEAF_NAME_PREFIX + short_n;
//        			//        				String long_nn = dendro_names_dict.get(short_n).replaceAll(", ", "_");
//        			//        				output_str = output_str.replaceAll(short_nn + ":", long_nn + ":");
//        			//        			}
//        			//	        		
//        			//	        		out.println(output_str);
//        			//	        		
//        			//	        		out.close();
//        			//	        	} catch (FileNotFoundException e) {
//        			//	        		// TODO Auto-generated catch block
//        			//	        		e.printStackTrace();
//        			//	        	}
//
//
//        			//void setup() {
//        			BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
//        			//      BufferedReader r = null;
//        			//		try {
//        			//			r = new BufferedReader(new StringReader(hierarClusterer.getClusterer().graph()));
//        			//		} catch (Exception e) {
//        			//			// TODO Auto-generated catch block
//        			//			e.printStUSE_HIERARackTrace();
//        			//		}
//        			//        BufferedReader r = null;
//        			//		try {
//        			//			r = new BufferedReader(new FileReader(nwk_ok));
//        			//		} catch (FileNotFoundException e) {
//        			//			// TODO Auto-generated catch block
//        			//			e.printStackTrace();
//        			//		}
//        			if (JoinAlignerGCParameters.CLUST_METHOD == 1) {
//
//        				TreeParser tp = new TreeParser(r);
//        				tree_1 = tp.tokenize(1, "tree", null);
//        				int tree_height = tree_1.getHeight();
//        				System.out.println("# Largest tree height is: " + tree_height);
//        				//
//        				if (DEBUG)
//        					System.out.println("####" + recursive_print(tree_1, 0, 0));
//
//        				// 2nd parsing method
//        			} else if (JoinAlignerGCParameters.CLUST_METHOD == 2) {
//
//        				// Parse Newick formatted string
//        				BufferedReader r2 = new BufferedReader(new StringReader(newickCluster_clean));
//
//        				NewickImporter importer = new NewickImporter(r2, true);
//
//        				try {
//        					while (importer.hasTree()) {
//        						tree_2 = importer.importNextTree();
//        					}
//        				} catch (IOException | ImportException e) {
//        					// TODO Auto-generated catch block
//        					e.printStackTrace();
//        				}
//
//        			}
//
//        		} 
//
//    		} else if (CLUSTERER_TYPE == 2) {
//    			
//        		// WEKA k-means clustering
//        		XMeansClusterer xMeansClusterer = new XMeansClusterer(clustProgress, distances);
////        		public static final int RANDOM = 0;
////        		public static final int KMEANS_PLUS_PLUS = 1;
////        		public static final int CANOPY = 2;
////        		public static final int FARTHEST_FIRST = 3;
//
//
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER CREATED");
//
//        		//
//        		startTime2 = System.currentTimeMillis();
//        		//
//        		clusteringResult = xMeansClusterer.performClustering(/*linkageStartegyType_12*/);
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER PERFORMED");
//        		//
//        		endTime2 = System.currentTimeMillis();
//        		ms2 = (endTime2 - startTime2);
//        		System.out.println("Done clustering: " + clusteringResult);
//
//        		System.out.println("Done clustering: " + clusteringResult + "[took: " + ms2 + "ms. to build tree]");
//
//
//        		//
//        		System.out.println("RESULT 1: " + xMeansClusterer.getClusterer().toString());
//        		System.out.println("RESULT 2: " + xMeansClusterer.getClusterer().numberOfClusters());
//        		
//        		
//        		
//        		if (isCanceled())
//        			return;
//
//
//        		// Getting the result of the clustering in Newick format
//        		newickCluster = clusteringResult.getHierarchicalCluster();
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER NEWICKED");
//
//
//        		//////
//
//
//        		// 1st parsing method
//        		if (JoinAlignerGCParameters.CLUST_METHOD >= 1) {
//
//        			int current_depth = 0;
//
//        			// Make string really Newick standard
//        			String line_sep = "\n";//System.getProperty("line.separator");
//        			//	        	System.out.println("Line sep = '" + line_sep + "'.");
//        			//	        	System.out.println("Line sep found at index '" + newickCluster.indexOf(line_sep) + "'.");
//        			newickCluster_clean = newickCluster.substring(newickCluster.indexOf(line_sep) + line_sep.length()) + ";";
//
//        			//	        	PrintWriter out;
//        			//	        	try {
//        			//	        		out = new PrintWriter("newick_check.txt");
//        			//	        		
//        			//        			String output_str = newickCluster_clean ;
//        			//        			
//        			//        			for (String short_n : dendro_names_dict.keySet()) {
//        			//        				String short_nn = HierarClusterer.NEWICK_LEAF_NAME_PREFIX + short_n;
//        			//        				String long_nn = dendro_names_dict.get(short_n).replaceAll(", ", "_");
//        			//        				output_str = output_str.replaceAll(short_nn + ":", long_nn + ":");
//        			//        			}
//        			//	        		
//        			//	        		out.println(output_str);
//        			//	        		
//        			//	        		out.close();
//        			//	        	} catch (FileNotFoundException e) {
//        			//	        		// TODO Auto-generated catch block
//        			//	        		e.printStackTrace();
//        			//	        	}
//
//
//        			//void setup() {
//        			BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
//        			//      BufferedReader r = null;
//        			//		try {
//        			//			r = new BufferedReader(new StringReader(hierarClusterer.getClusterer().graph()));
//        			//		} catch (Exception e) {
//        			//			// TODO Auto-generated catch block
//        			//			e.printStUSE_HIERARackTrace();
//        			//		}
//        			//        BufferedReader r = null;
//        			//		try {
//        			//			r = new BufferedReader(new FileReader(nwk_ok));
//        			//		} catch (FileNotFoundException e) {
//        			//			// TODO Auto-generated catch block
//        			//			e.printStackTrace();
//        			//		}
//        			if (JoinAlignerGCParameters.CLUST_METHOD == 1) {
//
//        				TreeParser tp = new TreeParser(r);
//        				tree_1 = tp.tokenize(1, "tree", null);
//        				int tree_height = tree_1.getHeight();
//        				System.out.println("# Largest tree height is: " + tree_height);
//        				//
//        				if (DEBUG)
//        					System.out.println("####" + recursive_print(tree_1, 0, 0));
//
//        				// 2nd parsing method
//        			} else if (JoinAlignerGCParameters.CLUST_METHOD == 2) {
//
//        				// Parse Newick formatted string
//        				BufferedReader r2 = new BufferedReader(new StringReader(newickCluster_clean));
//
//        				NewickImporter importer = new NewickImporter(r2, true);
//
//        				try {
//        					while (importer.hasTree()) {
//        						tree_2 = importer.importNextTree();
//        					}
//        				} catch (IOException | ImportException e) {
//        					// TODO Auto-generated catch block
//        					e.printStackTrace();
//        				}
//
//        			}
//
//        		} 
//
//    		} else if (CLUSTERER_TYPE == 3) {
//
//        		// WEKA Cobweb clustering
//        		CobwebClusterer cobwebClusterer = new CobwebClusterer(clustProgress, distances);
//
//
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER CREATED");
//
//        		//
//        		startTime2 = System.currentTimeMillis();
//        		//
//        		clusteringResult = cobwebClusterer.performClustering(/*linkageStartegyType_12*/);
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER PERFORMED");
//        		//
//        		endTime2 = System.currentTimeMillis();
//        		ms2 = (endTime2 - startTime2);
//        		System.out.println("Done clustering: " + clusteringResult);
//
//        		System.out.println("Done clustering: " + clusteringResult + "[took: " + ms2 + "ms. to build tree]");
//
//
//        		//
//        		System.out.println("RESULT 1: " + cobwebClusterer.getClusterer().toString());
//        		System.out.println("RESULT 2: " + cobwebClusterer.getClusterer().numberOfClusters());
//        		
//        		
//        		
//        		if (isCanceled())
//        			return;
//
//
//        		// Getting the result of the clustering in Newick format
//        		newickCluster = clusteringResult.getHierarchicalCluster();
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER NEWICKED");
//
//
//        		//////
//
//
//        		// 1st parsing method
//        		if (JoinAlignerGCParameters.CLUST_METHOD >= 1) {
//
//        			int current_depth = 0;
//
//        			// Make string really Newick standard
//        			String line_sep = "\n";//System.getProperty("line.separator");
//        			//	        	System.out.println("Line sep = '" + line_sep + "'.");
//        			//	        	System.out.println("Line sep found at index '" + newickCluster.indexOf(line_sep) + "'.");
//        			newickCluster_clean = newickCluster.substring(newickCluster.indexOf(line_sep) + line_sep.length()) + ";";
//
//        			//	        	PrintWriter out;
//        			//	        	try {
//        			//	        		out = new PrintWriter("newick_check.txt");
//        			//	        		
//        			//        			String output_str = newickCluster_clean ;
//        			//        			
//        			//        			for (String short_n : dendro_names_dict.keySet()) {
//        			//        				String short_nn = HierarClusterer.NEWICK_LEAF_NAME_PREFIX + short_n;
//        			//        				String long_nn = dendro_names_dict.get(short_n).replaceAll(", ", "_");
//        			//        				output_str = output_str.replaceAll(short_nn + ":", long_nn + ":");
//        			//        			}
//        			//	        		
//        			//	        		out.println(output_str);
//        			//	        		
//        			//	        		out.close();
//        			//	        	} catch (FileNotFoundException e) {
//        			//	        		// TODO Auto-generated catch block
//        			//	        		e.printStackTrace();
//        			//	        	}
//
//
//        			//void setup() {
//        			BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
//        			//      BufferedReader r = null;
//        			//		try {
//        			//			r = new BufferedReader(new StringReader(hierarClusterer.getClusterer().graph()));
//        			//		} catch (Exception e) {
//        			//			// TODO Auto-generated catch block
//        			//			e.printStUSE_HIERARackTrace();
//        			//		}
//        			//        BufferedReader r = null;
//        			//		try {
//        			//			r = new BufferedReader(new FileReader(nwk_ok));
//        			//		} catch (FileNotFoundException e) {
//        			//			// TODO Auto-generated catch block
//        			//			e.printStackTrace();
//        			//		}
//        			if (JoinAlignerGCParameters.CLUST_METHOD == 1) {
//
//        				TreeParser tp = new TreeParser(r);
//        				tree_1 = tp.tokenize(1, "tree", null);
//        				int tree_height = tree_1.getHeight();
//        				System.out.println("# Largest tree height is: " + tree_height);
//        				//
//        				if (DEBUG)
//        					System.out.println("####" + recursive_print(tree_1, 0, 0));
//
//        				// 2nd parsing method
//        			} else if (JoinAlignerGCParameters.CLUST_METHOD == 2) {
//
//        				// Parse Newick formatted string
//        				BufferedReader r2 = new BufferedReader(new StringReader(newickCluster_clean));
//
//        				NewickImporter importer = new NewickImporter(r2, true);
//
//        				try {
//        					while (importer.hasTree()) {
//        						tree_2 = importer.importNextTree();
//        					}
//        				} catch (IOException | ImportException e) {
//        					// TODO Auto-generated catch block
//        					e.printStackTrace();
//        				}
//
//        			}
//
//        		} 
//
//    		} 
    	// NOT "HIERAR"! (OPTICS/DBSCAN)
        // !!! NEED Weka 3.6.13 to have this work !!! (see pom.xml)
        else if (CLUSTERER_TYPE == 4) {
    			
//        		// WEKA DBSCAN clustering
//        		//OPTICSClusterer opticsClusterer = new OPTICSClusterer(clustProgress, distances);
//	        	DBSCANClusterer dbscanClusterer = new DBSCANClusterer(clustProgress, distances);
//
//        		printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: created");
//
//        		//
//        		startTime2 = System.currentTimeMillis();
//        		//
//        		//***clusteringResult = opticsClusterer.performClustering(/*linkageStartegyType_12*/);
//        		clusteringResult = dbscanClusterer.performClustering(/*linkageStartegyType_12*/);
//        		//            
//        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER: performed");
//        		//
//        		endTime2 = System.currentTimeMillis();
//        		ms2 = (endTime2 - startTime2);
//        		System.out.println("Done clustering: " + clusteringResult);
//
//        		System.out.println("Done clustering: " + clusteringResult + "[took: " + ms2 + "ms. to build tree]");
//
//
//        		//
//        		System.out.println("RESULT 1: " + dbscanClusterer.getClusterer().toString());
//        		try {
//					//***System.out.println("RESULT 2: " + hdbscanClusterer.getClusterer().numberOfClusters());
//        			// ...
//				} catch (Exception e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//        		
//        		
//        		
//        		if (isCanceled())
//        			return;

    		}
    	// SEMI "HIERAR"! "HDBSCAN*/ELKI"
        else if (CLUSTERER_TYPE == 5) {
    			
        		// HDBSCAN* clustering
	        	HDBSCANClusterer hdbscanClusterer = new HDBSCANClusterer(clustProgress, distances);

        		printMemoryUsage(run_time, prevTotal, prevFree, "HDBSCAN CLUSTERER: created");

        		//
        		startTime2 = System.currentTimeMillis();
        		//
        		//***clusteringResult = opticsClusterer.performClustering(/*linkageStartegyType_12*/);
        		clusteringResult = hdbscanClusterer.performClustering(/*linkageStartegyType_12*/);
        		//            
        		printMemoryUsage(run_time, prevTotal, prevFree, "WEKA CLUSTERER: performed");
        		//
        		endTime2 = System.currentTimeMillis();
        		ms2 = (endTime2 - startTime2);
        		System.out.println("Done clustering: " + clusteringResult);

        		System.out.println("Done clustering: " + clusteringResult + "[took: " + ms2 + "ms. to build tree]");
        		
        		
        		if (isCanceled())
        			return;


        		// Getting the result of the clustering
        		clustersHDBSCAN = hdbscanClusterer.getResultingClusters();
        		// TODO: !!!!!!
        		// ...
        		// "HDBSCAN*" produces 3 output files "_hierarchy.csv, _tree.csv, ...": need to build a parser able to 
        		// recover proper clusters from those results...
        		
    		}

        }
            
        ////// Arrange row clustered list with method 0,1,2
    	List<List<PeakListRow>> clustersList = new ArrayList<>();
    	//
    	int finalNbPeaks = 0;
    	Set<String> leaf_names = new HashSet<>();
       
        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
        	//List<Cluster> validatedClusters = getValidatedClusters(clusteringResult, newIds.length, max_dist);
        	//...
 

        	for (Cluster c: validatedClusters_0) {

        		if (isCanceled())
        			return;

        		List<Cluster> c_leafs = getClusterLeafs_0(c);
        		List<PeakListRow> rows_cluster = new ArrayList<>();
        		RawDataFile rdf = null;
        		for (Cluster l: c_leafs) {
        			// Recover related PeakListRow
        			rows_cluster.add(row_names_dict.get(l.getName()));
        			leaf_names.add(l.getName());

        		}
        		clustersList.add(rows_cluster);
        		finalNbPeaks += rows_cluster.size();
        	}
        }
        else if (JoinAlignerGCParameters.CLUST_METHOD == 1) {
        	
        	tot = 0;
        	tot2 = 0;
	            
        	List<Integer> validatedClusters;
        	if (USE_DOUBLE_PRECISION_FOR_DIST) {
	        	validatedClusters = getValidatedClusters_11(clusteringResult, newIds.length, max_dist, distancesAsVector, numInstances);
	        	//List<Integer> validatedClusters = getValidatedClusters_1(clusteringResult, newIds.length, max_dist, distances);
        	} else {
	        	validatedClusters = getValidatedClusters_11(clusteringResult, newIds.length, max_dist, distancesAsFloatVector, numInstances);
        	}
        	
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
        		
        		processedRows += rows_cluster.size();
        	}

        } else if (JoinAlignerGCParameters.CLUST_METHOD == 2) {
        	
        	tot = 0;
        	tot2 = 0;
        	
        	List<Node> validatedClusters = getValidatedClusters_2(clusteringResult, newIds.length, max_dist);

        	int nbLeaves = 0;
        	for (Node node: validatedClusters) {
        		nbLeaves += getClusterLeafs_2((RootedTree) tree_2, node).size();
        	}
        	System.out.println("NbLeaves = " + nbLeaves);
        	System.out.println("tot = " + tot + " | tot2 = " + tot2);
        	//            if (nbLeaves != 752)
        	//            	return;


        	List<String> leaf_names_flat = new ArrayList<>();
        	List<Integer> leaf_names_keys = new ArrayList<>();
        	for (Node node: validatedClusters) {

        		if (isCanceled())
        			return;


        		List<Node> leafs_nodes = getClusterLeafs_2((RootedTree) tree_2, node);
        		List<PeakListRow> rows_cluster = new ArrayList<>();
        		RawDataFile rdf = null;
        		for (Node leaf: leafs_nodes) {

        			// Recover related PeakListRow
        			int leaf_id = Integer.valueOf(leaf.getAttribute("name").toString().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
        			rows_cluster.add(full_rows_list.get(leaf_id));
        			leaf_names.add(leaf.getAttribute("name").toString());

        		}
        		clustersList.add(rows_cluster);
        		finalNbPeaks += rows_cluster.size();
        	}
        } else if (JoinAlignerGCParameters.CLUST_METHOD == 3) {
        	
        	// TODO: !!!!!!
        	// ...
        	for (ca.ualberta.cs.hdbscanstar.Cluster hdbscan_clust: clustersHDBSCAN) {
        		//...
        	}
        }


        //        // Make clusters from remaining singles
        //        for (PeakListRow single_row: singleRowsList) {
        //            
        //            clustersList.add(new ArrayList<PeakListRow>(Arrays.asList(new PeakListRow[] { single_row })));
        //        }


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
                    double score = rowIDsScores.get(rdf);

                    strIdentities += id.getName() + AlignedRowProps.PROP_SEP;
                    strScores += score + AlignedRowProps.PROP_SEP;


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
                        Object[] infos = { score, 1 };
                        scoreQuantMapping.put(id.getName(), infos);
                    } else {
                        Object[] infos = scoreQuantMapping.get(id.getName());
                        infos[0] = score + (double) infos[0];
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


            // Notify MZmine about the change in the project, necessary ???
            MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
            project.notifyObjectChanged(targetRow, false);
        }





        // Add new aligned peak list to the project
        project.addPeakList(alignedPeakList);

        // Add task description to peakList
        alignedPeakList
        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                JoinAlignerGCTask.TASK_NAME, parameters));

        logger.info("Finished join aligner");
        setStatus(TaskStatus.FINISHED);


//        if (DEBUG) {
        	int finalNbPeaks2 = 0;

        	for (PeakListRow plr: alignedPeakList.getRows()) {
        		finalNbPeaks2 += plr.getNumberOfPeaks();

        		for (int i=0; i <  plr.getNumberOfPeaks(); i++) {

        			Feature peak = plr.getPeaks()[i];

        			String str = peak.getDataFile() + ", @" + peak.getRT();

        			//if (!rows_list.contains(str))
        			//logger.info("# MISSING Peak: " + str);
        			rows_list.remove(str);
        			//                else
        			//                    logger.info("> OK Peak: " + str);
        		}
        	}
        	for (String str: rows_list)
        		logger.info("# MISSING Peak: " + str);

        	logger.info("Nb peaks ...: " + finalNbPeaks_0 + " / " + leaf_names.size() + " | " + nbAddedRows + " / " + alignedPeakList.getNumberOfRows());
        	//logger.info("Nb peaks ...: bip = " + bip);
        	logger.info("Nb peaks treated: " + short_names.length + " | " + full_rows_list.size() + " | " + row_names_dict.size() + " | " + finalNbPeaks + " | " + nbAddedPeaks + " | " + finalNbPeaks2 + " | >>> " + nbUniquePeaks);

        	if (JoinAlignerGCParameters.CLUST_METHOD == 0)
        		logger.info("Nb peaks treated - suite: " + leaf_names.size() + " | " + clust.countLeafs() + " | " + full_rows_list.size()); 
        	else if (JoinAlignerGCParameters.CLUST_METHOD == 1)
        		logger.info("Nb peaks treated - suite: " + leaf_names.size() + " | " + tree_1.getLeafCount() + " | " + full_rows_list.size());                    
        	else if (JoinAlignerGCParameters.CLUST_METHOD == 1)
        		logger.info("Nb peaks treated - suite: " + leaf_names.size() + " | " + getClusterLeafs_2((RootedTree) tree_2, ((RootedTree) tree_2).getRootNode()) + " | " + full_rows_list.size());                    

//        }


        //
        endTime = System.currentTimeMillis();
        ms = (endTime - startTime);
        logger.info("## >> Whole JoinAlignerGCTask processing took " + Float.toString(ms) + " ms.");
        
        
        //System.out.println("globalInfo: " + hierarClusterer.getClusterer().globalInfo());

        //----------------------------------------------------------------------

        if (JoinAlignerGCParameters.CLUST_METHOD == 0) {
        	try {
        		if (exportDendrogramAsPng && dendrogramPngFilename != null) {          
        			saveDendrogramAsPng(clust, dendrogramPngFilename, dendro_names_dict);
        		} else {

        		}
        	} catch (Exception e) {
        		logger.info("! saveDendrogramAsPng failed...");
        		e.printStackTrace();
        	}
        	try {
        		if (exportDendrogramAsTxt && dendrogramTxtFilename != null) {          
        			saveDendrogramAsTxt(clust, dendrogramTxtFilename, dendro_names_dict);
        		}
        	} catch (Exception e) {
        		logger.info("! saveDendrogramAsTxt failed...");
        		e.printStackTrace();
        	}

        } else {

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
        			
	        		if (USE_EXPLICIT_NAMES) {
	        			for (String short_n : dendro_names_dict.keySet()) {
	        				String short_nn = HierarClusterer.NEWICK_LEAF_NAME_PREFIX + short_n;
	        				String long_nn = dendro_names_dict.get(short_n).replaceAll(", ", "_");
	        				output_str = output_str.replaceAll(short_nn + ":", ((SHOW_NODE_KEY) ? short_nn + "-" : "") + long_nn + ":");
	        			}
	        		}
	        		
	        		out.println(output_str);
	        		
	        		out.close();
	        	} catch (FileNotFoundException e) {
	        		// TODO Auto-generated catch block
	        		e.printStackTrace();
	        	}
        	}

        }

        //----------------------------------------------------------------------


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
    private List<Cluster> getValidatedClusters_0(Cluster clust, int level, double max_dist) {

        List<Cluster> validatedClusters = new ArrayList<>();
        if (!clust.isLeaf()) {

            for (Cluster child : clust.getChildren())
            {
                // Trick to get number of leafs without browsing the whole tree (unlike how it's done in ".countLeafs()")
                //      => Weight gives the number of leafs
                //if (child.countLeafs() <= level) {
                if (child.getWeightValue() < level + EPSILON && child.getDistanceValue() < max_dist + EPSILON) {
                    validatedClusters.add(child);
                    //if (child.isLeaf()) {
                    //    System.out.println(">>> Found shity leaf: '" + child.getName() + "' => " + child.getParent().getDistanceValue() + " | " + child.getParent().getTotalDistance());
                    //}
                } else {
                    validatedClusters.addAll(getValidatedClusters_0(child, level, max_dist));
                }
            }
        }

        return validatedClusters;
    }
    //
    private List<Integer> getValidatedClusters_1(ClusteringResult clusteringResult, int level, double max_dist, double[][] distMtx) {

    	List<Cluster> validatedClusters = new ArrayList<>();

    	String newickCluster = clusteringResult.getHierarchicalCluster();
    	// Make string really Newick standard
    	//String newickCluster_clean = newickCluster.substring(newickCluster.indexOf(System.getProperty("line.separator"))+1) + ";";
    	String newickCluster_clean = newickCluster.substring(newickCluster.indexOf("\n")+1) + ";";

    	// Parse Newick formatted string
        BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
		TreeParser tp = new TreeParser(r);
        Tree tree = tp.tokenize(1, "tree", null);

        
//        // clusteringResult.
//        if (!clust.isLeaf()) {
//            for (Cluster child : clust.getChildren())
//            {
//                // Trick to get number of leafs without browsing the whole tree (unlike how it's done in ".countLeafs()")
//                //      => Weight gives the number of leafs
//                //if (child.countLeafs() <= level) {
//                if (child.getWeightValue() < level + EPSILON && child.getDistanceValue() < max_dist + EPSILON) {
//                    validatedClusters.add(child);
//                    //if (child.isLeaf()) {
//                    //    System.out.println(">>> Found shity leaf: '" + child.getName() + "' => " + child.getParent().getDistanceValue() + " | " + child.getParent().getTotalDistance());
//                    //}
//                } else {
//                    validatedClusters.addAll(getValidatedClusters(child, level, max_dist));
//                }
//            }
//        }
//
//        return validatedClusters;
        
        return recursive_validate_clusters_1(tree, level, max_dist, 0, 0, distMtx);
	}
    private List<Integer> getValidatedClusters_11(ClusteringResult clusteringResult, int level, double max_dist, double[] distVect, int dim) {

    	List<Cluster> validatedClusters = new ArrayList<>();

    	String newickCluster = clusteringResult.getHierarchicalCluster();
    	// Make string really Newick standard
    	//String newickCluster_clean = newickCluster.substring(newickCluster.indexOf(System.getProperty("line.separator"))+1) + ";";
    	String newickCluster_clean = newickCluster.substring(newickCluster.indexOf("\n")+1) + ";";

    	// Parse Newick formatted string
        BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
		TreeParser tp = new TreeParser(r);
        Tree tree = tp.tokenize(1, "tree", null);
        
        return recursive_validate_clusters_11(tree, level, max_dist, 0, 0, distVect, dim);
	}
    private List<Integer> getValidatedClusters_11(ClusteringResult clusteringResult, int level, double max_dist, float[] distVect, int dim) {

    	List<Cluster> validatedClusters = new ArrayList<>();

    	String newickCluster = clusteringResult.getHierarchicalCluster();
    	// Make string really Newick standard
    	//String newickCluster_clean = newickCluster.substring(newickCluster.indexOf(System.getProperty("line.separator"))+1) + ";";
    	String newickCluster_clean = newickCluster.substring(newickCluster.indexOf("\n")+1) + ";";

    	// Parse Newick formatted string
        BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));//createReader("treeoflife.tree");
		TreeParser tp = new TreeParser(r);
        Tree tree = tp.tokenize(1, "tree", null);
        
        return recursive_validate_clusters_11(tree, level, max_dist, 0, 0, distVect, dim);
	}

    static int tot = 0;
    static int tot2 = 0;
    List<Integer> recursive_validate_clusters_11(Tree tree, int level, double max_dist, int currkey, int currdepth, double[] distVect, int dim) {
    	
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
    				
    				/** Leafs must all be an acceptable distance from each other for node to be considered a cluster! */
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
		    				double dist = getValueFromVector(left_leaf_id, right_leaf_id, dim, distVect);//distMtx[left_leaf_id][right_leaf_id];
		    				
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
    				validatedClusters.addAll(recursive_validate_clusters_11(tree, level, max_dist, child.key, currdepth + 1, distVect, dim));
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
    //-
    List<Integer> recursive_validate_clusters_11(Tree tree, int level, double max_dist, int currkey, int currdepth, float[] distVect, int dim) {
    	
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
    				
    				/** Leafs must all be an acceptable distance from each other for node to be considered a cluster! */
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
		    				double dist = getValueFromVector(left_leaf_id, right_leaf_id, dim, distVect);//distMtx[left_leaf_id][right_leaf_id];
		    				
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
    				validatedClusters.addAll(recursive_validate_clusters_11(tree, level, max_dist, child.key, currdepth + 1, distVect, dim));
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
    //-
    List<Integer> recursive_validate_clusters_1(Tree tree, int level, double max_dist, int currkey, int currdepth, double[][] distMtx) {
    	
    	List<Integer> validatedClusters = new ArrayList<>();

    	TreeNode currNode = tree.getNodeByKey(currkey);
    	
    	//if (!clust.isLeaf()) {
    	if (!currNode.isLeaf()) {
    		
    		int numChildren = currNode.numberChildren();
    		
    		//for (Cluster child : clust.getChildren())
    		for (int i = 0; i < numChildren; i++) {
    			
    			TreeNode child = currNode.getChild(i);
    		
    			//System.out.println("Study child '" + child.key + "' > " + child.weight + " | " + child.height + " (" + level + " | " + max_dist + ")");
    			
//    			//if (child.getWeightValue() < level + EPSILON && child.getDistanceValue() < max_dist + EPSILON) {
//    			//////if (child.weight < level + EPSILON && child.height < max_dist + EPSILON) {
//    			if (tree.getHeight() - child.height < level) {
//    				validatedClusters.add(child.key);
//    				tot++;
//    				//if (child.isLeaf()) {
//    				//    System.out.println(">>> Found shity leaf: '" + child.getName() + "' => " + child.getParent().getDistanceValue() + " | " + child.getParent().getTotalDistance());
//    				//}
//    				tot2 += child.numberLeaves;
//    			} else {
//    				//validatedClusters.addAll(getValidatedClusters(child, level, max_dist));
//    				validatedClusters.addAll(recursive_validate_clusters(tree, level, max_dist, child.key, currdepth + 1));
//    			}
    			
    			//if (child.numberLeaves <= level && child.height < level) {
    			
    			
    			if (child.isLeaf()) {
    				validatedClusters.add(child.key);
    				tot++;
    				tot2++;
    				continue;
    			}
    			
    			
    			boolean node_is_cluster = true;
//    			//- Mod 1 (works only with "AVERAGE linkage mode")
//    			for (int j = 0; j < child.numberChildren(); j++) {
//    				if (child.getChild(j).getWeight() > max_dist/*veryLongDistance*/) {
//    					node_is_cluster = false;
//    					break;
//    				}
//    			}
    			//- Mod 2 (general case - works all the time - recover max_dist on the fly - can have time computing impact)
    			double max_dist_2 = -1d;
    			if (child.numberLeaves <= level /*&& !child.isLeaf()*/) {
    				
//    				System.out.println("child.numberChildren()= " + child.numberChildren());
//    				List<Integer> leafs_nodekeys_left = getClusterLeafs_1(child.getChild(0));
//        			List<Integer> leafs_nodekeys_right = getClusterLeafs_1(child.getChild(1));
    				List<Integer> leafs_nodekeys = getClusterLeafs_1(child);
        			
//        			List<Integer> allLeaves = getClusterLeafs_1(child);
//	    			for (int k = 0; k < allLeaves.size(); k++) {
//	    				
//	    				System.out.println("leaf_" + k + " = " + tree.getNodeByKey(allLeaves.get(k)));
//	    			}
//	    			for (int k = 0; k < leafs_nodekeys_left.size(); k++) {
//	    				
//	    				System.out.println("left_leaf_" + k + " = " + tree.getNodeByKey(leafs_nodekeys_left.get(k)).getName());
//	    				if (!tree.getNodeByKey(leafs_nodekeys_left.get(k)).isLeaf())
//	    					System.out.println("\t => '" + tree.getNodeByKey(leafs_nodekeys_left.get(k)).getName() + "' NOT A LEAF!!!");
//	    			}
        			
//        			// For each left leaf
//	    			for (int k = 0; k < leafs_nodekeys_left.size(); k++) {
//	    				
//	    				TreeNode left_leaf = tree.getNodeByKey(leafs_nodekeys_left.get(k));
//	    				//System.out.println("left_leaf.getName() = " + left_leaf.getName());
//	        			int left_leaf_id = Integer.valueOf(left_leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
//
//	        			// For each right leaf
//		    			for (int l = 0; l < leafs_nodekeys_right.size(); l++) {
//		    				
//		    				TreeNode right_leaf = tree.getNodeByKey(leafs_nodekeys_right.get(l));
//		    				//System.out.println("right_leaf.getName() = " + right_leaf);
//		        			int right_leaf_id = Integer.valueOf(right_leaf.getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
//
//		        			// Get distance between left and right leafs
//		    				double dist = distMtx[left_leaf_id][right_leaf_id];
//		    				
//		    				if (max_dist_2 < dist) {
//		    					max_dist_2 = dist;
//		    				}
//		    			}
//	    			}
    				
    				/** Leafs must all be an acceptable distance from each other for node to be considered a cluster! */
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
		    				double dist = distMtx[left_leaf_id][right_leaf_id];
		    				
		    				if (max_dist_2 < dist) {
		    					max_dist_2 = dist;
		    				}
		    			}
	    			}

//	    			node_is_cluster = (max_dist_2 >= 0d && max_dist_2 < max_dist + EPSILON);
//	    			if (!node_is_cluster) {
//		    			System.out.println("Separating clusters at node [left: n" + child.getChild(0).key + ", right: n" + child.getChild(1).key + "': " + max_dist_2 + " <= " + max_dist);
//		    			System.out.println("\tLeft:");
//		    			for (int k = 0; k < leafs_nodekeys_left.size(); k++) {
//	//	    				
//	//	    				    				
//	//	        			int leaf_id = Integer.valueOf(tree.getNodeByKey(leafs_nodekeys_left.get(k)).getName().substring(HierarClusterer.NEWICK_LEAF_NAME_PREFIX.length()));
//	//	        			rows_cluster.add(full_rows_list.get(leaf_id));
//	//	    				String row_name = ;
//	//	    				System.out.println("\t\t - " + full_rows_list.get(leaf_id)tree.getNodeByKey(leafs_nodekeys_left.get(k)).getName());
//		    				System.out.println("\t\t - " + tree.getNodeByKey(leafs_nodekeys_left.get(k)).getName());
//		    			}
//		    			System.out.println("\tRight:");
//		    			for (int k = 0; k < leafs_nodekeys_right.size(); k++) {
//		    				System.out.println("\t\t - " + tree.getNodeByKey(leafs_nodekeys_right.get(k)).getName());
//		    			}
//	    			}

    			}
    			node_is_cluster = (child.numberLeaves <= level && (max_dist_2 >= 0d && max_dist_2 < max_dist + EPSILON));
    			
    			///////boolean is_cluster_node = (child.numberLeaves <= level && node_is_cluster);
    			///////if (child.numberLeaves <= level && node_is_cluster) {
//    			if (child.numberLeaves <= level && child.weight < /*max_dist*/10d + EPSILON) {
    			if (node_is_cluster) {//is_cluster_node) {
    				validatedClusters.add(child.key);
    				tot++;
    				tot2 += child.numberLeaves;
//    				tot += 2;
//    				tot2 += child.getChild(0).numberLeaves + child.getChild(1).numberLeaves;
//    				validatedClusters.add(child.getChild(0).key);
//    				validatedClusters.add(child.getChild(1).key);
//    				validatedClusters.addAll(recursive_validate_clusters_1(tree, level, max_dist, child.getChild(0).key, currdepth + 1, distMtx));
//    				validatedClusters.addAll(recursive_validate_clusters_1(tree, level, max_dist, child.getChild(1).key, currdepth + 1, distMtx));
    			} else {
    				//validatedClusters.addAll(getValidatedClusters(child, level, max_dist));
    				///////validatedClusters.addAll(recursive_validate_clusters_1(tree, level, max_dist, child.key, currdepth + 1, distMtx));
//    				validatedClusters.addAll(recursive_validate_clusters_1(tree, level, max_dist, child.getChild(0).key, currdepth + 1, distMtx));
//    				validatedClusters.addAll(recursive_validate_clusters_1(tree, level, max_dist, child.getChild(1).key, currdepth + 1, distMtx));
    				validatedClusters.addAll(recursive_validate_clusters_1(tree, level, max_dist, child.key, currdepth + 1, distMtx));
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
    
//    /**
//     * Set the node heights from the current node branch lengths. Actually
//     * sets distance from root so the heights then need to be reversed.
//     */
//    private void nodeLengthsToHeights(Tree tree, TreeNode node, double height) {
//
//        double newHeight = height;
//
//        if (node.getWeight() > 0.0) {
//            newHeight += node.getWeight();
//        }
//
//        node.setWeight(newHeight);
//
//        for (int i=0; i < node.numberChildren(); i++) {
//        	
//        	TreeNode child = node.getChild(i);
//            nodeLengthsToHeights(tree, child, newHeight);
//        }
//    }
//    /**
//     * Calculate branch lengths from the current node heights.
//     */
//    private void nodeHeightsToLengths(Tree tree, TreeNode node, double height) {
//    	
//        final double h = node.getWeight();
//        
//        node.setWeight(h >= 0 ? height - h : 1);
//
//        for (int i=0; i < node.numberChildren(); i++) {
//        	
//        	TreeNode child = node.getChild(i);
//            nodeHeightsToLengths(tree, child, node.getWeight());
//        }
//
//    }

    private List<Node> getValidatedClusters_2(ClusteringResult clusteringResult, int level, double max_dist) {

    	//List<Node> validatedClusters = new ArrayList<>();

    	String newickCluster = clusteringResult.getHierarchicalCluster();
    	// Make string really Newick standard
    	//String newickCluster_clean = newickCluster.substring(newickCluster.indexOf(System.getProperty("line.separator"))+1) + ";";
    	String newickCluster_clean = newickCluster.substring(newickCluster.indexOf("\n")+1) + ";";

    	// Parse Newick formatted string
        BufferedReader r = new BufferedReader(new StringReader(newickCluster_clean));

        NewickImporter importer = new NewickImporter(r, true);
        jebl.evolution.trees.Tree tree = null;
        try {
        	while (importer.hasTree()) {
        		tree = importer.importNextTree();
        	}
        } catch (IOException | ImportException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        return recursive_validate_clusters_2(((RootedTree) tree), ((RootedTree) tree).getRootNode(), level, max_dist);
	}

    //tot = 0;
    //tot2 = 0;
    List<Node> recursive_validate_clusters_2(jebl.evolution.trees.RootedTree tree, Node node, int level, double max_dist) {
    	
    	List<Node> validatedClusters = new ArrayList<>();

    	Node currNode = node;
    	
    	//if (!clust.isLeaf()) {
    	//if (!(currNode.getDegree() == 0)) {
    	if (!(tree.getExternalNodes().contains(currNode))) {
    		
    		//int numChildren = currNode.numberChildren();
    		
    		//for (Cluster child : clust.getChildren())
    		for (Node child : tree.getChildren(currNode)) {
    			
    			int nbLeaves = getClusterLeafs_2(tree, currNode).size();
    			
    			if (nbLeaves <= level && tree.getHeight(child) < max_dist + EPSILON) {
    				
    				validatedClusters.add(child);
    				tot++;
    				//if (child.isLeaf()) {
    				//    System.out.println(">>> Found shity leaf: '" + child.getName() + "' => " + child.getParent().getDistanceValue() + " | " + child.getParent().getTotalDistance());
    				//}
    				tot2 += nbLeaves;
    			} else {
    				//validatedClusters.addAll(getValidatedClusters(child, level, max_dist));
    				validatedClusters.addAll(recursive_validate_clusters_2(tree, child, level, max_dist));
    			}
    		}
    	} else {
    		validatedClusters.add(currNode);
    	}

//    	for (int i = 0; i < numChildren; i++) {
//    	
//    		int childkey = currNode.getChild(i).key;
//    		TreeNode childnode = tree.getNodeByKey(childkey);
//    		System.out.println("child name is: " + childnode.getName() + " depth is: " + currdepth);
//    		recursive_print(tree, childkey, currdepth+1);
//    	}
    	
    	return validatedClusters;	
    }

    private List<Cluster> getClusterLeafs_0(Cluster clust) {

        List<Cluster> leafs = new ArrayList<>();

        if (clust.isLeaf()) {
            leafs = new ArrayList<>();
            leafs.add(clust);
        } else {

            for (Cluster child : clust.getChildren())
            {
                if (child.isLeaf()) {
                    leafs.add(child);
                } else {
                    leafs.addAll(getClusterLeafs_0(child));
                }
            }
        }

        return leafs;
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

    private List<Node> getClusterLeafs_2(RootedTree tree, Node node) {

        List<Node> leafs = new ArrayList<>();

        if (tree.getExternalNodes().contains(node)) {
            leafs = new ArrayList<>();
            leafs.add(node);
        } else {

            for (Node child : tree.getChildren(node))
            {
                if (tree.getExternalNodes().contains(child)) {
                    leafs.add(child);
                } else {
                    leafs.addAll(getClusterLeafs_2(tree, child));
                }
            }
        }

        return leafs;
    }


    private void swapClusterNames(Cluster clust, Map<String, String> swap_names_dict) {

        List<Cluster> leafs = getClusterLeafs_0(clust);

        for (Cluster leaf : leafs)
        {
            leaf.setName(swap_names_dict.get(leaf.getName()));
        }
    }

    
    
//    private List<String> getClusterLeafNames(Cluster clust) {
//
//        List<String> leafNames = new ArrayList<>();
//
//        if (clust.isLeaf()) {
//            leafNames = new ArrayList<>();
//            leafNames.add(clust.getName());
//        } else {
//
//            for (Cluster child : clust.getChildren())
//            {
//                if (child.isLeaf()) {
//                    leafNames.add(child.getName());
//                } else {
//                    leafNames.addAll(getClusterLeafNames(child));
//                }
//            }
//        }
//
//        return leafNames;
//    }

    private void saveDendrogramAsPng(Cluster clust, File dendrogramPngFilename, Map<String, String> swap_names_dict) {

        // Swap to human readable names
        swapClusterNames(clust, swap_names_dict);
        
        JPanel content = new JPanel();
        DendrogramPanel dp = new DendrogramPanel();


        // Dimension requiredSize = new Dimension(400, 300);
        int min_width = 200;//px
        int dst_width = 500;
        int leaf_height = 30;
        //


        int nbLeafs = clust.countLeafs();
        Dimension requiredSize = new Dimension((int) (min_width + dst_width * clust.getTotalDistance()), leaf_height * nbLeafs);

        content.setPreferredSize(requiredSize);
        dp.setPreferredSize(requiredSize);
        content.setSize(requiredSize);
        dp.setSize(requiredSize);

        content.setBackground(Color.red);
        content.setLayout(new BorderLayout());
        content.add(dp, BorderLayout.CENTER);
        dp.setBackground(Color.WHITE);
        dp.setLineColor(Color.BLACK);
        dp.setScaleValueDecimals(2);
        dp.setScaleValueInterval(0);//1d);
        //dp.setScaleTickLength(1);
        dp.setShowDistances(true);

        dp.setModel(clust);

        BufferedImage im = new BufferedImage(dp.getWidth(), dp.getHeight(), BufferedImage.TYPE_INT_ARGB);
        dp.paint(im.getGraphics());
//        try {
//            ImageIO.write(im, "PNG", dendrogramPngFilename);
//        } catch (IOException e) {
//            e.printStackTrace();
//            logger.info("Could not export dendrogram to file: '" + dendrogramPngFilename + "' !");
//        }
        createPdf(true, dp);
        createPdf(false, dp);
    }

    public void createPdf(boolean shapes, JPanel panel) {
        Document document = new Document();
        try {
            PdfWriter writer;
            
            logger.info("Writing PDF: " + "/home/golgauth/my_jtable_shapes.pdf");
            
            if (shapes)
                writer = PdfWriter.getInstance(document,
                        new FileOutputStream("/home/golgauth/my_jtable_shapes.pdf"));
            else
                writer = PdfWriter.getInstance(document,
                        new FileOutputStream("/home/golgauth/my_jtable_fonts.pdf"));
            document.open();
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(500, 500);
            Graphics2D g2;
            if (shapes)
                g2 = tp.createGraphicsShapes(500, 500);
            else
                g2 = tp.createGraphics(500, 500);
            panel.print(g2);
            g2.dispose();
            cb.addTemplate(tp, 30, 300);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        document.close();
    }

    private void saveDendrogramAsTxt(Cluster clust, File dendrogramTxtFilename, Map<String, String> swap_names_dict) {

        // Swap to human readable names
        swapClusterNames(clust, swap_names_dict);
        
        try{
            FileWriter writer = new FileWriter(dendrogramTxtFilename);
            writer.write(toConsole(clust, 0));
            writer.close();
        } catch (IOException e) {
            logger.info("Could not export dendrogram to file: '" + dendrogramTxtFilename + "' !");
        }
    }
    //-
    public String toConsole(Cluster clust, int indent)
    {
        String str = "";

        for (int i = 0; i < indent; i++)
        {
            str += "  ";

        }
        String name = clust.getName() 
                + (clust.isLeaf() ? " (leaf)" : "") 
                + (clust.getDistanceValue() != null ? "  distance: " + clust.getDistanceValue() : "")
                + (clust.getDistanceValue() != null ? "  t-distance: " + clust.getTotalDistance() : "")
                ;
        str += name + "\n";
        for (Cluster child : clust.getChildren())
        {
            str += toConsole(child, indent + 1);
        }

        return str;
    }
    
    
    


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



    // Rewriting class "DefaultClusteringAlgorithm" to be able to update the progress bar!
    public class DefaultClusteringAlgorithmWithProgress implements ClusteringAlgorithm //extends DefaultClusteringAlgorithm
    {

        @Override
        public Cluster performClustering(double[][] distances,
                String[] clusterNames, LinkageStrategy linkageStrategy)
        {
            
            if (isCanceled())
                return null;

            long startTime, endTime;
            float ms;
            
            startTime = System.currentTimeMillis();
            //
            checkArguments(distances, clusterNames, linkageStrategy);
            //
            endTime = System.currentTimeMillis();
            ms = (endTime - startTime);
            logger.info("> checkArguments (" + processedRows + " | elapsed time: " + Float.toString(ms) + " ms.)");


            if (isCanceled())
                return null;

            
            /* Setup model */
            startTime = System.currentTimeMillis();
            //
            List<Cluster> clusters = createClusters(clusterNames);
            //
            endTime = System.currentTimeMillis();
            ms = (endTime - startTime);
            logger.info("> createClusters (elapsed time: " + Float.toString(ms) + " ms.)");
            

            if (isCanceled())
                return null;

            startTime = System.currentTimeMillis();
            //
            DistanceMap linkages = createLinkages(distances, clusters);
            //
            endTime = System.currentTimeMillis();
            ms = (endTime - startTime);
            logger.info("> createLinkages (elapsed time: " + Float.toString(ms) + " ms.)");
            //logger.info("> Nb Linkages : " + linkages.list().size());


            if (isCanceled())
                return null;

            /* Process */
            startTime = System.currentTimeMillis();
            //
            HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);
            //
            endTime = System.currentTimeMillis();
            ms = (endTime - startTime);
            logger.info("> HierarchyBuilder (elapsed time: " + Float.toString(ms) + " ms.)");

            /** --------------------------------------------------------------*/
            // GLG HACK: update progress bar
            int nb_leafs = clusterNames.length;
            int progress_base = processedRows;
            //
            while (!builder.isTreeComplete())
            {

                if (isCanceled())
                    return null;

                startTime = System.currentTimeMillis();
                //
                builder.agglomerate(linkageStrategy);
                //
                endTime = System.currentTimeMillis();
                ms = (endTime - startTime);
                //logger.info("> builder.agglomerate (elapsed time: " + Float.toString(ms) + " ms.)");

                // GLG HACK: update progress bar
                processedRows = progress_base + nb_leafs - builder.getClusters().size() + 1;
            }
            /** --------------------------------------------------------------*/

            return builder.getRootCluster();
        }

        @Override
        public List<Cluster> performFlatClustering(double[][] distances,
                String[] clusterNames, LinkageStrategy linkageStrategy, Double threshold)
                {

            
            if (isCanceled())
                return null;

            checkArguments(distances, clusterNames, linkageStrategy);

            
            if (isCanceled())
                return null;

            /* Setup model */
            List<Cluster> clusters = createClusters(clusterNames);
            
            
            if (isCanceled())
                return null;

            DistanceMap linkages = createLinkages(distances, clusters);

            
            if (isCanceled())
                return null;

            /* Process */
            HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);
            
            
            if (isCanceled())
                return null;

            /** --------------------------------------------------------------*/
            // GLG HACK: update progress bar not possible here, unless we modify 'HierarchyBuilder.flatAgg()'
            List<Cluster> aggClusters = builder.flatAgg(linkageStrategy, threshold);
            /** --------------------------------------------------------------*/
            
            return aggClusters;
                }

        private void checkArguments(double[][] distances, String[] clusterNames,
                LinkageStrategy linkageStrategy)
        {
            if (distances == null || distances.length == 0
                    || distances[0].length != distances.length)
            {
                throw new IllegalArgumentException("Invalid distance matrix");
            }
            if (distances.length != clusterNames.length)
            {
                throw new IllegalArgumentException("Invalid cluster name array");
            }
            if (linkageStrategy == null)
            {
                throw new IllegalArgumentException("Undefined linkage strategy");
            }
            int uniqueCount = new HashSet<String>(Arrays.asList(clusterNames)).size();
            if (uniqueCount != clusterNames.length)
            {
                throw new IllegalArgumentException("Duplicate names");
            }
        }

        @Override
        public Cluster performWeightedClustering(double[][] distances, String[] clusterNames,
                double[] weights, LinkageStrategy linkageStrategy)
        {

            
            if (isCanceled())
                return null;

            checkArguments(distances, clusterNames, linkageStrategy);

            if (weights.length != clusterNames.length)
            {
                throw new IllegalArgumentException("Invalid weights array");
            }

            
            if (isCanceled())
                return null;

            /* Setup model */
            List<Cluster> clusters = createClusters(clusterNames, weights);
            
            if (isCanceled())
                return null;

            DistanceMap linkages = createLinkages(distances, clusters);

            
            if (isCanceled())
                return null;

            /* Process */
            HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);


            /** --------------------------------------------------------------*/
            // GLG HACK: update progress bar
            int nb_leafs = clusterNames.length;
            int progress_base = processedRows;
            //
            while (!builder.isTreeComplete())
            {
                
                if (isCanceled())
                    return null;

                builder.agglomerate(linkageStrategy);

                // GLG HACK: update progress bar
                processedRows = progress_base + nb_leafs - builder.getClusters().size() + 1;
            }
            /** --------------------------------------------------------------*/


            return builder.getRootCluster();
        }

        private DistanceMap createLinkages(double[][] distances,
                List<Cluster> clusters)
        {
            
            DistanceMap linkages = new DistanceMap();
            //ClusterPair link;
            for (int col = 0; col < clusters.size(); col++)
            {
                for (int row = col + 1; row < clusters.size(); row++)
                {
                    
                    // GLG HACK: Stop processing if user cancelled
                    if (isCanceled())
                        return null;

                    ClusterPair link = new ClusterPair(); //1
                    //Cluster lCluster = clusters.get(col);
                    //Cluster rCluster = clusters.get(row);
                    link.setLinkageDistance(distances[col][row]);
//                    link.setlCluster(lCluster);
//                    link.setrCluster(rCluster);
                    link.setlCluster(clusters.get(col));
                    link.setrCluster(clusters.get(row));
                    linkages.add(link);
                    
//                    // GLG HACK: Help GC to free memory!
                    link = null; //2 -> Help Garbage collector here! //System.gc(); //3

       /*         
                    linkages.add(new ClusterPair());
                    linkages.list().get(linkages.list().size()-1).setLinkageDistance(distances[col][row]);
                    linkages.list().get(linkages.list().size()-1).setlCluster(clusters.get(col));
                    linkages.list().get(linkages.list().size()-1).setrCluster(clusters.get(row));
        */
                }
                
//                System.out.println(">>>>>>> FULL: DistanceMap linkages size: " + RamUsageEstimator.shallowSizeOf(linkages));
//                System.out.println(">>>>>>> FULL: DistanceMap linkages size: " + RamUsageEstimator.sizeOf(linkages));
                // GLG HACK: Help GC to free memory! 
                //              >> Guess this line is not mandatory if run outside of Eclipse
                //System.gc(); //3
                
                // GLG HACK: update progress bar
                processedRows++;
                //logger.info("\t - createLinkages (" + (col+1) + " / " + clusters.size() + ").");
                System.out.println("\t - createLinkages (" + (col+1) + " / " + clusters.size() + ").");
            }
            return linkages;
        }

        private List<Cluster> createClusters(String[] clusterNames)
        {
            List<Cluster> clusters = new ArrayList<Cluster>();
            for (String clusterName : clusterNames)
            {
                
                if (isCanceled())
                    return null;

                Cluster cluster = new Cluster(clusterName);
                clusters.add(cluster);
            }
            return clusters;
        }

        private List<Cluster> createClusters(String[] clusterNames, double[] weights)
        {
            List<Cluster> clusters = new ArrayList<Cluster>();
            for (int i = 0; i < weights.length; i++)
            {
                
                if (isCanceled())
                    return null;

                Cluster cluster = new Cluster(clusterNames[i]);
                cluster.setDistance(new Distance(0.0, weights[i]));
                clusters.add(cluster);
            }
            return clusters;
        }
    }

    
//    public class NaiveAgglomerativeHierarchicalClustering<O, D extends NumberDistance<D, ?>>
//    extends AbstractDistanceBasedAlgorithm<O, D, Result> {
//
//    protected NaiveAgglomerativeHierarchicalClustering(DistanceFunction<? super O, D> distanceFunction) {
//      super(distanceFunction);
//      // TODO Auto-generated constructor stub
//    }
//
//    @Override
//    public TypeInformation[] getInputTypeRestriction() {
//      // TODO Auto-generated method stub
//      return null;
//    }
//
//    @Override
//    protected Logging getLogger() {
//      // TODO Auto-generated method stub
//      return null;
//    }
//  }
    
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

    // Using vector: Requires only N(N+1)/2 memory !!!
    /**
     * DOUBLE storage
     */
    public static double[] symmetricMatrixToVector(double[][] matrix) {
    	
    	int n = matrix.length;
    	
    	double[] array = new double[(n * (n+1)) / 2]; // allocate
    	for (int r = 0; r < matrix.length; ++r) // each row
    	{
    		for (int c = r; c < matrix[0].length; ++c)
    		{
    			int i = (n * r) + c - ((r * (r+1)) / 2); // corrected from earlier post
    			
    			array[i] = matrix[r][c];
    			
    			//System.out.println(Arrays.toString(array));
    		}
    	}
    	
    	return array;
    }
    public static double getValueFromVector(int r, int c, int n, double[] vector) {
    	
    	if (r < c)
    		return vector[(n * r) + c - ((r * (r+1)) / 2)];
    	else
    		return vector[(n * c) + r - ((c * (c+1)) / 2)];
    }
    /**
     * FLOAT storage
     */
    public static float[] symmetricMatrixToFloatVector(double[][] matrix) {
    	
    	int n = matrix.length;
    	
    	float[] array = new float[(n * (n+1)) / 2]; // allocate storage
    	for (int r = 0; r < matrix.length; ++r) // each row
    	{
    		for (int c = r; c < matrix[0].length; ++c)
    		{
    			int i = (n * r) + c - ((r * (r+1)) / 2); // corrected from earlier post
    			
    			array[i] = (float) matrix[r][c];
    			
    			//System.out.println(Arrays.toString(array));
    		}
    	}
    	
    	return array;
    }
    public static double getValueFromVector(int r, int c, int n, float[] vector) {
    	
    	if (r < c)
    		return vector[(n * r) + c - ((r * (r+1)) / 2)];
    	else
    		return vector[(n * c) + r - ((c * (c+1)) / 2)];
    }

    
}
