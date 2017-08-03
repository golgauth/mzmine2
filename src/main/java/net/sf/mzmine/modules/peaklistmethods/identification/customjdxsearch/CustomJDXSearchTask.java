/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.customjdxsearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.AlignedRowProps;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RowVsRowScoreGC;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.ArrayComparator;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationParameters;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.SimilarityMethodType;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.DataFileUtils;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;


// Dot
import org.apache.commons.math.linear.ArrayRealVector;
//Pearson
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.jcamp.parser.JCAMPException;

import com.google.common.collect.Range;



public class CustomJDXSearchTask extends AbstractTask {

    // Logger.
    private static Logger logger = Logger.getLogger(CustomJDXSearchTask.class.getName());

    private final MZmineProject project;

    // Minimum abundance.
    private static final double MIN_ABUNDANCE = 0.001;
    
    // Minimum score ever.
    // TODO: better use "Double.MIN_VALUE" rather than zero (it has consequences !!!!)
    //       (0.0 is fine for 'Dot Product' method, but not for 'Person Correlation')
    ////public static final double MIN_SCORE_ABSOLUTE = Double.MIN_VALUE;
    public static final double MIN_SCORE_ABSOLUTE = 0.0;

    // Counters.
    //private int finishedItemsTotal;
    //private int finishedItems;
    //private int numItems;
    //-
    private int progressItemNumberTotal;
    private int progressItemNumber;

    private final PeakList[] peakLists;
    
    private File searchDir;
    private boolean bruteForceErase;
    private boolean useAsStdCompound;
//    private File jdxFileC1, jdxFileC2;
//    private JDXCompound jdxComp1, jdxComp2;
//    private Range<Double> rtSearchRangeC1, rtSearchRangeC2;
    private SimilarityMethodType simMethodType;
    private double areaMixFactor;
    private double minScore;
    
    private boolean useDetectedMzOnly;
    
    private boolean ignoreRanges;
//    private boolean applyWithoutCheck;
    private File blastOutputFilename;
    private String fieldSeparator;

    private PeakListRow currentRow;
    private PeakList currentPeakList;

    private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    private NumberFormat areaFormat = MZmineCore.getConfiguration().getIntensityFormat();

    private Range<Double>[] rtSearchRanges;


    /**
     * Create the identification task.
     * 
     * @param parameters
     *            task parameters.
     * @param list
     *            peak list to operate on.
     */
    @SuppressWarnings("unchecked")
    CustomJDXSearchTask(final MZmineProject project, 
            final ParameterSet parameters, final PeakList[] lists) {

        this.project = project;

        peakLists = lists;
        //numItems = 0;
        //finishedItems = 0;
        currentRow = null;

        searchDir = parameters.getParameter(CustomJDXSearchParameters.JDX_DIR).getValue();
        bruteForceErase = parameters.getParameter(CustomJDXSearchParameters.BRUTE_FORCE_ERASE).getValue();
        useAsStdCompound = parameters.getParameter(CustomJDXSearchParameters.USE_AS_STD_COMPOUND).getValue();
//        jdxFileC1 = parameters.getParameter(CustomJDXSearchParameters.JDX_FILE_C1).getValue();
//        jdxFileC2 = parameters.getParameter(CustomJDXSearchParameters.JDX_FILE_C2).getValue();
//        rtSearchRangeC1 = parameters.getParameter(CustomJDXSearchParameters.RT_SEARCH_WINDOW_C1).getValue();
//        rtSearchRangeC2 = parameters.getParameter(CustomJDXSearchParameters.RT_SEARCH_WINDOW_C2).getValue();
        simMethodType = parameters.getParameter(CustomJDXSearchParameters.SIMILARITY_METHOD).getValue();
        areaMixFactor = parameters.getParameter(CustomJDXSearchParameters.AREA_MIX_FACTOR).getValue();
        minScore = parameters.getParameter(CustomJDXSearchParameters.MIN_SCORE).getValue();
        
        useDetectedMzOnly = parameters.getParameter(CustomJDXSearchParameters.useDetectedMzOnly).getValue();

        ignoreRanges = parameters.getParameter(CustomJDXSearchParameters.IGNORE_RT_RANGE_FILES).getValue();
//        applyWithoutCheck = parameters.getParameter(CustomJDXSearchParameters.APPLY_WITHOUT_CHECK).getValue();
        blastOutputFilename = parameters.getParameter(CustomJDXSearchParameters.BLAST_OUTPUT_FILENAME).getValue();
        fieldSeparator = parameters.getParameter(CustomJDXSearchParameters.FIELD_SEPARATOR).getValue();

    }

