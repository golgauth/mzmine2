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

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.base.Strings;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.AlignedRowProps;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.JoinAlignerGcModule;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.RawDataFileSorter;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.visualization.peaklisttable.ColumnSettingParameter;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableParameters;
import net.sf.mzmine.modules.visualization.peaklisttable.table.CommonColumnType;
import net.sf.mzmine.modules.visualization.peaklisttable.table.DataFileColumnType;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.components.ColumnGroup;
import net.sf.mzmine.util.components.GroupableTableHeader;


/**
 * 
 */
public class PeakListFullTableModel extends DefaultTableModel implements
	MouseListener {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final Font editFont = new Font("SansSerif", Font.PLAIN, 10);

    private FormattedCellRenderer mzRenderer, rtRenderer, intensityRenderer;
    private TableCellRenderer peakShapeRenderer, identityRenderer,
	    peakStatusRenderer, datapointsRenderer, qcRenderer;
    private DefaultTableCellRenderer defaultRenderer, defaultRendererLeft;

    private ParameterSet parameters;
    private PeakList peakList;
//    private GroupableTableHeader header;

    private TableColumn columnBeingResized;
    
    private Format rtFormat = MZmineCore.getConfiguration().getRTFormat();
    private Format areaFormat = MZmineCore.getConfiguration().getIntensityFormat();


    /**
     * 
     */
    PeakListFullTableModel(ParameterSet parameters, PeakList peakList) {

	this.parameters = parameters;
	this.peakList = peakList;

//	this.header = header;

//	header.addMouseListener(this);

	// prepare formatters
	NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
	NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	NumberFormat intensityFormat = MZmineCore.getConfiguration()
		.getIntensityFormat();

	// prepare cell renderers
	mzRenderer = new FormattedCellRenderer(mzFormat);
	rtRenderer = new FormattedCellRenderer(rtFormat);
	intensityRenderer = new FormattedCellRenderer(intensityFormat);
	peakShapeRenderer = new PeakShapeCellRenderer(peakList, parameters);
	identityRenderer = new CompoundIdentityCellRenderer();
	peakStatusRenderer = new PeakStatusCellRenderer();
	defaultRenderer = new DefaultTableCellRenderer();
	defaultRenderer.setHorizontalAlignment(SwingConstants.CENTER);
	defaultRendererLeft = new DefaultTableCellRenderer();
	defaultRendererLeft.setHorizontalAlignment(SwingConstants.LEFT);
	datapointsRenderer = new FormattedCellRenderer(new DecimalFormat());
	qcRenderer = new FormattedCellRenderer(new DecimalFormat());

    }


    public void setValueAt(Object value, int row, int col) {

        if (value == null || col < 1) return;

        // Sort rows by ascending RT
        final PeakListRow[] peakListRows = peakList.getRows().clone();
        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.RT,
                SortingDirection.Ascending));

        PeakListRow peakListRow = peakListRows[col-1]; //peakList.getRow(col-1);
        
        if (value instanceof PeakIdentity) {
            peakListRow.setPreferredPeakIdentity((PeakIdentity) value);
            super.setValueAt(value, row, col);
//            // Indicate the change has happened:
//            fireTableDataChanged();
        }
    }
    public boolean isCellEditable(int row, int col) {

        return (row == 1 && col > 0);

    }

    
    
    public void fillRows() {


//        JTextField editorField = new JTextField();
//        editorField.setFont(editFont);
//        DefaultCellEditor defaultEditor = new DefaultCellEditor(editorField);

        // Sort rows by ascending RT
        final PeakListRow[] peakListRows = peakList.getRows().clone();
        Arrays.sort(peakListRows, new PeakListRowSorter(SortingProperty.RT,
                SortingDirection.Ascending));

        
        //**** Build rows (cell by cell)
        int nbHeaderRows = PeakListTable.NB_HEADER_ROWS;
        // RT row + Identity row + Identities frequencies + Peak Shape row + 1 row per sample (=Piaf)
        int nbRows = nbHeaderRows + peakList.getNumberOfRawDataFiles();
        // Row header col + 1 col per RT (=PeakListRow)
        int nbCols = 1 + peakList.getNumberOfRows();
        
        logger.info("Table size: " + nbRows + "/" + nbCols);
        
        
        int[] arrNbDetected = new int[peakList.getNumberOfRows()];
        PeakIdentity[] mainIdentities = new PeakIdentity[peakList.getNumberOfRows()];
        
        String strAdjustedRTs = "", strIdentities = "", strScores = "";
        String[] arrAdjustedRTs = null, arrIdentities = null, arrScores = null;
        
        // Build reference RDFs index
        RawDataFile[] rdf_sorted = peakList.getRawDataFiles().clone();
        Arrays.sort(rdf_sorted, new RawDataFileSorter(SortingDirection.Ascending));

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
                        objects.add("Identities info");
                        break;
                    case 3:
                        //objects.add("Peak Shape");
                        objects.add("Peaks detected");
                        break;
                    default:
                        //objects.add(a_pl_row.getPeaks()[i-nbHeaderRows].getDataFile().getName());
                        RawDataFile rdf = this.peakList.getRawDataFiles()[i - nbHeaderRows];
                        objects.add(rdf.getName().substring(0, rdf.getName().indexOf(" ")));
                        break;
                    }
                } else {
                    
                    PeakListRow a_pl_row = peakListRows[j-1];
                    
                    switch (i) {
                    case 0:
                        objects.add(rtFormat.format(a_pl_row.getAverageRT()));
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

                        RawDataFile rdf = this.peakList.getRawDataFiles()[i - nbHeaderRows];
                        Feature peak = a_pl_row.getPeak(rdf);
                        if (peak != null) {
                            
                            PeakIdentity mainIdentity = a_pl_row.getPreferredPeakIdentity();

                            if (mainIdentity != null) {
                            
                                strAdjustedRTs = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowProps.PROPERTY_RTS);
                                strIdentities = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_NAMES);
                                strScores = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_SCORES);

                                // More than one rdf (align peak list) 
                                if (this.peakList.getRawDataFiles().length > 1) {
                                    
                                    if (strAdjustedRTs != null)
                                        arrAdjustedRTs = strAdjustedRTs.split(AlignedRowProps.PROP_SEP, -1);
                                    if (strIdentities != null)
                                        arrIdentities = strIdentities.split(AlignedRowProps.PROP_SEP, -1);
                                    if (strScores != null)
                                        arrScores = strScores.split(AlignedRowProps.PROP_SEP, -1);
                                    
                                    int rdf_idx = Arrays.asList(rdf_sorted).indexOf(rdf);
                                    //
                                    String peakAjustedRT = null;
                                    if (strAdjustedRTs != null) 
                                        peakAjustedRT = arrAdjustedRTs[rdf_idx];
                                    String peakIdentity = arrIdentities[rdf_idx];
                                    
                                    String score = arrScores[rdf_idx];
                                    String strScore = "";
                                    if (score != null && !score.isEmpty())
                                        strScore = " (" + rtFormat.format(Double.valueOf(score)) + ")";
                                    
                                    // Handle gap filled peaks
                                    if (peak.getFeatureStatus() == FeatureStatus.ESTIMATED
                                            || Strings.isNullOrEmpty(score)) {
                                        peakAjustedRT = "ESTIMATED";
                                        if (strAdjustedRTs != null && !Strings.isNullOrEmpty(arrAdjustedRTs[rdf_idx]))
                                                peakAjustedRT += peakAjustedRT;
                                        //peakAjustedRT = "ESTIMATED" + ((Strings.isNullOrEmpty(arrIdentities[rdf_idx])) ? "" : "/" + peakAjustedRT);
                                        peakIdentity = "NONE";
                                    }
                                    
                                    objects.add(rtFormat.format(peak.getRT()) + 
                                                ((strAdjustedRTs != null) ? " [" + peakAjustedRT + "]" : "") + 
                                                " / " + areaFormat.format(peak.getArea()) + 
                                                " / " + peakIdentity
                                                + strScore);
                                } 
                                // Handle regular single rdf peak list
                                else {
                                    objects.add(rtFormat.format(peak.getRT()) + 
                                            " / " + areaFormat.format(peak.getArea()));
                                }
                                //
                                mainIdentities[j-1] = mainIdentity;                                  
                            
                            } else {
                                // Case row identity is "null" (i.e. != "Unknown")
                                objects.add(rtFormat.format(peak.getRT()) + 
                                        " / " + areaFormat.format(peak.getArea()));
                            }
                            
                            arrNbDetected[j-1] += 1;
                        } else {
                            objects.add(JoinAlignerGcModule.MISSING_PEAK_VAL);
                        }
                        break;
                    }

                }
            }
            super.addRow(objects);               
        }
        
        // Update number of detected peaks
        for (int i=0; i < peakList.getNumberOfRows(); ++i) {
            super.setValueAt(arrNbDetected[i], nbHeaderRows-1, i+1);
        }
        // Update main identity + identities info
        for (int i=0; i < peakList.getNumberOfRows(); ++i) {
            PeakIdentity mainIdentity = mainIdentities[i];
            String strIdentities2 = "";
            if (mainIdentity != null) {
                // Set identities info string (leave blank if single rdf/sample peak list)
                if (this.peakList.getRawDataFiles().length > 1) {
                    
                    strIdentities2 = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_NAMES);
                    strScores = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_SCORES);
                    if (strIdentities2 != null) {
                        
                        arrIdentities = strIdentities2.split(AlignedRowProps.PROP_SEP, -1);
                        arrScores = strScores.split(AlignedRowProps.PROP_SEP, -1);
                        
//                        //--- Get sums
//                        
//                        Hashtable<String, Double> scoreAvgMap = new Hashtable<String, Double>();
//                        for (int i_s=0; i_s < arrIdentities.length; ++i_s) {
//                            
//                            // Skip 'no identity' peaks (that are not even 'Unknown') 
//                            if (arrIdentities[i_s] == null || arrIdentities[i_s].isEmpty())
//                                continue;
//                            
//                            if (!scoreAvgMap.keySet().contains(arrIdentities[i_s])) {
//                                scoreAvgMap.put(arrIdentities[i_s], Double.valueOf(arrScores[i_s]));
//                            } else {
//                                double curSum = scoreAvgMap.get(arrIdentities[i_s]);
//                                scoreAvgMap.put(arrIdentities[i_s], curSum + Double.valueOf(arrScores[i_s]));
//                            }
//                        }
                        
                        //---
                        
                        Set<String> aSet = new HashSet<String>(Arrays.asList(arrIdentities));
                        strIdentities2 = "";
                        
                        for (String str: aSet) {
                            
                            if(str != null && !str.isEmpty()) {
                                //int cardinality = CollectionUtils.cardinality(str, Arrays.asList(arrIdentities));
                                //strIdentities2 += str + " (" + cardinality + ")" + AlignedRowIdentity.IDENTITY_SEP;
                                
                                ////double avgScore = scoreAvgMap.get(str) / (double) peakList.getRawDataFiles().length; // / (double) cardinality;
                                ////strIdentities2 += str + " (" + rtFormat.format(avgScore) + ")" + AlignedRowIdentity.IDENTITY_SEP;
                                String strAvgScores = ((SimplePeakIdentity) mainIdentity).getPropertyValue(AlignedRowProps.PROPERTY_IDENTITIES_QUANT);
                                String[] arrAvgScores = strAvgScores.split(AlignedRowProps.PROP_SEP, -1);
                                // Find the proper avgScore
                                double avgScore = 0.0;
                                for (int k = 0; k < arrAvgScores.length; ++k) {
                                    String[] key_val = arrAvgScores[k].split(AlignedRowProps.KEYVAL_SEP, -1);
                                    if (key_val[0].equals(str)) {
                                        avgScore = Double.valueOf(key_val[1]);
                                        break;
                                    }
                                }
                                strIdentities2 += str + " (" + rtFormat.format(avgScore) + ")" + AlignedRowProps.PROP_SEP;
                            }
                        }
                        strIdentities2 = strIdentities2.substring(0, strIdentities2.length()-1);
                    }
                } else {
                    //strIdentities2 = mainIdentity.getName() + " (1)";
                    String strScore = mainIdentity.getPropertyValue(AlignedRowProps.PROPERTY_ID_SCORE);
                    strIdentities2 = mainIdentity.getName() + ((strScore != null) ? " (" + strScore + ")" : "");
                }
                // Set most frequent identity
                super.setValueAt(mainIdentity, nbHeaderRows-3, i+1);
            }
            super.setValueAt(strIdentities2, nbHeaderRows-2, i+1);
        }

    }

    
    public void createColumns() {
        
        int nbCols = 1 + peakList.getNumberOfRows();  
    
        for (int i = 0; i < nbCols; ++i) {

            TableColumn newColumn = new TableColumn();
            this.addColumn(newColumn);
            
        }
    }

