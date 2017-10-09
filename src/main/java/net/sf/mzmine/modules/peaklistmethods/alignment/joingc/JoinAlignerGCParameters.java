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

import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.ClustererType;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.clusterers.LinkType;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationSingleTask;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class JoinAlignerGCParameters extends SimpleParameterSet {


    // Clustering method 
	// [0:RAM greedy but cool, 1: OK but poor parser, 2: Good but too many dependencies and may be less consuming]
    public static final int CLUST_METHOD = 1;

    
    //-- Use unaltered RDF...
    public static final BooleanParameter saveRAMratherThanCPU = new BooleanParameter(
            "Save RAM (!! <LAST-PART-ONLY> !!)", 
            "Ignored if clusterer type is not 'Cached'. Saves RAM at the expense of CPU during the 2nd step of the clustering (building clusters from tree).",
            false
            );

	
	
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
    
    
    // Clusterer choice
    public static final ComboParameter<ClustererType> clusterer_type = new ComboParameter<ClustererType>(
            "Hierarchical clusterer", 
            "Which clustering algorithm should be used (See: \"Hierarchical clustering\" algorithms in general).", 
            ClustererType.values(),
            ClustererType.CACHED
            );
//    public static final ComboParameter<Integer> clusterer_type = new ComboParameter<Integer>(
//            "Clusterer", 
//            "...", 
//            new Integer[] { 0, 1, 2 },
//            1
//            );

//    // Clusterer k
//    public static final IntegerParameter clusterer_k = new IntegerParameter(
//            "k", 
//            "...", 
//            1
//            );
//    // Clusterer cluster size
//    public static final IntegerParameter clusterer_minClusterSize = new IntegerParameter(
//            "minClusterSize", 
//            "...", 
//            1
//            );
//    public static final BooleanParameter clusterer_useConstraints = new BooleanParameter(
//            "useConstraints", 
//            "...",
//            false
//            );
//    public static final BooleanParameter clusterer_selfEdges = new BooleanParameter(
//            "selfEdges", 
//            "...",
//            true
//            );
    
    // Clustering linkage strategy
    public static final ComboParameter<ClusteringLinkageStrategyType> linkageStartegyType_0 = new ComboParameter<ClusteringLinkageStrategyType>(
            "Clustering strategy", 
            "What strategy shall be used for the clustering algorithm decision making (See: \"Hierarchical clustering\" algorithms in general).", 
            ClusteringLinkageStrategyType.values(),
            ClusteringLinkageStrategyType.AVERAGE
            );
//    // Clustering linkage strategy
//    public static final ComboParameter<LinkType> linkageStartegyType_12 = new ComboParameter<LinkType>(
//            "Clustering strategy", 
//            "What strategy shall be used for the clustering algorithm decision making (See: \"Hierarchical clustering\" algorithms in general).", 
//            LinkType.values(),
//            LinkType.AVERAGE
//            );
    
    // Clusterer k
//    public static final BooleanParameter use_hybrid_K = new BooleanParameter(
//            "Use 'K' value (Hybrid clusterer)",
//            "Shall we pass a K value to the 'Hybrid' clusterer, or let it decide.",
//            false);
    public static final IntegerParameter hybrid_K_value = new IntegerParameter(
            "'K' value (Hybrid clusterer)", 
            "Ignored if clusterer type is not 'Hybrid'. Number of clusters for 1st pass (-1 => Let the algorithm decide) - 'Hybrid' uses 'K-Mean algorithm' as a first clustering pass.", 
            -1
            );

    
    //-- Use unaltered RDF...
    public static final BooleanParameter useOldestRDFAncestor = new BooleanParameter(
            "Use original raw data file", 
            "Chemical similarity is computed using unaltered m/z profile at given scan from the very oldest Raw Data File ancestor (if it has not been removed). "
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
    public static final BooleanParameter useDetectedMzOnly = new BooleanParameter(
            "Use DETECTED m/z only",
            "If checked, uses simplified spectra resulting from a previous 'merge step' to compute chemical similarity score",
            false);

    public static final RTToleranceParameter RTToleranceAfter = new RTToleranceParameter(
            "RT tolerance post-recalibration",
            "Ignored if \"Use RT recalibration\" is unchecked. Maximum allowed difference between two RT values after RT recalibration");
    
    
    // if CLUST_METHOD == 0 !!!
    public static final BooleanParameter exportDendrogramPng = new BooleanParameter(
            "Export dendrogram as PNG",
            "If checked, exports the clustering resulting dendrogram to the given PNG file.",
            false);
    public static final FileNameParameter dendrogramPngFilename = new FileNameParameter(
            "Dendrogram output image filename",
            " Requires \"Export dendrogram as PNG\" checked."
                    + " Name of the resulting PNG file to write the clustering resulting dendrogram to."
                    + " If the file already exists, it will be overwritten.",
            "png");
    public static final BooleanParameter exportDendrogramTxt = new BooleanParameter(
            "Export dendrogram as TXT",
            "If checked, exports the clustering resulting dendrogram to the given TXT file.",
            false);
    public static final FileNameParameter dendrogramTxtFilename = new FileNameParameter(
            "Dendrogram output text filename",
            " Requires \"Export dendrogram as TXT\" checked."
                    + " Name of the resulting TXT file to write the clustering resulting dendrogram to."
                    + " If the file already exists, it will be overwritten.",
            "txt");
    
    // Export type for dendrogram
    public static final ComboParameter<DendrogramFormatType> dendrogramFormatType = new ComboParameter<DendrogramFormatType>(
            "Dendrogram text format", 
            "Ignored if \"Export dendrogram as TXT\" is unchecked. NEWICK (standard printing), or RAW (flat human readable non-standard printing).", 
            DendrogramFormatType.values(),
            DendrogramFormatType.NEWICK
            );

//    // if CLUST_METHOD >= 1 !!!
//    public static final BooleanParameter exportDendrogramNewickTxt = new BooleanParameter(
//            "Export dendrogram as Newick TXT",
//            "If checked, exports the clustering resulting dendrogram to the given PNG file.",
//            false);
//    public static final FileNameParameter dendrogramNewickTxtFilename = new FileNameParameter(
//            "Dendrogram Newick output text filename",
//            " Requires \"Export dendrogram as Newick TXT\" checked."
//                    + " Name of the resulting TXT file to write the clustering resulting dendrogram to."
//                    + " If the file already exists, it will be overwritten.",
//            "txt");
    
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
    
    
    static private Parameter[] getParameters(int clust_method) {
    	
    		return new Parameter[] { peakLists, 
        			useOldestRDFAncestor, 
        			/*comparisonOrder,*/
        			clusterer_type,
        			saveRAMratherThanCPU,
        			linkageStartegyType_0, 
        			/*use_hybrid_K,*/ hybrid_K_value,
        			peakListName, 
        			MZTolerance, MZWeight,
        			RTTolerance, RTWeight,
        			minScore,
        			//		IDWeight,
        			useApex, useKnownCompoundsAsRef, 
        			useDetectedMzOnly,
        			RTToleranceAfter, 
        			/*SameChargeRequired, SameIDRequired, compareIsotopePattern*/ 
        			
        			// Removed: used only for CLUST_METHOD == 0
//        			exportDendrogramNewickTxt, dendrogramNewickTxtFilename
        			dendrogramFormatType,
        			exportDendrogramTxt, dendrogramTxtFilename,
        			
        			//clusterer_type//, clusterer_k, clusterer_minClusterSize, clusterer_useConstraints, clusterer_selfEdges
        	};
    }
    

    // Since clustering is now order independent, option removed!
    public JoinAlignerGCParameters() {
    	super(getParameters(CLUST_METHOD));
    }

}
