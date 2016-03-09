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

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
//import net.sf.mzmine.datamodel.PeakListRow;
//import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Module for identifying peaks by searching on-line databases.
 */
public class CustomJDXSearchModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Custom JDX(s) search";
    private static final String MODULE_DESCRIPTION = "This module attempts to identify peaks from JDX files contained in the given directory.";

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.IDENTIFICATION;
    }

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

		// TODO: We could keep N tasks and add a watcher that updates the result table each time a Task is FINISHED.
		//			But for now, we just turn it into a single Task...
//		final PeakList[] peakLists = parameters.getParameter(CustomJDXSearchParameters.PEAK_LISTS).getValue();
//		for (final PeakList peakList : peakLists) {
//			Task newTask = new CustomJDXSearchTask(parameters, peakList);
//			tasks.add(newTask);
//		}
		final PeakList[] peakLists = parameters.getParameter(CustomJDXSearchParameters.PEAK_LISTS).getValue().getMatchingPeakLists();
		Task newTask = new CustomJDXSearchTask(project, parameters, peakLists);
		tasks.add(newTask);

		return ExitCode.OK;
	}

	@Override
	public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
		return CustomJDXSearchParameters.class;
	}
}
