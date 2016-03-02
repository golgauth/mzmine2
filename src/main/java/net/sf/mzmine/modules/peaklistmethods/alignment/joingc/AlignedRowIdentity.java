package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class AlignedRowIdentity extends SimplePeakIdentity {

    public static final String PROPERTY_RTS = "Retention times used for alignment";
    public static final String PROPERTY_IDENTITIES_FREQ = "Source identities frequencies";
    public static final String IDENTITY_SEP = ":";
    public static final String PROPERTY_IS_REF = "Is reference compound";
    public static final String FALSE = "0";
    public static final String TRUE = "1";

}
