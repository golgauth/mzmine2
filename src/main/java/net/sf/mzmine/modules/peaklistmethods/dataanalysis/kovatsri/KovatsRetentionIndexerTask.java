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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.kovatsri;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;


// Dot
import org.apache.commons.math.linear.ArrayRealVector;
//Pearson
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.jcamp.parser.JCAMPException;

import com.google.common.collect.Range;



public class KovatsRetentionIndexerTask extends AbstractTask {

    // Logger.
    private static Logger logger = Logger.getLogger(KovatsRetentionIndexerTask.class.getName());

    String PROPERTY_FORMULA = "Molecular formula";

    private final MZmineProject project;


    // Counters.
//    private int progressItemNumberTotal;
//    private int progressItemNumber;

    private final PeakList[] peakLists;
    private final PeakList alkanesPeakList;
    
    private final KovatsMethodType kovatsMethod;
    

    /**
     * Create the identification task.
     * 
     * @param parameters
     *            task parameters.
     * @param list
     *            peak list to operate on.
     */
    KovatsRetentionIndexerTask(final MZmineProject project, final ParameterSet parameters) {

        this.project = project;

        peakLists = parameters.getParameter(KovatsRetentionIndexerParameters.PEAK_LISTS).getValue().getMatchingPeakLists();
        
//        alkanesPeakList = parameters.getParameter(KovatsRetentionIndexParameters.ALKANES_REF_PEAKLIST).getValue().getMatchingPeakLists()[0];
        String alkanesPeakListName = parameters.getParameter(KovatsRetentionIndexerParameters.ALKANES_REF_PEAKLIST_NAME).getValue();
        alkanesPeakList = KovatsRetentionIndexerParameters.getPeakListByName(alkanesPeakListName);
        
        kovatsMethod = parameters.getParameter(KovatsRetentionIndexerParameters.KOVATS_METHOD).getValue();
    }

    @Override
    public double getFinishedPercentage() {

        return 0d;//(double) progressItemNumber / (double) progressItemNumberTotal;
    }

    @Override
    public String getTaskDescription() {
        
        return "Computing of the Kovats RI from alkanes list '"
              + this.alkanesPeakList.getName() + "'";    
    }

