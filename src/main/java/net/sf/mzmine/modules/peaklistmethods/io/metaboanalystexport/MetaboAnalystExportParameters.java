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

package net.sf.mzmine.modules.peaklistmethods.io.metaboanalystexport;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

public class MetaboAnalystExportParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
	    1);

    public static final FileNameParameter filename = new FileNameParameter(
	    "Filename",
	    "Name of exported CSV file for MetaboAnalyst. If the file already exists, it will be overwritten.",
	    "csv");

    public static final ComboParameter<UserParameter<?, ?>> groupParameter = new ComboParameter<UserParameter<?, ?>>(
	    "Grouping parameter",
	    "Project parameter that will be used to obtain group information to each sample (e.g. control vs disease). Please set parameters in the Project/Set sample parameters menu.",
	    new UserParameter[0]);

    public MetaboAnalystExportParameters() {
	super(new Parameter[] { peakList, filename, groupParameter });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

	UserParameter<?, ?> projectParams[] = MZmineCore.getProjectManager()
		.getCurrentProject().getParameters();
	getParameter(MetaboAnalystExportParameters.groupParameter).setChoices(
		projectParams);

	return super.showSetupDialog(parent, valueCheckRequired);
    }
}
