package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.util.Arrays;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;

/** 
 * GLG HACK: 
 * Added constructor for "SimpleScan" that guarantees DEEP COPY.
 */
public class SimpleScanClonable extends SimpleScan {
    
    private RawDataFile dataFile;
    private int fragmentScans[];
    private DataPoint dataPoints[];

    public SimpleScanClonable(Scan sc, RawDataFile rawDataFile) {
        
        // Call above clone constructor
        super(sc.getDataFile(), sc.getScanNumber(), sc.getMSLevel(), sc
                .getRetentionTime(), sc
                .getPrecursorMZ(), sc.getPrecursorCharge(), sc
                .getFragmentScanNumbers(), sc.getDataPoints(), sc
                .getSpectrumType(), sc.getPolarity(), sc.getScanDefinition(),
                sc.getScanningMZRange());
        
        
        // Handle non-primitive attributes
        
        if (rawDataFile != null) { this.dataFile = rawDataFile; }
        
        if (sc.getFragmentScanNumbers() != null) 
                this.fragmentScans = Arrays.copyOf(sc.getFragmentScanNumbers(), sc.getFragmentScanNumbers().length);
        else 
                this.fragmentScans = sc.getFragmentScanNumbers();
        this.setFragmentScanNumbers(fragmentScans);
        
        this.dataPoints = new DataPoint[sc.getNumberOfDataPoints()]; 
        for (int i=0; i < sc.getNumberOfDataPoints(); ++i)
                this.dataPoints[i] = new SimpleDataPoint(sc.getDataPoints()[i]);
        
        if (this.dataPoints != null) { this.setDataPoints(this.dataPoints); }
    }
    
    @Override
    public @Nonnull RawDataFile getDataFile() {
        return dataFile;
    }
}