    @Override
    public void run() {

        setStatus(TaskStatus.PROCESSING);
    	
        if (!isCanceled()) {
        	
        	// Get reference alkane peaks
        	List<PeakListRow> alkanesList = new ArrayList<>();
        	for (PeakListRow a_row: alkanesPeakList.getRows()) {
        		
        		////Feature alkane_peak = a_row.getBestPeak();
        		// Alkane row was found
        		////boolean isKnownCompound = (!a_row.getPreferredPeakIdentity().getName().equals(JDXCompound.UNKNOWN_JDX_COMP.getName()));
        		////if (isKnownCompound) {
        		if (JDXCompound.isKnownIdentity(a_row.getPreferredPeakIdentity())) {
        			alkanesList.add(a_row);
        			System.out.println(">>>> Found alkane: " + a_row.getPreferredPeakIdentity());
        		} else {
        			//
        		}
        	}
        	// Sort by ascending RT
        	PeakListRow[] alkanesArr = alkanesList.toArray(new PeakListRow[alkanesList.size()]);
        	Arrays.sort(alkanesArr, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
        	
        	// Need at least 2 alkanes
        	if (alkanesList.size() > 1) {
        		
        		List<Feature> ir_finding_failures = new ArrayList<>();
        		
        		for (PeakList peakList : this.peakLists) {
        			
        			for (PeakListRow plRow : peakList.getRows()) {
        				
        				// For each peak overall...
        				for (Feature peak : plRow.getPeaks()) {
        					
        	            	// Try Find adjacent n-alkanes 'n' and 'N'
        					PeakListRow n_alkane = null;
        					PeakListRow N_alkane = null;
        					
        					int i = 0;
    						// Abort if before '1st' alkane
        					if (peak.getRT() >= alkanesArr[i].getAverageRT()) {
    							
	        					for (i=1; i < alkanesArr.length; ++i) { // !! Start at "i=1" !!
	        						
	        						PeakListRow alkane_row = alkanesArr[i];
	        						
	        						// Found 'N' alkane
	        						if (alkane_row.getAverageRT() >= peak.getRT()) {
	        							N_alkane = alkane_row;
	        							break;
	        						}
	        					}
    						}
    						
        					// If adjacent alkanes found
        					if (N_alkane != null) {
	        					
        						n_alkane = alkanesArr[i - 1];
        						
        						//String n_alkane_molform = ((JDXCompound) n_alkane.getPreferredPeakIdentity()).getFormula();
        						String n_alkane_molform = n_alkane.getPreferredPeakIdentity().getPropertyValue(PROPERTY_FORMULA);
           						//String N_alkane_molform = ((JDXCompound) N_alkane.getPreferredPeakIdentity()).getFormula();
        						String N_alkane_molform = N_alkane.getPreferredPeakIdentity().getPropertyValue(PROPERTY_FORMULA);

        						int nC = 0;
//	        	            	try {
//		        	            	
//	        						// Nb atomes of Carbon in alkane 'n'
//	        						int indexOfC = n_alkane_molform.indexOf('C');
//	        						int indexOfSpace = n_alkane_molform.indexOf(' ');
//	        	            		nC = ((indexOfC == 0 && indexOfSpace > indexOfC) 
//		        	            			? Integer.valueOf(n_alkane_molform.substring(indexOfC + 1, indexOfSpace)) : -1);
//		        	            	
//	        	            		
//	        	            		/** --- Check alkane --- */
//		        	            	// Make also sure we are talking about proper alkanes [ form: C(n)H(2n+2) ]
//	        						// Nb atomes of Hydrogen in alkane 'n'
//	        						int indexOfH = n_alkane_molform.indexOf('H');
//	        						int indexOfSecondSpace = n_alkane_molform.indexOf(' ', indexOfH);
//	        						int indexOfEnd = ((indexOfSecondSpace == -1) ? n_alkane_molform.length() : indexOfSecondSpace);
//	        	            		nH = ((indexOfH == 0 && indexOfEnd > indexOfH) 
//		        	            			? Integer.valueOf(n_alkane_molform.substring(indexOfH + 1, indexOfEnd)) : -1);
//	        	            		// Shall we accept "cyclic" alkanes [ form: C(n)H(2n) ] ?
//	        	            		if ((indexOfSecondSpace == -1) && (nH != (2 * nC + 2)) /*&& (nH != (2 * nC))*/) {
//	        	            			
//		        	        			setErrorMessage("Formula for peak '" + n_alkane 
//		        	        					+ "' (" + n_alkane_molform + ") couldn't be parsed as an actual "
//		        	        							+ "alkane molecule (not of form: 'C(n)H(2n+2)' )!");
//		        	            		setStatus(TaskStatus.ERROR);
//	        	            		}
//	        	            		/** ------------------- */
//	        	            		
//	        	            	} catch (NumberFormatException nfe) {
//	        	            		// 
//	        	            	}

        	            		nC = isAlcane(n_alkane_molform, true); //Shall we accept "cyclic" alkanes
        	            		int NC = isAlcane(N_alkane_molform, true);
	        	            	
	        	            	if (nC > 0 && NC > 0) {
	        	            		
		        	            	// RTs for alkanes 'n' and 'N' alkanesList
		        	            	double n_rt = n_alkane.getAverageRT();
		        	            	double N_rt = N_alkane.getAverageRT();
		        	            	
		        	            	double ratio;
		        	            	// Linear [Programmed temperature chromatography]
		        	            	// 		=> Usually this one is used with 'Gas Chromato' !
		        	            	if (this.kovatsMethod == KovatsMethodType.PROGRAMMED_TEMPERATURE) {
		        	            		ratio = (peak.getRT() - n_rt) / (N_rt - n_rt);
		        	            	}
		        	            	// Log [Isothermal chromatography]
		        	            	else {
		        	            		ratio = (Math.log(peak.getRT()) - Math.log(n_rt)) / (Math.log(N_rt) - Math.log(n_rt));
		        	            	} 
		        	            	
		        	            	// Set 'I' (the Kovats retention index)
		        	            	double d_ri = 100d * (nC + ratio);
		        	            	KovatsRetentionIndexerTask.setRetentionIndex(peak, d_ri);
		        	            	
	        	            		
	        	            		System.out.println("Success! - Peak '" + peak 
	        	            				+ "' is in between alkanes [" + n_alkane_molform + " @" + n_alkane.getAverageRT() 
	        	            				+ ", " + N_alkane_molform + " @" + N_alkane.getAverageRT() + "]");
	        	            	
	        	            	} else {
	        	            		
	        	        			setErrorMessage("Formula for peak '" + n_alkane 
	        	        					+ "' (" + n_alkane_molform + ") or (" + N_alkane_molform + ") "
	        	        							+ "couldn't be parsed as an alkane one!");
	        	            		setStatus(TaskStatus.ERROR);
	        	            		
	        	            	}
	        		
        					} else {
        						
        						ir_finding_failures.add(peak);
        					}
        					
        				}
        			}
        		}
        		
        		if (ir_finding_failures.size() > 0) {
        			
        			for (Feature fail_peak : ir_finding_failures) {
        				String msg = "## FAILED ALKANES: Couldn't find bounding alkanes for peak: " + fail_peak + "!";
        				System.out.println(msg);
        			}
//        			String msg = "Couldn't find bounding alkanes for peak: " + ir_finding_failures.get(0) + "!";
//        			setErrorMessage(msg);
//            		setStatus(TaskStatus.ERROR);
        			
        		}
        		
        		setStatus(TaskStatus.FINISHED);

            	
        		
        	} else {
        		setErrorMessage("Need at least TWO alkanes identified in alkanes reference peaklist!");
        		setStatus(TaskStatus.ERROR);
        	}
        	
        }
        
    }

    /**
     * 
     */
    public static void setRetentionIndex(Feature peak, double d_ri) {
        
    	int nb_digits = MZmineCore.getConfiguration().getRTFormat().getMaximumFractionDigits();
    	
    	// Store double 'RI' into an int
    	int i_ri = (int) Math.round(d_ri * Math.pow(10, nb_digits));
    	peak.setCharge(i_ri);
    }
    /**
     * 
     */
    public static double getRetentionIndex(Feature peak) {
        
    	int nb_digits = MZmineCore.getConfiguration().getRTFormat().getMaximumFractionDigits();
        
    	// 'd_ri' from RI strored as int
    	double d_ri = ((double) peak.getCharge()) / (Math.pow(10, nb_digits));
    	return d_ri;
    }
    /**
     * 
     */
    public static boolean isRetentionIndexSet(Feature peak) {
        
    	return (peak.getCharge() != 0);
    }

    
    private static int isAlcane(String molform, boolean acceptCyclic) {
    	
    	int nC = 0, nH;
    	boolean is_alcane = false;
    	
    	try {
    		// Nb atomes of Carbon in alkane 'n'
    		int indexOfC = molform.indexOf('C');
    		int indexOfSpace = molform.indexOf(' ');
    		// Methane (C H4) case
    		if (indexOfSpace == indexOfC + 1)
    			nC = 1;
    		else
	    		nC = ((indexOfC == 0 && indexOfSpace > indexOfC) 
	    				? Integer.valueOf(molform.substring(indexOfC + 1, indexOfSpace)) : -1);

    		// Nb atomes of Hydrogen in alkane 'n'
    		int indexOfH = molform.indexOf('H');
    		int indexOfSecondSpace = molform.indexOf(' ', indexOfH);
    		int indexOfEnd = ((indexOfSecondSpace == -1) ? molform.length() : indexOfSecondSpace);
    		nH = ((indexOfH == 0 && indexOfEnd > indexOfH) 
    				? Integer.valueOf(molform.substring(indexOfH + 1, indexOfEnd)) : -1);

    		// Shall we accept "cyclic" alkanes [ form: C(n)H(2n) ] ?
    		is_alcane =  (indexOfSecondSpace == -1 && (nH == (2 * nC + 2) || (nH != (2 * nC) && acceptCyclic)));
    		
    	} catch (NumberFormatException nfe) {
    		// 
    	}
		
		return (is_alcane ? nC : 0);
    }
    
    
    
}
