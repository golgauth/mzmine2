package net.sf.mzmine.modules.peaklistmethods.alignment.joingc;

import java.util.Objects;

/**
 * Container to ease passing around a tuple of two objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 */
public class PairTwoWays<F, S> extends Pair<F, S> {

    public PairTwoWays(F first, S second) {
        super(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PairTwoWays)) {
            return false;
        }
        PairTwoWays<?, ?> p = (PairTwoWays<?, ?>) o;
        return (Objects.equals(p.first, first) && Objects.equals(p.second, second)) 
                || (Objects.equals(p.first, second) && Objects.equals(p.second, first));
    }

}