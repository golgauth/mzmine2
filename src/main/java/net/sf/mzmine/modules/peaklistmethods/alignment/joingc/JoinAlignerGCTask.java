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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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

import javax.imageio.ImageIO;
import javax.swing.JPanel;

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
//import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Range;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;



//import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
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
    private ClusteringLinkageStrategyType linkageStartegyType;
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
    private File dendrogramTxtFilename;
    private boolean exportDendrogramAsTxt;
    private File dendrogramPngFilename;


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
        linkageStartegyType = parameters.getParameter(
                JoinAlignerGCParameters.linkageStartegyType).getValue();

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

        exportDendrogramAsPng = parameters.getParameter(
                JoinAlignerGCParameters.exportDendrogramPng).getValue();
        dendrogramPngFilename = parameters.getParameter(
                JoinAlignerGCParameters.dendrogramPngFilename).getValue();
        exportDendrogramAsTxt = parameters.getParameter(
                JoinAlignerGCParameters.exportDendrogramTxt).getValue();
        dendrogramTxtFilename = parameters.getParameter(
                JoinAlignerGCParameters.dendrogramTxtFilename).getValue();

        //
        maximumScore = mzWeight + rtWeight;
        veryLongDistance = 10.0d * maximumScore;
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
        return (double) processedRows / (double) totalRows;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        if ((Math.abs(mzWeight) < EPSILON) && (Math.abs(rtWeight) < EPSILON)) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Cannot run alignment, all the weight parameters are zero");
            return;
        }

        setStatus(TaskStatus.PROCESSING);
        logger.info("Running join aligner");

        // Remember how many rows we need to process. Each row will be processed
        // /*twice*/ three times:
        //      - first for score calculation
        //      - second for...
        //      - third for actual alignment
        for (int i = 0; i < peakLists.length; i++) {
            totalRows += peakLists[i].getNumberOfRows();
        }
        totalRows *= 3;

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
        ///boolean useOffsetOnly = false;

        //Hashtable<RawDataFile, double[]> rtAdjustementMapping = new Hashtable<>();
        Hashtable<RawDataFile, List<double[]>> rtAdjustementMapping = new Hashtable<>();

        if (recalibrateRT) {

            boolean rtAdjustOk = true;
            // Iterate source peak lists
            // RT mapping based on the first PeakList
            //            double rt1 = -1.0;
            //            double rt2 = -1.0;

            // Need all list to have AT LEAST all identities of reference list (first list in selection = peakLists[0])
            // TODO: Smarter way: Allow having less ref identities in other lists and fill the gap!
            // TODO: Smarter way: Or, at least use compounds found in all selected lists!
            ArrayList<String> allIdentified_0 = new ArrayList<>();
            
            for (int i=0; i < peakLists.length; ++i) {
                //double offset, scale;
                PeakList a_pl = peakLists[i];
                
                logger.info("# Search identities for list: " + a_pl.getRawDataFile(0) + " (nb peakLits = " + peakLists.length + ")");
                
                // Get ref RT1 and RT2
                // Sort peaks by ascending RT
                PeakListRow a_pl_rows[] = a_pl.getRows().clone(); 
                Arrays.sort(a_pl_rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
                //
                ArrayList<PeakListRow> allIdentified = new ArrayList<>();
                ArrayList<String> allIdentified_i = new ArrayList<>();
                List<Double> rtList = new ArrayList<>();
                //
                for (int j=0; j < a_pl_rows.length; ++j) {
                    PeakListRow row = a_pl_rows[j];
                    //                    // If row actually was identified
                    //                    if (row.getPreferredPeakIdentity() != null 
                    //                            && !row.getPreferredPeakIdentity().getName().equals(JDXCompound.UNKNOWN_JDX_COMP.getName())) {
                    //                        allIdentified.add(row);
                    //                    }
                    // If row actually was identified AND is a "reference compound"
                    //**if (row.getPreferredPeakIdentity() != null) {
                    if (JDXCompound.isKnownIdentity(row.getPreferredPeakIdentity())) {
                        
                        logger.info("\t* Trying with identity: " + row.getPreferredPeakIdentity().getName());
                        
                        String isRefCompound = row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF);
                        if (isRefCompound != null && isRefCompound.equals(AlignedRowProps.TRUE)) {
                            
                            logger.info("\t\t !! OK with identity: " + row.getPreferredPeakIdentity().getName());
                            
                            if (i == 0) {
                                allIdentified.add(row);
                                allIdentified_0.add(row.getPreferredPeakIdentity().getName());
                            } else if (allIdentified_0.contains(row.getPreferredPeakIdentity().getName())) {
                                allIdentified.add(row);
                                allIdentified_i.add(row.getPreferredPeakIdentity().getName());
                            }
                                
                        }
                    } else {
//                        logger.info("aFailed 0: " + (row.getPreferredPeakIdentity().getName() != JDXCompound.UNKNOWN_JDX_COMP.getName()));
//                        logger.info("aFailed 1: " + row.getPreferredPeakIdentity());
//                        logger.info("aFailed 2: " + row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF));                       
                    }
                }
                //
                logger.info("> allIdentified: NB found compounds: " + allIdentified.size());
                boolean allRefsFound = true;
//                for (PeakListRow r : allIdentified) {
//                    
//                    String peakIdentityName = r.getPreferredPeakIdentity().toString();
//                    logger.info(peakIdentityName);
//                    
//                    if (i > 0 && !allIdentified_0.contains(peakIdentityName)) {
//                        allRefsFound = false;
//                        logger.info("\t> Compound '" + peakIdentityName + "' not found for list '" + peakLists[i].getRawDataFile(0) + "'!");
//                    }
//                }
                for (String name : allIdentified_0) {
                    
                    if (i > 0 && !allIdentified_i.contains(name)) {
                        allRefsFound = false;
                        logger.info("\t> Ref compound '" + name + "' not found for list '" + peakLists[i].getRawDataFile(0) + "'!");
                    }
                }
                //
                if (!allRefsFound) {
                    //logger.info("> Compound(s) missing for list '" + peakLists[i].getRawDataFile(0) + "' => ABORT!!");
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("> Ref compound(s) missing for list '" + peakLists[i].getRawDataFile(0) + "' => ABORT!!");
                    return;
                }
                    
//                //
//                if (i > 0 && allIdentified.size() != allIdentified_0.size()) {
//                    
//                    //logger.info("> Found '" + allIdentified.size() + "' compounds in list '" + allIdentified.size() 
//                    //        + "', Expected: '" + allIdentified_0.size() + "' => ABORT!!");
//                    setStatus(TaskStatus.ERROR);
//                    setErrorMessage("> Found '" + allIdentified.size() + "' compounds in list '" + peakLists[i].getRawDataFile(0) 
//                            + "', Expected: '" + allIdentified_0.size() + "' => ABORT!!");
//                    return;
//                }
                
                
                //                // Two ref compounds max, for now...
                //                if (allIdentified.size() == 2) {
                //                    // TODO: Or even better: duplicate the Peaklists involved and update the peaks RT
                //                    //          using "Feature.setRT()", as planned some time ago via button "Apply & Adjust RT"
                //                    //          from RTAdjuster module's result table !!!
                //                    rt1 = allIdentified.get(0).getAverageRT();
                //                    rt2 = allIdentified.get(1).getAverageRT();
                //                } else {
                //                    logger.info("Error => Couldn't identify the 2 required ref compounds for peaklist: " + a_pl);
                //                    rtAdjustOk = false;
                //                    continue;
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
//                    setStatus(TaskStatus.ERROR);
//                    setErrorMessage("! Warn: Couldn't identify at least 1 ref compound for peaklist: " + a_pl
//                            + " => No RT recalibration (not gonna apply any offset, nor scale) !");
//                    return;

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

        LinkageStrategy linkageStrategy;
        //
        switch (linkageStartegyType) {
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



        double[][] distances;
        String[] names;
        int nbPeaks = 0;
        for (int i = 0; i < newIds.length; ++i) {
            PeakList peakList = peakLists[newIds[i]];
            nbPeaks += peakList.getNumberOfRows();
            System.out.println("> Peaklist '" + peakList.getName() + "' [" + newIds[i] + "] has '" + peakList.getNumberOfRows() + "' rows.");
        }
        distances = new double[nbPeaks][nbPeaks];
        names = new String[nbPeaks];

        Map<String, PeakListRow> row_names_dict = new HashMap<>();
        List<String> rows_list = new ArrayList<>();

        int x = 0;
        for (int i = 0; i < newIds.length; ++i) {


            PeakList peakList = peakLists[newIds[i]];

            PeakListRow allRows[] = peakList.getRows();
            logger.info("Treating list " + peakList + " / NB rows: " + allRows.length);

            // Calculate scores for all possible alignments of this row
            for (int j = 0; j < allRows.length; ++j) {

                ////int x = (i * allRows.length) + j;

                PeakListRow row = allRows[j];

                // Each name HAS to be unique
                RawDataFile ancestorRDF = DataFileUtils.getAncestorDataFile(project, peakList.getRawDataFile(0), true);
                // Avoid exception if ancestor RDFs have been removed...
                String suitableRdfName = (ancestorRDF == null) ? peakList.getRawDataFile(0).getName() : ancestorRDF.getName();
                names[x] = "[" + suitableRdfName + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
                row_names_dict.put(names[x], row);
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
                            writer.write(names[j] + "\t");
                        }

                    for (int j = 0; j < distances[i].length; j++) {
                        if (j == 0)
                            writer.write(names[i] + "\t");
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


        ClusteringAlgorithm alg = new DefaultClusteringAlgorithmWithProgress(); //DefaultClusteringAlgorithm();
        //      System.out.println(Arrays.toString(names));
        //      System.out.println(Arrays.deepToString(distances));
        //      for (int i=0; i<names.length; i++)
        //          System.out.println(names[i]);
        //      for (int i=0; i<distances.length; i++)
        //          System.out.println(Arrays.toString(distances[i]));
        
        Cluster clust = alg.performClustering(distances, names, linkageStrategy);

        //clust.toConsole(0);

        System.out.println("Done clustering");

        double max_dist = maximumScore; //Math.abs(row.getBestPeak().getRT() - k_row.getBestPeak().getRT()) / ((RangeUtils.rangeLength(rtRange) / 2.0));
        List<Cluster> validatedClusters = getValidatedClusters(clust, newIds.length, max_dist);
        ////////******List<Cluster> validatedClusters = alg.performFlatClustering(distances, names, linkageStrategy, maximumScore - minScore);
        //
        if (DEBUG) {
            for (Cluster c: validatedClusters) {
                //System.out.println(c.getName() + " : [" + c.getLeafNames() + "]");
                System.out.println();
                c.toConsole(0);
            }
        }

        // VERIF
        if (DEBUG) {
//            //
//            int missing1 = 0;
//            for (Cluster c: getClusterLeafs(clust)) {
//                String name = c.getName();//"[" + DataFileUtils.getAncestorDataFile(project, peakList.getRawDataFile(0), true).getName() 
//                        //+ "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
//                if (!row_names_dict.keySet().contains(name))
//                    missing1++;
//            }
//            int missing2 = 0;
//            for (Cluster c: validatedClusters) {
//            
//                for (String name: c.getLeafNames()) {
//                
//                    if (!row_names_dict.keySet().contains(name))
//                        missing2++;
//                }
//            }
//            //System.out.println(Arrays.toString(getClusterLeafNames(clust).toArray()));
//            int missing3 = 0;
//            for (String name: row_names_dict.keySet()) {
//            
//    //            String name = "[" + DataFileUtils.getAncestorDataFile(project, row.getRawDataFiles()[0], true).getName() 
//    //                    + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
//                //System.out.println("> Name: " + name);
//                if (!getClusterLeafNames(clust).contains(name))
//                    missing3++;
//            }
//            int missing4 = 0;
//            for (String name: row_names_dict.keySet()) {
//            
//    //            String name = "[" + DataFileUtils.getAncestorDataFile(project, row.getRawDataFiles()[0], true).getName() 
//    //                    + "], #" + row.getID() + ", @" + rtFormat.format(row.getBestPeak().getRT());
//    
//                boolean found_in_v_clusters = false;
//                for (Cluster c: validatedClusters) {
//                    for (String n: getClusterLeafNames(c)) {
//                        if (n.equals(name)) {
//                            found_in_v_clusters = true;
//                        }
//                    }
//                }
//                if (!found_in_v_clusters)
//                    missing4++;
//            }
//            System.out.println("Missing summary: " + missing1 + " | " + missing2 + " | " + missing3 + " | " + missing4);
        }

        //----------------------------------------------------------------------

//        try {
//            if (exportDendrogramAsPng && dendrogramPngFilename != null) {          
//                saveDendrogramAsPng(clust, dendrogramPngFilename);
//            } else {
//                
//            }
//        } catch (Exception e) {
//            logger.info("! saveDendrogramAsPng failed...");
//            e.printStackTrace();
//        }
//        try {
//            if (exportDendrogramAsTxt && dendrogramTxtFilename != null) {          
//                saveDendrogramAsTxt(clust, dendrogramTxtFilename);
//            }
//        } catch (Exception e) {
//            logger.info("! saveDendrogramAsTxt failed...");
//            e.printStackTrace();
//        }

        //----------------------------------------------------------------------



        // Create a table of mappings for best scores
        Hashtable<PeakListRow, PeakListRow> alignmentMapping = new Hashtable<PeakListRow, PeakListRow>();

        //        // Iterate scores by descending order
        //        Iterator<RowVsRowScoreGC> scoreIterator = scoreSet.iterator();
        //        //Hashtable<PeakListRow, RowVsRowScore> scoresMapping = new Hashtable<PeakListRow, RowVsRowScore>();
        //        while (scoreIterator.hasNext()) {
        //
        //            RowVsRowScoreGC score = scoreIterator.next();
        //
        //            // Check if the row is already mapped
        //            if (alignmentMapping.containsKey(score.getPeakListRow()))
        //                continue;
        //
        //            // Check if the aligned row is already filled
        //            if (alignmentMapping.containsValue(score.getAlignedRow()))
        //                continue;
        //
        //            alignmentMapping.put(score.getPeakListRow(),
        //                    score.getAlignedRow());
        //            //scoresMapping.put(score.getPeakListRow(), score);
        //            //scoresMapping.put(score.getAlignedRow(), score);
        //
        //            //...
        //            alignmentMapping.put(score.getAlignedRow(),
        //                    score.getPeakListRow());
        //        }

        //        logger.info("AAAAAAAAAAAAAAAAAAA: alignmentMapping list " + alignmentMapping + " / NB entries: " + alignmentMapping.size());
        //        logger.info("BBBBBBBBBBBBBBBBBBB: scoreSet list " + scoreSet + " / NB entries: " + scoreSet.size());

        //        // First clustering pass: try to find the N best matching buddies (from the N raw data files)
        //        for (RawDataFile dataFile : alignedPeakList.getRawDataFiles()) {
        //            
        //            
        //        }


        List<List<PeakListRow>> clustersList = new ArrayList<>();

        /**
        // Iterate scores by descending order
        Iterator<RowVsRowScoreGC> scoreIterator = scoreSet.iterator();
        //Hashtable<PeakListRow, RowVsRowScore> scoresMapping = new Hashtable<PeakListRow, RowVsRowScore>();
        //
        // A score must be used once, at most!
        List<RowVsRowScoreGC> consumedScores = new ArrayList<>();
        // A peak must be clustered once, at most!
        List<PeakListRow> clusteredPeaks = new ArrayList<>();
        //
        ////int n = -1, nn = -1;
        while (scoreIterator.hasNext()) {

            ////n++;

            RowVsRowScoreGC score = scoreIterator.next();

            if (consumedScores.contains(score)) { continue; }


//            // Check if the row is already mapped
//            if (alignmentMapping.containsKey(score.getPeakListRow()))
//                continue;
//
//            // Check if the aligned row is already filled
//            if (alignmentMapping.containsValue(score.getAlignedRow()))
//                continue;
//
//            alignmentMapping.put(score.getPeakListRow(),
//                    score.getAlignedRow());
//            //scoresMapping.put(score.getPeakListRow(), score);
//            //scoresMapping.put(score.getAlignedRow(), score);

            // Consume score
            consumedScores.add(score);

            //int nb_buddies = 1;
            List<PeakListRow> cluster = new ArrayList<>();
            // Add current pair
            cluster.add(score.getPeakListRow());
            cluster.add(score.getAlignedRow());
            clusteredPeaks.add(score.getPeakListRow());
            clusteredPeaks.add(score.getAlignedRow());
            //-

            // Try find the (N-2) potential other buddies 
            Iterator<RowVsRowScoreGC> scoreIterator_2 = scoreSet.iterator();
            while (scoreIterator_2.hasNext()) {

                ////nn++;
                ////if (nn <= n) { continue; };

                RowVsRowScoreGC score_2 = scoreIterator_2.next();

                if (score_2 == score) { continue; }

                // Already clustered?
                if (clusteredPeaks.contains(score_2.getPeakListRow()) || clusteredPeaks.contains(score_2.getAlignedRow())) { continue; }


                // If score_2 is referencing one of the rows of interest
                if (!consumedScores.contains(score_2) 
                        // Already clustered?
                        //&& (!(cluster.contains(score_2.getPeakListRow()) && cluster.contains(score_2.getAlignedRow())))
                        // Match?
                        && (cluster.contains(score_2.getPeakListRow()) || cluster.contains(score_2.getAlignedRow()))
                        ) {

                    if (cluster.contains(score_2.getPeakListRow()) /\*&& !clusteredPeaks.contains(score_2.getAlignedRow())*\/) {
                        cluster.add(score_2.getAlignedRow());
                        // Consume score
                        //consumedScores.add(score_2);
                        clusteredPeaks.add(score_2.getAlignedRow());
                    } else /\*if (!clusteredPeaks.contains(score_2.getPeakListRow()))*\/ {
                        cluster.add(score_2.getPeakListRow());
                        // Consume score
                        //consumedScores.add(score_2);
                        clusteredPeaks.add(score_2.getPeakListRow());
                    }


                    // Consume score
                    consumedScores.add(score_2);
                }

                // If maximum number of buddies in cluster is reached, stop looking
                // Otherwise, keep going
                if (cluster.size() == alignedPeakList.getRawDataFiles().length) { break; }
            }

            // Store cluster
            clustersList.add(cluster);

            // Clear non-single (=clustered) from list
            for (PeakListRow c_plr: cluster) {
                singleRowsList.remove(c_plr);
                // Cluster is full => lock contained peaks
//                if (cluster.size() == alignedPeakList.getRawDataFiles().length)
//                    clusteredPeaks.add(c_plr);
            }


        } 
         **/

        int finalNbPeaks = 0;
        Set<String> leaf_names = new HashSet<>();
        for (Cluster c: validatedClusters) {

            List<Cluster> c_leafs = getClusterLeafs(c);
            List<PeakListRow> rows_cluster = new ArrayList<>();
            RawDataFile rdf = null;
            for (Cluster l: c_leafs) {
                // Recover related PeakListRow
                rows_cluster.add(row_names_dict.get(l.getName()));
                leaf_names.add(l.getName());

                if (DEBUG) {
                    RawDataFile a_rdf = row_names_dict.get(l.getName()).getBestPeak().getDataFile();
                    if (a_rdf == rdf) {
                        logger.info("RDF duplicate for row: " + row_names_dict.get(l.getName()) 
                                + " [Peak: " + row_names_dict.get(l.getName()).getBestPeak() + "]");
                    }
                    rdf = a_rdf;
                }
            }
            clustersList.add(rows_cluster);
            finalNbPeaks += rows_cluster.size();
        }


        //        // Make clusters from remaining singles
        //        for (PeakListRow single_row: singleRowsList) {
        //            
        //            clustersList.add(new ArrayList<PeakListRow>(Arrays.asList(new PeakListRow[] { single_row })));
        //        }


        int nbAddedPeaks = 0;
        int nbAddedRows = 0;
        // Fill alignment table: One row per cluster
        for (List<PeakListRow> cluster: clustersList) {

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
                                logger.info("{" + row_offset_scale[0] + ", " + row_offset_scale[1] + "}");
                                double b_offset = row_offset_scale[0];
                                double a_scale = row_offset_scale[1];
                                //
                                double adjustedRT = JoinAlignerGCTask.getAdjustedRT(originalPeak.getRT(), b_offset, a_scale);

                                SimpleFeature adjustedPeak = new SimpleFeature(originalPeak);
                                PeakUtils.copyPeakProperties(originalPeak, adjustedPeak);
                                adjustedPeak.setRT(adjustedRT);
                                logger.info("adjusted Peak/RT = " + originalPeak + ", " + adjustedPeak + " / " + originalPeak.getRT() + ", " + adjustedPeak.getRT());

                                targetRow.addPeak(file, adjustedPeak);
                                nbAddedPeaks++;
                                // Adjusted RT info
                                rtPeaksBackup.put(adjustedPeak, originalPeak.getRT());
                                ((HashMap<RawDataFile, Double[]>) infoRowsBackup.get(targetRow)[0]).put(file, new Double[] { adjustedRT, b_offset, a_scale });//originalPeak.getRT());

                            } else {

                                // HELP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                                ////if (!Arrays.asList(targetRow.getPeaks()).contains(originalPeak)) {

                                targetRow.addPeak(file, originalPeak);
                                nbAddedPeaks++;
                                logger.info("Added peak: " + originalPeak);
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

                            logger.info("targetRow >>> Added Peak @" + originalPeak.getClass().getName() + '@' + Integer.toHexString(originalPeak.hashCode()) 
                                    + ",  RT=" + targetRow.getPeaks()[targetRow.getPeaks().length-1].getRT() + " / ID: " + targetRow.getID());
                            logger.info(".");
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
                logger.info(">> found max for: " + mainIdentity);
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


        //if (DEBUG) {
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
                        logger.info("Nb peaks treated: " + names.length + " | " + row_names_dict.size() + " | " + finalNbPeaks + " | " + nbAddedPeaks + " | " + finalNbPeaks2);
        //}
    }


    //    private static int encodeIndexes8(int index1, int index2) {
    //        return (index1 << 8) | index2;
    //    }
    //
    //    private static int[] decodeIndexes8(int combinedIds) {
    //        return new int[] { combinedIds >> 8, combinedIds & 0x00FF };
    //    }


    /**
     * Two clusters can be merged if and only if:
     *  - The resulting merged cluster: (their parent) doesn't exceed 'level' leaves
     *  - The distance between them two is acceptable (close enough)
     */
    private List<Cluster> getValidatedClusters(Cluster clust, int level, double max_dist) {

        List<Cluster> validatedClusters = new ArrayList<>();
        if (!clust.isLeaf()) {

            for (Cluster child : clust.getChildren())
            {
                // Trick to get number of leafs without browsing the whole tree (unlike how it's done in ".countLeafs()")
                //      => Weight gives the number of leafs
                //if (child.countLeafs() <= level) {
                if (child.getWeightValue() < level + EPSILON && child.getDistanceValue() < max_dist + EPSILON) {
                    validatedClusters.add(child);
                    if (child.isLeaf()) {
                        System.out.println(">>> Found shity leaf: '" + child.getName() + "' => " + child.getParent().getDistanceValue() + " | " + child.getParent().getTotalDistance());
                    }
                } else {
                    validatedClusters.addAll(getValidatedClusters(child, level, max_dist));
                }
            }
        }

        return validatedClusters;
    }

    private List<Cluster> getClusterLeafs(Cluster clust) {

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
                    leafs.addAll(getClusterLeafs(child));
                }
            }
        }

        return leafs;
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

    private void saveDendrogramAsPng(Cluster clust, File dendrogramPngFilename) {

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
            
            System.out.println("Writing PDF: " + "/home/golgauth/my_jtable_shapes.pdf");
            
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

    private void saveDendrogramAsTxt(Cluster clust, File dendrogramTxtFilename) {

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
            
            long startTime, endTime;
            float seconds;
            
            startTime = System.currentTimeMillis();
            //
            checkArguments(distances, clusterNames, linkageStrategy);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> checkArguments (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");

            
            /* Setup model */
            startTime = System.currentTimeMillis();
            //
            List<Cluster> clusters = createClusters(clusterNames);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> createClusters (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");
            
            startTime = System.currentTimeMillis();
            //
            DistanceMap linkages = createLinkages(distances, clusters);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> createLinkages (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");
            System.out.println("> Nb Linkages : " + linkages.list().size());

            /* Process */
            startTime = System.currentTimeMillis();
            //
            HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> HierarchyBuilder (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");

            /** --------------------------------------------------------------*/
            // GLG HACK: update progress bar
            int nb_leafs = clusterNames.length;
            int progress_base = processedRows;
            //
            while (!builder.isTreeComplete())
            {
                startTime = System.currentTimeMillis();
                //
                builder.agglomerate(linkageStrategy);
                //
                endTime = System.currentTimeMillis();
                seconds = (endTime - startTime);
                System.out.println("> builder.agglomerate (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");

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

            long startTime, endTime;
            float seconds;

            startTime = System.currentTimeMillis();
            //
            checkArguments(distances, clusterNames, linkageStrategy);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> checkArguments (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");


            /* Setup model */
            startTime = System.currentTimeMillis();
            //
            List<Cluster> clusters = createClusters(clusterNames);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> createClusters (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");

            startTime = System.currentTimeMillis();
            //
            DistanceMap linkages = createLinkages(distances, clusters);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> createLinkages (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");
            System.out.println("> Nb Linkages : " + linkages.list().size());

            /* Process */
            startTime = System.currentTimeMillis();
            //
            HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> HierarchyBuilder (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");

            startTime = System.currentTimeMillis();
            //
            /** --------------------------------------------------------------*/
            // GLG HACK: update progress bar not possible here, unless we modify 'HierarchyBuilder.flatAgg()'
            List<Cluster> aggClusters = builder.flatAgg(linkageStrategy, threshold);
            /** --------------------------------------------------------------*/
            //
            endTime = System.currentTimeMillis();
            seconds = (endTime - startTime);
            System.out.println("> builder.flatAgg (" + processedRows + " | elapsed time: " + Float.toString(seconds) + " ms.)");
            
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

            checkArguments(distances, clusterNames, linkageStrategy);

            if (weights.length != clusterNames.length)
            {
                throw new IllegalArgumentException("Invalid weights array");
            }

            /* Setup model */
            List<Cluster> clusters = createClusters(clusterNames, weights);
            DistanceMap linkages = createLinkages(distances, clusters);

            /* Process */
            HierarchyBuilder builder = new HierarchyBuilder(clusters, linkages);


            /** --------------------------------------------------------------*/
            // GLG HACK: update progress bar
            int nb_leafs = clusterNames.length;
            int progress_base = processedRows;
            //
            while (!builder.isTreeComplete())
            {
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
            for (int col = 0; col < clusters.size(); col++)
            {
                for (int row = col + 1; row < clusters.size(); row++)
                {
                    ClusterPair link = new ClusterPair(); //1
                    Cluster lCluster = clusters.get(col);
                    Cluster rCluster = clusters.get(row);
                    link.setLinkageDistance(distances[col][row]);
                    link.setlCluster(lCluster);
                    link.setrCluster(rCluster);
                    linkages.add(link);

                    link = null; //2
                }
//                System.gc(); //3
            }
            return linkages;
        }

        private List<Cluster> createClusters(String[] clusterNames)
        {
            List<Cluster> clusters = new ArrayList<Cluster>();
            for (String clusterName : clusterNames)
            {
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
}
