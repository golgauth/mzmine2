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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.RowSorterEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
//import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableParameters;
//import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTablePopupMenu;
//import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableWindow;
import net.sf.mzmine.modules.visualization.peaklisttable.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.components.ComponentToolTipManager;
import net.sf.mzmine.util.components.ComponentToolTipProvider;
import net.sf.mzmine.util.components.GroupableTableHeader;
import net.sf.mzmine.util.components.PeakSummaryComponent;
import net.sf.mzmine.util.components.PopupListener;
import net.sf.mzmine.util.dialogs.PeakIdentitySetupDialog;
import javax.swing.table.DefaultTableModel;

import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table.PeakListTablePopupMenu;


public class PeakListTable extends JTable implements ComponentToolTipProvider {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    static final String EDIT_IDENTITY = "Edit";
    static final String REMOVE_IDENTITY = "Remove";
    static final String NEW_IDENTITY = "Add new...";

    private static final Font comboFont = new Font("SansSerif", Font.PLAIN, 10);

    private PeakListTableAlaJulWindow window;
//    private PeakListTableModel pkTableModel;
    private PeakList peakList;
    private PeakListRow peakListRow;
    private TableRowSorter<PeakListTableModel> sorter;
    private PeakListFullTableModel tm;
    private ComponentToolTipManager ttm;
    private DefaultCellEditor currentEditor = null;
    
    public static final int NB_HEADER_ROWS = 3;
    
    private static String csvFilename = "";
    private static String[] csvFilenames = null;

    public PeakListTable(PeakListTableAlaJulWindow window, ParameterSet parameters,
	    PeakList peakList) {

	this.window = window;
	this.peakList = peakList;

	this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//	this.setAutoCreateColumnsFromModel(false);

//	this.pkTableModel = new PeakListTableModel(peakList);
//	setModel(pkTableModel);

//	GroupableTableHeader header = new GroupableTableHeader();
//	setTableHeader(header);

	tm = new PeakListFullTableModel(parameters, peakList);
//	cm.setColumnMargin(0);
//	setColumnModel(cm);
	setModel(tm);
	
	// create default columns
	tm.createColumns();
//	cm.fillRows();
	
	logger.info("Nb columns = " + tm.getColumnCount());
	

//	// Initialize sorter
//	sorter = new TableRowSorter<PeakListTableModel>(pkTableModel);
//	setRowSorter(sorter);

	//---
	PeakListTablePopupMenu popupMenu = new PeakListTablePopupMenu(window,
		this, tm, peakList);
	addMouseListener(new PopupListener(popupMenu));
	//---
//	header.addMouseListener(new PopupListener(popupMenu));
//
//	int rowHeight = parameters.getParameter(
//		PeakListTableParameters.rowHeight).getValue();
//	setRowHeight(rowHeight)
	setRowHeight(30);
	
	// Mimic the table header renderer for first column,
	//cm.getColumn(0).setCellRenderer(new RowHeaderRenderer());
	getColumnModel().getColumn(0).setCellRenderer(new RowHeaderRenderer());
////	// And the 3 first rows as well
////	Done in renderers themselves
	
	// Hide columns header
	setTableHeader(null);

	for (int i=1; i < getColumnModel().getColumnCount(); ++i) {
	    getColumnModel().getColumn(i).setCellRenderer(new UnifiedCellRenderer(new DecimalFormat(), peakList, parameters));
	}
	
	
	ttm = new ComponentToolTipManager();
	ttm.registerComponent(this);
	
	tm.fillRows();
//	resizeColumns();
	
	handleDoubleClick();
   }
    
    
    void handleDoubleClick() {

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                PeakListTable table = (PeakListTable)me.getSource();
                
                Point p = me.getPoint();
                int row = table.rowAtPoint(p);
                int col = table.columnAtPoint(p);
                if (me.getClickCount() == 2) {
                    PeakListRow pl_row = table.peakList.getRow(col-1);
                    // Show MZ averaged profile for column
                    if (row == 0) {
                        // TODO: Requires to build a new type of Feature
                        //      (See: "MergedPeak implements Feature")
                        //      which we can store MZ peaks (See: "MergedPeak.addMzPeak") in.
                        //      after having averaged MZ intensities along all "table.peakList.getRawDataFiles()"
                        // ...
                    }
                    // Show MZ profile for given peak
                    else if (row > 2) {
                        Object value = table.getValueAt(row, col);
                        if (value != null) {
                            if (value instanceof String && !value.equals("") && !value.equals("-")) {
                                RawDataFile rdf = table.peakList.getRawDataFile(row-3);
                                final Feature selectedPeak = pl_row.getPeak(rdf);
                                SpectraVisualizerModule.showNewSpectrumWindow(
                                        selectedPeak.getDataFile(),
                                        selectedPeak.getRepresentativeScanNumber(), selectedPeak);
                            }
                        }
                    }
                }
            }
        });
    }


