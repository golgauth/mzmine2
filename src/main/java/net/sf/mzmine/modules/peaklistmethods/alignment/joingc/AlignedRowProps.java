package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;


public class AlignedRowProps {

    public static final String PROPERTY_RTS = "Adjusted RTs used for alignment";
    public static final String PROPERTY_OFFSETS = "RT offsets used for alignment";
    public static final String PROPERTY_SCALES = "RT scales used for alignment";
    public static final String PROPERTY_IDENTITIES_NAMES = "Source identities";
    public static final String PROPERTY_IDENTITIES_SCORES = "Source identities scores";
    public static final String PROPERTY_IDENTITIES_QUANT = "Source identities quant";
    public static final String PROP_SEP = ":";
    public static final String KEYVAL_SEP = "=";
    public static final String PROPERTY_IS_REF = "Is reference compound";
    public static final String FALSE = "false";
    public static final String TRUE = "true";
    public static final String PROPERTY_ID_SCORE = "Identitification score";

    
    public static String implode(String separator, boolean trim, String... data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length - 1; i++) {
        //data.length - 1 => to not add separator at the end
            if (!trim || !data[i].matches(" *")) {//empty string are ""; " "; "  "; and so on
                sb.append(data[i]);
                sb.append(separator);
            }
        }
        if (!trim)
            sb.append(data[data.length - 1]);
        else
            sb.append(data[data.length - 1].trim());
        return sb.toString();
    }

//    public static String implode(String separator, String... data) {
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < data.length - 1; i++) {
//            sb.append(data[i]);
//            sb.append(separator);
//        }
//        sb.append(data[data.length - 1]);
//        return sb.toString();
//    }
}
