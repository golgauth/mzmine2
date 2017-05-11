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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.text.NumberFormat;

import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationSingleTask;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class JoinAlignerGCParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final StringParameter peakListName = new StringParameter(
            "Peak list name", "Peak list name", "Aligned peak list");

    // Since clustering is now order independent, option removed!
    /*
    public static final ComboParameter<RowVsRowOrderType> comparisonOrder = new ComboParameter<RowVsRowOrderType>(
            "Comparison Order", 
            "In which order peak lists (samples) should be compared to each other while running the alignment algorihtm", 
            RowVsRowOrderType.values()
            );
    */
    
    
    // Clustering linkage strategy
    public static final ComboParameter<ClusteringLinkageStrategyType> linkageStartegyType = new ComboParameter<ClusteringLinkageStrategyType>(
            "Clustering strategy", 
            "What strategy shall be used for the clustering algorithm decision making (See: \"Hierarchical clustering\" algorithms in general).", 
            ClusteringLinkageStrategyType.values(),
            ClusteringLinkageStrategyType.AVERAGE
            );
    
    
    
    //-- Use unaltered RDF...
    public static final BooleanParameter useOldestRDFAncestor = new BooleanParameter(
            "Use original raw data file", 
            "Chemical similarity is computed using unaleterd m/z profile at given scan from the very oldest Raw Data File ancestor (if it has not been removed). "
                            + "Unchecked: information are grabbed as usual (from the data file the peak list to be merged was built from).",
                            false
            );
    //--
    
    public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();
    public static final DoubleParameter MZWeight = new DoubleParameter(
	    "Weight for m/z", "Weight for chemical similarity. Score for perfectly matching m/z values.");

    public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();
    public static final DoubleParameter RTWeight = new DoubleParameter(
	    "Weight for RT", "Weight for retention times similarity. Score for perfectly matching RT values.");
    
    public static final DoubleParameter minScore = new DoubleParameter(
            "Minimum score", 
            "Minimum score for blast to be considered as successful " +
            "(WARN: 'Pearson correlation' similarity method can imply scores < 0.0 and/or > 1.0)",
            NumberFormat.getNumberInstance(), JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE);
    
//    public static final DoubleParameter IDWeight = new DoubleParameter(
//            "Weight for identity", "Weight for identities similarity. Score for perfectly matching identities.");


    //*** GLG HACK: Added...
    public static final BooleanParameter useApex = new BooleanParameter(
            "Use apex",
            "If checked, uses apex scan for RT and MZ profile. Otherwise, 5% of the scans around the apex are used to redefine averaged RT and MZ profile",
            true);
    public static final BooleanParameter useKnownCompoundsAsRef = new BooleanParameter(
            "Use RT recalibration",
            "If checked, uses compounds with known identities to ease alignment",
            true);

    public static final RTToleranceParameter RTToleranceAfter = new RTToleranceParameter(
            "RT tolerance post-recalibration",
            "Ignored if \"Use RT recalibration\" is unchecked. Maximum allowed difference between two RT values after RT recalibration");
    
    
    public static final BooleanParameter exportDendrogram = new BooleanParameter(
            "Export dendrogram",
            "If checked, exports the clustering resulting dendrogram to the given PNG file.",
            false);
    public static final FileNameParameter dendrogramPngFilename = new FileNameParameter(
            "Dendrogram output filename",
            " Requires \"Export dendrogram\" checked."
                    + " Name of the resulting PNG file to write the clustering resulting dendrogram to."
                    + " If the file already exists, it will be overwritten.",
            "png");

    
    //***
    
    /** GLG HACK: temporarily removed for clarity
    public static final BooleanParameter SameChargeRequired = new BooleanParameter(
	    "Require same charge state",
	    "If checked, only rows having same charge state can be aligned");

    public static final BooleanParameter SameIDRequired = new BooleanParameter(
	    "Require same ID",
	    "If checked, only rows having same compound identities (or no identities) can be aligned");

    public static final OptionalModuleParameter compareIsotopePattern = new OptionalModuleParameter(
	    "Compare isotope pattern",
	    "If both peaks represent an isotope pattern, add isotope pattern score to match score",
	    new IsotopePatternScoreParameters());
    **/
    
    
    
    

    // Since clustering is now order independent, option removed!
    public JoinAlignerGCParameters() {
	super(new Parameter[] { peakLists, 
	        useOldestRDFAncestor, 
	        /*comparisonOrder,*/
	        linkageStartegyType,  peakListName, 
	        MZTolerance, MZWeight,
		RTTolerance, RTWeight,
		minScore,
//		IDWeight,
		useApex, useKnownCompoundsAsRef, RTToleranceAfter, 
		/*SameChargeRequired, SameIDRequired,
		compareIsotopePattern*/ 
		exportDendrogram, dendrogramPngFilename
		});
    }

}
