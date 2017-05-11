package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

public enum ClusteringLinkageStrategyType {


    AVERAGE("Average"),
    COMPLETE("Comlpete"),
    SINGLE("Single"),
    WEIGHTED("Weighted");

    private final String name;

    ClusteringLinkageStrategyType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

}
