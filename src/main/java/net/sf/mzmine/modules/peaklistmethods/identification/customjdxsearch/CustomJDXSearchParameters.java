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

package net.sf.mzmine.modules.peaklistmethods.identification.customjdxsearch;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.SimilarityMethodType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class CustomJDXSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final DirectoryParameter JDX_DIR = new DirectoryParameter(
            "JDX(s) directory",
            "Directory path containing jdx files to blast from");

    // TODO: ???
    /*
     * public static final RTToleranceParameter RI_SEARCH_WINDOW = new
     * RTToleranceParameter( "Retention Index (RI) tolerance", "...");
     */

    public static final ComboParameter<SimilarityMethodType> SIMILARITY_METHOD = new ComboParameter<SimilarityMethodType>(
            "Similarity method", "Similarity method",
            SimilarityMethodType.values());

    public static final DoubleParameter RI_MIX_FACTOR = new DoubleParameter(
            "RI Mix factor",
            "Weight for balancing between Similarity, RI and Area (use 0.0 to ignore RI while computing score).",
            MZmineCore.getConfiguration().getIntensityFormat(), 0.0, 0.0, 1.0);

    public static final DoubleParameter AREA_MIX_FACTOR = new DoubleParameter(
            "Area Mix factor",
            "Weight for balancing between Similarity, RI and Area (use 0.0 to ignore Area while computing score).",
            MZmineCore.getConfiguration().getIntensityFormat(), 0.0, 0.0, 1.0);

    public static final DoubleParameter MIN_SCORE = new DoubleParameter(
            "Minimum score",
            "Minimum score for matching between two peaks to be considered as successful "
                    + "(WARN: 'Pearson correlation' similarity method can imply scores < 0.0 and/or > 1.0)",
            NumberFormat.getNumberInstance(),
            CustomJDXSearchTask.MIN_SCORE_ABSOLUTE);
    //
    public static final BooleanParameter useDetectedMzOnly = new BooleanParameter(
            "Use DETECTED m/z only",
            "If checked, uses simplified spectra resulting from a previous 'merge step' to compute chemical similarity score",
            false);

    public static final BooleanParameter IGNORE_RT_RANGE_FILES = new BooleanParameter(
            "Ignore range files", "Ignore files containing RT search ranges (.rts files), "
                    + "even if present in the \"JDX(s) directory\"", false);

    // TODO: ???
    /*Minimum score
     * public static final BooleanParameter APPLY_WITHOUT_CHECK = new
     * BooleanParameter( "Apply without checking",
     * "Apply best scoring compounds without checking manually " +
     * "(displaying validation table) first", false);
     */
    public static final FileNameParameter BLAST_OUTPUT_FILENAME = new FileNameParameter(
            "Blast output filename",
            " Requires \"Apply without checking\" checked."
                    + " Name of the resulting CSV file to write standard compounds best blast scores into."
                    + " If the file already exists, it will be overwritten.",
            "csv");
    public static final StringParameter FIELD_SEPARATOR = new StringParameter(
            "Field separator",
            " Requires \"Apply without checking\" checked."
                    + "Character(s) used to separate fields in the exported file",
            ",");

    public static final BooleanParameter BRUTE_FORCE_ERASE = new BooleanParameter(
            "Can erase previous identities",
            "Allows to erases any previous identification operation on the fly "
                    + "(use with caution)", false);
    public static final BooleanParameter CLEAR_ALL = new BooleanParameter(
            "Clear previous identities",
            "Clear all previous identification operations", true);

    public static final BooleanParameter USE_AS_STD_COMPOUND = new BooleanParameter(
            "Tag as standard compound",
            "Use as reference/standard compound for later use in 'Join Aligner GC'.", false);

    public CustomJDXSearchParameters() {
        super(new Parameter[] { PEAK_LISTS,
                // RI_SEARCH_WINDOW
                JDX_DIR, 
//                BRUTE_FORCE_ERASE, 
                CLEAR_ALL, USE_AS_STD_COMPOUND,
                SIMILARITY_METHOD, 
                RI_MIX_FACTOR, AREA_MIX_FACTOR,
                MIN_SCORE, useDetectedMzOnly, 
                IGNORE_RT_RANGE_FILES,
                // APPLY_WITHOUT_CHECK,
                BLAST_OUTPUT_FILENAME, FIELD_SEPARATOR });
    }

}
