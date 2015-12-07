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

package net.sf.mzmine.modules.peaklistmethods.filtering.shapefilter;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Duplicate peak filter
 * 
 * This filter cleans up a peak list by keeping only the row with the strongest
 * median peak area of all rows having same (optionally) identification and
 * similar m/z and rt values (within tolerances)
 * 
 * Idea is to run this filter before alignment on peak lists with peaks from a
 * single raw data file in each list, but it will work on aligned peak lists
 * too.
 * 
 */
/**
 * @description TODO
 * @author Gauthier Boaglio
 * @date Nov 30, 2014
 */
public class ShapeFilterModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Shape based peak filter";
    private static final String MODULE_DESCRIPTION = "This method removes false peaks (shape criterion) from the peak list.";

    @Override
    public @Nonnull
    String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull
    String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        PeakList[] peakLists = parameters
                .getParameter(ShapeFilterParameters.PEAKLISTS).getValue()
                .getMatchingPeakLists();

        for (PeakList peakList : peakLists) {
            Task newTask = new ShapeFilterTask(project, peakList, parameters);
            tasks.add(newTask);
        }

        return ExitCode.OK;

    }

    @Override
    public @Nonnull
    MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLISTFILTERING;
    }

    @Override
    public @Nonnull
    Class<? extends ParameterSet> getParameterSetClass() {
        return ShapeFilterParameters.class;
    }
}
