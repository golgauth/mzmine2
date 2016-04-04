package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class AlignedRowIdentity extends SimplePeakIdentity {

    public static final String PROPERTY_RTS = "Adjusted RTs used for alignment";
    public static final String PROPERTY_OFFSETS = "RT offsets used for alignment";
    public static final String PROPERTY_SCALES = "RT scales used for alignment";
    public static final String PROPERTY_IDENTITIES_NAMES = "Source identities";
    public static final String PROPERTY_IDENTITIES_SCORES = "Source identities scores";
    public static final String PROPERTY_IDENTITIES_QUANT = "Source identities quant";
    public static final String IDENTITY_SEP = ":";
    public static final String KEYVAL_SEP = "=";
    public static final String PROPERTY_IS_REF = "Is reference compound";
    public static final String FALSE = "false";
    public static final String TRUE = "true";
    public static final String PROPERTY_ID_SCORE = "Identitification score";

}
