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

package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
//import net.sf.mzmine.modules.peaklistmethods.identification.dbsearch.ResultWindow;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.ScoresResultTableModel.ArrayComparator;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.ScoresResultTableModel.ComboboxPeak;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.DataFileUtils;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;


// Dot
import org.apache.commons.math.linear.ArrayRealVector;
//Pearson
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;

import com.google.common.collect.Range;
import com.mysql.fabric.xmlrpc.base.Array;



public class JDXCompoundsIdentificationSingleTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(JDXCompoundsIdentificationSingleTask.class.getName());

    private final MZmineProject project;

    // Minimum abundance.
    private static final double MIN_ABUNDANCE = 0.001;

    // Counters.
    private int finishedItemsTotal;
    private int finishedItems;
    private int numItems;

    private final PeakList[] peakLists;

    private File jdxFileC1, jdxFileC2;
    private JDXCompound jdxComp1, jdxComp2;
    private Range<Double> rtSearchRangeC1, rtSearchRangeC2;
    private SimilarityMethodType simMethodType;
    private double areaMixFactor;
    private boolean applyWithoutCheck;
    private File blastOutputFilename;
    private String fieldSeparator;

    private PeakListRow currentRow;
    private PeakList currentPeakList;

    private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    private NumberFormat areaFormat = MZmineCore.getConfiguration().getIntensityFormat();



    /**
     * Create the identification task.
     * 
     * @param parameters
     *            task parameters.
     * @param list
     *            peak list to operate on.
     */
    @SuppressWarnings("unchecked")
    JDXCompoundsIdentificationSingleTask(final MZmineProject project, 
            final ParameterSet parameters, final PeakList[] lists) {

        this.project = project;

        peakLists = lists;
        numItems = 0;
        finishedItems = 0;
        currentRow = null;

        jdxFileC1 = parameters.getParameter(JDXCompoundsIdentificationParameters.JDX_FILE_C1).getValue();
        jdxFileC2 = parameters.getParameter(JDXCompoundsIdentificationParameters.JDX_FILE_C2).getValue();
        rtSearchRangeC1 = parameters.getParameter(JDXCompoundsIdentificationParameters.RT_SEARCH_WINDOW_C1).getValue();
        rtSearchRangeC2 = parameters.getParameter(JDXCompoundsIdentificationParameters.RT_SEARCH_WINDOW_C2).getValue();
        simMethodType = parameters.getParameter(JDXCompoundsIdentificationParameters.SIMILARITY_METHOD).getValue();
        areaMixFactor = parameters.getParameter(JDXCompoundsIdentificationParameters.MIX_FACTOR).getValue();
        applyWithoutCheck = parameters.getParameter(JDXCompoundsIdentificationParameters.APPLY_WITHOUT_CHECK).getValue();
        blastOutputFilename = parameters.getParameter(JDXCompoundsIdentificationParameters.BLAST_OUTPUT_FILENAME).getValue();
        fieldSeparator = parameters.getParameter(JDXCompoundsIdentificationParameters.FIELD_SEPARATOR).getValue();

        jdxComp1 = JDXCompound.parseJDXfile(jdxFileC1);
        jdxComp2 = JDXCompound.parseJDXfile(jdxFileC2);


        LOG.info("JDX parsing: " + jdxComp1);
    }

    @Override
    public double getFinishedPercentage() {

        //return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
        return numItems == 0 ? 0.0 : (double) finishedItemsTotal / (double) (finishedItems * numItems * peakLists.length);
    }

    @Override
    public String getTaskDescription() {

        return "Identification of standard peaks in "
                + "selected peak lists"
                + " using " + " standards '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'";
    }

    @Override
    public void run() {

        if (!isCanceled()) {
            PeakList curPeakList = null;
            RawDataFile curRefRDF = null;
            try {

                setStatus(TaskStatus.PROCESSING);
                finishedItemsTotal = 0;

                final JDXCompound[] findCompounds = { (JDXCompound) jdxComp1/*.clone()*/, (JDXCompound) jdxComp2/*.clone()*/ };
                final Range[] findRTranges = { rtSearchRangeC1, rtSearchRangeC2 };

                String[] columnNames = { "Data file", 
                        jdxComp1.getName(), /*jdxComp1.getName() +*/ " score", /*jdxComp1.getName() +*/ " rt", /*jdxComp1.getName() +*/ " area", 
                        jdxComp2.getName(), /*jdxComp2.getName() +*/ " score" , /*jdxComp1.getName() +*/ " rt", /*jdxComp1.getName() +*/ " area", 
                };
                ScoresResultWindow window = null;

                // Show result window only if no manual check is expected
                if (!applyWithoutCheck) {
                    window = new ScoresResultWindow(columnNames, this);
                    window.setVisible(true);
                }

                FileWriter fileWriter;
                BufferedWriter writer = null;
                if (applyWithoutCheck && blastOutputFilename != null) {
                    // Open file
                    fileWriter = new FileWriter(blastOutputFilename);
                    writer = new BufferedWriter(fileWriter);
                }

                //              for (final PeakList peakList : peakLists) {
                for (int j=0; j< peakLists.length; ++j) {

                    PeakList peakList = peakLists[j];

                    // Update window title.
                    if (!applyWithoutCheck)
                        window.setTitle(peakList.getName() + ": Searching for compounds '" + jdxComp1.getName() + "' and '" + jdxComp2.getName() + "'");

                    curPeakList = peakList;
                    curRefRDF = curPeakList.getRawDataFile(0);

                    // Identify the peak list rows starting from the biggest peaks.
                    final PeakListRow[] rows = peakList.getRows();
                    //**Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));
                    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

                    // Initialize counters.
                    numItems = rows.length;

                    // Keep score for each std component for each peak/row of the current list.
                    Double[][] scoreMatrix = new Double[numItems][1+findCompounds.length]; // +1: Store row id first

                    // Process rows.
                    for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {

                        PeakListRow a_row = rows[finishedItems];

                        // Retrieve results for each row.
                        scoreMatrix[finishedItems][0] = (double) finishedItems;
                        for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {
                            if (findRTranges[i].contains(a_row.getBestPeak().getRT()))
                                scoreMatrix[finishedItems][i+1] = computeCompoundRowScore(DataFileUtils.getAncestorDataFile(this.project, curRefRDF, false), curPeakList, a_row, findCompounds[i]);
                            else
                                // Out of range.
                                scoreMatrix[finishedItems][i+1] = 0.0;
                        }

                        finishedItemsTotal++;
                    }

                    // Add piaf to the list and display it in results window.
                    if (!applyWithoutCheck)
                        window.addNewListItem(peakList, findCompounds, scoreMatrix);

                    //                    printMatrixToFile(scoreMatrix, "scores_" + (simMethodType == SimilarityMethodType.DOT ? "dot" : "pearson") + "_" + peakList.getName() + ".txt");
                    //                    for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {
                    //                        Double[][] mtx_sorted = new Double[scoreMatrix.length][scoreMatrix[0].length]; //scoreMatrix;
                    //                        arrayCopy(scoreMatrix, mtx_sorted);
                    //                        Arrays.sort(mtx_sorted, new ScoresResultTableModel.ArrayComparator(i+1, false));
                    //                        printMatrixToFile(mtx_sorted, "scores_" + (simMethodType == SimilarityMethodType.DOT ? "dot" : "pearson") + "_" + i + "_" + peakList.getName() + ".txt");
                    //                    }

                    // If apply automatically, just get best scoring row id for each compound
                    // TODO: Make CSV export("blastOutputFilename") available even if not "applyWithoutCheck".
                    else if (!isCanceled()) {

                        Vector<Object> objects = new Vector<Object>(/*columnNames.length*/);
                        for (int i=0; i < findCompounds.length; ++i) {

                            // Sort matrix for compound i (by score - descending order)
                            Double[][] mtx = new Double[scoreMatrix.length][scoreMatrix[0].length]; //scoreMatrix;
                            CollectionUtils.matrixCopy(scoreMatrix, mtx);
                            Arrays.sort(mtx, new ArrayComparator(i+1, false)); // +1: skip first column (row number)

                            PeakListRow bestRow = peakList.getRow((int) Math.round(mtx[0][0]));

                            // Update identities
                            applyIdentity(peakList, findCompounds[i], bestRow.getID());

                            // CSV export...
                            if (blastOutputFilename != null) {

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

                        }

                        // CSV export: Write row
                        if (applyWithoutCheck && blastOutputFilename != null) {
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
                }
                
                // CSV export: Close file
                if (applyWithoutCheck && blastOutputFilename != null) {
                    writer.close();
                }


                if (!isCanceled()) {
                    setStatus(TaskStatus.FINISHED);
                }
            } catch (Throwable t) {
                StringWriter errors = new StringWriter();
                t.printStackTrace(new PrintWriter(errors));
                LOG.log(Level.WARNING, "Error stack!!! > " + errors.toString());

                final String msg = "Could not search standard compounds for list '" 
                        + ((curPeakList != null) ? curPeakList.getName() : "null") + "'."
                        + "Error stack!!! > " + errors.toString();
                LOG.log(Level.WARNING, msg, t);
                setStatus(TaskStatus.ERROR);
                setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
            }
        }
    }

    /**
     * Apply an identity (type JDX) to a peak list
     * @param peaklist
     * @param peak
     */
    public void applyIdentity(PeakList peaklist, JDXCompound identity, int rowId) {
        for (int i=0; i < peaklist.getNumberOfRows(); ++i) {
            PeakListRow a_pl_row = peaklist.getRows()[i];

            JDXCompound unknownComp = new JDXCompound("Unknown", null, null, null, null);

            // Add possible identities to peaks
            a_pl_row.addPeakIdentity(identity, false);
            a_pl_row.addPeakIdentity(unknownComp, false);

            // Set new identity.
            if (a_pl_row.getID() == rowId) {
                a_pl_row.setPreferredPeakIdentity(identity);
            }
            // Erase / reset identity.
            else if (a_pl_row.getPreferredPeakIdentity().getName().equals(identity.getName())) {
                a_pl_row.setPreferredPeakIdentity(unknownComp);
            }

            // Notify MZmine about the change in the project
            // TODO: Get the "project" from the instantiator of this class instead.
            MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
            project.notifyObjectChanged(i, false);
        }
    }

    private double computeCompoundRowScore(final RawDataFile refRDF, final PeakList curPeakList, final PeakListRow row, final JDXCompound compound)
            throws IOException {

        double score = 0.0;

        currentRow = row;
        currentPeakList = curPeakList;

        // Get scan of interest.
        Scan apexScan = refRDF.getScan(row.getBestPeak().getRepresentativeScanNumber());
        LOG.info(refRDF + " | Computing score for row of id: " + row.getID());
        LOG.info("Scan: " + apexScan);

        // Get scan m/z vector.
        double[] vec1 = new double[JDXCompound.MAX_MZ];
        Arrays.fill(vec1, 0.0);
        DataPoint[] dataPoints = apexScan.getDataPoints();
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


        LOG.info("Score: " + score);

        newComp.setBestScore(score);

        return score;
    }

    private double computeSimilarityScore(double[] vec1, double[] vec2) {

        double simScore = 0.0;

        try {

            if (this.simMethodType == SimilarityMethodType.DOT) {
                double[] vec1_norm = new double[vec1.length];
                double[] vec2_norm = new double[vec2.length];

                double div1 = 0.0, div2 = 0.0;
                for (int i=0; i < vec1.length; ++i) {
                    div1 += vec1[i] * vec1[i];
                    div2 += vec2[i] * vec2[i];
                }
                for (int i=0; i < vec1.length; ++i) {
                    vec1_norm[i] = vec1[i] / Math.sqrt(div1);
                    vec2_norm[i] = vec2[i] / Math.sqrt(div2);
                }
                simScore = (new ArrayRealVector(vec1_norm)).dotProduct(vec2_norm);
            } else if (this.simMethodType == SimilarityMethodType.PEARSON) {
                simScore = new PearsonsCorrelation().correlation(vec1, vec2);
            }
        } catch (IllegalArgumentException e ) {
            LOG.severe("Failed to compute similarity score for vec1.length=" + vec1.length + " and vec2.length=" + vec2.length);
        } 

        return simScore;
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


    /**
     * Search the database for the peak's identity.
     * 
     * @param row
     *            the peak list row.
     * @throws IOException
     *             if there are i/o problems.
     */
    private void retrieveIdentification(final PeakListRow row)
            throws IOException {

        currentRow = row;

        //		// Determine peak charge.
        //		final Feature bestPeak = row.getBestPeak();
        //		int charge = bestPeak.getCharge();
        //		if (charge <= 0) {
        //			charge = 1;
        //		}
        //
        //		// Calculate mass value.
        //		final double massValue = (row.getAverageMZ() - ionType.getAddedMass())
        //				* (double) charge;
        //
        //		// Isotope pattern.
        //		final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

        // Process each one of the result ID's.
        //		final String[] findCompounds = gateway.findCompounds(massValue,
        //		mzTolerance, numOfResults, db.getParameterSet());
        final JDXCompound[] findCompounds = { jdxComp1, jdxComp2 };
        for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

            //			final DBCompound compound = gateway.getCompound(findCompounds[i],
            //			db.getParameterSet());
            final JDXCompound compound = findCompounds[i];

            //			final String formula = compound
            //					.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
            //
            //			// If required, check isotope score.
            //			if (isotopeFilter && rowIsotopePattern != null && formula != null) {
            //
            //				// First modify the formula according to ionization.
            //				final String adjustedFormula = FormulaUtils.ionizeFormula(
            //						formula, ionType, charge);
            //
            //				LOG.finest("Calculating isotope pattern for compound formula "
            //						+ formula + " adjusted to " + adjustedFormula);
            //
            //				// Generate IsotopePattern for this compound
            //				final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
            //						.calculateIsotopePattern(adjustedFormula,
            //								MIN_ABUNDANCE, charge, ionType.getPolarity());
            //
            //				// Check isotope pattern match
            //				boolean check = IsotopePatternScoreCalculator.checkMatch(
            //						rowIsotopePattern, compoundIsotopePattern,
            //						isotopeFilterParameters);
            //
            //				if (!check)
            //					continue;
            //			}

            // Add the retrieved identity to the peak list row
            row.addPeakIdentity(compound, false);

            // Notify the GUI about the change in the project
            this.project.notifyObjectChanged(row, false);
            MZmineCore.getDesktop().getMainWindow().repaint();
        }
    }
}