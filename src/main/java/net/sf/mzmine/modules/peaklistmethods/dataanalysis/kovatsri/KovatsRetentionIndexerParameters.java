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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.kovatsri;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class KovatsRetentionIndexerParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    // I.e. Can use the "Custom JDX search" to identify alkanes in a sample peak list from a JDX directory
//    public static final PeakListsParameter ALKANES_REF_PEAKLIST = new PeakListsParameter(1, 1);
    public static final StringParameter ALKANES_REF_PEAKLIST_NAME = new StringParameter(
    		"Alkanes peak list name",
    		"Name of the peak list containing the required alkanes properly identified."
    		);
    
    public static final ComboParameter<KovatsMethodType> KOVATS_METHOD = new ComboParameter<KovatsMethodType>(
            "Rentention Index method", 
            "Rentention Index (Kovats) computing method (if you don't know, use the default linear 'Programmed temperature').",
            KovatsMethodType.values(),
            KovatsMethodType.PROGRAMMED_TEMPERATURE);


    public KovatsRetentionIndexerParameters() {
        super(new Parameter[] { PEAK_LISTS,
                // RI_SEARCH_WINDOW
        		////ALKANES_REF_PEAKLIST,
        		ALKANES_REF_PEAKLIST_NAME,
        		KOVATS_METHOD,
        		});
    }

    public static PeakList getPeakListByName(String plName) {
    	
        PeakList[] lists = MZmineCore.getProjectManager().getCurrentProject().getPeakLists();
        for (PeakList lst : lists) {
        	if (lst.getName().equals(plName)) {
        		return lst;
        	}
        }
        
        return null;
    }
    
}
