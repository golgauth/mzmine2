package net.sf.mzmine.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;

public class DataFileUtils {

    static private class RawDFComparator implements Comparator<RawDataFile> {
        @Override
        public int compare(RawDataFile rdf1, RawDataFile rdf2) {
            return rdf1.getName().compareTo(rdf2.getName());
        }
    }
    
    //
    static public RawDataFile getAncestorDataFile(MZmineProject project, RawDataFile refDF, boolean oldest) {


        RawDataFile[] allRDF = project.getDataFiles();

        // Look for the whole RawDF family in the current project
        ArrayList<RawDataFile> family = new ArrayList<RawDataFile>();
        for (int i=0; i < allRDF.length; i++)
        {
            if (refDF.getName().contains(allRDF[i].getName())
                    && !(refDF.getName().equals(allRDF[i].getName())))
                family.add(allRDF[i]);
        }

        if (family.size() == 0) return null;

        // Sort files by names
        Collections.sort(family, new RawDFComparator());
        // Oldest ancestor.
        if (oldest) return family.get(0);                   // The shortest name among ancestors
        // First (direct) ancestor.
        else return family.get(family.size()-1);            // The longest name among ancestors

    }


}
