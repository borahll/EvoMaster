package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import java.util.Collection;
import java.util.Objects;

public abstract class CollectionsDistanceUtils {

    public static double getHeuristicToContains(Collection c, Object o) {
        return getHeuristicToContains(c, o, -1);
    }

    public static double getHeuristicToContainsAll(Collection c, Collection other) {
        return getHeuristicToContainsAll(c, other, -1);
    }

    public static double getHeuristicToContainsAll(Collection c, Collection other, int limit) {
        Objects.requireNonNull(c);

        boolean result = c.containsAll(other);

        if (result) {
            return 1d;
        } else if(c.isEmpty()){
            return DistanceHelper.H_REACHED_BUT_EMPTY;
        }

        assert c!=null && other!=null && !other.isEmpty(); // otherwise function would had returned or exception

        double sum = 0d;
        //TODO should the "limit" applied to "other" as well?
        for(Object x : other.toArray()){
            sum += getHeuristicToContains(c, x, limit);
        }
        sum = sum / (double) other.size();

        assert sum >=0 && sum <= 1;

        return sum;
     }

    /**
     * Compute distance of object from each one of the elements in the collection.
     * But look only up to limit elements.
     * A negative values means look at all elements
     */
    public static double getHeuristicToContains(Collection c, Object o, int limit) {
        Objects.requireNonNull(c);

        boolean result = c.contains(o);
        if (result) {
            return 1d;
        } else if (c.isEmpty()) {
            return DistanceHelper.H_REACHED_BUT_EMPTY;
        } else if (o == null){
            //null gives no gradient
            return DistanceHelper.H_NOT_EMPTY;
        }else {

            int counter = 0;

            final double base = DistanceHelper.H_NOT_EMPTY;
            double max = base;

            for (Object value : c) {
                if (counter == limit) {
                    break;
                }
                counter++;
                if(value == null){
                    continue;
                }

                double distance = DistanceHelper.getDistance(o, value);
                if(distance == Double.MAX_VALUE){
                    continue;
                }

                double h = DistanceHelper.heuristicFromScaledDistanceWithBase(base, distance);

                if (h > max) {
                    max = h;
                }
            }
            assert max < 1d;
            return max;
        }

    }
}
