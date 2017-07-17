package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

public enum DendrogramFormatType {

    RAW("Raw"),
    NEWICK("Newick");

    private final String name;

    DendrogramFormatType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }


}
