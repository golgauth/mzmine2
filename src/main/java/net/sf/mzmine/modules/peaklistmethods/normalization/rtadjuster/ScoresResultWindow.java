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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.AlignedRowProps;
import net.sf.mzmine.modules.peaklistmethods.identification.customjdxsearch.CustomJDXSearchTask;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.ScoresResultTableModel.ComboboxPeak;
import net.sf.mzmine.modules.visualization.molstructure.MolStructureViewer;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

public class ScoresResultWindow extends JFrame implements ActionListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ScoresResultTableModel listElementModel;
    private CellEditorModel cellEditorModel;

    private PeakListRow peakListRow;
    private JTableXY piafsScoresTable;
    private Task searchTask;

    private FormattedCellRenderer scoreRenderer, rtRenderer, areaRenderer, dfltRenderer;
    private static final Icon widthIcon = new ImageIcon("icons/widthicon.png");

    private boolean singleCompoundSearch;


    //	public ScoresResultWindow(ArrayList<LinkedHashMap<PeakListRow, LinkedHashMap<JDXCompound, Double>>> piafsRowScores, 
    //			PeakListRow peakListRow, double searchedMass, Task searchTask) {
    public ScoresResultWindow(String[] columnNames, Task searchTask) {

        super("");

        //this.peakListRow = peakListRow;
        this.searchTask = searchTask;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        //		JPanel pnlLabelsAndList = new JPanel(new BorderLayout());
        //		pnlLabelsAndList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        //
        //		pnlLabelsAndList.add(new JLabel("List of possible identities"), BorderLayout.NORTH);

        //		class MyJTable extends JTable {
        //			@Override
        //			public TableCellEditor getCellEditor(int row, int column) {
        //			   Object value = super.getValueAt(row, column);
        //			   if(value != null) {
        //			      if(value instanceof JComboBox) {
        //			           return new DefaultCellEditor((JComboBox)value);
        //			      }
        //			            return getDefaultEditor(value.getClass());
        //			   }
        //			   return super.getCellEditor(row, column);
        //			}
        //		}

        cellEditorModel = new CellEditorModel();
        listElementModel = new ScoresResultTableModel(columnNames, 0);
        piafsScoresTable = new JTableXY(listElementModel);
        piafsScoresTable.setCellEditorModel(cellEditorModel);
        piafsScoresTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        piafsScoresTable.setFillsViewportHeight(true);
        //		piafsScoresTable.setModel(listElementModel);
        //		piafsScoresTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //		piafsScoresTable.getTableHeader().setReorderingAllowed(false);
        
        singleCompoundSearch = (piafsScoresTable.getColumnModel().getColumnCount() <= 5);

        
        //RtDecimalFormatRenderer rtFormatRenderer = new RtDecimalFormatRenderer();
        NumberFormat scoreFormat = new DecimalFormat("0.0000");
        NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
        NumberFormat areaFormat = MZmineCore.getConfiguration()
                .getIntensityFormat();
        scoreRenderer = new FormattedCellRenderer(scoreFormat);
        rtRenderer = new FormattedCellRenderer(rtFormat);
        areaRenderer = new FormattedCellRenderer(areaFormat);
        dfltRenderer = new FormattedCellRenderer(null);
        piafsScoresTable.getColumnModel().getColumn(2).setCellRenderer(
                scoreRenderer );
        piafsScoresTable.getColumnModel().getColumn(3).setCellRenderer(
                rtRenderer );
        piafsScoresTable.getColumnModel().getColumn(4).setCellRenderer(
                areaRenderer );
        //--- Non-number columns
        //                piafsScoresTable.getColumnModel().getColumn(0).setCellRenderer(
        //                        dfltRenderer );
        //                piafsScoresTable.getColumnModel().getColumn(1).setCellRenderer(
        //                        dfltRenderer );
        //                piafsScoresTable.getColumnModel().getColumn(5).setCellRenderer(
        //                        dfltRenderer );
        if (!singleCompoundSearch) {
            piafsScoresTable.getColumnModel().getColumn(6).setCellRenderer(
                    scoreRenderer );
            piafsScoresTable.getColumnModel().getColumn(7).setCellRenderer(
                    rtRenderer );
            piafsScoresTable.getColumnModel().getColumn(8).setCellRenderer(
                    areaRenderer );
        }
        

        piafsScoresTable.setRowHeight(25);

        //		TableRowSorter<ScoresResultTableModel> sorter = new TableRowSorter<ScoresResultTableModel>(listElementModel);
        //		piafsList.setRowSorter(sorter);

        JScrollPane listScroller = new JScrollPane(piafsScoresTable);
        listScroller.setPreferredSize(new Dimension(500, 100));
        listScroller.setAlignmentX(LEFT_ALIGNMENT);
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(2,0));//new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
        listPanel.add(listScroller);
        listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        //		pnlLabelsAndList.add(listPanel, BorderLayout.CENTER);

        listPanel.setOpaque(true);
        //		pnlLabelsAndList.setOpaque(true);

        JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new BoxLayout(pnlButtons, BoxLayout.X_AXIS));
        pnlButtons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        //		GUIUtils.addButton(pnlButtons, "Add identity", null, this, "ADD");
        //		GUIUtils.addButton(pnlButtons, "View structure", null, this, "VIEWER");
        //		GUIUtils.addButton(pnlButtons, "View isotope pattern", null, this,
        //				"ISOTOPE_VIEWER");
        //		GUIUtils.addButton(pnlButtons, "Open browser", null, this, "BROWSER");
        GUIUtils.addButton(pnlButtons, "Apply Identification", null, this, "APPLY_ID");
        GUIUtils.addButton(pnlButtons, "Apply & Adjust RT", null, this, "APPLY_RT");
        GUIUtils.addButton(pnlButtons, "View spectra (Actual VS JDX Compound)", null, this, "VIEW_SPECTRA");
        GUIUtils.addButton(pnlButtons, "Resize Columns", widthIcon, this, "AUTOCOLUMNWIDTH", "Auto resize columns");
        GUIUtils.addButton(pnlButtons, "Cancel", null, this, "CANCEL");

        //		setLayout(new BorderLayout());
        //		setSize(500, 200);
        //		add(pnlLabelsAndList, BorderLayout.CENTER);
        //		add(pnlButtons, BorderLayout.SOUTH);
        listPanel.add(pnlButtons, BorderLayout.SOUTH);

        this.setContentPane(listPanel);


        //        //Set up column sizes.
        //        initColumnSizes(piafsScoresTable);
        //
        //        //Fiddle with the Sport column's cell editors/renderers.
        //        setUpSportColumn(piafsScoresTable, piafsScoresTable.getColumnModel().getColumn(2));


        pack();
        //        //Display the window.
        //		this.pack();
        //		this.setVisible(true);

        resizeColumns();
        //listScroller.setPreferredSize(new Dimension(500, 20 + piafsScoresTable.getRowCount() * piafsScoresTable.getRowHeight()));
    }

    //    /*
    //     * This method picks good column sizes.
    //     * If all column heads are wider than the column's cells'
    //     * contents, then you can just use column.sizeWidthToFit().
    //     */
    //    private void initColumnSizes(JTable table) {
    //        ScoresResultTableModel model = (ScoresResultTableModel)table.getModel();
    //        TableColumn column = null;
    //        Component comp = null;
    //        int headerWidth = 0;
    //        int cellWidth = 0;
    //        Object[] longValues = model.longValues;
    //        TableCellRenderer headerRenderer =
    //            table.getTableHeader().getDefaultRenderer();
    //
    //        for (int i = 0; i < 5; i++) {
    //            column = table.getColumnModel().getColumn(i);
    //
    //            comp = headerRenderer.getTableCellRendererComponent(
    //                                 null, column.getHeaderValue(),
    //                                 false, false, 0, 0);
    //            headerWidth = comp.getPreferredSize().width;
    //
    //            comp = table.getDefaultRenderer(model.getColumnClass(i)).
    //                             getTableCellRendererComponent(
    //                                 table, longValues[i],
    //                                 false, false, 0, i);
    //            cellWidth = comp.getPreferredSize().width;
    //
    //            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
    //        }
    //    }

    //    public void setUpSportColumn(JTable table,
    //                                 TableColumn sportColumn) {
    //        //Set up the editor for the sport cells.
    //        JComboBox comboBox = new JComboBox();
    //        comboBox.addItem("Snowboarding");
    //        comboBox.addItem("Rowing");
    //        comboBox.addItem("Knitting");
    //        comboBox.addItem("Speed reading");
    //        comboBox.addItem("Pool");
    //        comboBox.addItem("None of the above");
    //        sportColumn.setCellEditor(new DefaultCellEditor(comboBox));
    //
    //        //Set up tool tips for the sport cells.
    //        DefaultTableCellRenderer renderer =
    //                new DefaultTableCellRenderer();
    //        renderer.setToolTipText("Click for combo box");
    //        sportColumn.setCellRenderer(renderer);
    //    }


    private void resizeColumns() {
        // Auto size column width based on data
        for (int column = 0; column < piafsScoresTable.getColumnCount(); column++) {
            TableColumn tableColumn = piafsScoresTable.getColumnModel().getColumn(
                    column);
            TableCellRenderer renderer = tableColumn
                    .getHeaderRenderer();
            if (renderer == null) {
                renderer = piafsScoresTable.getTableHeader().getDefaultRenderer();
            }
            Component component = renderer
                    .getTableCellRendererComponent(piafsScoresTable,
                            tableColumn.getHeaderValue(), false, false,
                            -1, column);
            int preferredWidth = component.getPreferredSize().width + 20;
            tableColumn.setPreferredWidth(preferredWidth);
        }

    }

    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if (command.equals("CANCEL")) {
            this.dispose();	
        }
        if (command.equals("APPLY_ID")) {
            this.applyFoundIdentities();
        }
        if (command.equals("APPLY_RT")) {
            // TODO ...
        }
        if (command.equals("VIEW_SPECTRA")) {
            // TODO ...
        }
        if (command.equals("AUTOCOLUMNWIDTH")) {
            resizeColumns();
        }

        //		if (command.equals("ADD")) {
        //
        //			int index = piafsScoresTable.getSelectedRow();
        //
        //			if (index < 0) {
        //				MZmineCore.getDesktop().displayMessage(
        //						"Select one result to add as compound identity");
        //				return;
        //
        //			}
        //			index = piafsScoresTable.convertRowIndexToModel(index);
        //
        //			peakListRow.addPeakIdentity(listElementModel.getCompoundAt(index), false);
        //
        //			// Notify the GUI about the change in the project
        //			MZmineCore.getCurrentProject().notifyObjectChanged(peakListRow,
        //					false);
        //
        //			// Repaint the window to reflect the change in the peak list
        //			MZmineCore.getDesktop().getMainWindow().repaint();
        //
        //			dispose();
        //		}
        //
        //		if (command.equals("VIEWER")) {
        //
        //			int index = piafsScoresTable.getSelectedRow();
        //
        //			if (index < 0) {
        //				MZmineCore.getDesktop().displayMessage("Select one result to display molecule structure");
        //				return;
        //			}
        //			index = piafsScoresTable.convertRowIndexToModel(index);
        //
        //			JDXCompound compound = listElementModel.getCompoundAt(index);
        ////			URL url2D = compound.get2DStructureURL();
        ////			URL url3D = compound.get3DStructureURL();
        //			String name = compound.getName() + " (" + compound.getPropertyValue(PeakIdentity.PROPERTY_ID) + ")";
        //			
        //			// TODO: Create a 'Spectrum visualizer' showing both jdx spectrum and detected peak apex spectrum.
        //			// 			=> New spectra plot from (datafile@scan_index / jdx: new Scan(spectrum))
        //			SpectraVisualizerWindow viewer = new SpectraVisualizerWindow(/*name, url2D, url3D*/null);
        //			viewer.setVisible(true);
        //
        //		}

        //		if (command.equals("ISOTOPE_VIEWER")) {
        //
        //			int index = IDList.getSelectedRow();
        //
        //			if (index < 0) {
        //				MZmineCore.getDesktop().displayMessage(
        //						"Select one result to display the isotope pattern");
        //				return;
        //			}
        //
        //			index = IDList.convertRowIndexToModel(index);
        //
        //			final IsotopePattern predictedPattern = listElementModel
        //					.getCompoundAt(index).getIsotopePattern();
        //
        //			if (predictedPattern == null)
        //				return;
        //
        //			Feature peak = peakListRow.getBestPeak();
        //
        //			RawDataFile dataFile = peak.getDataFile();
        //			int scanNumber = peak.getRepresentativeScanNumber();
        //			SpectraVisualizerModule.showNewSpectrumWindow(dataFile, scanNumber,
        //					null, peak.getIsotopePattern(), predictedPattern);
        //
        //		}

        //		if (command.equals("BROWSER")) {
        //			int index = IDList.getSelectedRow();
        //
        //			if (index < 0) {
        //				MZmineCore.getDesktop().displayMessage(
        //						"Select one compound to display in a web browser");
        //				return;
        //
        //			}
        //			index = IDList.convertRowIndexToModel(index);
        //
        //			logger.finest("Launching default browser to display compound details");
        //
        //			java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        //
        //			JDXCompound compound = listElementModel.getCompoundAt(index);
        //			String urlString = compound
        //					.getPropertyValue(PeakIdentity.PROPERTY_URL);
        //
        //			if ((urlString == null) || (urlString.length() == 0))
        //				return;
        //
        //			try {
        //				URL compoundURL = new URL(urlString);
        //				desktop.browse(compoundURL.toURI());
        //			} catch (Exception ex) {
        //				logger.severe("Error trying to launch default browser: "
        //						+ ex.getMessage());
        //			}
        //
        //		}

    }

    //	public void addNewListItem(final LinkedHashMap<PeakListRow, ArrayList<JDXCompound>> item) {
    //
    //		// Update the model in swing thread to avoid exceptions
    //		SwingUtilities.invokeLater(new Runnable() {
    //			public void run() {
    //				listElementModel.addElement(piafsScoresTable, item);
    //			}
    //		});
    //
    //	}
    // peakList, findCompounds, scoreMatrix
    public void addNewListItem(final PeakList peakList, final JDXCompound[] findCompounds, final Double[][] scoreMatrix) {

        // Update the model in swing thread to avoid exceptions
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                listElementModel.addElement(piafsScoresTable, peakList, findCompounds, scoreMatrix);
            }
        });

    }

    public void setColumnNames(String[] columnNames) {
        ((ScoresResultTableModel)piafsScoresTable.getModel()).setColumnNames(columnNames);
    }

