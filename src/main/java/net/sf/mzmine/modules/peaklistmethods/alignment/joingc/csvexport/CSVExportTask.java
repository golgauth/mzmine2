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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.csvexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.collections.CollectionUtils;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.AlignedRowIdentity;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGcModule;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RawDataFileSorter;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table.PeakListTable;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.RangeUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class CSVExportTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList[] peakLists;
    private int processedRows = 0, totalRows = 0;

    // parameter values
    private File fileName;
    private String plNamePattern = "{}";
    private static String csvFilename = "";
    private static String[] csvFilenames = null;

    private String fieldSeparator;
    private Boolean exportSeparate, exportRtAverage, exportNumDetected,
            exportIdentities;

    private static final String SEP_STR_CSV = " / ";

    public CSVExportTask(ParameterSet parameters) {

        peakLists = parameters.getParameter(CSVExportParameters.peakLists)
                .getValue().getMatchingPeakLists();
        fileName = parameters.getParameter(CSVExportParameters.filename)
                .getValue();
        fieldSeparator = parameters.getParameter(
                CSVExportParameters.fieldSeparator).getValue();
        exportSeparate = parameters.getParameter(
                CSVExportParameters.exportSeparate).getValue();
        exportRtAverage = parameters.getParameter(
                CSVExportParameters.exportRtAverage).getValue();
        exportNumDetected = parameters.getParameter(
                CSVExportParameters.exportNumDetected).getValue();
        exportIdentities = parameters.getParameter(
                CSVExportParameters.exportIdentities).getValue();
    }

    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    public String getTaskDescription() {
        return "Exporting peak list(s) " + Arrays.toString(peakLists)
                + " to CSV file(s)";
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Total number of rows
        for (PeakList peakList : peakLists) {
            totalRows += peakList.getNumberOfRows();
        }

        // Process peak lists
        for (PeakList peakList : peakLists) {

            // Filename
            File curFile = fileName;
            if (substitute) {
                // Substitute
                String newFilename = fileName.getPath().replaceAll(
                        Pattern.quote(plNamePattern), peakList.getName());
                // Cleanup from illegal filename characters
                newFilename = newFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
                curFile = new File(newFilename);
            }

            // Open file
            FileWriter writer;
            try {
                writer = new FileWriter(curFile);
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not open file " + curFile
                        + " for writing.");
                return;
            }

            boolean success = exportToCSV(peakList, curFile);

            // Cancel?
            if (isCanceled()) {
                return;
            }

            // Feedback
            if (success) {
                if (exportSeparate)
                    logger.log(
                            Level.INFO,
                            "Table saved to files: "
                                    + this.getCSVfilenames()[1] + " | "
                                    + this.getCSVfilenames()[2]);
                else
                    logger.log(Level.INFO,
                            "Table saved to file: " + this.getCSVfilename());
            } else {
                if (exportSeparate)
                    logger.log(
                            Level.INFO,
                            "Could not write to files: "
                                    + this.getCSVfilenames()[1] + " | "
                                    + this.getCSVfilenames()[2]);
                else
                    logger.log(Level.INFO,
                            "Could not write to file: " + this.getCSVfilename());
            }

            // Close file
            try {
                writer.close();
            } catch (Exception e) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not close file " + curFile);
                return;
            }

            // If peak list substitution pattern wasn't found,
            // treat one peak list only
            if (!substitute)
                break;
        }

        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);

    }

    boolean exportToCSV(final PeakList peakList, final File fileName) {

        // Sort rows by ascending RT
        final PeakListRow[] peakListRows = peakList.getRows().clone();
        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.RT,
                SortingDirection.Ascending));

        String selectedPath = fileName.getPath();
        boolean separatedOutputs = exportSeparate;

        // Unified output file
        if (!selectedPath.endsWith(".csv"))
            selectedPath += ".csv";

        // Separated output files (rt + area + ...)
        String basePath = null;
        String[] suffixes = new String[] { "-rt-orig.csv", "-rt-reca.csv",
                "-area.csv", "-ident.csv", "-scor.csv" };
        int nbFiles = suffixes.length;
        String[] filenames = new String[nbFiles];
        FileWriter[] fileWriters = new FileWriter[nbFiles];
        BufferedWriter[] buffWriters = new BufferedWriter[nbFiles];

        if (separatedOutputs) {
            basePath = selectedPath.substring(0, selectedPath.length() - 4);
            for (int i = 0; i < nbFiles; ++i) {
                filenames[i] = basePath + suffixes[i];
            }
        }

        // Try
        try {
            // Open merged file
            FileWriter fileWriter = new FileWriter(selectedPath);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            // Open rt + area + ... files
            if (separatedOutputs) {
                for (int i = 0; i < nbFiles; ++i) {
                    fileWriters[i] = new FileWriter(filenames[i]);
                    buffWriters[i] = new BufferedWriter(fileWriters[i]);
                }
            }

            // **** Build rows (cell by cell)
            // Formatting
            NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
            NumberFormat areaFormat = MZmineCore.getConfiguration()
                    .getIntensityFormat();

            int nbHeaderRows = 4;
            // RT row + Identity row + Peak Shape row + 1 row per sample (=Piaf)
            int nbRows = nbHeaderRows + peakList.getNumberOfRawDataFiles();
            // Row header col + 1 col per RT (=PeakListRow)
            int nbCols = 1 + peakList.getNumberOfRows();

            int[] arrNbDetected = new int[peakList.getNumberOfRows()];
            PeakIdentity[] mainIdentities = new PeakIdentity[peakList
                    .getNumberOfRows()];

            String strAdjustedRTs = "", strIdentities = "", strScores = "";
            String[] arrAdjustedRTs = null, arrIdentities = null, arrScores = null;

            // Build reference RDFs index
            RawDataFile[] rdf_sorted = peakList.getRawDataFiles().clone();
            Arrays.sort(rdf_sorted, new RawDataFileSorter(
                    SortingDirection.Ascending));

            ArrayList<Vector<Object>> rowsList = new ArrayList<Vector<Object>>();
            for (int i = 0; i < nbRows; ++i) {

                Vector<Object> objects = new Vector<Object>(/*
                                                             * columnNames.length
                                                             */);

                for (int j = 0; j < nbCols; ++j) {
                    // Set row headers
                    if (j == 0) {
                        switch (i) {
                        case 0:
                            objects.add("Average RT");
                            break;
                        case 1:
                            objects.add("Identity");
                            break;
                        case 2:
                            objects.add("Identities info");
                            break;
                        case 3:
                            objects.add("Peaks detected");
                            break;
                        default:
                            RawDataFile rdf = peakList.getRawDataFiles()[i
                                    - nbHeaderRows];
                            objects.add(rdf.getName().substring(0,
                                    rdf.getName().indexOf(" ")));
                            break;
                        }
                    } else {

                        PeakListRow a_pl_row = peakListRows[j - 1];

                        switch (i) {
                        case 0:
                            objects.add(a_pl_row.getAverageRT());
                            break;
                        case 1:
                            objects.add(a_pl_row.getPreferredPeakIdentity());
                            break;
                        case 2:
                            // Do nothing, update bellow
                            objects.add("");
                            break;
                        case 3:
                            // Do nothing, update bellow
                            objects.add("");
                            break;
                        default:

                            RawDataFile rdf = peakList.getRawDataFiles()[i
                                    - nbHeaderRows];
                            Feature peak = a_pl_row.getPeak(rdf);
                            if (peak != null) {

                                PeakIdentity mainIdentity = a_pl_row
                                        .getPreferredPeakIdentity();

                                if (mainIdentity != null) {

                                    strAdjustedRTs = ((SimplePeakIdentity) mainIdentity)
                                            .getPropertyValue(AlignedRowIdentity.PROPERTY_RTS);
                                    strIdentities = ((SimplePeakIdentity) mainIdentity)
                                            .getPropertyValue(AlignedRowIdentity.PROPERTY_IDENTITIES_NAMES);
                                    strScores = ((SimplePeakIdentity) mainIdentity)
                                            .getPropertyValue(AlignedRowIdentity.PROPERTY_IDENTITIES_SCORES);

                                    // More than one rdf (align peak list)
                                    if (peakList.getRawDataFiles().length > 1
                                            && (strAdjustedRTs != null && strIdentities != null)) {

                                        if (strAdjustedRTs != null)
                                            arrAdjustedRTs = strAdjustedRTs
                                                .split(AlignedRowIdentity.IDENTITY_SEP,
                                                        -1);
                                        if (strIdentities != null)
                                            arrIdentities = strIdentities
                                                .split(AlignedRowIdentity.IDENTITY_SEP,
                                                        -1);
                                        if (strScores != null)
                                            arrScores = strScores.split(AlignedRowIdentity.IDENTITY_SEP, -1);

                                        int rdf_idx = Arrays.asList(rdf_sorted)
                                                .indexOf(rdf);
                                        String peakAjustedRT = null;
                                        if (strAdjustedRTs != null)
                                            peakAjustedRT = arrAdjustedRTs[rdf_idx];
                                        String peakIdentity = arrIdentities[rdf_idx];

                                        // Handle gap filled peaks
                                        if (peak.getFeatureStatus() == FeatureStatus.ESTIMATED) {
                                            peakAjustedRT = "";
                                            peakIdentity = "";
                                        }
                                        String score = arrScores[rdf_idx];
                                        String strScore = "";
                                        if (score != null && !score.isEmpty())
                                            strScore = SEP_STR_CSV + score;

                                        objects.add(rtFormat.format(
                                                peak.getRT())
                                                + SEP_STR_CSV
                                                + ((strAdjustedRTs != null) ? " [" + peakAjustedRT + "]" + SEP_STR_CSV : "")
                                                + areaFormat.format(peak.getArea())
                                                + SEP_STR_CSV + peakIdentity
                                                + strScore);

                                    }
                                    // Handle regular single rdf peak list
                                    else {
                                        objects.add(rtFormat.format(peak
                                                .getRT())
                                                + SEP_STR_CSV
                                                + areaFormat.format(peak
                                                        .getArea()));
                                    }
                                    //
                                    mainIdentities[j - 1] = mainIdentity;

                                }
                                arrNbDetected[j - 1] += 1;
                            } else {
                                objects.add(JoinAlignerGcModule.MISSING_PEAK_VAL);
                            }
                            break;
                        }

                    }
                }
                // Save row
                rowsList.add(objects);
            }

            // Update number of detected peaks
            for (int i = 0; i < peakList.getNumberOfRows(); ++i) {
                rowsList.get(nbHeaderRows - 1).set(i + 1, arrNbDetected[i]);
            }
            // Update main identity + identities info
            for (int i = 0; i < peakList.getNumberOfRows(); ++i) {
                PeakIdentity mainIdentity = mainIdentities[i];
                String strIdentities2 = "";
                if (mainIdentity != null) {
                    // Set identities info string (leave blank if single
                    // rdf/sample peak list)
                    if (peakList.getRawDataFiles().length > 1) {
                        strIdentities2 = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowIdentity.PROPERTY_IDENTITIES_NAMES);
                        strScores = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowIdentity.PROPERTY_IDENTITIES_SCORES);
                        if (strIdentities2 != null) {
                            arrIdentities = strIdentities2.split(AlignedRowIdentity.IDENTITY_SEP, -1);
                            arrScores = strScores.split(AlignedRowIdentity.IDENTITY_SEP, -1);
                            
                            //--- Get sums
                            
                            Hashtable<String, Double> scoreAvgMap = new Hashtable<String, Double>();
                            for (int i_s=0; i_s < arrIdentities.length; ++i_s) {
                                
                                // Skip 'no identity' peaks (that are not even 'Unknown') 
                                if (arrIdentities[i_s] == null || arrIdentities[i_s].isEmpty())
                                    continue;
                                
                                if (!scoreAvgMap.keySet().contains(arrIdentities[i_s])) {
                                    scoreAvgMap.put(arrIdentities[i_s], Double.valueOf(arrScores[i_s]));
                                } else {
                                    double curSum = scoreAvgMap.get(arrIdentities[i_s]);
                                    scoreAvgMap.put(arrIdentities[i_s], curSum + Double.valueOf(arrScores[i_s]));
                                }
                            }
                            
                            //---
                            
                            Set<String> aSet = new HashSet<String>(
                                    Arrays.asList(arrIdentities));
                            strIdentities2 = "";
                            for (String str : aSet) {
                                if (str != null && !str.isEmpty()) {
                                    //int cardinality = CollectionUtils.cardinality(str, Arrays.asList(arrIdentities));
                                    //strIdentities2 += str + " (" + cardinality + ")" + AlignedRowIdentity.IDENTITY_SEP;
                                    double avgScore = scoreAvgMap.get(str) / (double) peakList.getRawDataFiles().length; // / (double) cardinality;
                                    strIdentities2 += str + " (" + /*rtFormat.format(*/ avgScore /*)*/ + ")" + AlignedRowIdentity.IDENTITY_SEP;
                                }
                            }
                            strIdentities2 = strIdentities2.substring(0, strIdentities2.length() - 1);
                        }
                    } else {
                        //strIdentities2 = mainIdentity.getName() + " (1)";
                        strIdentities2 = mainIdentity.getName() + " (" + mainIdentity.getPropertyValue(AlignedRowIdentity.PROPERTY_ID_SCORE) + ")";
                    }
                    // Set most frequent identity
                    rowsList.get(nbHeaderRows - 3).set(i + 1, mainIdentity);
                }
                rowsList.get(nbHeaderRows - 2).set(i + 1, strIdentities2);
            }

            for (int i = 0; i < rowsList.size(); ++i) {

                Vector<Object> objects = rowsList.get(i);

                // super.addRow(objects);
                boolean doWrite = true;
                if ((i == 0 && !exportRtAverage)
                        || (i == 1 && !exportIdentities)
                        || (i == 2 && !exportNumDetected))
                    doWrite = false;

                if (doWrite) {
                    // Write row into CSV file(s)
                    for (int j = 0; j < objects.size(); ++j) {

                        Object obj = objects.get(j);

                        String str = (obj != null) ? obj.toString() : "";

                        writer.write(str);
                        if (separatedOutputs) {
                            String[] arrStr = str.split(SEP_STR_CSV);
                            if (arrStr.length == nbFiles) {
                                for (int k = 0; k < nbFiles; ++k) {
                                    buffWriters[k].write(arrStr[k]);
                                }
                            } else {
                                for (int k = 0; k < nbFiles; ++k) {
                                    buffWriters[k].write(str);
                                }
                            }
                        }

                        if (j != objects.size() - 1) {
                            writer.write(fieldSeparator);
                            if (separatedOutputs) {
                                for (int k = 0; k < nbFiles; ++k) {
                                    buffWriters[k].write(fieldSeparator);
                                }
                            }
                        }
                    }
                    writer.newLine();
                    if (separatedOutputs) {
                        for (int k = 0; k < nbFiles; ++k) {
                            buffWriters[k].newLine();
                        }
                    }
                    ++processedRows;
                }

            }

            // Close file
            writer.close();
            if (separatedOutputs) {
                for (int k = 0; k < nbFiles; ++k) {
                    buffWriters[k].close();
                }
            }

            csvFilename = selectedPath;
            csvFilenames = new String[1 + nbFiles]; // { selectedPath, rtPath,
                                                    // areaPath };
            csvFilenames[0] = selectedPath;
            for (int k = 1; k < csvFilenames.length; ++k) {
                csvFilenames[k] = filenames[k - 1];
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    String getCSVfilename() {
        return CSVExportTask.csvFilename;
    }

    String[] getCSVfilenames() {
        return CSVExportTask.csvFilenames;
    }

    private String escapeStringForCSV(final String inputString) {

        if (inputString == null)
            return "";

        // Remove all special characters (particularly \n would mess up our CSV
        // format).
        String result = inputString.replaceAll("[\\p{Cntrl}]", " ");

        // If the text contains fieldSeparator, we will add
        // parenthesis
        if (result.contains(fieldSeparator)) {
            result = "\"" + result.replaceAll("\"", "'") + "\"";
        }

        return result;
    }
}