    @Override
    public double getFinishedPercentage() {

        //return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
        //return numItems == 0 ? 0.0 : (double) finishedItemsTotal / (double) (finishedItems * numItems * peakLists.length);
        return(double) progressItemNumber / (double) progressItemNumberTotal;
    }

    @Override
    public String getTaskDescription() {

//        if (jdxComp1 != null && jdxComp2 != null) {
//            return "Identification of standard peaks in "
//                    + "selected peak lists"
//                    + " using " + " standards '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'";
//        } else {
//            return "Identification of standard peaks in selected peak lists"
//                    + " using the 'Two standard compounds finder'";            
//        }
        
        return "Identification of JDX compounds in selected peak lists"
              + " from custom directory '" + this.searchDir.getPath() + "'";    
    }

    @Override
    public void run() {

        // List JDX files from search directory
        File[] jdxFiles = searchDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jdx");
            }
        });        
        JDXCompound[] jdxCompounds = new JDXCompound[jdxFiles.length];
        String[] columnNames = new String[1 + 4*jdxFiles.length];
        rtSearchRanges = (Range<Double>[]) new Range[jdxFiles.length];
        
        if (!isCanceled()) {
            int i_f = 0;
            try {
                
                columnNames[0] = "Data file";
                
                for (i_f=0; i_f < jdxFiles.length; i_f++) {
                    
                    // Parse jdx files
                    jdxCompounds[i_f] = JDXCompound.parseJDXfile(jdxFiles[i_f]);
                    logger.info("Parsed JDX file: " + jdxFiles[i_f].getName());

                    // Build csv header row
                    int col = 4 * (i_f % jdxFiles.length);
                    columnNames[1 + col] = jdxCompounds[i_f].getName();
                    columnNames[1 + col + 1] = " score";
                    columnNames[1 + col + 2] = " rt";
                    columnNames[1 + col + 3] = " area";
                    
                    // Try getting related ranges
                    String rtFilename = jdxFiles[i_f].getPath() + ".rts";
                    String line;
                    try (
                            InputStream fis = new FileInputStream(rtFilename);
                            InputStreamReader isr = new InputStreamReader(fis/*, Charset.forName("UTF-8")*/);
                            BufferedReader br = new BufferedReader(isr);
                            ) {
                        // Try get min and max
                        Double min = Double.MIN_VALUE, max = Double.MAX_VALUE;
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("MIN_RT")) {
                                min = Double.valueOf(line.split("=")[1]);
                            }
                            if (line.startsWith("MAX_RT")) {
                                max = Double.valueOf(line.split("=")[1]);
                            }
                        }
                        logger.info("Range file found: " + jdxFiles[i_f].getName() + ".rts" + " ([" + min + ", " + max + "])");
                        rtSearchRanges[i_f] = Range.closed(min, max);
                    } catch (/*FileNotFoundException |*/ IOException e) {
                        rtSearchRanges[i_f] = Range.closed(Double.MIN_VALUE, Double.MAX_VALUE);
                    }
                }
                
            } catch (JCAMPException e) {
                String msg = "Error while pasring JDX compound file: " + jdxFiles[i_f].getName();
                logger.log(Level.WARNING, msg, e);
                setStatus(TaskStatus.ERROR);
                setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(e));
                return;
            }
            
            PeakList curPeakList = null;
            RawDataFile curRefRDF = null;
            try {

                setStatus(TaskStatus.PROCESSING);
                //finishedItemsTotal = 0;
                
                //int nb_rows = 0;
                progressItemNumberTotal = 0;
                for (int j=0; j< peakLists.length; ++j) {
                    progressItemNumberTotal += (1 + jdxCompounds.length) * peakLists[j].getNumberOfRows();
                }
                //finishedItemsTotal2 = nb_rows + jdxCompounds.length * ;

                
                final JDXCompound[] findCompounds = jdxCompounds;
//                final Range[] findRTranges = { rtSearchRangeC1, rtSearchRangeC2 };
//
//                String[] columnNames = { "Data file", 
//                        jdxComp1.getName(), /*jdxComp1.getName() +*/ " score", /*jdxComp1.getName() +*/ " rt", /*jdxComp1.getName() +*/ " area", 
//                        jdxComp2.getName(), /*jdxComp2.getName() +*/ " score" , /*jdxComp1.getName() +*/ " rt", /*jdxComp1.getName() +*/ " area", 
//                };
//                ScoresResultWindow window = null;
//
//                // Show result window only if no manual check is expected
//                if (!applyWithoutCheck) {
//                    window = new ScoresResultWindow(columnNames, this);
//                    window.setVisible(true);
//                }

                FileWriter fileWriter;
                BufferedWriter writer = null;
                if (/*applyWithoutCheck &&*/ !isEmptyFilename(blastOutputFilename)) {
                    // Open file
                    fileWriter = new FileWriter(blastOutputFilename);
                    writer = new BufferedWriter(fileWriter);
                }

                //              for (final PeakList peakList : peakLists) {
                for (int j=0; j< peakLists.length; ++j) {

                    PeakList peakList = peakLists[j];

//                    // Update window title.
//                    if (!applyWithoutCheck)
//                        window.setTitle(peakList.getName() + ": Searching for compounds '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'");

                    curPeakList = peakList;
                    curRefRDF = curPeakList.getRawDataFile(0);

                    // Identify the peak list rows starting from the biggest peaks.
                    
                    /**
                     * TODO: Try with "clone()" !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                     */
                    final PeakListRow[] rows = peakList.getRows()/*.clone()*/;
                    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

                    // Initialize counters.
                    int numItems = rows.length;

                    // Keep score for each std component for each peak/row of the current list.
                    Double[][] scoreMatrix = new Double[numItems][1+findCompounds.length]; // +1: Store row id first

                    // Process rows.
                    for (int finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {

                        PeakListRow a_row = rows[finishedItems];

                        // Retrieve results for each row.
                        scoreMatrix[finishedItems][0] = (double) finishedItems;
                        for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {
                            
                            // Ignore range or inside range.
                            if (ignoreRanges || rtSearchRanges[i].contains(a_row.getBestPeak().getRT())) {
                                
                                RawDataFile rdf = DataFileUtils.getAncestorDataFile(this.project, curRefRDF, false);
                                // If finding the ancestor file failed, just keep working on the current one 
                                if (rdf == null) { rdf = curRefRDF; }
                                double score = computeCompoundRowScore(rdf, curPeakList, a_row, findCompounds[i], this.useDetectedMzOnly);
                                if (score < minScore)
                                    scoreMatrix[finishedItems][i+1] = MIN_SCORE_ABSOLUTE;
                                else
                                    scoreMatrix[finishedItems][i+1] = score;
                                
                            } else {
                                // Out of range.
                                scoreMatrix[finishedItems][i+1] = MIN_SCORE_ABSOLUTE;
                            }
                        }

                        //finishedItemsTotal++;
                        progressItemNumber++;
                    }

                    // Add piaf to the list and display it in results window.
//                    if (!applyWithoutCheck)
//                        window.addNewListItem(peakList, findCompounds, scoreMatrix);
                    // If apply automatically, just get best scoring row id for each compound
                    // TODO: Make CSV export("blastOutputFilename") available even if not "applyWithoutCheck".
//                    else
                    if (!isCanceled()) {

                        // Clear if necessary/requested
                        if (bruteForceErase) {
                            for (int i=0; i < peakList.getNumberOfRows(); ++i) {
                                PeakListRow a_pl_row = peakList.getRows()[i];
                                for (final PeakIdentity id : a_pl_row.getPeakIdentities()) {
                                        a_pl_row.removePeakIdentity(id);
                                }
                            }
                        }
                        
                        if (isCanceled()) {
                            return;
                        }
                        
                        Vector<Object> objects = new Vector<Object>(/*columnNames.length*/);
                        for (int i=0; i < findCompounds.length; ++i) {

                            // Sort matrix for compound i (by score - descending order)
                            Double[][] mtx = new Double[scoreMatrix.length][scoreMatrix[0].length]; //scoreMatrix;
                            CollectionUtils.matrixCopy(scoreMatrix, mtx);
                            Arrays.sort(mtx, new ArrayComparator(i+1, false)); // +1: skip first column (row number)

                            PeakListRow bestRow = peakList.getRow((int) Math.round(mtx[0][0]));
                            double bestScore = mtx[0][i+1];

                            // Update identities
                            applyIdentityBF(peakList, findCompounds[i], bestRow.getID(), bestScore, bruteForceErase, useAsStdCompound);

                            // CSV export...
                            if (!isEmptyFilename(blastOutputFilename)) {

                                Feature bestPeak = bestRow.getBestPeak();

                                // CSV export: Write header
                                if (i == 0 && j == 0) {
                                    objects.addAll(Arrays.asList(columnNames));
                                    // Write to CSV
                                    for (Object obj: objects) {
                                        writer.write(obj.toString());
                                        writer.write(fieldSeparator);
                                    }
                                    writer.newLine();
                                    // Reset vector
                                    objects = new Vector<Object>();
                                }

                                // Piaf name
                                if (i == 0) {
                                    String name = peakList.getName();
                                    objects.add(name.substring(0, name.indexOf(' ')));
                                }
                                // Add some peak string representation
                                String peakToStr = "#" + bestRow.getID() + " @" 
                                        + rtFormat.format(bestPeak.getRT()) + " / " + areaFormat.format(bestPeak.getArea());
                                objects.add(peakToStr);
                                // Add score of selected peak
                                objects.add(mtx[0][i+1]);
                                // Add RT  of selected peak
                                objects.add(bestPeak.getRT());
                                // Add area  of selected peak
                                objects.add(bestPeak.getArea());

                            }

                            ////progressItemNumber += peakList.getNumberOfRows();
                            
                        }
                        setAllScores(peakList, findCompounds, scoreMatrix);


                        // CSV export: Write row
                        if (!isEmptyFilename(blastOutputFilename)) {
                            // Write to CSV
                            for (int k=0; k < objects.size(); ++k) {

                                Object obj = objects.get(k);

                                writer.write(obj.toString());
                                if (k != objects.size() - 1) {
                                    writer.write(fieldSeparator);
                                }
                            }
                            writer.newLine();
                        }

                    }
                    
                    
                    // Repaint the window to reflect the change in the peak list
                    // (Only if not in "headless" mode)
                    Desktop desktop = MZmineCore.getDesktop();
                    if (!(desktop instanceof HeadLessDesktop))
                        desktop.getMainWindow().repaint();

                }
                
                // CSV export: Close file
                if (/*applyWithoutCheck &&*/ !isEmptyFilename(blastOutputFilename)) {
                    writer.close();
                }


                if (!isCanceled()) {
                    setStatus(TaskStatus.FINISHED);
                }
            } catch (Throwable t) {
                StringWriter errors = new StringWriter();
                t.printStackTrace(new PrintWriter(errors));
                logger.log(Level.WARNING, "Error stack!!! > " + errors.toString());

                final String msg = "Could not search standard compounds for list '" 
                        + ((curPeakList != null) ? curPeakList.getName() : "null") + "'."
                        + "Error stack!!! > " + errors.toString();
                logger.log(Level.WARNING, msg, t);
                setStatus(TaskStatus.ERROR);
                setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
            }
        }
    }

