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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table.PeakListTable;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RangeUtils;

class CSVExportTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private PeakList[] peakLists;
    private int processedRows = 0, totalRows = 0;

    // parameter values
    private File fileName;
    private String plNamePattern = "{}";
    private static String csvFilename = "";
    private static String[] csvFilenames = null;

    private String fieldSeparator;
//    private ExportRowCommonElement[] commonElements;
//    private String[] identityElements;
//    private ExportRowDataFileElement[] dataFileElements;
//    private Boolean exportAllIDs;
    private Boolean exportSeparate, exportRtAverage, exportNumDetected, exportIdentities;
//    private String idSeparator;
    
    
    public CSVExportTask(ParameterSet parameters) {

        peakLists = parameters.getParameter(CSVExportParameters.peakLists)
                .getValue().getMatchingPeakLists();
        fileName = parameters.getParameter(CSVExportParameters.filename)
                .getValue();
        fieldSeparator = parameters.getParameter(
                CSVExportParameters.fieldSeparator).getValue();
//        commonElements = parameters.getParameter(
//                CSVExportParameters.exportCommonItems).getValue();
//        identityElements = parameters.getParameter(
//                CSVExportParameters.exportIdentityItems).getValue();
//        dataFileElements = parameters.getParameter(
//                CSVExportParameters.exportDataFileItems).getValue();
//        exportAllIDs = parameters.getParameter(
//                CSVExportParameters.exportAllIDs).getValue();
        exportSeparate = parameters.getParameter(
                CSVExportParameters.exportSeparate).getValue();
        exportRtAverage = parameters.getParameter(
                CSVExportParameters.exportRtAverage).getValue();
        exportNumDetected = parameters.getParameter(
                CSVExportParameters.exportNumDetected).getValue();
        exportIdentities = parameters.getParameter(
                CSVExportParameters.exportIdentities).getValue();
//        idSeparator = parameters.getParameter(
//                CSVExportParameters.idSeparator).getValue();
    }

    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    public String getTaskDescription() {
        //return "Exporting peak list " + peakLists + " to " + fileName;
        return "Exporting peak list(s) " + Arrays.toString(peakLists) + " to CSV file(s)";
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        // Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);
        
        // Total number of rows
        for (PeakList peakList: peakLists) {
            totalRows += peakList.getNumberOfRows();
        }

        // Process peak lists
        for (PeakList peakList: peakLists) {
            
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
                setErrorMessage("Could not open file " + curFile + " for writing.");
                return;
            }
            
            //exportPeakList(peakList, writer);
            boolean success = exportToCSV(peakList, curFile);

            // Cancel?
            if (isCanceled()) {
                return;
            }
            
            // Feedback
            if (success) {
                if (exportSeparate)
                    logger.log(Level.INFO, "Table saved to files: " 
                            + this.getCSVfilenames()[1] + " | " + this.getCSVfilenames()[2]);
                else
                    logger.log(Level.INFO, "Table saved to file: " + this.getCSVfilename());
            } else {
                if (exportSeparate)
                    logger.log(Level.INFO, "Could not write to files: " 
                            + this.getCSVfilenames()[1] + " | " + this.getCSVfilenames()[2]);
                else
                    logger.log(Level.INFO, "Could not write to file: " + this.getCSVfilename());
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
    
    boolean exportToCSV(PeakList peakList, File fileName) {

        String selectedPath = fileName.getPath();
        boolean separatedOutputs = exportSeparate;

        // Unified output file
        if (!selectedPath.endsWith(".csv"))
            selectedPath += ".csv";
        // Separated output files (rt + area)
        String basePath = null, rtPath = null, areaPath = null;
        if (separatedOutputs) {
            basePath = selectedPath.substring(0, selectedPath.length()-4);
            rtPath = basePath + "-rt.csv";
            areaPath = basePath + "-area.csv";
        }

        // Try 
        try {
            // Open file
            FileWriter fileWriter = new FileWriter(selectedPath);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            // Open rt + area files
            FileWriter fileWriterRt, fileWriterArea;
            BufferedWriter writerRt = null, writerArea = null;
            if (separatedOutputs) {
                fileWriterRt = new FileWriter(rtPath);
                writerRt = new BufferedWriter(fileWriterRt);
                fileWriterArea = new FileWriter(areaPath);
                writerArea = new BufferedWriter(fileWriterArea);
            }

            //**** Build rows (cell by cell)
            // Formating
            NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
            NumberFormat areaFormat = MZmineCore.getConfiguration().getIntensityFormat();

            // RT row + Identity row + Peak Shape row + 1 row per sample (=Piaf)
            int nbRows = 3 + peakList.getNumberOfRawDataFiles();
            // Row header col + 1 col per RT (=PeakListRow)
            int nbCols = 1 + peakList.getNumberOfRows();                

            int[] row2 = new int[peakList.getNumberOfRows()];
            
            ArrayList<Vector<Object>> rowsList = new ArrayList<Vector<Object>>();
            for (int i=0; i < nbRows; ++i) {

                Vector<Object> objects = new Vector<Object>(/*columnNames.length*/);

                for (int j=0; j < nbCols; ++j) {
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
                            objects.add("Peaks Detected");
                            break;
                        default:
                            RawDataFile rdf = peakList.getRawDataFiles()[i-3];
                            objects.add(rdf.getName().substring(0, rdf.getName().indexOf(" ")));
                            break;
                        }
                    } else {

                        PeakListRow a_pl_row = peakList.getRow(j-1);

                        switch (i) {
                        case 0:
                            objects.add(a_pl_row.getAverageRT());
                            break;
                        case 1:
                            objects.add(a_pl_row.getPreferredPeakIdentity());
                            break;
                        case 2:
                            objects.add("");
                            break;
                        default:
                            RawDataFile rdf = peakList.getRawDataFiles()[i-3];
                            Feature peak = a_pl_row.getPeak(rdf);//this.peakList.getPeak(j, rdf);
                            if (peak != null) {
                                objects.add("" + rtFormat.format(peak.getRT()) + " / " + areaFormat.format(peak.getArea()));
                                row2[j-1] += 1;
                            } else {
                                //objects.add("-");
                                objects.add("0");
                            }
                            break;
                        }

                    }
                }
                // Save row
                rowsList.add(objects);
            }
            
            // Update number of detected
            for (int i=0; i < row2.length; ++i) {
                //super.setValueAt(row2[i], 2, i+1);
                rowsList.get(2).set(i+1, row2[i]);
            }

                
            for (int i=0; i < rowsList.size(); ++i) {
                
                Vector<Object> objects = rowsList.get(i);
                
                //super.addRow(objects);
                boolean doWrite = true;
                if ((i == 0 && !exportRtAverage) || (i == 1 && !exportIdentities) || (i == 2 && !exportNumDetected))
                    doWrite = false;

                if (doWrite) {
                    // Write row into CSV file(s)
                    String sepStr = " / ";
                    for (int j=0; j < objects.size(); ++j) {

                        Object obj = objects.get(j);

                        String str = (obj != null) ? obj.toString() : "";

                        writer.write(str);
                        if (separatedOutputs) {
                            String rtStr, areaStr;

                            int sepIdx = str.lastIndexOf(sepStr);
                            if (sepIdx != -1) {
                                rtStr = str.substring(0, sepIdx);
                                areaStr = str.substring(sepIdx + sepStr.length(), str.length());
                                writerRt.write(rtStr);
                                writerArea.write(areaStr);
                            } else {
                                writerRt.write(str);
                                writerArea.write(str);
                            }
                        }

                        if (j != objects.size() - 1) {
                            writer.write(fieldSeparator);
                            if (separatedOutputs) {
                                writerRt.write(fieldSeparator);
                                writerArea.write(fieldSeparator);
                            }
                        }
                    }
                    writer.newLine();
                    if (separatedOutputs) {
                        writerRt.newLine();
                        writerArea.newLine();                        
                    }
                    ++processedRows;
                }

            }


            // Close file
            writer.close();
            if (separatedOutputs) {
                writerRt.close();
                writerArea.close();                                            
            }
            
          csvFilename = selectedPath;
          csvFilenames = new String[] { selectedPath, rtPath, areaPath };

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
