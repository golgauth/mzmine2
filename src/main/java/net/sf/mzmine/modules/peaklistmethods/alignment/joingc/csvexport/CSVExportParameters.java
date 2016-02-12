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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.csvexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class CSVExportParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter(1/*,
	    1*/);

    public static final FileNameParameter filename = new FileNameParameter(
	    "Filename",
	    "Name of the output CSV file. " +
	    "Use pattern \"{}\" in the file name to substitute with peak list name. " +
	    "(i.e. \"blah{}blah.csv\" would become \"blahSourcePeakListNameblah.csv\"). " +
	    "If the file already exists, it will be overwritten.",
	    "csv");

    public static final StringParameter fieldSeparator = new StringParameter(
	    "Field separator",
	    "Character(s) used to separate fields in the exported file", ",");


    public static final BooleanParameter exportSeparate = new BooleanParameter(
             "Export in 2 separate files", 
             "If checked, result table will be exported to 2 distinct CSV files: \"-rt.csv\" + \"-area.csv\"). ",
            true);

    public static final BooleanParameter exportRtAverage = new BooleanParameter(
             "Export RT average", 
             "If checked, all averaged RT will be exported. ",
             true);

    public static final BooleanParameter exportNumDetected = new BooleanParameter(
             "Export number of detected peaks", 
             "If checked, the number of detected samples header row will be exported. ",
             true);

    public static final BooleanParameter exportIdentities = new BooleanParameter(
             "Export identities", 
             "If checked, identification header row will be exported. ",
             true);

//    public static final StringParameter idSeparator = new StringParameter(
//	    "Identification separator",
//	    "Character(s) used to separate identification results in the exported file", ";");

    public CSVExportParameters() {
	super(new Parameter[] { peakLists, filename, fieldSeparator, exportSeparate,
	        exportRtAverage, exportNumDetected, exportIdentities/*, idSeparator*/ });
    }

}
