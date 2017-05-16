package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

public enum ClusteringLinkageStrategyType {


    AVERAGE("Average"),         // Distance of a new cluster is the Average distance of children
    COMPLETE("Complete"),       // Distance of a new cluster is the Max distance over all children
    SINGLE("Single"),           // Distance of a new cluster is the Min distance over all children
    WEIGHTED("Weighted");       // Distance of a new cluster is the Weighted distance over all children

    private final String name;

    ClusteringLinkageStrategyType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

}
