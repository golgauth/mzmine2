package net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster;

import java.util.Comparator;


public class ArrayComparator implements Comparator<Double[]> {
        private final int columnToSort;
        private final boolean ascending;

        public ArrayComparator(int columnToSort, boolean ascending) {
                this.columnToSort = columnToSort;
                this.ascending = ascending;
        }

        @Override
        public int compare(Double[] d1, Double[] d2) {
                int cmp = d1[columnToSort].compareTo(d2[columnToSort]);
                return ascending ? cmp : -cmp;
        }
}
