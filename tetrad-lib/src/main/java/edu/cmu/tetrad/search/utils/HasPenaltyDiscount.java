package edu.cmu.tetrad.search.utils;

import edu.cmu.tetrad.search.score.Score;

/**
 * Provides an interface for an algorithm can can get/set a value for penalty disoucnt.
 *
 * @author josephramsey
 */
public interface HasPenaltyDiscount extends Score {
    double getPenaltyDiscount();

    void setPenaltyDiscount(double penaltyDiscount);
}
