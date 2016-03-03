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

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table.PeakListTableAlaJulWindow;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class JoinAlignerGcModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Join aligner GC";
    private static final String MODULE_DESCRIPTION = "This method aligns detected peaks using a match score. This score is calculated based on the MZ profile and RT of each peak using preset tolerances.";

    public static final String MISSING_PEAK_VAL = "0";
    
    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
	return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
	    @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {
	Task newTask = new JoinAlignerTask(project, parameters);
	tasks.add(newTask);
	return ExitCode.OK;

    }

    public static void showNewPeakListVisualizerWindow(PeakList peakList) {
        ParameterSet parameters = MZmineCore.getConfiguration()
                .getModuleParameters(PeakListTableModule.class);
        final PeakListTableAlaJulWindow window = new PeakListTableAlaJulWindow(peakList,
                parameters);
        window.setVisible(true);
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
	return MZmineModuleCategory.ALIGNMENT;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return JoinAlignerParameters.class;
    }

}
