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

package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.weka;

import weka.clusterers.forOPTICSAndDBScan.DataObjects.DataObject;
import weka.clusterers.forOPTICSAndDBScan.DataObjects.EuclideanDataObject;
import weka.clusterers.forOPTICSAndDBScan.Databases.Database;
import weka.core.Instance;



	public class TestDistClass extends EuclideanDataObject {
		
		private double[][] distMtx;
		
	    /**
		 * 
		 */
		private static final long serialVersionUID = 963552817253622349L;

		public TestDistClass(Instance originalInstance, String key, Database database, Object distMtx) {
			super(originalInstance, key, database);
			
			this.distMtx = (double[][]) distMtx;
		}

		/**
	     * Calculates the euclidian-distance between dataObject and this.dataObject
	     * @param dataObject The DataObject, that is used for distance-calculation with this.dataObject;
	     *        now assumed to be of the same type and with the same structure
	     * @return double-value The euclidian-distance between dataObject and this.dataObject
	     */
		@Override
	    public double distance(DataObject dataObject) {

			int attr_index = distMtx[0].length;
	    	
			TestDistClass ddo = (TestDistClass) dataObject;

	    	int i, j;
			// Integer ID
			i = (int) Math.round(this.getInstance().value(attr_index));
			j = (int) Math.round(ddo.getInstance().value(attr_index));

			return distMtx[i][j];

		}

	}


