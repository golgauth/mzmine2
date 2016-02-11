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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import com.jogamp.nativewindow.util.Rectangle;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableParameters;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakShapeNormalization;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.components.ColorCircle;
import net.sf.mzmine.util.components.CombinedXICComponent;
import net.sf.mzmine.util.components.PeakXICComponent;

/**
 * Table cell renderer
 */
public class UnifiedCellRenderer implements TableCellRenderer {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    static final Font defaultFont = new Font("SansSerif", Font.PLAIN, 10);
    
    private NumberFormat format;
    private PeakList peakList;
    private ParameterSet parameters;
    
    private static final ColorCircle greenCircle = new ColorCircle(Color.green);
    private static final ColorCircle redCircle = new ColorCircle(Color.red);
    private static final ColorCircle yellowCircle = new ColorCircle(
            Color.yellow);
    private static final ColorCircle orangeCircle = new ColorCircle(
            Color.orange);

    /**
     */
    UnifiedCellRenderer(NumberFormat format, PeakList peakList, ParameterSet parameters) {
        this.format = format;
        this.peakList = peakList;
        this.parameters = parameters;
    }

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable,
     *      java.lang.Object, boolean, boolean, int, int)
     */
    public Component getTableCellRendererComponent(JTable table, Object value,
	    boolean isSelected, boolean hasFocus, int row, int column) {

	JComponent newComp = null;
        if (value != null) {

            // CompoundIdentityCellRenderer
            if (value instanceof PeakIdentity) {
                newComp = new JLabel();
            }
            // Otherwise
            else { 
               newComp = new JPanel();
            }
        } else {
            newComp = new JPanel();
        }

	Color bgColor, fgColor;

	if (isSelected)
	    bgColor = table.getSelectionBackground();
	else
	    //bgColor = table.getBackground();
            if (column == 0 || row < PeakListTable.NB_HEADER_ROWS) {
                bgColor = Color.lightGray; //table.getTableHeader().getBackground();
            } else {
                bgColor = table.getBackground();
            }

        newComp.setBackground(bgColor);

	if (hasFocus) {
	    Border border = null;
	    if (isSelected)
		border = UIManager
			.getBorder("Table.focusSelectedCellHighlightBorder");
	    if (border == null)
		border = UIManager.getBorder("Table.focusCellHighlightBorder");

	    /*
	     * The "border.getBorderInsets(newPanel) != null" is a workaround
	     * for OpenJDK 1.6.0 bug, otherwise setBorder() may throw a
	     * NullPointerException
	     */
	    if ((border != null) && (border.getBorderInsets(newComp) != null)) {
		newComp.setBorder(border);
	    }
	}
	

	newComp.setFont(defaultFont);
	// CompoundIdentityCellRenderer
	if (value instanceof PeakIdentity) {
	    //newComp = new JLabel();
	    ((JLabel) newComp).setHorizontalAlignment(JLabel.LEFT);
	    newComp.setOpaque(true);

	    PeakIdentity identity = (PeakIdentity) value;
	    ((JLabel) newComp).setText(identity.getName());
	    String toolTipText = identity.getDescription();
	    newComp.setToolTipText(toolTipText);
	    
	    // Blue for not UNKNOWN...
            if (!identity.getName().equals("Unknown"))
                bgColor = Color.cyan;
            newComp.setBackground(bgColor);


	    //return (JLabel) newComp;
	}
	// PeakShapeCellRenderer
	else if (value instanceof Feature || value instanceof PeakListRow) {
	    if (value instanceof Feature) {

	        Feature peak = (Feature) value;
	        double maxHeight = 0;

	        PeakShapeNormalization norm = parameters.getParameter(
	                PeakListTableParameters.peakShapeNormalization).getValue();
	        if (norm == null)
	            norm = PeakShapeNormalization.ROWMAX;
	        switch (norm) {
	        case GLOBALMAX:
	            maxHeight = peakList.getDataPointMaxIntensity();
	            break;
	        case ROWMAX:
	            int rowNumber = peakList.getPeakRowNum(peak);
	            maxHeight = peakList.getRow(rowNumber)
	                    .getDataPointMaxIntensity();
	            break;
	        default:
	            maxHeight = peak.getRawDataPointsIntensityRange()
	            .upperEndpoint();
	            break;
	        }

	        PeakXICComponent xic = new PeakXICComponent(peak, maxHeight);

	        newComp.add(xic);

	        newComp.setToolTipText(xic.getToolTipText());

	    }

	    if (value instanceof PeakListRow) {

	        PeakListRow plRow = (PeakListRow) value;

	        RawDataFile[] dataFiles = peakList.getRawDataFiles();
	        Feature[] peaks = new Feature[dataFiles.length];
	        for (int i = 0; i < dataFiles.length; i++) {
	            peaks[i] = plRow.getPeak(dataFiles[i]);
	        }

	        CombinedXICComponent xic = new CombinedXICComponent(peaks,
	                plRow.getID());

	        newComp.add(xic);

	        newComp.setToolTipText(xic.getToolTipText());

	    }

	}
	else {

	    if (value != null) {
	        
	        // PeakStatusCellRenderer
	        if (value instanceof FeatureStatus) {
	            FeatureStatus status = (FeatureStatus) value;

	            switch (status) {
	            case DETECTED:
	                newComp.add(greenCircle);
	                break;
	            case ESTIMATED:
	                newComp.add(yellowCircle);
	                break;
	            case MANUAL:
	                newComp.add(orangeCircle);
	                break;
	            default:
	                newComp.add(redCircle);
	                break;
	            }

	            newComp.setToolTipText(status.toString());

	        }

	        // FormattedCellRenderer
	        String text;
	        if (value instanceof Number) {
	            text = format.format((Number) value);
	            if (value instanceof Integer) {
	                if ((Integer) value <= this.peakList.getNumberOfRawDataFiles() * 1.0 / 3.0)
	                    bgColor = Color.pink;
                        else if ((Integer) value <= this.peakList.getNumberOfRawDataFiles() * 2.0 / 3.0)
                            bgColor = Color.orange;
                        else
                            bgColor = Color.green;
	                newComp.setBackground(bgColor);
	            }
	        } else {
	            text = value.toString();
	            if (text.equals("0")) {
                        bgColor = new Color(230, 230, 230); // Light light gray
                        newComp.setBackground(bgColor);
	            }
	        }

	        JLabel newLabel = new JLabel(text, JLabel.CENTER);

	        //	        if (font != null)
	        //	            newLabel.setFont(font);
	        //	        else if (table.getFont() != null)
	        //	            newLabel.setFont(table.getFont());

	        newComp.add(newLabel);
	    } else {
                newComp.add(redCircle);
            }
	}



	return newComp;

    }

}
