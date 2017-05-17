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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import org.jcamp.parser.JCAMPException;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.AlignedRowProps;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCParameters;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGCTask;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RowVsRowScoreGC;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.SimpleScanClonable;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.csvexport.CSVExportParameters;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.csvexport.CSVExportTask;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.FormulaPredictionModule;
import net.sf.mzmine.modules.peaklistmethods.identification.nist.NistMsSearchModule;
import net.sf.mzmine.modules.peaklistmethods.identification.nistgc.NistMsSearchGCParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.nistgc.NistMsSearchGCTask;
import net.sf.mzmine.modules.peaklistmethods.identification.onlinedbsearch.OnlineDBSearchModule;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.SimilarityMethodType;
import net.sf.mzmine.modules.rawdatamethods.peakpicking.manual.ManualPeakPickerModule;
import net.sf.mzmine.modules.visualization.intensityplot.IntensityPlotModule;
import net.sf.mzmine.modules.visualization.peaklisttable.export.IsotopePatternExportModule;
import net.sf.mzmine.modules.visualization.peaklisttable.export.MSMSExportModule;
//import net.sf.mzmine.modules.visualization.peaklisttable.table.CommonColumnType;
//import net.sf.mzmine.modules.visualization.peaklisttable.table.DataFileColumnType;
//import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTable;
//import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTableColumnModel;
import net.sf.mzmine.modules.visualization.peaksummary.PeakSummaryVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.threed.ThreeDVisualizerModule;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerModule;
import net.sf.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;

import com.google.common.collect.Range;

/**
 * Peak-list table pop-up menu.
 */