//    public void applyIdentities(PeakList peaklist, ComboboxPeak peak1, ComboboxPeak peak2) {
//        for (int i=0; i < peaklist.getNumberOfRows(); ++i) {
//            PeakListRow a_pl_row = peaklist.getRows()[i];
//
//            JDXCompound unknownComp = JDXCompound.createUnknownCompound();
//
//            // Add possible identities to peaks /*with score over 0.0*/.
//            a_pl_row.addPeakIdentity(peak1.getJDXCompound(), false);
//            a_pl_row.addPeakIdentity(peak2.getJDXCompound(), false);
//            a_pl_row.addPeakIdentity(unknownComp, false);
//
//            // Set new identity.
//            if (a_pl_row.getID() == peak1.getRowID()) {
//                // Mark as ref compound (for later use in "JoinAlignerTask(GC)")
//                peak1.getJDXCompound().setPropertyValue(AlignedRowIdentity.PROPERTY_IS_REF, AlignedRowIdentity.TRUE);
//                //
//                a_pl_row.setPreferredPeakIdentity(peak1.getJDXCompound());
//            } else if (a_pl_row.getID() == peak2.getRowID()) {
//                // Mark as ref compound (for later use in "JoinAlignerTask(GC)")
//                peak2.getJDXCompound().setPropertyValue(AlignedRowIdentity.PROPERTY_IS_REF, AlignedRowIdentity.TRUE);
//                //
//                a_pl_row.setPreferredPeakIdentity(peak2.getJDXCompound());
//            } 
//            // Erase / reset identity.
//            else if (a_pl_row.getPreferredPeakIdentity().getName().equals(peak1.getJDXCompound().getName())
//                    || a_pl_row.getPreferredPeakIdentity().getName().equals(peak2.getJDXCompound().getName())) {
//                a_pl_row.setPreferredPeakIdentity(unknownComp);
//            }
//
//            // Notify the GUI about the change in the project
//            // TODO: Get the "project" from the instantiator of this class instead.
//            MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
//            project.notifyObjectChanged(i, false);
//            // Repaint the window to reflect the change in the peak list
//            Desktop desktop = MZmineCore.getDesktop();
//            if (!(desktop instanceof HeadLessDesktop))
//                desktop.getMainWindow().repaint();
//        }
//
//    }


    private void applyFoundIdentities() {

        Vector<Vector<Object>> tableData = this.listElementModel.getData();

        for (Vector<Object> tableRow : tableData) {
            PeakList pl = (PeakList) tableRow.get(0);

            logger.info("Apply identities to list: " + pl.getName());

            JComboBox<ComboboxPeak> cb1 = (JComboBox<ComboboxPeak>) tableRow.get(1);
            ComboboxPeak peak1 = (ComboboxPeak) cb1.getSelectedItem();
            double scorePeak1 = (double) tableRow.get(2);
            //applyIdentities(pl, peak1, peak2);
            CustomJDXSearchTask.applyIdentity(pl, peak1.getJDXCompound(), peak1.getRowID(), scorePeak1, /*false,*/ true);
            //
            if (!singleCompoundSearch) {
                JComboBox<ComboboxPeak> cb5 = (JComboBox<ComboboxPeak>) tableRow.get(5);
                ComboboxPeak peak2 = (ComboboxPeak) cb5.getSelectedItem();
                double scorePeak2 = (double) tableRow.get(6);
                CustomJDXSearchTask.applyIdentity(pl, peak2.getJDXCompound(), peak2.getRowID(), scorePeak2, /*false,*/ true);
            }
            
        }
        
        // Repaint the window to reflect the change in the peak list
        // (Only if not in "headless" mode)
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

        this.dispose(); 
    }
    
    public ScoresResultTableModel getTableModel() {
        return listElementModel;
    }

    public void dispose() {

        // Cancel the search task if it is still running
        TaskStatus searchStatus = searchTask.getStatus();
        if ((searchStatus == TaskStatus.WAITING) || (searchStatus == TaskStatus.PROCESSING))
            searchTask.cancel();

        super.dispose();

    }

}
