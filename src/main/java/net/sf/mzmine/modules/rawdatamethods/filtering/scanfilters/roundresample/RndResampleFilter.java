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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.modules.batchmode.BatchSetupComponent;
import net.sf.mzmine.modules.rawdatamethods.filtering.scanfilters.ScanFilter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;


public class RndResampleFilter implements ScanFilter {

    // Logger.
    private static final Logger logger = Logger
            .getLogger(BatchSetupComponent.class.getName());

    private static boolean DEBUG = false;
    
    public Scan filterScan(Scan scan, ParameterSet parameters) {
        
        boolean sum_duplicates = parameters.getParameter(
                RndResampleFilterParameters.SUM_DUPLICATES).getValue();
        boolean remove_zero_intensity = parameters.getParameter(
                RndResampleFilterParameters.REMOVE_ZERO_INTENSITY).getValue();
        


        if (scan.getScanNumber() != 5023 && DEBUG) {
            return scan;
        }
        
        DataPoint dps[] = scan.getDataPoints();
        SimpleDataPoint[] newDps = new SimpleDataPoint[dps.length];
        for (int i = 0; i < dps.length; ++i) {
            
            boolean done = false;
            
            // Set the new m/z value to nearest integer / unit value
            int newMz = (int) Math.round(dps[i].getMZ());
            // Check if current dp/ion is not conflicting with previous or next dp/ion
            if (i > 0 /*&& dps[i-1] != null*/) {
                //int prevMz = (int) Math.round(dps[i-1].getMZ());
                int prevMz = (int) Math.round(newDps[i-1].getMZ());
                if (newMz == prevMz && i < dps.length-1) {
                    // Move forward
                    ++newMz;
                    // Check if error (can't move either forward nor backward)
                    int nextMz = (int) Math.round(dps[i+1].getMZ());
                    // If next unit is about to be taken by another dp, we check the next
                    if (newMz == nextMz && i < dps.length-2) {
                        
//                        // First try to go left
//                        if (i < dps.length-1) {
//                            //int nextMz = (int) Math.round(dps[i+1].getMZ());
//                            //if (newMz == nextMz) {
//                                // Move backward
//                                --newMz;
//                                // Check if error (can't move either forward nor backward)
//                                //int prevMz = (int) Math.round(newDps[i-1].getMZ());
//                                // If prev unit has already been taken by another dp, then we do have a problem
//                                if (newMz == prevMz) {
//                                    //if (i < 100) {
//                                    logger.log(Level.SEVERE, "Error on scan #'" + scan.getScanNumber() + "'");
//                                    logger.log(Level.SEVERE, "Bwd: Dp " + dps[i] + " cannot be rounded to '" + nextMz + "' neither to '" + prevMz + "'");
//                                    //continue;
//
//                                    //}
//                                } else {
//                                    logger.log(Level.SEVERE, "OK6: #'" + scan.getScanNumber() + "', dp shifted LEFT, from '" + (newMz+1) + "' to '" + newMz + "'");
//                                    done = true;
//                                }
//                            //} else {
//                            //    logger.log(Level.SEVERE, "OK5: #'" + scan.getScanNumber() + "', dp shifted from '" + ((int) Math.round(dps[i].getMZ())) + "' to '" + newMz + "'");
//                            //    done = true;
//                            //}
//                        }
                        
                        // If going left failed, try shifting next
                        if (!done) {
                            int nextNextMz = (int) Math.round(dps[i+2].getMZ());
                            // If next unit can be shifted to the right
                            if (nextMz+1 != nextNextMz) {
                                if (DEBUG) logger.log(Level.SEVERE, "Thanks got on scan #'" + scan.getScanNumber() + "'");
                                if (DEBUG) logger.log(Level.SEVERE, "Next Dp was shiftable from '" + nextMz + "' to '" + (nextMz+1) + "'");
                                done = true;
                            }
                            // Otherwise: we really do have a problem
                            else {
                                //if (i < 100) {
                                if (DEBUG) logger.log(Level.SEVERE, "Error on scan #'" + scan.getScanNumber() + "'");
                                if (DEBUG) logger.log(Level.SEVERE, "Fwd: Dp " + dps[i] + " cannot be rounded to '" + prevMz + "' neither to '" + nextMz + "'");
                                //}
                                //continue;
                                // Two candidates after all: keep them as duplicates :(
                                --newMz; // Restore newMz
                                if (DEBUG) logger.log(Level.SEVERE, "KO3: #'" + scan.getScanNumber() + "', 2 dps overlap at '" + newMz + "'");
                                if (DEBUG) logger.log(Level.SEVERE, "KO3: #'" + scan.getScanNumber() + "', cannot shift next from '" + nextMz + "' to " + nextNextMz + "'");
                                done = true;
                            }
                        }
                    } else {
                        if (DEBUG) logger.log(Level.SEVERE, "OK2: #'" + scan.getScanNumber() + "', dp shifted RIGHT, from '" + (newMz-1) + "' to '" + newMz + "'");
                        done = true;
                    }
                } else {
                    if (i < dps.length-1) {
                        int nextMz = (int) Math.round(dps[i+1].getMZ());
                        // If next unit is about to be taken by next dp, we try to go left (be polite, when possible)
                        if (newMz == nextMz /*&& newMz-1 != prevMz &&*/ /*i < dps.length-1*/ && newMz-1 != prevMz) {
                            --newMz;
                            if (DEBUG) logger.log(Level.SEVERE, "OK1: #'" + scan.getScanNumber() + "', dp shifted LEFT (polite) to '" + newMz + "'");
                            done = true;
                        }
                    }
                    if (!done) {
                        if (DEBUG) logger.log(Level.SEVERE, "OK0: #'" + scan.getScanNumber() + "', dp rounded to '" + newMz + "'");
                        done = true;
                    }
                }
            } else {
                if (DEBUG) logger.log(Level.SEVERE, "OK-start: #'" + scan.getScanNumber() + "', starting dp at '" + newMz + "'");
                done = true;
            }
            
            
//            if (i > 0 && /*i != 0 &&*/ !done && i < dps.length-1) {
//                int nextMz = (int) Math.round(dps[i+1].getMZ());
//                if (newMz == nextMz) {
//                    // Move backward
//                    --newMz;
//                    // Check if error (can't move either forward nor backward)
//                    int prevMz = (int) Math.round(newDps[i-1].getMZ());
//                    // If prev unit has already been taken by another dp, then we do have a problem
//                    if (newMz == prevMz) {
//                        //if (i < 100) {
//                            logger.log(Level.SEVERE, "Error on scan #'" + scan.getScanNumber() + "'");
//                            logger.log(Level.SEVERE, "Bwd: Dp " + dps[i] + " cannot be rounded to '" + nextMz + "' neither to '" + prevMz + "'");
//                            //continue;
//                            
//                        //}
//                    } else {
//                        logger.log(Level.SEVERE, "OK6: #'" + scan.getScanNumber() + "', dp shifted from '" + (newMz+1) + "' to '" + newMz + "'");
//                        done = true;
//                    }
//                } else {
//                    logger.log(Level.SEVERE, "OK5: #'" + scan.getScanNumber() + "', dp shifted from '" + ((int) Math.round(dps[i].getMZ())) + "' to '" + newMz + "'");
//                    done = true;
//                }
//            }
            // Create new DataPoint accordingly (intensity untouched)
            newDps[i] = new SimpleDataPoint(newMz, dps[i].getIntensity());
        }
        
        // Post-treatments
        // Cleanup: Remove duplicates/overlap and zero intensity dps
        ArrayList<SimpleDataPoint> dpsList = new ArrayList<SimpleDataPoint>();
        double prevMz = -1.0, curMz = -1.0;
        double sum = 0.0;
        for (int i = 0; i < newDps.length; ++i) {
            if (newDps[i].getIntensity() == 0.0)
                if (DEBUG) logger.log(Level.SEVERE, "ZERO 2 intensity at: " + newDps[i].getMZ());
            if (i > 0 && newDps[i-1].getMZ() == newDps[i].getMZ()) {
                if (DEBUG) logger.log(Level.SEVERE, "DUPLICATE at: " + dps[i].getMZ());
            }
            // Zeroed
            if (!remove_zero_intensity || newDps[i].getIntensity() > 0.0)
                dpsList.add(newDps[i]);
            // Duplicates
            curMz = newDps[i].getMZ();
            if (i == 0) {
                dpsList.add(newDps[i]);
            } else {
                if (sum_duplicates && curMz == prevMz) {
                    sum = dpsList.get(dpsList.size()-1).getIntensity() + newDps[i].getIntensity();
                    dpsList.set(i, new SimpleDataPoint(prevMz, sum));                    
                } else {
                    dpsList.add(newDps[i]);
                }
            }
            prevMz = newDps[i].getMZ();
        }

	// Create updated scan
	SimpleScan newScan = new SimpleScan(scan);
	////newScan.setDataPoints(newDps);
	newScan.setDataPoints(dpsList.toArray(new SimpleDataPoint[dpsList.size()]));
	newScan.setSpectrumType(MassSpectrumType.CENTROIDED);

	return newScan;
    }

    @Override
    public @Nonnull String getName() {
	return "Round resampling filter";
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return RndResampleFilterParameters.class;
    }
}