public class PeakListTablePopupMenu extends JPopupMenu implements
        ActionListener {

    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final PeakListTable table;
    private final PeakList peakList;
    private final PeakListFullTableModel model;

//    private final JMenu showMenu;
//    private final JMenu searchMenu;
//    private final JMenu idsMenu;
    private final JMenu exportMenu;
    private final JMenu indentificationMenu;
    private final JMenu visualizationMenu;
    
//    private final JMenuItem deleteRowsItem;
//    private final JMenuItem addNewRowItem;
//    private final JMenuItem plotRowsItem;
//    private final JMenuItem showSpectrumItem;
//    private final JMenuItem showXICItem;
//    private final JMenuItem showXICSetupItem;
//    private final JMenuItem showMSMSItem;
//    private final JMenuItem showIsotopePatternItem;
//    private final JMenuItem show2DItem;
//    private final JMenuItem show3DItem;
//    private final JMenuItem exportIsotopesItem;
//    private final JMenuItem exportMSMSItem;
    
    ////private final JMenuItem exportTableToCSVunified;
    private final JMenuItem exportTableToCSV;
    private final JMenuItem exportColumnToJDX_Avg;
    private final JMenuItem exportTableToJDX_Avg;
    //
    private static String latestImportDir = ".";
    
    private final JMenuItem computeRowVsRowSim;
    private final JMenuItem computeRowVsRefCompSim;
    private final JMenuItem computeRowVsNistSim;
    //
    private static String latestExportDir = ".";
    
    
    private final JMenuItem showDectedOnlySpectrum;
    //
    private final JMenuItem computeRowVsRowSimDetectedOnly;
    private final JMenuItem computeRowVsRefCompSimDetectedOnly;
    private final JMenuItem computeRowVsNistSimDetectedOnly;
    
    
    private final NumberFormat format = new DecimalFormat("#0.0000");
    
    
//    private final JMenuItem manuallyDefineItem;
//    private final JMenuItem showPeakRowSummaryItem;
//    private final JMenuItem clearIdsItem;
//    private final JMenuItem dbSearchItem;
//    private final JMenuItem formulaItem;
//    private final JMenuItem nistSearchItem;
//    private final JMenuItem copyIdsItem;
//    private final JMenuItem pasteIdsItem;

    private final PeakListTableAlaJulWindow window;
    private RawDataFile clickedDataFile;
    private PeakListRow clickedPeakListRow;
    private PeakListRow[] allClickedPeakListRows;

    private MZmineProject project;
    private boolean useOldestRDF;

    // For copying and pasting IDs - shared by all peak-list table instances.
    // Currently only accessed from this
    // class.
    private static PeakIdentity copiedId = null;

    public PeakListTablePopupMenu(final PeakListTableAlaJulWindow window,
            PeakListTable peakListTable, final PeakListFullTableModel model,
            final PeakList list) {

        
        this.window = window;
        table = peakListTable;
        peakList = list;
        this.model = model;
        
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // TODO: WARNING => this should be passed smoothly by a task, not by this
        //              'brutal' singleton usage...
        this.project = (MZmineProject) MZmineCore.getProjectManager().getCurrentProject();
        
        String search_descr_str = JoinAlignerGCTask.TASK_NAME;
        String search_param_str = JoinAlignerGCParameters.useOldestRDFAncestor.getName() + ": true";
        int method_id = -1;
        for (int i=0; i < this.peakList.getAppliedMethods().length; i++) {
            if (this.peakList.getAppliedMethods()[i].getDescription().equals(search_descr_str)) {
                method_id = i;
                break;
            }
        }
        this.useOldestRDF = ((method_id != -1) &&
            (this.peakList.getAppliedMethods()[method_id].getParameters().contains(search_param_str)));//table.getUseOldestRDF();
        logger.info("Method param 'Use original raw data file' is '" + this.useOldestRDF + "'");
        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        
        
        clickedDataFile = null;
        clickedPeakListRow = null;
        allClickedPeakListRows = null;

//        showMenu = new JMenu("Show");
//        add(showMenu);
//
//        showXICItem = GUIUtils.addMenuItem(showMenu, "XIC (base peak) (quick)",
//                this);
//        showXICSetupItem = GUIUtils.addMenuItem(showMenu, "XIC (dialog)", this);
//        showSpectrumItem = GUIUtils
//                .addMenuItem(showMenu, "Mass spectrum", this);
//        show2DItem = GUIUtils.addMenuItem(showMenu, "Peak in 2D", this);
//        show3DItem = GUIUtils.addMenuItem(showMenu, "Peak in 3D", this);
//        showMSMSItem = GUIUtils.addMenuItem(showMenu, "MS/MS", this);
//        showIsotopePatternItem = GUIUtils.addMenuItem(showMenu,
//                "Isotope pattern", this);
//        showPeakRowSummaryItem = GUIUtils.addMenuItem(showMenu,
//                "Peak row summary", this);
//
//        searchMenu = new JMenu("Search");
//        add(searchMenu);
//        dbSearchItem = GUIUtils.addMenuItem(searchMenu,
//                "Search online database", this);
//        nistSearchItem = GUIUtils.addMenuItem(searchMenu, "NIST MS Search",
//                this);
//        formulaItem = GUIUtils.addMenuItem(searchMenu,
//                "Predict molecular formula", this);

        exportMenu = new JMenu("Export");
        add(exportMenu);
//        exportIsotopesItem = GUIUtils.addMenuItem(exportMenu,
//                "Isotope pattern", this);
//        exportMSMSItem = GUIUtils
//                .addMenuItem(exportMenu, "MS/MS pattern", this);
        ////exportTableToCSVunified = GUIUtils.addMenuItem(exportMenu,
        ////        "Table to CSV (at once)", this);
        ////exportTableToCSV = GUIUtils.addMenuItem(exportMenu,
        ////        "Table to CSV (2 files: \"-rt.csv\" + \"-area.csv\")", this);
        exportTableToCSV = GUIUtils.addMenuItem(exportMenu,
                "Export table to CSV...", this);
        exportColumnToJDX_Avg = GUIUtils.addMenuItem(exportMenu,
                "Export selected column averaged m/z profile to JDX...", this);
        exportTableToJDX_Avg = GUIUtils.addMenuItem(exportMenu,
                "Export table's averaged m/z profiles to JDX... (one JDX file per column)", this);
        
        indentificationMenu = new JMenu("Identification");
        add(indentificationMenu);
        //
        computeRowVsRowSim = GUIUtils.addMenuItem(indentificationMenu,
                "Compute similarity between two selected peaks", this);
        computeRowVsRefCompSim = GUIUtils.addMenuItem(indentificationMenu,
                "Compute similarity between the selected peak and a JDX compound", this);
        computeRowVsNistSim = GUIUtils.addMenuItem(indentificationMenu,
                "Compute similarity between the selected peak and the NIST", this);
        //
        computeRowVsRowSimDetectedOnly = GUIUtils.addMenuItem(indentificationMenu,
                "[DETECTED only] Compute similarity between two selected peaks", this);
        computeRowVsRefCompSimDetectedOnly = GUIUtils.addMenuItem(indentificationMenu,
                "[DETECTED only] Compute similarity between the selected peak and a JDX compound", this);
        computeRowVsNistSimDetectedOnly = GUIUtils.addMenuItem(indentificationMenu,
                "[DETECTED only] Compute similarity between the selected peak and the NIST", this);

//        // Identities menu.
//        idsMenu = new JMenu("Identities");
//        add(idsMenu);
//        clearIdsItem = GUIUtils.addMenuItem(idsMenu, "Clear", this);
//        copyIdsItem = GUIUtils.addMenuItem(idsMenu, "Copy", this);
//        pasteIdsItem = GUIUtils.addMenuItem(idsMenu, "Paste", this);
//
//        plotRowsItem = GUIUtils.addMenuItem(this,
//                "Plot using Intensity Plot module", this);
//        manuallyDefineItem = GUIUtils.addMenuItem(this, "Manually define peak",
//                this);
//        deleteRowsItem = GUIUtils.addMenuItem(this, "Delete selected row(s)",
//                this);
//        addNewRowItem = GUIUtils.addMenuItem(this, "Add new row", this);
        
        
        visualizationMenu = new JMenu("Visualization");
        add(visualizationMenu);
        showDectedOnlySpectrum = GUIUtils.addMenuItem(visualizationMenu,
                "Show \"DETECTED only\" spectrum", this);
    }

//    @Override
//    public void show(final Component invoker, final int x, final int y) {
//
//        // Select the row where clicked?
//        final Point clickedPoint = new Point(x, y);
//        final int clickedRow = table.rowAtPoint(clickedPoint);
//        if (table.getSelectedRowCount() < 2) {
//            ListSelectionModel selectionModel = table.getSelectionModel();
//            selectionModel.setSelectionInterval(clickedRow, clickedRow);
//        }
//
//        // First, disable all the Show... items
//        show2DItem.setEnabled(false);
//        show3DItem.setEnabled(false);
//        manuallyDefineItem.setEnabled(false);
//        showMSMSItem.setEnabled(false);
//        showIsotopePatternItem.setEnabled(false);
//        showPeakRowSummaryItem.setEnabled(false);
//        exportIsotopesItem.setEnabled(false);
//        exportMSMSItem.setEnabled(false);
//        exportMenu.setEnabled(false);
//
//        // Enable row items if applicable
//        final int[] selectedRows = table.getSelectedRows();
//        final boolean rowsSelected = selectedRows.length > 0;
//        deleteRowsItem.setEnabled(rowsSelected);
//        clearIdsItem.setEnabled(rowsSelected);
//        pasteIdsItem.setEnabled(rowsSelected && copiedId != null);
//        plotRowsItem.setEnabled(rowsSelected);
//        showMenu.setEnabled(rowsSelected);
//        idsMenu.setEnabled(rowsSelected);
//        exportIsotopesItem.setEnabled(rowsSelected);
//        exportMenu.setEnabled(rowsSelected);
//
//        final boolean oneRowSelected = selectedRows.length == 1;
//        searchMenu.setEnabled(oneRowSelected);
//
//        // Find the row and column where the user clicked
//        clickedDataFile = null;
//        final int clickedColumn = columnModel.getColumn(
//                table.columnAtPoint(clickedPoint)).getModelIndex();
//        if (clickedRow >= 0 && clickedColumn >= 0) {
//
//            final int rowIndex = table.convertRowIndexToModel(clickedRow);
//            clickedPeakListRow = peakList.getRow(rowIndex);
//            allClickedPeakListRows = new PeakListRow[selectedRows.length];
//            for (int i = 0; i < selectedRows.length; i++) {
//
//                allClickedPeakListRows[i] = peakList.getRow(table
//                        .convertRowIndexToModel(selectedRows[i]));
//            }
//
//            // Enable items.
//            show2DItem.setEnabled(oneRowSelected);
//            show3DItem.setEnabled(oneRowSelected);
//            showPeakRowSummaryItem.setEnabled(oneRowSelected);
//
//            if (clickedPeakListRow.getBestPeak() != null) {
//                exportMSMSItem.setEnabled(oneRowSelected
//                        && clickedPeakListRow.getBestPeak()
//                                .getMostIntenseFragmentScanNumber() > 0);
//            }
//
//            // If we clicked on data file columns, check the peak
//            if (clickedColumn >= CommonColumnType.values().length) {
//
//                // Enable manual peak picking
//                manuallyDefineItem.setEnabled(oneRowSelected);
//
//                // Find the actual peak, if we have it.
//                clickedDataFile = peakList
//                        .getRawDataFile((clickedColumn - CommonColumnType
//                                .values().length)
//                                / DataFileColumnType.values().length);
//
//                final Feature clickedPeak = peakList.getRow(
//                        table.convertRowIndexToModel(clickedRow)).getPeak(
//                        clickedDataFile);
//
//                // If we have the peak, enable Show... items
//                if (clickedPeak != null && oneRowSelected) {
//                    showIsotopePatternItem.setEnabled(clickedPeak
//                            .getIsotopePattern() != null);
//                    showMSMSItem.setEnabled(clickedPeak
//                            .getMostIntenseFragmentScanNumber() > 0);
//                }
//
//            } else {
//
//                showIsotopePatternItem.setEnabled(clickedPeakListRow
//                        .getBestIsotopePattern() != null && oneRowSelected);
//                if (clickedPeakListRow.getBestPeak() != null) {
//                    showMSMSItem.setEnabled(clickedPeakListRow.getBestPeak()
//                            .getMostIntenseFragmentScanNumber() > 0
//                            && oneRowSelected);
//                }
//            }
//        }
//
//        copyIdsItem
//                .setEnabled(oneRowSelected
//                        && allClickedPeakListRows[0].getPreferredPeakIdentity() != null);
//
//        super.show(invoker, x, y);
//    }

    @Override
    public void actionPerformed(final ActionEvent e) {

        final Object src = e.getSource();

//        if (deleteRowsItem.equals(src)) {
//
//            final int[] rowsToDelete = table.getSelectedRows();
//
//            final int[] unsortedIndexes = new int[rowsToDelete.length];
//            for (int i = rowsToDelete.length - 1; i >= 0; i--) {
//
//                unsortedIndexes[i] = table
//                        .convertRowIndexToModel(rowsToDelete[i]);
//            }
//
//            // sort row indexes and start removing from the last
//            Arrays.sort(unsortedIndexes);
//
//            // delete the rows starting from last
//            for (int i = unsortedIndexes.length - 1; i >= 0; i--) {
//                peakList.removeRow(unsortedIndexes[i]);
//            }
//
//            // Notify the GUI that peaklist contents have changed
//            updateTableGUI();
//        }
//
//        if (plotRowsItem.equals(src)) {
//
//            final int[] selectedTableRows = table.getSelectedRows();
//
//            final PeakListRow[] selectedRows = new PeakListRow[selectedTableRows.length];
//            for (int i = 0; i < selectedTableRows.length; i++) {
//
//                selectedRows[i] = peakList.getRow(table
//                        .convertRowIndexToModel(selectedTableRows[i]));
//            }
//
//            IntensityPlotModule.showIntensityPlot(MZmineCore
//                    .getProjectManager().getCurrentProject(), peakList,
//                    selectedRows);
//        }
//
//        if (showXICItem.equals(src) && allClickedPeakListRows.length != 0) {
//
//            // Map peaks to their identity labels.
//            final Map<Feature, String> labelsMap = new HashMap<Feature, String>(
//                    allClickedPeakListRows.length);
//
//            final RawDataFile selectedDataFile = clickedDataFile == null ? allClickedPeakListRows[0]
//                    .getBestPeak().getDataFile() : clickedDataFile;
//
//            Range<Double> mzRange = null;
//            final List<Feature> selectedPeaks = new ArrayList<Feature>(
//                    allClickedPeakListRows.length);
//            for (final PeakListRow row : allClickedPeakListRows) {
//
//                for (final Feature peak : row.getPeaks()) {
//                    if (mzRange == null) {
//                        mzRange = peak.getRawDataPointsMZRange();
//                    } else {
//                        mzRange = mzRange.span(peak.getRawDataPointsMZRange());
//                    }
//                }
//
//                final Feature filePeak = row.getPeak(selectedDataFile);
//                if (filePeak != null) {
//
//                    selectedPeaks.add(filePeak);
//
//                    // Label the peak with the row's preferred identity.
//                    final PeakIdentity identity = row
//                            .getPreferredPeakIdentity();
//                    if (identity != null) {
//                        labelsMap.put(filePeak, identity.getName());
//                    }
//                }
//            }
//
//            ScanSelection scanSelection = new ScanSelection(
//                    selectedDataFile.getDataRTRange(1), 1);
//
//            TICVisualizerModule.showNewTICVisualizerWindow(
//                    new RawDataFile[] { selectedDataFile },
//                    selectedPeaks.toArray(new Feature[selectedPeaks.size()]),
//                    labelsMap, scanSelection, TICPlotType.BASEPEAK, mzRange);
//        }
//
//        if (showXICSetupItem.equals(src) && allClickedPeakListRows.length != 0) {
//
//            // Map peaks to their identity labels.
//            final Map<Feature, String> labelsMap = new HashMap<Feature, String>(
//                    allClickedPeakListRows.length);
//
//            final RawDataFile[] selectedDataFiles = clickedDataFile == null ? peakList
//                    .getRawDataFiles() : new RawDataFile[] { clickedDataFile };
//
//            Range<Double> mzRange = null;
//            final ArrayList<Feature> allClickedPeaks = new ArrayList<Feature>(
//                    allClickedPeakListRows.length);
//            final ArrayList<Feature> selectedClickedPeaks = new ArrayList<Feature>(
//                    allClickedPeakListRows.length);
//            for (final PeakListRow row : allClickedPeakListRows) {
//
//                // Label the peak with the row's preferred identity.
//                final PeakIdentity identity = row.getPreferredPeakIdentity();
//
//                for (final Feature peak : row.getPeaks()) {
//
//                    allClickedPeaks.add(peak);
//                    if (peak.getDataFile() == clickedDataFile) {
//                        selectedClickedPeaks.add(peak);
//                    }
//
//                    if (mzRange == null) {
//                        mzRange = peak.getRawDataPointsMZRange();
//                    } else {
//                        mzRange = mzRange.span(peak.getRawDataPointsMZRange());
//                    }
//
//                    if (identity != null) {
//                        labelsMap.put(peak, identity.getName());
//                    }
//                }
//            }
//
//            ScanSelection scanSelection = new ScanSelection(
//                    selectedDataFiles[0].getDataRTRange(1), 1);
//
//            TICVisualizerModule.setupNewTICVisualizer(MZmineCore
//                    .getProjectManager().getCurrentProject().getDataFiles(),
//                    selectedDataFiles, allClickedPeaks
//                            .toArray(new Feature[allClickedPeaks.size()]),
//                    selectedClickedPeaks
//                            .toArray(new Feature[selectedClickedPeaks.size()]),
//                    labelsMap, scanSelection, mzRange);
//        }
//
//        if (show2DItem.equals(src)) {
//
//            final Feature showPeak = getSelectedPeak();
//            if (showPeak != null) {
//
//                TwoDVisualizerModule.show2DVisualizerSetupDialog(
//                        showPeak.getDataFile(), getPeakMZRange(showPeak),
//                        getPeakRTRange(showPeak));
//            }
//        }
//
//        if (show3DItem.equals(src)) {
//
//            final Feature showPeak = getSelectedPeak();
//            if (showPeak != null) {
//
//                ThreeDVisualizerModule.setupNew3DVisualizer(
//                        showPeak.getDataFile(), getPeakMZRange(showPeak),
//                        getPeakRTRange(showPeak));
//            }
//        }
//
//        if (manuallyDefineItem.equals(src)) {
//
//            ManualPeakPickerModule.runManualDetection(clickedDataFile,
//                    clickedPeakListRow, peakList, table);
//        }
//
//        if (showSpectrumItem.equals(src)) {
//
//            final Feature showPeak = getSelectedPeak();
//            if (showPeak != null) {
//
//                SpectraVisualizerModule.showNewSpectrumWindow(
//                        showPeak.getDataFile(),
//                        showPeak.getRepresentativeScanNumber(), showPeak);
//            }
//        }
//
//        if (showMSMSItem.equals(src)) {
//
//            final Feature showPeak = getSelectedPeak();
//            if (showPeak != null) {
//
//                final int scanNumber = showPeak
//                        .getMostIntenseFragmentScanNumber();
//                if (scanNumber > 0) {
//                    SpectraVisualizerModule.showNewSpectrumWindow(
//                            showPeak.getDataFile(), scanNumber);
//                } else {
//                    MZmineCore.getDesktop().displayMessage(
//                            window,
//                            "There is no fragment for "
//                                    + MZmineCore.getConfiguration()
//                                            .getMZFormat()
//                                            .format(showPeak.getMZ())
//                                    + " m/z in the current raw data.");
//                }
//            }
//        }
//
//        if (showIsotopePatternItem.equals(src)) {
//
//            final Feature showPeak = getSelectedPeak();
//            if (showPeak != null && showPeak.getIsotopePattern() != null) {
//
//                SpectraVisualizerModule.showNewSpectrumWindow(
//                        showPeak.getDataFile(),
//                        showPeak.getRepresentativeScanNumber(),
//                        showPeak.getIsotopePattern());
//            }
//        }
//
//        if (formulaItem != null && formulaItem.equals(src)) {
//
//            FormulaPredictionModule
//                    .showSingleRowIdentificationDialog(clickedPeakListRow);
//        }
//
//        if (dbSearchItem != null && dbSearchItem.equals(src)) {
//
//            OnlineDBSearchModule
//                    .showSingleRowIdentificationDialog(clickedPeakListRow);
//        }
//
//        if (nistSearchItem != null && nistSearchItem.equals(src)) {
//
//            NistMsSearchModule.singleRowSearch(peakList, clickedPeakListRow);
//        }
//
//        if (addNewRowItem.equals(src)) {
//
//            // find maximum ID and add 1
//            int newID = 1;
//            for (final PeakListRow row : peakList.getRows()) {
//                if (row.getID() >= newID) {
//                    newID = row.getID() + 1;
//                }
//            }
//
//            // create a new row
//            final PeakListRow newRow = new SimplePeakListRow(newID);
//            ManualPeakPickerModule.runManualDetection(
//                    peakList.getRawDataFiles(), newRow, peakList, table);
//
//        }
//
//        if (showPeakRowSummaryItem.equals(src)) {
//
//            PeakSummaryVisualizerModule
//                    .showNewPeakSummaryWindow(clickedPeakListRow);
//        }
//
//        if (exportIsotopesItem.equals(src)) {
//            IsotopePatternExportModule.exportIsotopePattern(clickedPeakListRow);
//        }
//
//        if (exportMSMSItem.equals(src)) {
//            MSMSExportModule.exportMSMS(clickedPeakListRow);
//        }

//        if (exportTableToCSVunified.equals(src)) {
//            boolean success = this.table.exportToCSV(false);
//            if (success)
//                logger.log(Level.INFO, "Table saved to file: " + this.table.getCSVfilename());
//            else
//                logger.log(Level.SEVERE, "Could not write to file: " + this.table.getCSVfilename());
//        }
//        if (exportTableToCSV.equals(src)) {
//            boolean success = this.table.exportToCSV(true);
//            if (success)
//                logger.log(Level.INFO, "Table saved to files: " 
//                        + this.table.getCSVfilenames()[1] + " | " + this.table.getCSVfilenames()[2]);
//            else
//                logger.log(Level.SEVERE, "Could not write to files: " 
//                        + this.table.getCSVfilenames()[1] + " | " + this.table.getCSVfilenames()[2]);
//        }
        if (exportTableToCSV.equals(src)) {
            
            // Get export parameters
            CSVExportParameters params = new CSVExportParameters();
            ExitCode exitCode = params.showSetupDialog(null, true);
            
            if (exitCode == ExitCode.OK) {
                CSVExportTask task = new CSVExportTask(params);
                task.run();
            }
        }
        
        if (exportTableToJDX_Avg.equals(src)) {
            this.exportAvgToJDX(-1);
        }
        if (exportColumnToJDX_Avg.equals(src)) {
            this.exportAvgToJDX(this.table.getSelectedColumn());
        }
        
        
        
//        if (computeRowVsRowSim.equals(src)) {
//            
//            // Get peaks
//            Map<RawDataFile, PeakListRow> peak1 = null, peak2 = null;           
//            for(int row = 0 ; row < table.getRowCount() ; row++) {
//                for(int col = 0 ; col < table.getColumnCount() ; col++) {
//                    if(table.isCellSelected(row, col)) {
//                        if (peak1 == null)
//                            peak1 = table.getPeakAt(row, col);
//                        else if (peak2 == null)
//                            peak2 = table.getPeakAt(row, col);
//                        else
//                            break;
//                    }
//                }
//            }
//            
//            String msg_title = "Similarity score";
//            int info = JOptionPane.INFORMATION_MESSAGE;
//            // Compute & display
//            if (peak1 != null && peak2 != null) {
//                
//                // Compute
//                double[] vec1 = new double[JDXCompound.MAX_MZ];
//                Arrays.fill(vec1, 0.0);
//                double[] vec2 = new double[JDXCompound.MAX_MZ];
//                Arrays.fill(vec2, 0.0);
//                
//                Entry<RawDataFile, PeakListRow> entry = peak1.entrySet().iterator().next();
//                RawDataFile rdf = entry.getKey();
//                PeakListRow pl_row = entry.getValue();                
//                Scan apexScan = rdf.getScan(pl_row.getPeak(rdf).getRepresentativeScanNumber());
//                DataPoint[] dataPoints = apexScan.getDataPoints();
//                for (int j=0; j < dataPoints.length; ++j) {
//                    DataPoint dp = dataPoints[j];
//                    vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
//                }
//                //-
//                entry = peak2.entrySet().iterator().next();
//                rdf = entry.getKey();
//                pl_row = entry.getValue();                
//                apexScan = rdf.getScan(pl_row.getPeak(rdf).getRepresentativeScanNumber());
//                dataPoints = apexScan.getDataPoints();
//                for (int j=0; j < dataPoints.length; ++j) {
//                    DataPoint dp = dataPoints[j];
//                    vec2[(int) Math.round(dp.getMZ())] += dp.getIntensity();
//                }
//                //-
//                double score = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
//                
//                // Display
//                JOptionPane.showMessageDialog(this, "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + score, msg_title, info);
//            
//            } else {
//                JOptionPane.showMessageDialog(this, "Select 2 peaks in the table using 'Ctrl+LMB'...", msg_title, info);                
//            }
//        }
        if (computeRowVsRowSim.equals(src)) {
            
            // Get peaks
            String rt1 = null, rt2 = null;
            Scan peak1 = null, peak2 = null;           
            for(int row = 0 ; row < table.getRowCount() ; row++) {
                for(int col = 0 ; col < table.getColumnCount() ; col++) {
                    if(table.isCellSelected(row, col)) {
                        if (peak1 == null) {
                            //peak1 = table.getPeakAt(row, col);
                            peak1 = table.getApexScanAt(row, col, this.project, this.useOldestRDF);
                            if (peak1 != null)
                                rt1 = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRetentionTime());
                        } else if (peak2 == null) {
                            //peak2 = table.getPeakAt(row, col);
                            peak2 = table.getApexScanAt(row, col, this.project, this.useOldestRDF);
                            if (peak2 != null)
                                rt2 = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak2.getRetentionTime());
                        } else
                            break;
                    }
                }
            }
            
            String msg_title = "Similarity";
            int info = JOptionPane.INFORMATION_MESSAGE;
            // Compute & display
            if (peak1 != null && peak2 != null) {
                
                msg_title += " - [" + format.format(Double.valueOf(rt1)) + " / " + format.format(Double.valueOf(rt2)) + "]";
                
                // Compute
                double[] vec1 = new double[JDXCompound.MAX_MZ];
                Arrays.fill(vec1, 0.0);
                double[] vec2 = new double[JDXCompound.MAX_MZ];
                Arrays.fill(vec2, 0.0);
                
                DataPoint[] dataPoints = peak1.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
                logger.info("PEAK_1: " + peak1 + "\n\t- vec_1: " + Arrays.toString(vec1));
                //-
                dataPoints = peak2.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec2[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
                logger.info("PEAK_2: " + peak2 + "\n\t- vec_2: " + Arrays.toString(vec2));
                //-
                double score = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
                logger.info("PEAK_1 VS PEAK_2 score: " + score);
                
                // Display
                //JOptionPane.showMessageDialog(this, "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + score, msg_title, info);
                JDialog dialog = createInfoDialog(table.getWindow(), 
                        "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + format.format(score), 
                        msg_title);
                dialog.setVisible(true);
                
            } else {
                JOptionPane.showMessageDialog(this, "Select 2 peaks in the table using 'Ctrl+LMB'...", msg_title, info);                
            }
        }
        
        if (computeRowVsRefCompSim.equals(src)) {
            
            final JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("JDX Files", "jdx");
            chooser.setFileFilter(filter);
            chooser.setCurrentDirectory(new java.io.File(latestImportDir));
            chooser.setDialogTitle("Select a JDX file to compare to...");
            // Disable the "All files" option
            chooser.setAcceptAllFileFilterUsed(false);
            int ret = chooser.showOpenDialog(this);
            if(ret == JFileChooser.APPROVE_OPTION) {
                
                latestImportDir = chooser.getSelectedFile().getPath();
                
                String msg_title = "Similarity";
                int info = JOptionPane.INFORMATION_MESSAGE;
                int err  = JOptionPane.ERROR_MESSAGE;
                
                try {
                    // Get selected peak
                    //Map<RawDataFile, PeakListRow> peak1 = null;
                    Scan peak1 = null;
                    String rt = null;
                    for(int row = 0 ; row < table.getRowCount() ; row++) {
                        for(int col = 0 ; col < table.getColumnCount() ; col++) {
                            if(table.isCellSelected(row, col)) {
                                if (peak1 == null) {
                                    //peak1 = table.getPeakAt(row, col);
                                    peak1 = table.getApexScanAt(row, col, this.project, this.useOldestRDF);
                                    if (peak1 != null)
                                        rt = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRetentionTime());
                                } else
                                    break;
                            }
                        }
                    }
                    // Compute & display
                    if (peak1 != null) {
                        
                        msg_title += " - [" + format.format(Double.valueOf(rt)) + "]";
                        
                        JDXCompound jdxComp = JDXCompound.parseJDXfile(chooser.getSelectedFile());
                        
                        // Compute
                        double[] vec1 = new double[JDXCompound.MAX_MZ];
                        Arrays.fill(vec1, 0.0);
                        double[] vec2 = new double[JDXCompound.MAX_MZ];
                        Arrays.fill(vec2, 0.0);
                        
                        DataPoint[] dataPoints = peak1.getDataPoints();
                        for (int j=0; j < dataPoints.length; ++j) {
                            DataPoint dp = dataPoints[j];
                            vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                        }
                        //-
                        vec2 = jdxComp.getSpectrum();
                        //-
                        double score = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
                        
                        // Display
//                        JOptionPane.showMessageDialog(this, "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + score, msg_title, info);
                        JDialog dialog = createInfoDialog(table.getWindow(), 
                                "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + format.format(score), 
                                msg_title);
                        dialog.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(this, "Select a peak in the table...", msg_title, err);                
                    }
                } catch (JCAMPException e1) {
                    logger.log(Level.SEVERE, e1.getMessage());
                    JOptionPane.showMessageDialog(this, e1.getMessage(), "Error [" + msg_title + "]", err);
                }
                
            }
            
        }

        
        if (computeRowVsNistSim.equals(src)) {
            
            // Get NIST parameters
            NistParameters params = new NistParameters();
            ExitCode exitCode = params.showSetupDialog(null, true);
            
            if (exitCode == ExitCode.OK) {
                File nistMsSearchDir = params.getParameter(NistParameters.NIST_MS_SEARCH_DIR).getValue();
                File nistMsSearchExe = ((NistParameters) params).getNistMsSearchExecutable();
                int minMatchFactor = params.getParameter(NistParameters.MIN_MATCH_FACTOR).getValue();
                int minReverseMatchFactor = params.getParameter(NistParameters.MIN_REVERSE_MATCH_FACTOR).getValue();
                           
                // Get selected peak
                //Map<RawDataFile, PeakListRow> peak1 = null;
                Scan peak1 = null;
                String rt = null;
                for(int row = 0 ; row < table.getRowCount() ; row++) {
                    for(int col = 0 ; col < table.getColumnCount() ; col++) {
                        if(table.isCellSelected(row, col)) {
                            if (peak1 == null) {
                                //peak1 = table.getPeakAt(row, col);
                                peak1 = table.getApexScanAt(row, col, this.project, this.useOldestRDF);
                                if (peak1 != null)
                                    rt = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRetentionTime());
                            } else
                                break;
                        }
                    }
                }
                
                String msg_title = "Similarities (NIST)";
                int info = JOptionPane.INFORMATION_MESSAGE;
                int err  = JOptionPane.ERROR_MESSAGE;
                // Compute & display
                if (peak1 != null) {
                    
                    msg_title += " - [" + format.format(Double.valueOf(rt)) + "]";
                    // Compute...
                
                    try {
                        // Configure locator files.
                        final File locatorFile1 = new File(nistMsSearchDir,
                                NistMsSearchGCTask.PRIMARY_LOCATOR_FILE_NAME);
                        File locatorFile2 = NistMsSearchGCTask.getSecondLocatorFile(nistMsSearchDir, locatorFile1);
                        if (locatorFile2 == null) {
        
                            throw new IOException("Primary locator file "
                                    + locatorFile1
                                    + " doesn't contain the name of a valid file.");
                        }
        
                        // Is MS Search already running?
                        if (locatorFile2.exists()) {
        
                            throw new IllegalStateException(
                                    "NIST MS Search appears to be busy - please wait until it finishes its current task and then try again.  Alternatively, try manually deleting the file "
                                            + locatorFile2);
                        }
                        
                        // Search command string.
                        final String command = nistMsSearchExe.getAbsolutePath() + ' '
                                + NistMsSearchGCTask.COMMAND_LINE_ARGS;
                        
                        final int resultId = peak1.getScanNumber();
                        
                        //-
                        // Write spectra file.
                        //final File spectraFile = NistMsSearchGCTask.writeSpectraFile(table.getPeakList(), pl_row, null);
                        final File spectraFile = NistMsSearchGCTask.writeSpectraFile(peak1, resultId, null);
                        
                        // Write locator file.
                        NistMsSearchGCTask.writeSecondaryLocatorFile(locatorFile2, spectraFile);
        
                        // Run the search.
                        NistMsSearchGCTask.runNistMsSearchNoTask(nistMsSearchDir, command);
        
                        // Read the search results file and store the
                        // results.
                        List<PeakIdentity> identities = NistMsSearchGCTask.readSearchResults(nistMsSearchDir, minMatchFactor, minReverseMatchFactor, resultId);
                        
                        
                        //-
                        String msg = "";
                        for (PeakIdentity id : identities) {
                            double a_score = Double.valueOf(id.getPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE));
                            msg += format.format(a_score) + " : \t" + id.getName() + "\n";
                        }
                        
                        // Display
//                        JOptionPane.showMessageDialog(this, msg, msg_title, info);
                        JDialog dialog = createInfoDialog(table.getWindow(), msg, msg_title);
                        dialog.setVisible(true);
                        
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(this, e1.getMessage(), msg_title, err);
                        e1.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Select a peak in the table...", msg_title, err);                
                }
            }
        }

        
        //        if (clearIdsItem.equals(src)) {
