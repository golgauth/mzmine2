package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class AlignedRowIdentity extends SimplePeakIdentity {

    public static final String PROPERTY_RTS = "Retention times used for alignment";
    public static final String PROPERTY_IDENTITIES_NAMES = "Source identities";
    public static final String PROPERTY_IDENTITIES_SCORES = "Source identities scores";
    public static final String IDENTITY_SEP = ":";
    public static final String PROPERTY_IS_REF = "Is reference compound";
    public static final String FALSE = "false";
    public static final String TRUE = "true";
    public static final String PROPERTY_ID_SCORE = "Preferred identity score";

}