//    private void resizeColumns() {
//        // Auto size column width based on data
//        for (int column = 0; column < getColumnCount(); column++) {
//            TableColumn tableColumn = getColumnModel().getColumn(
//                    column);
//            TableCellRenderer renderer = tableColumn
//                    .getHeaderRenderer();
//            if (renderer == null) {
//                renderer = getTableHeader().getDefaultRenderer();
//            }
//            Component component = renderer
//                    .getTableCellRendererComponent(this,
//                            tableColumn.getHeaderValue(), false, false,
//                            -1, column);
//            int preferredWidth = component.getPreferredSize().width + 20;
//            tableColumn.setPreferredWidth(preferredWidth);
//        }
//
//    }

    
    public JComponent getCustomToolTipComponent(MouseEvent event) {

	JComponent component = null;
	String text = this.getToolTipText(event);
	if (text == null) {
	    return null;
	}

	if (text.contains(ComponentToolTipManager.CUSTOM)) {
	    String values[] = text.split("-");
	    int myID = Integer.parseInt(values[1].trim());
	    for (PeakListRow row : peakList.getRows()) {
		if (row.getID() == myID) {
		    component = new PeakSummaryComponent(row,
			    peakList.getRawDataFiles(), true, false, false,
			    true, false, ComponentToolTipManager.bg);
		    break;
		}
	    }

	} else {
	    text = "<html>" + text.replace("\n", "<br>") + "</html>";
	    JLabel label = new JLabel(text);
	    label.setFont(UIManager.getFont("ToolTip.font"));
	    JPanel panel = new JPanel();
	    panel.setBackground(ComponentToolTipManager.bg);
	    panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	    panel.add(label);
	    component = panel;
	}

	return component;

    }

    public PeakList getPeakList() {
	return peakList;
    }

    public TableCellEditor getCellEditor(int row, int column) {

//	CommonColumnType commonColumn = pkTableModel.getCommonColumn(column);
//	if (commonColumn == CommonColumnType.IDENTITY) {
        if (row == 1) {
            int col = this.convertColumnIndexToModel(column);
	    peakListRow = peakList.getRow(col-1);

	    PeakIdentity identities[] = peakListRow.getPeakIdentities();
	    PeakIdentity preferredIdentity = peakListRow
		    .getPreferredPeakIdentity();
	    JComboBox<Object> combo;

	    if ((identities != null) && (identities.length > 0)) {
		combo = new JComboBox<Object>(identities);
		combo.addItem("-------------------------");
		combo.addItem(REMOVE_IDENTITY);
		combo.addItem(EDIT_IDENTITY);
	    } else {
		combo = new JComboBox<Object>();
	    }

	    combo.setFont(comboFont);
	    combo.addItem(NEW_IDENTITY);
	    if (preferredIdentity != null) {
		combo.setSelectedItem(preferredIdentity);
	    }

	    combo.addActionListener(new ActionListener() {

		public void actionPerformed(ActionEvent e) {
		    JComboBox<?> combo = (JComboBox<?>) e.getSource();
		    Object item = combo.getSelectedItem();
		    if (item != null) {
			if (item.toString() == NEW_IDENTITY) {
			    PeakIdentitySetupDialog dialog = new PeakIdentitySetupDialog(
				    window, peakListRow);
			    dialog.setVisible(true);
			    return;
			}
			if (item.toString() == EDIT_IDENTITY) {
			    PeakIdentitySetupDialog dialog = new PeakIdentitySetupDialog(
				    window, peakListRow, peakListRow
					    .getPreferredPeakIdentity());
			    dialog.setVisible(true);
			    return;
			}
			if (item.toString() == REMOVE_IDENTITY) {
			    PeakIdentity identity = peakListRow
				    .getPreferredPeakIdentity();
			    if (identity != null) {
				peakListRow.removePeakIdentity(identity);
				DefaultComboBoxModel<?> comboModel = (DefaultComboBoxModel<?>) combo
					.getModel();
				comboModel.removeElement(identity);
			    }
			    return;
			}
			if (item instanceof PeakIdentity) {
			    peakListRow
				    .setPreferredPeakIdentity((PeakIdentity) item);
			    return;
			}
		    }

		}
	    });

	    // Keep the reference to the editor
	    currentEditor = new DefaultCellEditor(combo);

	    return currentEditor;
	}

	return super.getCellEditor(row, column);

    }

    /**
     * When user sorts the table, we have to cancel current combobox for
     * identity selection. Unfortunately, this doesn't happen automatically.
     */
    public void sorterChanged(RowSorterEvent e) {
	if (currentEditor != null) {
	    currentEditor.stopCellEditing();
	}
	super.sorterChanged(e);
    }

    
    /*
     *  Attempt to mimic the table header renderer
     */
    private static class RowHeaderRenderer extends DefaultTableCellRenderer
    {
            public RowHeaderRenderer()
            {
                    setHorizontalAlignment(JLabel.CENTER);
            }

            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                    if (table != null)
                    {
//                            JTableHeader header = table.getTableHeader();
//
//                            if (header != null)
//                            {
//                                    setForeground(header.getForeground());
//                                    setBackground(header.getBackground());
//                                    setFont(header.getFont());
//                            }
                    }
                  setForeground(Color.BLACK);
                  setBackground(Color.lightGray);

                    if (isSelected)
                    {
                            setFont( getFont().deriveFont(Font.BOLD) );
                    }

                    setText((value == null) ? "" : value.toString());
                    setBorder(UIManager.getBorder("TableHeader.cellBorder"));

                    return this;
            }
    }

    // Make sure to resize columns based on content
    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int rendererWidth = component.getPreferredSize().width;
        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
        return component;
     }

    
    boolean exportToCSV(boolean separatedOutputs) {
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV FILES", "csv");
        fileChooser.setFileFilter(filter);

        String currentPath = csvFilename;
        if (currentPath.length() > 0) {
            File currentFile = new File(currentPath);
            File currentDir = currentFile.getParentFile();
            if (currentDir != null && currentDir.exists())
                fileChooser.setCurrentDirectory(currentDir);
        }

        int returnVal = fileChooser.showDialog(null, "Select file");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getPath();
            
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

                // Export table values
                String sepStr = " / ";
                for (int i=0; i < tm.getRowCount(); ++i) {
                    for (int j=0; j < tm.getColumnCount(); ++j) {
                        
                        String str = tm.getValueAt(i, j).toString();
                        
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
                        
                        if (j != tm.getColumnCount() - 1) {
                            writer.write(";");
                            if (separatedOutputs) {
                                writerRt.write(";");
                                writerArea.write(";");
                            }
                        }
                    }
                    writer.newLine();
                    if (separatedOutputs) {
                        writerRt.newLine();
                        writerArea.newLine();                        
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
       
        return false;
    }
    
    String getCSVfilename() {
        return PeakListTable.csvFilename;
    }
    String[] getCSVfilenames() {
        return PeakListTable.csvFilenames;
    }
    
}