//  /**
//  * Apply an identity (type JDX) to a peak list
//  * @param peaklist
//  * @param peak
//  */
// public void applyIdentity(PeakList peaklist, JDXCompound identity, int rowId, double score, boolean bruteForce) {
//     
//     // Do not overrule the identity if marked as "ref compound"
//     if (!bruteForce) {
//         // Check if identity is available (no "IS_REF" using it)
//         for (int i=0; i < peaklist.getNumberOfRows(); ++i) {
//             PeakListRow a_pl_row = peaklist.getRows()[i];
//             if (a_pl_row.getPreferredPeakIdentity().getName().equals(identity.getName())) {
//                 String isRefCompound = a_pl_row.getPreferredPeakIdentity().getPropertyValue(AlignedRowIdentity.PROPERTY_IS_REF);
//                 if (isRefCompound != null && isRefCompound.equals(AlignedRowIdentity.TRUE)) {
//                     // Not available: the identity cannot be touched
//                     return;
//                 }
//             }
//         }
//     }
//     
//     // If identity is available for change, feel free
//     for (int i=0; i < peaklist.getNumberOfRows(); ++i) {
//         PeakListRow a_pl_row = peaklist.getRows()[i];
//
//         // Add possible identities to peaks (need to renew for the sake of unicity)
//         JDXCompound unknownComp = JDXCompound.createUnknownCompound();
//         JDXCompound newIdentity = (JDXCompound) identity.clone();
//         // Remove current (make sure we replace current identity by a copy)
//         a_pl_row.removePeakIdentity(identity);
//         // Add clone and use as preferred
//         a_pl_row.addPeakIdentity(newIdentity, false);
//         a_pl_row.addPeakIdentity(unknownComp, false);
//
//         // Set new identity.
//         if (a_pl_row.getID() == rowId && score > MIN_SCORE_ABSOLUTE) {
//             // Save score
//             newIdentity.setPropertyValue(AlignedRowIdentity.PROPERTY_ID_SCORE, String.valueOf(score));
//             a_pl_row.setPreferredPeakIdentity(newIdentity);
//         }
//         // Erase / reset identity.
//         else if (a_pl_row.getPreferredPeakIdentity().getName().equals(newIdentity.getName())) {
//             unknownComp.setPropertyValue(AlignedRowIdentity.PROPERTY_ID_SCORE, String.valueOf(0.0));
//             a_pl_row.setPreferredPeakIdentity(unknownComp);
//         }
//
//         // Notify MZmine about the change in the project
//         // TODO: Get the "project" from the instantiator of this class instead.
//         MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
//         project.notifyObjectChanged(a_pl_row, false);
//     }
// }
    public void applyIdentityBF(PeakList peaklist, JDXCompound identity, int rowId, double score, boolean bruteForce, boolean canTagAsRef) {

        // Do not overrule the identity if marked as "ref compound"
        if (!bruteForce) {
            // Check if identity is available (no "IS_REF" using it)
            for (int i=0; i < peaklist.getNumberOfRows(); ++i) {
                PeakListRow a_pl_row = peaklist.getRows()[i];
                if (a_pl_row.getPreferredPeakIdentity() != null 
                        && a_pl_row.getPreferredPeakIdentity().getName().equals(identity.getName())) {
                    String isRefCompound = a_pl_row.getPreferredPeakIdentity().getPropertyValue(AlignedRowProps.PROPERTY_IS_REF);
                    if (isRefCompound != null && isRefCompound.equals(AlignedRowProps.TRUE)) {
                        // Not available: the identity cannot be touched
                        return;
                    }
                }
            }
        }

        // If identity is available for change, feel free
        //CustomJDXSearchTask.applyIdentity(peaklist, identity, rowId, score, bruteForce, canTagAsRef);
        for (int i=0; i < peaklist.getNumberOfRows(); ++i) {

            if (isCanceled()) {
                return;
            }
            
            PeakListRow a_pl_row = peaklist.getRows()[i];
            CustomJDXSearchTask.applyRowIdentity(a_pl_row, identity, rowId, score, /*bruteForce,*/ canTagAsRef);
            progressItemNumber++;
        }
    }
    /**
     * Apply an identity (type JDX) to a peak list
     * @param peaklist
     * @param peak
     */
    public static void applyIdentity(PeakList peaklist, JDXCompound identity, 
            int rowId, double score, /*boolean bruteForce,*/ boolean canTagAsRef) {
        
        for (int i=0; i < peaklist.getNumberOfRows(); ++i) {

            PeakListRow a_pl_row = peaklist.getRows()[i];
            CustomJDXSearchTask.applyRowIdentity(a_pl_row, identity, rowId, score, /*bruteForce,*/ canTagAsRef);
        }
    }
    public static void applyRowIdentity(PeakListRow row, JDXCompound identity, 
            int rowId, double score, /*boolean bruteForce,*/ boolean canTagAsRef) {

        // Add possible identities to peaks (need to renew for the sake of unicity)
        JDXCompound unknownComp = JDXCompound.createUnknownCompound();
        JDXCompound newIdentity = (JDXCompound) identity.clone();
        // Remove current (make sure we replace current identity by a copy)
        ////logger.info("Removed identity: " + identity + " for row: " + row.getID());
        row.removePeakIdentity(identity);
        // Add clone and use as preferred
        row.addPeakIdentity(newIdentity, false);
        row.addPeakIdentity(unknownComp, false);

        // Mark as ref compound (for later use in "JoinAlignerTask(GC)")
        if (canTagAsRef) {
            newIdentity.setPropertyValue(AlignedRowProps.PROPERTY_IS_REF, AlignedRowProps.TRUE);
        }
//        newIdentity.setPropertyValue(AlignedRowProps.PROPERTY_IS_REF, AlignedRowProps.TRUE);
        unknownComp.setPropertyValue(AlignedRowProps.PROPERTY_IS_REF, AlignedRowProps.FALSE);

        // Set new identity.
        if (row.getID() == rowId && score > MIN_SCORE_ABSOLUTE) {
            row.setPreferredPeakIdentity(newIdentity);
            ////logger.info("Set preferred identity: " + newIdentity + " for row: " + row.getID());
            // Save score
            newIdentity.setPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE, String.valueOf(score));
        }
        // Erase / reset identity.
        else if (
                //bruteForce || 
                row.getPreferredPeakIdentity().getName().equals(newIdentity.getName())) {

            unknownComp.setPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE, String.valueOf(0.0));
            row.setPreferredPeakIdentity(unknownComp);
            ////logger.info("Set preferred identity: " + unknownComp + " for row: " + row.getID());
        }

        // Notify MZmine about the change in the project
        // TODO: Get the "project" from the instantiator of this class instead.
        MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
        project.notifyObjectChanged(row, false);

    }
    public static void setAllScores(PeakList peaklist, JDXCompound[] findCompounds, Double[][] scoreMatrix) {
        for (int i=0; i < peaklist.getNumberOfRows(); ++i) {
            PeakListRow a_pl_row = peaklist.getRows()[i];
            
            //int id=0;
            for (int j=0; j < a_pl_row.getPeakIdentities().length; ++j) {
                for (int k=0; k < findCompounds.length; ++k) {
                    if (a_pl_row.getPeakIdentities()[j].getName().equals(findCompounds[k].getName())) {
                        SimplePeakIdentity curIdentity = (SimplePeakIdentity) a_pl_row.getPeakIdentities()[j];
                        // Score at row 'i' for compound 'k'
                        double score = scoreMatrix[i][k+1];
                        curIdentity.setPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE, String.valueOf(score));
                    }
                }
            }
        }
    }

    private double computeCompoundRowScore(final RawDataFile refRDF, final PeakList curPeakList, final PeakListRow row, 
            final JDXCompound compound, boolean useDetectedMzOnly)
            throws IOException {

        double score = 0.0;

        currentRow = row;
        currentPeakList = curPeakList;

        // Get scan of interest.
        Scan apexScan = refRDF.getScan(row.getBestPeak().getRepresentativeScanNumber());
//        logger.info(refRDF + " | Computing score for row of id: " + row.getID());
//        logger.info("Scan: " + apexScan);

        // Get scan m/z vector.
        double[] vec1 = new double[JDXCompound.MAX_MZ];
        Arrays.fill(vec1, 0.0);
        //DataPoint[] dataPoints = apexScan.getDataPoints();
        DataPoint[] dataPoints;
        if (useDetectedMzOnly)
            dataPoints = row.getBestPeak().getIsotopePattern().getDataPoints();
        else
            dataPoints = apexScan.getDataPoints();
        for (int j=0; j < dataPoints.length; ++j) {
            DataPoint dp = dataPoints[j];
            vec1[(int) Math.round(dp.getMZ())] = dp.getIntensity();
        }
        // Get std compound vector
        double[] vec2 = compound.getSpectrum();
        // Get similarity score.
        JDXCompound newComp = (JDXCompound) compound.clone();

        score = computeSimilarityScore(vec1, vec2);	

        // Adjust taking area in account (or not: areaMixFactor = 0.0).
        // TODO: Check if the following is pertinent with Pearson's correlation method
        //			(where scores are not normalized and can be negative).
        if (areaMixFactor > 0.0) {
            double maxArea = Double.MIN_VALUE;
            for (PeakListRow plr : currentPeakList.getRows()) {
                if (plr.getBestPeak().getArea() > maxArea) { maxArea = plr.getBestPeak().getArea(); }
            }
            score = (1.0 - areaMixFactor) * score + (areaMixFactor) * row.getBestPeak().getArea() / maxArea;
        }


//        logger.info("Score: " + score);

        newComp.setBestScore(score);

        return score;
    }

    // Compute chemical similarity score according to given similarity method
    private double computeSimilarityScore(double[] vec1, double[] vec2) {

        double simScore = 0.0;

        try {
            simScore = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, this.simMethodType);
        } catch (IllegalArgumentException e ) {
            logger.severe(e.getMessage());
        } 

        return simScore;
    }
    
    
    private boolean isEmptyFilename(File file) {
        return (file == null 
                || file.getPath().isEmpty() 
                || file.getPath().toLowerCase().equals("csv")
                || file.getPath().toLowerCase().equals(".csv"));
    }

    private void printMatrixToFile(Object[][] mtx, String filename) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(filename, "UTF-8");
            for (int i=0; i < mtx.length; ++i) {
                for (int j=0; j < mtx[i].length; ++j) {
                    writer.print(mtx[i][j] + " ");
                }
                writer.println();
            }
            writer.close();
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
