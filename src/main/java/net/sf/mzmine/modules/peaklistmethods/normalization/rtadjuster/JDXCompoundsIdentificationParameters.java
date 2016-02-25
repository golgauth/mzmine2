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

import java.text.NumberFormat;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;


public class JDXCompoundsIdentificationParameters extends SimpleParameterSet {

	public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
    	    "Name suffix", "Suffix to be added to peak list name", "adjusted");

    //    public static final ComboParameter<IonizationType> ionizationType = new ComboParameter<IonizationType>(
	//	    "Ionization type", "Ionization type", IonizationType.values());
	//
	//    public PeakListIdentificationParameters() {
	//	super(new Parameter[] { peakLists,
	//		SingleRowIdentificationParameters.DATABASE, ionizationType,
	//		SingleRowIdentificationParameters.MAX_RESULTS,
	//		SingleRowIdentificationParameters.MZ_TOLERANCE,
	//		SingleRowIdentificationParameters.ISOTOPE_FILTER });
	//    }

	public static final FileNameParameter JDX_FILE_C1 = new FileNameParameter(
			"Compound 1 file (JDX)", "JDX file for standard compound 1", ".jdx");
	public static final RTRangeParameter RT_SEARCH_WINDOW_C1 = new RTRangeParameter(
			"Compound 1 search window", "Search window for standard compound 1", true, null);
	public static final FileNameParameter JDX_FILE_C2 = new FileNameParameter(
			"Compound 2 file (JDX)", "JDX file for standard compound 2", ".jdx");
	public static final RTRangeParameter RT_SEARCH_WINDOW_C2 = new RTRangeParameter(
			"Compound 2 search window", "Search window for standard compound 2", true, null);

	public static final ComboParameter<SimilarityMethodType> SIMILARITY_METHOD = new ComboParameter<SimilarityMethodType>(
			"Similarity method", "Similarity method", SimilarityMethodType.values());
        public static final DoubleParameter MIX_FACTOR = new DoubleParameter(
                "Area Mix factor", "Weight for balancing between Similarity and Area (0.0 is 'Similarity only')",
                MZmineCore.getConfiguration().getIntensityFormat(), 0.0, 0.0, 1.0);
        public static final DoubleParameter MIN_SCORE = new DoubleParameter(
                "Minimum score", 
                "Minimum score for matching between two peaks to be considered as successful " +
                "(WARN: 'Pearson correlation' similarity method can imply scores < 0.0 and/or > 1.0)",
                NumberFormat.getNumberInstance(), JDXCompoundsIdentificationSingleTask.MIN_SCORE_ABSOLUTE);
	
	public static final BooleanParameter APPLY_WITHOUT_CHECK = new BooleanParameter(
	        "Apply without checking", "Apply best scoring compounds without checking manually " 
	                + "(displaying validation table) first", false);
        public static final FileNameParameter BLAST_OUTPUT_FILENAME = new FileNameParameter(
                "Blast output filename",
                " Requires \"Apply without checking\" checked." +
                        " Name of the resulting CSV file to write standard compounds best blast scores into." +
                        " If the file already exists, it will be overwritten.",
                "csv");
        public static final StringParameter FIELD_SEPARATOR = new StringParameter(
                "Field separator",
                " Requires \"Apply without checking\" checked." +
                "Character(s) used to separate fields in the exported file", ",");
	
	public JDXCompoundsIdentificationParameters() {
		super(new Parameter[] { PEAK_LISTS,
				SUFFIX,
				JDX_FILE_C1, RT_SEARCH_WINDOW_C1, 
				JDX_FILE_C2, RT_SEARCH_WINDOW_C2, 
				SIMILARITY_METHOD, MIX_FACTOR,
				MIN_SCORE,
				APPLY_WITHOUT_CHECK, BLAST_OUTPUT_FILENAME, FIELD_SEPARATOR });
	}

}
