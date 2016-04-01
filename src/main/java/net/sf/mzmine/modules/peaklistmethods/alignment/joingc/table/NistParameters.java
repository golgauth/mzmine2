package net.sf.mzmine.modules.peaklistmethods.alignment.joingc.table;

import java.io.File;
import java.util.Collection;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.DirectoryParameter;


/**
 * Holds NIST MS Search parameters.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NistParameters extends SimpleParameterSet {

    /**
     * NIST MS Search path.
     */
    public static final DirectoryParameter NIST_MS_SEARCH_DIR = new DirectoryParameter(
            "NIST MS Search directory",
            "Full path of the directory containing the NIST MS Search executable (nistms$.exe)");

    /**
     * Match factor cut-off.
     */
    public static final IntegerParameter MIN_MATCH_FACTOR = new IntegerParameter(
            "Min. match factor",
            "The minimum match factor (0 .. 1000) that search hits must have",
            500, 0, 1000);

    /**
     * Match factor cut-off.
     */
    public static final IntegerParameter MIN_REVERSE_MATCH_FACTOR = new IntegerParameter(
            "Min. reverse match factor",
            "The minimum reverse match factor (0 .. 1000) that search hits must have",
            500, 0, 1000);

    // NIST MS Search executable.
    private static final String NIST_MS_SEARCH_EXE = "nistms$.exe";

    /**
     * Construct the parameter set.
     */
    public NistParameters() {
        super(new Parameter[] { NIST_MS_SEARCH_DIR,
                MIN_MATCH_FACTOR, MIN_REVERSE_MATCH_FACTOR });
    }

    @Override
    public boolean checkParameterValues(final Collection<String> errorMessages) {

        // Unsupported OS.
        if (!isWindows()) {
            errorMessages
                    .add("NIST MS Search is only supported on the Windows operating system.");
            return false;
        }

        boolean result = super.checkParameterValues(errorMessages);

        // NIST MS Search home directory and executable.
        final File executable = getNistMsSearchExecutable();

        // Executable missing.
        if (executable == null || !executable.exists()) {

            errorMessages
                    .add("NIST MS Search executable ("
                            + NIST_MS_SEARCH_EXE
                            + ") not found.  Please set the to the full path of the directory containing the NIST MS Search executable.");
            result = false;
        }

        return result;
    }

    /**
     * Gets the full path to the NIST MS Search executable.
     *
     * @return the path.
     */
    public File getNistMsSearchExecutable() {

        final File dir = getParameter(NIST_MS_SEARCH_DIR).getValue();
        return dir == null ? null : new File(dir, NIST_MS_SEARCH_EXE);
    }

    /**
     * Is this a Windows OS?
     *
     * @return true/false if the os.name property does/doesn't contain
     *         "Windows".
     */
    private static boolean isWindows() {

        return System.getProperty("os.name").toUpperCase().contains("WINDOWS");
    }
}