//
//            // Delete identities of selected rows.
//            for (final PeakListRow row : allClickedPeakListRows) {
//
//                // Selected row index.
//                for (final PeakIdentity id : row.getPeakIdentities()) {
//
//                    // Remove id.
//                    row.removePeakIdentity(id);
//                }
//            }
//
//            // Update table GUI.
//            updateTableGUI();
//        }
//
//        if (copyIdsItem.equals(src) && allClickedPeakListRows.length > 0) {
//
//            final PeakIdentity id = allClickedPeakListRows[0]
//                    .getPreferredPeakIdentity();
//            if (id != null) {
//
//                copiedId = (PeakIdentity) id.clone();
//            }
//        }
//
//        if (pasteIdsItem.equals(src) && copiedId != null) {
//
//            // Paste identity into selected rows.
//            for (final PeakListRow row : allClickedPeakListRows) {
//
//                row.setPreferredPeakIdentity((PeakIdentity) copiedId.clone());
//            }
//
//            // Update table GUI.
//            updateTableGUI();
//        }        
        
        
        if (showDectedOnlySpectrum.equals(src)) {

            // Get peak
//            Map<RawDataFile, PeakListRow> peak1 = null;           
//            for(int row = 0 ; row < table.getRowCount() ; row++) {
//                for(int col = 0 ; col < table.getColumnCount() ; col++) {
//                    if(table.isCellSelected(row, col)) {
//                        if (peak1 == null)
//                            peak1 = table.getPeakAt(row, col);
//                        else
//                            break;
//                    }
//                }
//            }
//
//            Entry<RawDataFile, PeakListRow> entry = peak1.entrySet().iterator().next();
//            RawDataFile rdf = entry.getKey();
//            PeakListRow pl_row = entry.getValue();                
//            // Show spectrum
//            Feature peak = pl_row.getPeak(rdf);
            Feature peak1 = null;           
            for(int row = 0 ; row < table.getRowCount() ; row++) {
                for(int col = 0 ; col < table.getColumnCount() ; col++) {
                    if(table.isCellSelected(row, col)) {
                        if (peak1 == null)
                            peak1 = table.getPeakAt(row, col);
                        else
                            break;
                    }
                }
            }

            // Show spectrum
            SpectraVisualizerModule.showNewSpectrumWindow(
                    peak1.getDataFile(),
                    peak1.getRepresentativeScanNumber(),
                    peak1.getIsotopePattern());

        }

        
        if (computeRowVsRowSimDetectedOnly.equals(src)) {
            
            // Get peaks
            String rt1 = null, rt2 = null;
            //Map<RawDataFile, PeakListRow> peak1_map = null, peak2_map = null;           
            Feature peak1 = null, peak2 = null;           
            for(int row = 0 ; row < table.getRowCount() ; row++) {
                for(int col = 0 ; col < table.getColumnCount() ; col++) {
                    if(table.isCellSelected(row, col)) {
//                        if (peak1_map == null) {
//                            peak1_map = table.getPeakAt(row, col);
//                            if (peak1_map != null) {
//                                Entry<RawDataFile, PeakListRow> entry = peak1_map.entrySet().iterator().next();
//                                RawDataFile rdf = entry.getKey();
//                                PeakListRow pl_row = entry.getValue();                
//                                peak1 = pl_row.getPeak(rdf);
//                                rt1 = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRT());
//                            }
//                        }
//                        else if (peak2_map == null) {
//                            peak2_map = table.getPeakAt(row, col);
//                            if (peak2_map != null) {
//                                Entry<RawDataFile, PeakListRow> entry = peak1_map.entrySet().iterator().next();
//                                RawDataFile rdf = entry.getKey();
//                                PeakListRow pl_row = entry.getValue();                
//                                peak2 = pl_row.getPeak(rdf);
//                                rt2 = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak2.getRT());
//                            }
//                        }
//                        else
//                            break;peak1.
                        if (peak1 == null) {
                            peak1 = table.getPeakAt(row, col);
                            if (peak1 != null)
                                rt1 = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRT());
                        }
                        else if (peak2 == null) {
                            peak2 = table.getPeakAt(row, col);
                            if (peak2 != null)
                                rt2 = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak2.getRT());
                        }
                        else
                            break;
                    }
                }
            }

            
            String msg_title = "Similarity";
            int info = JOptionPane.INFORMATION_MESSAGE;
            // Compute & display
            if (peak1 != null && peak2 != null) {
                
                msg_title += " - [" + format.format(Double.valueOf(rt1)) + " / " + format.format(Double.valueOf(rt2)) + "]";
                
                // Compute
                double[] vec1 = new double[JDXCompound.MAX_MZ];
                Arrays.fill(vec1, 0.0);
                double[] vec2 = new double[JDXCompound.MAX_MZ];
                Arrays.fill(vec2, 0.0);
                
                DataPoint[] dataPoints = peak1.getIsotopePattern().getDataPoints();//peak1.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
                logger.info("PEAK_1: " + peak1 + "\n\t- vec_1: " + Arrays.toString(vec1));
                //-
                dataPoints = peak2.getIsotopePattern().getDataPoints();//peak2.getDataPoints();
                for (int j=0; j < dataPoints.length; ++j) {
                    DataPoint dp = dataPoints[j];
                    vec2[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                }
                logger.info("PEAK_2: " + peak2 + "\n\t- vec_2: " + Arrays.toString(vec2));
                //-
                double score = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
                logger.info("PEAK_1 VS PEAK_2 score: " + score);
                
                // Display
                //JOptionPane.showMessageDialog(this, "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + score, msg_title, info);
                JDialog dialog = createInfoDialog(table.getWindow(), 
                        "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + format.format(score), 
                        msg_title);
                dialog.setVisible(true);
                
            } else {
                JOptionPane.showMessageDialog(this, "Select 2 peaks in the table using 'Ctrl+LMB'...", msg_title, info);                
            }
        }

        
        if (computeRowVsRefCompSimDetectedOnly.equals(src)) {
            
            final JFileChooser chooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("JDX Files", "jdx");
            chooser.setFileFilter(filter);
            chooser.setCurrentDirectory(new java.io.File(latestImportDir));
            chooser.setDialogTitle("Select a JDX file to compare to...");
            // Disable the "All files" option
            chooser.setAcceptAllFileFilterUsed(false);
            int ret = chooser.showOpenDialog(this);
            if(ret == JFileChooser.APPROVE_OPTION) {
                
                latestImportDir = chooser.getSelectedFile().getPath();
                
                String msg_title = "Similarity";
                int info = JOptionPane.INFORMATION_MESSAGE;
                int err  = JOptionPane.ERROR_MESSAGE;
                
                try {
                    // Get selected peak
                    Feature peak1 = null;
                    String rt = null;
                    for(int row = 0 ; row < table.getRowCount() ; row++) {
                        for(int col = 0 ; col < table.getColumnCount() ; col++) {
                            if(table.isCellSelected(row, col)) {
                                if (peak1 == null) {
                                    peak1 = table.getPeakAt(row, col);
                                    if (peak1 != null)
                                        rt = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRT());
                                } else
                                    break;
                            }
                        }
                    }
                    // Compute & display
                    if (peak1 != null) {
                        
                        msg_title += " - [" + format.format(Double.valueOf(rt)) + "]";
                        
                        JDXCompound jdxComp = JDXCompound.parseJDXfile(chooser.getSelectedFile());
                        
                        // Compute
                        double[] vec1 = new double[JDXCompound.MAX_MZ];
                        Arrays.fill(vec1, 0.0);
                        double[] vec2 = new double[JDXCompound.MAX_MZ];
                        Arrays.fill(vec2, 0.0);
                        
                        DataPoint[] dataPoints = peak1.getIsotopePattern().getDataPoints();//peak1.getDataPoints();
                        for (int j=0; j < dataPoints.length; ++j) {
                            DataPoint dp = dataPoints[j];
                            vec1[(int) Math.round(dp.getMZ())] += dp.getIntensity();
                        }
                        //-
                        vec2 = jdxComp.getSpectrum();
                        //-
                        double score = RowVsRowScoreGC.computeSimilarityScore(vec1, vec2, SimilarityMethodType.DOT);
                        
                        // Display
//                        JOptionPane.showMessageDialog(this, "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + score, msg_title, info);
                        JDialog dialog = createInfoDialog(table.getWindow(), 
                                "Method: \t <" + SimilarityMethodType.DOT + ">" + "\nScore: \t" + format.format(score), 
                                msg_title);
                        dialog.setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(this, "Select a peak in the table...", msg_title, err);                
                    }
                } catch (JCAMPException e1) {
                    logger.log(Level.SEVERE, e1.getMessage());
                    JOptionPane.showMessageDialog(this, e1.getMessage(), "Error [" + msg_title + "]", err);
                }
                
            }
            
        }

        
        if (computeRowVsNistSimDetectedOnly.equals(src)) {
            
            // Get NIST parameters
            NistParameters params = new NistParameters();
            ExitCode exitCode = params.showSetupDialog(null, true);
            
            if (exitCode == ExitCode.OK) {
                File nistMsSearchDir = params.getParameter(NistParameters.NIST_MS_SEARCH_DIR).getValue();
                File nistMsSearchExe = ((NistParameters) params).getNistMsSearchExecutable();
                int minMatchFactor = params.getParameter(NistParameters.MIN_MATCH_FACTOR).getValue();
                int minReverseMatchFactor = params.getParameter(NistParameters.MIN_REVERSE_MATCH_FACTOR).getValue();
                           
                // Get selected peak
                Feature peak1 = null;
                String rt = null;
                for(int row = 0 ; row < table.getRowCount() ; row++) {
                    for(int col = 0 ; col < table.getColumnCount() ; col++) {
                        if(table.isCellSelected(row, col)) {
                            if (peak1 == null) {
                                peak1 = table.getPeakAt(row, col);
                                if (peak1 != null)
                                    rt = (row == 0) ? (String) table.getValueAt(row, col) : String.valueOf(peak1.getRT());
                            } else
                                break;
                        }
                    }
                }
                
                String msg_title = "Similarities (NIST)";
                int info = JOptionPane.INFORMATION_MESSAGE;
                int err  = JOptionPane.ERROR_MESSAGE;
                // Compute & display
                if (peak1 != null) {
                    
                    msg_title += " - [" + format.format(Double.valueOf(rt)) + "]";
                    // Compute...
                
                    try {
                        // Configure locator files.
                        final File locatorFile1 = new File(nistMsSearchDir,
                                NistMsSearchGCTask.PRIMARY_LOCATOR_FILE_NAME);
                        File locatorFile2 = NistMsSearchGCTask.getSecondLocatorFile(nistMsSearchDir, locatorFile1);
                        if (locatorFile2 == null) {
        
                            throw new IOException("Primary locator file "
                                    + locatorFile1
                                    + " doesn't contain the name of a valid file.");
                        }
        
                        // Is MS Search already running?
                        if (locatorFile2.exists()) {
        
                            throw new IllegalStateException(
                                    "NIST MS Search appears to be busy - please wait until it finishes its current task and then try again.  Alternatively, try manually deleting the file "
                                            + locatorFile2);
                        }
                        
                        // Search command string.
                        final String command = nistMsSearchExe.getAbsolutePath() + ' '
                                + NistMsSearchGCTask.COMMAND_LINE_ARGS;
                        
                        final int resultId = peak1.getRepresentativeScanNumber();//peak1.getScanNumber();
                        
                        //-
                        // Write spectra file.
                        // Need a Scan object to grab spectrum from 
                        Scan peak1_scan = new SimpleScanClonable(
                                peak1.getDataFile().getScan(peak1.getRepresentativeScanNumber()),
                                peak1.getDataFile()
                                );
                        final File spectraFile = NistMsSearchGCTask.writeSpectraFile(peak1_scan, resultId, null);//peak1, resultId, null);
                        
                        // Write locator file.
                        NistMsSearchGCTask.writeSecondaryLocatorFile(locatorFile2, spectraFile);
        
                        // Run the search.
                        NistMsSearchGCTask.runNistMsSearchNoTask(nistMsSearchDir, command);
        
                        // Read the search results file and store the
                        // results.
                        List<PeakIdentity> identities = NistMsSearchGCTask.readSearchResults(nistMsSearchDir, minMatchFactor, minReverseMatchFactor, resultId);
                        
                        
                        //-
                        String msg = "";
                        for (PeakIdentity id : identities) {
                            double a_score = Double.valueOf(id.getPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE));
                            msg += format.format(a_score) + " : \t" + id.getName() + "\n";
                        }
                        
                        // Display
//                        JOptionPane.showMessageDialog(this, msg, msg_title, info);
                        JDialog dialog = createInfoDialog(table.getWindow(), msg, msg_title);
                        dialog.setVisible(true);
                        
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(this, e1.getMessage(), msg_title, err);
                        e1.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Select a peak in the table...", msg_title, err);                
                }
            }
        }

        

    }

    /**
     * Update the table.
     */
    private void updateTableGUI() {
        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
        MZmineCore.getProjectManager().getCurrentProject()
                .notifyObjectChanged(peakList, true);
    }

    /**
     * Get a peak's m/z range.
     * 
     * @param peak
     *            the peak.
     * @return The peak's m/z range.
     */
    private static Range<Double> getPeakMZRange(final Feature peak) {

        final Range<Double> peakMZRange = peak.getRawDataPointsMZRange();

        // By default, open the visualizer with the m/z range of
        // "peak_width x 2", but no smaller than 0.1 m/z, because with smaller
        // ranges VisAD tends to show nasty anti-aliasing artifacts.
        // For example of such artifacts, set mzMin = 440.27, mzMax = 440.28 and
        // mzResolution = 500
        final double minRangeCenter = (peakMZRange.upperEndpoint() + peakMZRange
                .lowerEndpoint()) / 2.0;
        final double minRangeWidth = Math
                .max(0.1, (peakMZRange.upperEndpoint() - peakMZRange
                        .lowerEndpoint()) * 2);
        double mzMin = minRangeCenter - (minRangeWidth / 2);
        if (mzMin < 0)
            mzMin = 0;
        double mzMax = minRangeCenter + (minRangeWidth / 2);
        return Range.closed(mzMin, mzMax);
    }

    /**
     * Get a peak's RT range.
     * 
     * @param peak
     *            the peak.
     * @return The peak's RT range.
     */
    private static Range<Double> getPeakRTRange(final Feature peak) {

        final Range<Double> range = peak.getRawDataPointsRTRange();
        final double rtLen = range.upperEndpoint() - range.lowerEndpoint();
        return Range.closed(Math.max(0.0, range.lowerEndpoint() - rtLen),
                range.upperEndpoint() + rtLen);
    }