//    public void createColumns() {
//
//        // clear column groups
//        ColumnGroup groups[] = header.getColumnGroups();
//        if (groups != null) {
//            for (ColumnGroup group : groups) {
//                header.removeColumnGroup(group);
//            }
//        }
//
//        // clear the column model
//        while (getColumnCount() > 0) {
//            TableColumn col = getColumn(0);
//            removeColumn(col);
//        }
//
//        // create the "average" group
//        ColumnGroup averageGroup = new ColumnGroup("Average");
//        header.addColumnGroup(averageGroup);
//
//        JTextField editorField = new JTextField();
//        editorField.setFont(editFont);
//        DefaultCellEditor defaultEditor = new DefaultCellEditor(editorField);
//
//        ColumnSettingParameter<CommonColumnType> csPar = parameters
//                .getParameter(PeakListTableParameters.commonColumns);
//        CommonColumnType visibleCommonColumns[] = csPar.getValue();
//
//        // This is a workaround for a bug - we need to always show the ID, m/z
//        // and RT columns, otherwise manual editing of peak identities does not
//        // work.
//        ArrayList<CommonColumnType> commonColumnsList = new ArrayList<>(
//                Arrays.asList(visibleCommonColumns));
//        commonColumnsList.remove(CommonColumnType.ROWID);
//        commonColumnsList.remove(CommonColumnType.AVERAGEMZ);
//        commonColumnsList.remove(CommonColumnType.AVERAGERT);
//        commonColumnsList.add(0, CommonColumnType.ROWID);
//        commonColumnsList.add(1, CommonColumnType.AVERAGEMZ);
//        commonColumnsList.add(2, CommonColumnType.AVERAGERT);
//
//        visibleCommonColumns = commonColumnsList.toArray(visibleCommonColumns);
//
//        ColumnSettingParameter<DataFileColumnType> dfPar = parameters
//                .getParameter(PeakListTableParameters.dataFileColumns);
//        DataFileColumnType visibleDataFileColumns[] = dfPar.getValue();
//
//        for (int i = 0; i < visibleCommonColumns.length; i++) {
//
//            CommonColumnType commonColumn = visibleCommonColumns[i];
//            int modelIndex = Arrays.asList(CommonColumnType.values()).indexOf(
//                    commonColumn);
//
//            TableColumn newColumn = new TableColumn(modelIndex);
//            newColumn.setHeaderValue(commonColumn.getColumnName());
//            newColumn.setIdentifier(commonColumn);
//
//            switch (commonColumn) {
//            case AVERAGEMZ:
//                newColumn.setCellRenderer(mzRenderer);
//                break;
//            case AVERAGERT:
//                newColumn.setCellRenderer(rtRenderer);
//                break;
//            case IDENTITY:
//                newColumn.setCellRenderer(identityRenderer);
//                break;
//            case COMMENT:
//                newColumn.setCellRenderer(defaultRendererLeft);
//                newColumn.setCellEditor(defaultEditor);
//                break;
//            case PEAKSHAPE:
//                newColumn.setCellRenderer(peakShapeRenderer);
//                break;
//            default:
//                newColumn.setCellRenderer(defaultRenderer);
//            }
//
//            this.addColumn(newColumn);
//            newColumn.setPreferredWidth(csPar.getColumnWidth(modelIndex));
//            if ((commonColumn == CommonColumnType.AVERAGEMZ)
//                    || (commonColumn == CommonColumnType.AVERAGERT)) {
//                averageGroup.add(newColumn);
//            }
//
//        }
//
//        for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
//
//            RawDataFile dataFile = peakList.getRawDataFile(i);
//            ColumnGroup fileGroup = new ColumnGroup(dataFile.getName());
//            header.addColumnGroup(fileGroup);
//
//            for (int j = 0; j < visibleDataFileColumns.length; j++) {
//
//                DataFileColumnType dataFileColumn = visibleDataFileColumns[j];
//                int dataFileColumnIndex = Arrays.asList(
//                        DataFileColumnType.values()).indexOf(dataFileColumn);
//                int modelIndex = CommonColumnType.values().length
//                        + (i * DataFileColumnType.values().length)
//                        + dataFileColumnIndex;
//
//                TableColumn newColumn = new TableColumn(modelIndex);
//                newColumn.setHeaderValue(dataFileColumn.getColumnName());
//                newColumn.setIdentifier(dataFileColumn);
//
//                switch (dataFileColumn) {
//                case MZ:
//                    newColumn.setCellRenderer(mzRenderer);
//                    break;
//                case PEAKSHAPE:
//                    newColumn.setCellRenderer(peakShapeRenderer);
//                    break;
//                case STATUS:
//                    newColumn.setCellRenderer(peakStatusRenderer);
//                    break;
//                case RT:
//                    newColumn.setCellRenderer(rtRenderer);
//                    break;
//                case RT_START:
//                    newColumn.setCellRenderer(rtRenderer);
//                    break;
//                case RT_END:
//                    newColumn.setCellRenderer(rtRenderer);
//                    break;
//                case DURATION:
//                    newColumn.setCellRenderer(rtRenderer);
//                    break;
//                case HEIGHT:
//                    newColumn.setCellRenderer(intensityRenderer);
//                    break;
//                case AREA:
//                    newColumn.setCellRenderer(intensityRenderer);
//                    break;
//                case CHARGE:
//                    newColumn.setCellRenderer(datapointsRenderer);
//                    break;
//                case DATAPOINTS:
//                    newColumn.setCellRenderer(datapointsRenderer);
//                    break;
//                case FWHM:
//                    newColumn.setCellRenderer(qcRenderer);
//                    break;
//                case TF:
//                    newColumn.setCellRenderer(qcRenderer);
//                    break;
//                case AF:
//                    newColumn.setCellRenderer(qcRenderer);
//                    break;
//                default:
//                    newColumn.setCellRenderer(defaultRenderer);
//                    break;
//                }
//
//                this.addColumn(newColumn);
//                newColumn.setPreferredWidth(dfPar
//                        .getColumnWidth(dataFileColumnIndex));
//                fileGroup.add(newColumn);
//            }
//
//        }
//
//    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e) {
	// ignore
    }

    /**
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
	// ignore
    }

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
	// ignore
    }

    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
	//columnBeingResized = header.getResizingColumn();
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
        // TODO Auto-generated method stub
        
    }

//    /**
//     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
//     */
//    public void mouseReleased(MouseEvent e) {
//
//	if (columnBeingResized == null)
//	    return;
//
//	ColumnSettingParameter<CommonColumnType> csPar = parameters
//		.getParameter(PeakListTableParameters.commonColumns);
//
//	ColumnSettingParameter<DataFileColumnType> dfPar = parameters
//		.getParameter(PeakListTableParameters.dataFileColumns);
//
//	final int modelIndex = columnBeingResized.getModelIndex();
//	final int newWidth = columnBeingResized.getPreferredWidth();
//
//	final int numOfCommonColumns = CommonColumnType.values().length;
//	final int numOfDataFileColumns = DataFileColumnType.values().length;
//
//	if (modelIndex < numOfCommonColumns) {
//	    csPar.setColumnWidth(modelIndex, newWidth);
//	} else {
//	    int dataFileColumnIndex = (modelIndex - numOfCommonColumns)
//		    % numOfDataFileColumns;
//	    dfPar.setColumnWidth(dataFileColumnIndex, newWidth);
//
//	    // set same width to other data file columns of this type
//	    for (int dataFileIndex = peakList.getNumberOfRawDataFiles() - 1; dataFileIndex >= 0; dataFileIndex--) {
//		int columnIndex = numOfCommonColumns
//			+ (dataFileIndex * numOfDataFileColumns)
//			+ dataFileColumnIndex;
//
//		TableColumn col = this.getColumnByModelIndex(columnIndex);
//
//		int currentWidth = col.getPreferredWidth();
//
//		if (currentWidth != newWidth) {
//		    col.setPreferredWidth(newWidth);
//		}
//	    }
//
//	}
//
//    }
//
//    public TableColumn getColumnByModelIndex(int modelIndex) {
//	Enumeration<TableColumn> allColumns = this.getColumns();
//	while (allColumns.hasMoreElements()) {
//	    TableColumn col = allColumns.nextElement();
//	    if (col.getModelIndex() == modelIndex)
//		return col;
//	}
//	return null;
//    }

}