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
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.jcamp.parser.JCAMPException;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
//import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableParameters;
//import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTablePopupMenu;
//import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableWindow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.components.ComponentToolTipManager;
import net.sf.mzmine.util.components.ComponentToolTipProvider;
import net.sf.mzmine.util.components.PeakSummaryComponent;
import net.sf.mzmine.util.components.PopupListener;
import net.sf.mzmine.util.dialogs.PeakIdentitySetupDialog;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGcModule;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table.PeakListTablePopupMenu;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;


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
    
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    
    public static final int NB_HEADER_ROWS = 4;
    
    private static String csvFilename = "";
    private static String[] csvFilenames = null;
    
    private static String[] jdxFilenames = null;
    
    

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

                    // Show MZ averaged profile for column
                    if (row == 0) {
                        // TODO: Requires to build a new type of Feature
                        //      (See: "MergedPeak implements Feature")
                        //      which we can store MZ peaks (See: "MergedPeak.addMzPeak") in.
                        //      after having averaged MZ intensities along all "table.peakList.getRawDataFiles()"
                        // ...

                        int halfNbMarginScans = 10;
                        double avgRT = Double.valueOf((String)table.getValueAt(row, col));
                        RawDataFile avgRDF = PeakListTable.buildAverageRDF(table.peakList, col-1, halfNbMarginScans, avgRT);
                        
                        // And display
                        SpectraVisualizerModule.showNewSpectrumWindow(
                                // avgPeak.getDataFile(),
                                avgRDF
                                , halfNbMarginScans + 1//1//avg_scan_num
                                //**, avgPeak.getRepresentativeScanNumber(), avgPeak
                                );
                    
                    }
                    // Show MZ profile for given peak
                    else if (row >= NB_HEADER_ROWS) {
                        // Sort rows by ascending RT
                        final PeakListRow[] peakListRows = PeakListTable.getPeakListSortedByRT(table.peakList);
                        PeakListRow pl_row = peakListRows[col-1];
                        
                        Object value = table.getValueAt(row, col);
                        if (value != null) {
                            if (value instanceof String && !value.equals("") && !value.equals(JoinAlignerGcModule.MISSING_PEAK_VAL)) {
                                RawDataFile rdf = table.peakList.getRawDataFile(row - NB_HEADER_ROWS);
                                logger.info("rdf: " + rdf.getName());
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

    public boolean exportToJDX(int column, String directoryPath) {
        
        // Export whole table
        if (column == -1) {
            jdxFilenames = new String[this.getColumnCount()-1];
            for (int col=1; col < this.getColumnCount(); col++) {
                
                double avgRT = Double.valueOf((String)this.getValueAt(0, col));
                String avgRT_formatted = String.format("%03d", col) + "_" + avgRT;
                String identity = ((PeakIdentity)this.getValueAt(1, col)).getName();
                
                RawDataFile avgRDF = PeakListTable.buildAverageRDF(this.peakList, col-1, 0, avgRT);
                Scan scan = avgRDF.getScan(avgRDF.getScanNumbers()[0]);
                
                // Scan m/z profile to JDX file
                jdxFilenames[col-1] = directoryPath + File.separator + avgRT_formatted + "_" + identity + ".jdx";
                try {
                    JDXCompound.saveAsJDXfile(identity, scan.getDataPoints(), new File(jdxFilenames[col-1]));
                } catch (JCAMPException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return false;
                }
            }
        } 
        // Export single column
        else {
            // TODO: ... 
        }
        
        return true;
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

    private static RawDataFile buildAverageRDF(PeakList peakList, int col, int halfNbMarginScans, double avgRT) {
        
        // Sort rows by ascending RT
        final PeakListRow[] peakListRows = PeakListTable.getPeakListSortedByRT(peakList);
        //PeakListRow pl_row = peakListRows[col];

        Scan refScan = peakList.getRawDataFile(0).getScan(peakList.getRawDataFile(0).getScanNumbers()[0]);
        Scan refScan1 = peakList.getRawDataFile(0).getScan(peakList.getRawDataFile(0).getScanNumbers()[1]);
        Scan refScan2 = peakList.getRawDataFile(0).getScan(peakList.getRawDataFile(0).getScanNumbers()[2]);
        // Approximate time duration between two scans
        double approxDeltaRT = (
                (refScan1.getRetentionTime() - refScan.getRetentionTime())
                + (refScan2.getRetentionTime() - refScan1.getRetentionTime())
                ) / 2.0;
        
        RawDataFileWriter rawDataFileWriter = null;
        RawDataFile avgRDF = null;
        //**AveragedPeak avgPeak = null;
        
        try {
            //---- Build the said averaged feature
            //-- Averaged RDF
            rawDataFileWriter = MZmineCore.createNewFile("Averaged_RDF_for_column_" + col);

            //-- Averaged Peak
            //**avgPeak = new AveragedPeak(avgRDF); //table.peakList.getRawDataFile(0));
            
            //-- Browse column
            // Scan numbers at average RT
            HashMap<RawDataFile, Integer> scansNumAtAvgMapping = new HashMap<RawDataFile, Integer>();
            
            // 1st pass
            for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
                
                RawDataFile rdf = peakList.getRawDataFile(i);
                PeakListRow a_row = peakListRows[col];
                final Feature curPeak = a_row.getPeak(rdf);
                
                // Skip non-detected rows
                if (curPeak != null) {
                    // Add all dps to new peak
                    System.out.println(Arrays.toString(curPeak.getScanNumbers()));
                    // Scan number at average RT
                    double best_delta_rt = Double.MAX_VALUE;
                    double prev_delta_rt = Double.MAX_VALUE;
                    for (int scan_num : curPeak.getScanNumbers()) {
                        //**avgPeak.addMzPeak(/*fakeScanNum*/scan_num, curPeak.getDataPoint(scan_num));
                        
                        // Find scan nearest from average RT overall column
                        double delta_rt = Math.abs(avgRT - rdf.getScan(scan_num).getRetentionTime());
                        // If we go away back again from nearest, just stop, since scan_nums are RT ascending ordered
                        if (prev_delta_rt < delta_rt) break;
                        //
                        if (delta_rt < best_delta_rt) {
                            best_delta_rt = delta_rt;
                            scansNumAtAvgMapping.put(rdf, scan_num);
                        }
                        prev_delta_rt = delta_rt;
                    }
                }
            }
            
            // 2nd pass
            for (int n = -halfNbMarginScans; n <= halfNbMarginScans; n++) {
                // Mapping per scan AND per mz
                HashMap<Double, ArrayList<DataPoint>> perMzDataPointsMapping = new HashMap<Double, ArrayList<DataPoint>>();
                
                RawDataFile cur_rdf;
                for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
                    
                    cur_rdf = peakList.getRawDataFile(i);
                    Integer scn = scansNumAtAvgMapping.get(cur_rdf);
                    
                    // Skip non-detected rows
                    if (scn != null) {
                        int scan_num = scn + n;
                        
                        Scan s = cur_rdf.getScan(scan_num);
                        // If current RDF does have a scan at index 'scan_num'
                        if (s != null) {
                            for (DataPoint dp : s.getDataPoints()) {
                                ArrayList<DataPoint> perMzDps = perMzDataPointsMapping.get(dp.getMZ());
                                if (perMzDps == null) {
                                    perMzDataPointsMapping.put(dp.getMZ(), new ArrayList<DataPoint>());
                                    perMzDps = perMzDataPointsMapping.get(dp.getMZ());
                                }
                                perMzDps.add(dp);
                            }
                        }
                    }
                }
                
                // Build averaged dps list
                ArrayList<DataPoint> avgDps = new ArrayList<DataPoint>();
                for (Double mz : perMzDataPointsMapping.keySet()) {
                    ArrayList<DataPoint> mzDps = perMzDataPointsMapping.get(mz);
                    Double avg = 0d;
                    for (DataPoint dp : mzDps) {
                        avg += dp.getIntensity();
                    }
                    avg /= mzDps.size();
                    // New DataPoint at "fake" scan num
                    DataPoint avg_dp = new SimpleDataPoint(mz, avg);
                    avgDps.add(avg_dp);
                }
                
                SimpleScan newScan = new SimpleScan(refScan);
                newScan.setScanNumber(halfNbMarginScans + n + 1);
                newScan.setRetentionTime(avgRT + approxDeltaRT * n);
                newScan.setSpectrumType(MassSpectrumType.CENTROIDED);
                newScan.setDataPoints(avgDps.toArray(new DataPoint[avgDps.size()]));
                
                rawDataFileWriter.addScan(newScan);
            }
                
            // Create new averaged RDF
            // Finalize writing
            avgRDF = rawDataFileWriter.finishWriting();
            // project.addFile(avgRDF);
            
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        // Done: finalize
        //**avgPeak.finishAveragedPeak();
        
        return avgRDF;
        
    }
    
    
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

    //Implement table cell tool tips.  
    @Override
    public String getToolTipText(MouseEvent me) {
        
    
        PeakListTable table = (PeakListTable)me.getSource();
        
        String tip = null;
        java.awt.Point p = me.getPoint();

        int row = table.rowAtPoint(p);
        int col = table.columnAtPoint(p);
        
        try {
            if (row >= NB_HEADER_ROWS) {
                Object value = table.getValueAt(row, col);
                if (value != null) {
                    if (value instanceof String && !value.equals("") && !value.equals(JoinAlignerGcModule.MISSING_PEAK_VAL)) {

                        // Sort rows by ascending RT
                        final PeakListRow[] peakListRows = PeakListTable.getPeakListSortedByRT(table.peakList);

                        PeakListRow pl_row = peakListRows[col-1];
                        RawDataFile rdf = table.peakList.getRawDataFile(row - NB_HEADER_ROWS);
                        //final Feature selectedPeak = pl_row.getPeak(rdf);
                        int len = rdf.getName().indexOf(" ");
                        tip = "<b>(<span color=#009900>" + rdf.getName().substring(0, len + 1) + "</span> | " +
                                "<span color=#0000CC>" + rtFormat.format(pl_row.getAverageRT()) + "</span>)</b>";
                    }
                }
            }
            //tip = getValueAt(row, col).toString();
        } catch (RuntimeException e1) {
            //catch null pointer exception if mouse is over an empty line
        }

        return tip;
    }

    
    public PeakList getPeakList() {
	return peakList;
    }

    // Returns a cloned sorted rows list
    private static PeakListRow[] getPeakListSortedByRT(PeakList pl) {
            
        // Sort rows by ascending RT
        final PeakListRow[] peakListRows = pl.getRows().clone();
        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.RT,
                SortingDirection.Ascending));
        
        return peakListRows;
    }

    public TableCellEditor getCellEditor(int row, int column) {
        
//	CommonColumnType commonColumn = pkTableModel.getCommonColumn(column);
//	if (commonColumn == CommonColumnType.IDENTITY) {
        if (column > 0 && row == 1) {
            int col = column; //this.convertColumnIndexToModel(column);
            peakListRow = PeakListTable.getPeakListSortedByRT(peakList)[col-1]; //peakList.getRow(col-1);
            this.convertRowIndexToModel(row);
            
            

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

    
//    boolean exportToCSV(boolean separatedOutputs) {
//        
//        JFileChooser fileChooser = new JFileChooser();
//        fileChooser.setMultiSelectionEnabled(false);
//        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV FILES", "csv");
//        fileChooser.setFileFilter(filter);
//
//        String currentPath = csvFilename;
//        if (currentPath.length() > 0) {
//            File currentFile = new File(currentPath);
//            File currentDir = currentFile.getParentFile();
//            if (currentDir != null && currentDir.exists())
//                fileChooser.setCurrentDirectory(currentDir);
//        }
//
//        int returnVal = fileChooser.showDialog(null, "Select file");
//
//        if (returnVal == JFileChooser.APPROVE_OPTION) {
//            String selectedPath = fileChooser.getSelectedFile().getPath();
//            
//            // Unified output file
//            if (!selectedPath.endsWith(".csv"))
//                selectedPath += ".csv";
//            // Separated output files (rt + area)
//            String basePath = null, rtPath = null, areaPath = null;
//            if (separatedOutputs) {
//                basePath = selectedPath.substring(0, selectedPath.length()-4);
//                rtPath = basePath + "-rt.csv";
//                areaPath = basePath + "-area.csv";
//            }
//            
//            // Try 
//            try {
//                // Open file
//                FileWriter fileWriter = new FileWriter(selectedPath);
//                BufferedWriter writer = new BufferedWriter(fileWriter);
//                // Open rt + area files
//                FileWriter fileWriterRt, fileWriterArea;
//                BufferedWriter writerRt = null, writerArea = null;
//                if (separatedOutputs) {
//                    fileWriterRt = new FileWriter(rtPath);
//                    writerRt = new BufferedWriter(fileWriterRt);
//                    fileWriterArea = new FileWriter(areaPath);
//                    writerArea = new BufferedWriter(fileWriterArea);
//                }
//
//                // Export table values
//                String sepStr = " / ";
//                for (int i=0; i < tm.getRowCount(); ++i) {
//                    for (int j=0; j < tm.getColumnCount(); ++j) {
//                        
//                        String str = tm.getValueAt(i, j).toString();
//                        
//                        writer.write(str);
//                        if (separatedOutputs) {
//                            String rtStr, areaStr;
//                            
//                            int sepIdx = str.lastIndexOf(sepStr);
//                            if (sepIdx != -1) {
//                                rtStr = str.substring(0, sepIdx);
//                                areaStr = str.substring(sepIdx + sepStr.length(), str.length());
//                                writerRt.write(rtStr);
//                                writerArea.write(areaStr);
//                            } else {
//                                writerRt.write(str);
//                                writerArea.write(str);
//                            }
//                        }
//                        
//                        if (j != tm.getColumnCount() - 1) {
//                            writer.write(";");
//                            if (separatedOutputs) {
//                                writerRt.write(";");
//                                writerArea.write(";");
//                            }
//                        }
//                    }
//                    writer.newLine();
//                    if (separatedOutputs) {
//                        writerRt.newLine();
//                        writerArea.newLine();                        
//                    }
//                }
//
//                // Close file
//                writer.close();
//                if (separatedOutputs) {
//                    writerRt.close();
//                    writerArea.close();                                            
//                }
//                
//                csvFilename = selectedPath;
//                csvFilenames = new String[] { selectedPath, rtPath, areaPath };
//                
//                return true;
//                
//            } catch (Exception e) {
//                e.printStackTrace();
//                return false;
//            }
//        }
//       
//        return false;
//    }
    
    String getCSVfilename() {
        return PeakListTable.csvFilename;
    }
    String[] getCSVfilenames() {
        return PeakListTable.csvFilenames;
    }
    
    String[] getJDXfilenames() {
        return PeakListTable.jdxFilenames;
    }
    
    
}