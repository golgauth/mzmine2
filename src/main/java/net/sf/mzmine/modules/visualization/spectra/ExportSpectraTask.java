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

package net.sf.mzmine.modules.visualization.spectra;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Exports a spectrum to a file.
 *
 */
public class ExportSpectraTask extends AbstractTask {

    // Logger
    private static final Logger LOG = Logger.getLogger(ExportSpectraTask.class
            .getName());

    private final File exportFile;
    private final Scan scan;
    private final String extension;

    private int progress;
    private int progressMax;

    /**
     * Create the task.
     *
     * @param currentScan
     *            scan to export.
     * @param file
     *            file to save scan data to.
     */
    public ExportSpectraTask(final Scan currentScan, final File file,
            final String ext) {
        scan = currentScan;
        exportFile = file;
        extension = ext;
        progress = 0;
        progressMax = 0;
    }

    @Override
    public String getTaskDescription() {
        return "Exporting spectrum # " + scan.getScanNumber() + " for "
                + scan.getDataFile().getName();
    }

    @Override
    public double getFinishedPercentage() {
        return progressMax == 0 ? 0.0 : (double) progress
                / (double) progressMax;
    }

    @Override
    public void run() {

        // Update the status of this task
        setStatus(TaskStatus.PROCESSING);

        try {

            // Do the export
            export();

            // Success
            LOG.info("Exported spectrum # " + scan.getScanNumber()
                    + " for " + scan.getDataFile().getName());

            setStatus(TaskStatus.FINISHED);

        } catch (Throwable t) {

            LOG.log(Level.SEVERE, "Spectrum export error", t);
            setStatus(TaskStatus.ERROR);
            setErrorMessage(t.getMessage());
        }
    }

    /**
     * Export the chromatogram.
     *
     * @throws IOException
     *             if there are i/o problems.
     */
    public void export() throws IOException {

        // Open the writer - append data if file already exists
        final BufferedWriter writer = new BufferedWriter(new FileWriter(
                exportFile, true));
        try {
            // Write Header row
            switch (extension) {
            case "txt":
                writer.write("Name: Scan#: " + scan.getScanNumber() + ", RT: "
                        + scan.getRetentionTime() + " min");
                writer.newLine();
                break;
            case "mgf":
                writer.write("BEGIN IONS");
                writer.newLine();
                writer.write("PEPMASS=" + scan.getPrecursorMZ());
                writer.newLine();
                writer.write("CHARGE=" + scan.getPrecursorCharge());
                writer.newLine();
                writer.write("Title: Scan#: " + scan.getScanNumber() + ", RT: "
                        + scan.getRetentionTime() + " min");
                writer.newLine();
                writer.newLine();
                break;
            case "msp":
                break;
            }

            // Write the data points
            DataPoint[] dataPoints = scan.getDataPoints();
            final int itemCount = dataPoints.length;
            progressMax = itemCount;

            for (int i = 0; i < itemCount; i++) {

                // Write data point row
                writer.write(dataPoints[i].getMZ() + " "
                        + dataPoints[i].getIntensity());
                writer.newLine();

                progress = i + 1;
            }

            // Write footer row
            if (extension.equals("mgf")) {
                writer.newLine();
                writer.write("END IONS");
                writer.newLine();
            }

            writer.newLine();
        } finally {

            // Close
            writer.close();
        }
    }
}
