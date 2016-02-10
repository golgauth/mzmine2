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

package net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.roundresample;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilterSetupDialog;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.util.ExitCode;

public class RndResampleFilterParameters extends SimpleParameterSet {

//    public static final DoubleParameter binSize = new DoubleParameter(
//	    "m/z bin length", "The length of m/z bin", MZmineCore
//		    .getConfiguration().getMZFormat());

    public static final BooleanParameter SUM_DUPLICATES = new BooleanParameter(
            "Sum duplicates intensities",
            "Concatenates/sums ions count m/z peaks determined as being at same m/z unit. " +
            "If not checked, only the first m/z peak at given m/z is kept and the others removed.",
            true);

    public static final BooleanParameter REMOVE_ZERO_INTENSITY = new BooleanParameter(
            "Remove zero intensity m/z peaks",
            "Clear all scans spectra from m/z peaks with intensity equal to zero.",
            true);
    
    public RndResampleFilterParameters() {
	super(new Parameter[] { SUM_DUPLICATES, REMOVE_ZERO_INTENSITY });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
	ScanFilterSetupDialog dialog = new ScanFilterSetupDialog(parent,
		valueCheckRequired, this, RndResampleFilter.class);
	dialog.setVisible(true);
	return dialog.getExitCode();
    }
}
