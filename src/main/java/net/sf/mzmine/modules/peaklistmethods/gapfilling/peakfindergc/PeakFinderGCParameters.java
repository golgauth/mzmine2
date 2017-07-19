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

package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfindergc;

import java.text.DecimalFormat;

import com.google.common.collect.Range;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class PeakFinderGCParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final StringParameter suffix = new StringParameter(
	    "Name suffix", "Suffix to be added to peak list name", "gap-filled");

    public static final PercentParameter intTolerance = new PercentParameter(
	    "Intensity tolerance",
	    "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction");

    public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

    ////**public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();
    // Different usage!
    //          > We use this to add allow some amount of overlapping between adjacent columns
    //          > Left to zero means 'no overlapping' => a "gapfilled" peak won't be assigned to 
    //                  several columns!
    public static final DoubleParameter RTColumnTolerance = new DoubleParameter(
            "RT row deviation tolerance",
            "Amount of overlapping between search ranges for two adjacent RT values (0.0 => no overlap, i.e. a \"gap-filled\" peak won't be assigned to several columns)",
            MZmineCore.getConfiguration().getRTFormat(), 
            0.0);
    
    public static final DoubleParameter minSimScore = new DoubleParameter(
            "Minimum similarity score",
            "Minimum chemical similarity score to allow a peak to run for candidate to fill the gap.",
            MZmineCore.getConfiguration().getRTFormat(), 
            0.0);

    public static final BooleanParameter rtCorrection = new BooleanParameter(
            "RT correction",
            "If checked, correction of the retention time will be applied to avoid the"
                +"\nproblems caused by the deviation of the retention time between the samples.");
    
    public static final BooleanParameter useRegression = new BooleanParameter(
            "Use regression",
            "Ignored if \"RT correction\" isn't checked. If checked, RT correction is performed using a polynomial regression" +
            "approach among already detected peaks rather than the \"offset/scale\" values inherited from previous \"Join Aligner GC\".");
    
    
    //-------- Begin: Deconvolution params
    public static final PercentParameter CHROMATOGRAPHIC_THRESHOLD_LEVEL = new PercentParameter(
            "Chromatographic threshold",
            "Threshold for removing noise. The algorithm finds such intensity that given percentage of the"
                +"\nchromatogram data points is below that intensity, and removes all data points below that level."
                    , 35.0);

    public static final DoubleParameter SEARCH_RT_RANGE = new DoubleParameter(
            "Search minimum in RT range (min)",
            "If a local minimum is minimal in this range of retention time, it will be considered a border between two peaks",
            MZmineCore.getConfiguration().getRTFormat(), null, 0.001, null);

    public static final PercentParameter MIN_RELATIVE_HEIGHT = new PercentParameter(
            "Minimum relative height",
            "Minimum height of a peak relative to the chromatogram top data point",
            1.0);

    public static final DoubleParameter MIN_ABSOLUTE_HEIGHT = new DoubleParameter(
            "Minimum absolute height",
            "Minimum absolute height of a peak to be recognized", 
            MZmineCore.getConfiguration().getIntensityFormat(),
            1.0);

    public static final DoubleParameter MIN_RATIO = new DoubleParameter(
            "Min ratio of peak top/edge",
            "Minimum ratio between peak's top intensity and side (lowest) data points."
                +"\nThis parameter helps to reduce detection of false peaks in case the chromatogram is not smooth.",
                new DecimalFormat("0.0000"),
                1.0);

    public static /*final*/ DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
            "Peak duration range (min)", "Range of acceptable peak lengths",
            MZmineCore.getConfiguration().getRTFormat(),
            Range.closed(0.0, 3.0));

//    public MinimumSearchPeakDetectorParameters() {
//        super(new Parameter[] { CHROMATOGRAPHIC_THRESHOLD_LEVEL,
//                SEARCH_RT_RANGE, MIN_RELATIVE_HEIGHT, MIN_ABSOLUTE_HEIGHT,
//                MIN_RATIO, PEAK_DURATION });
//    }
    //-------- End: Deconvolution params



    public static final BooleanParameter autoRemove = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, the original peak list will be removed");

    public PeakFinderGCParameters() {
	super(new Parameter[] { peakLists, suffix, intTolerance, MZTolerance,
		/*RTTolerance,*/RTColumnTolerance, minSimScore,  
		rtCorrection, useRegression,
		SEARCH_RT_RANGE, PEAK_DURATION, 
		autoRemove });
    }

}
