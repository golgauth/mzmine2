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

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class JoinAlignerParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final StringParameter peakListName = new StringParameter(
            "Peak list name", "Peak list name", "Aligned peak list");

    public static final ComboParameter<RowVsRowOrderType> comparisonOrder = new ComboParameter<RowVsRowOrderType>(
            "Comparison Order", 
            "In which order peak lists (samples) should be compared to each other while running the alignment algorihtm", 
            RowVsRowOrderType.values()
            );

    public static final MZToleranceParameter MZTolerance = new MZToleranceParameter();

    public static final DoubleParameter MZWeight = new DoubleParameter(
	    "Weight for m/z", "Score for perfectly matching m/z values");

    public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();

    public static final DoubleParameter RTWeight = new DoubleParameter(
	    "Weight for RT", "Score for perfectly matching RT values");

    //*** GLG HACK: Added...
    public static final BooleanParameter useApex = new BooleanParameter(
            "Use apex",
            "If checked, use apex scan for RT and MZ profile. Otherwise, 5% of the scans around the apex are used to redefine averaged RT and MZ profile",
            true);
    public static final BooleanParameter useKnownCompoundsAsRef = new BooleanParameter(
            "Use RT recalibration",
            "If checked, use compounds with known identities to ease alignment",
            true);

    public static final RTToleranceParameter RTToleranceAfter = new RTToleranceParameter(
            "RT tolerance post-recalibration",
            "Ignored if \"Use RT recalibration\" is unchecked. Maximum allowed difference between two RT values after RT recalibration");
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

    public JoinAlignerParameters() {
	super(new Parameter[] { peakLists, comparisonOrder, peakListName, 
	        MZTolerance, MZWeight,
		RTTolerance, RTWeight, 
		useApex, useKnownCompoundsAsRef, RTToleranceAfter, 
		/*SameChargeRequired, SameIDRequired,
		compareIsotopePattern*/ 
		});
    }

}