//    /**
//     * Get the selected peak.
//     * 
//     * @return the peak.
//     */
//    private Feature getSelectedPeak() {
//
//        return clickedDataFile != null ? clickedPeakListRow
//                .getPeak(clickedDataFile) : clickedPeakListRow.getBestPeak();
//    }            
        
    private void exportAvgToJDX(int col) { 
    
        final JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(latestExportDir));
        chooser.setDialogTitle("Select a directory to save JDX files to...");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        // Disable the "All files" option
        chooser.setAcceptAllFileFilterUsed(false);
        
        int ret = chooser.showOpenDialog(this);
        if(ret == JFileChooser.APPROVE_OPTION) {
            
            latestExportDir = chooser.getSelectedFile().getPath();
            
            boolean success = this.table.exportToJDX(col, chooser.getSelectedFile().getAbsolutePath());
            if (success) {
                logger.log(Level.INFO, "Table column(s) saved to file(s): ");
                for (int i = 0; i < this.table.getJDXfilenames().length; i++)
                    logger.log(Level.INFO, "> " + this.table.getJDXfilenames()[i]);
            } else {
                logger.log(Level.SEVERE, "Could not write to file(s)...");
            }
            
        }
    }
    
    // Non-modal dialog...
    static JDialog createInfoDialog(Frame parent, String message, String title) {
        
        final JDialog optionPaneDialog = new JDialog(parent, title);
        
        //Note we are creating an instance of a JOptionPane
        //Normally it's just a call to a static method.
        Object[] options = {"OK"};
        JOptionPane optPane = new JOptionPane(message, 
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_OPTION, 
                UIManager.getIcon("OptionPane.informationIcon"),
                options); //JOptionPane.OK_CANCEL_OPTION);
             
        //Listen for the JOptionPane button click. It comes through as property change 
        //event with the property called "value". 
        optPane.addPropertyChangeListener(new PropertyChangeListener()
        {
           public void propertyChange(PropertyChangeEvent e)
           {
              if (e.getPropertyName().equals("value"))
              {
//                switch ((Integer)e.getNewValue())
//                {
//                   case JOptionPane.OK_OPTION:
//                       //user clicks OK
//                      break;
//                   case JOptionPane.CANCEL_OPTION:
//                      //user clicks CANCEL
//                      break;                       
//                }
                  switch ((String) e.getNewValue())
                  {
                  case "OK":
                      //user clicks OK
                      break;
                  }
                  optionPaneDialog.dispose();
              }
              //System.out.println(e.getPropertyName());
           }
        });
        optionPaneDialog.setContentPane(optPane);
             
        //Let the JDialog figure out how big it needs to be
        //based on the size of JOptionPane by calling the pack() method
        optionPaneDialog.pack();
        optionPaneDialog.setLocationRelativeTo(parent);
        optionPaneDialog.setVisible(true);

        return optionPaneDialog;
    }
    

}
