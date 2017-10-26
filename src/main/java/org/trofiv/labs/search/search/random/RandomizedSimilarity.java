package org.trofiv.labs.search.search.random;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.Random;

public class RandomizedSimilarity extends Similarity {
    public static final float LOWER_BOUND = 0.0f;
    public static final float UPPER_BOUND = 100.0f;
    private static final Random RANDOM = new Random(172488);
    private final SimScorer simScorer;
    private final SimWeight simWeight;

    public RandomizedSimilarity() {
        this.simScorer = new RandomizedSimScorer();
        this.simWeight = new RandomizedSimWeight();
    }

    @Override
    public long computeNorm(final FieldInvertState state) {
        return 0;
    }

    @Override
    public SimWeight computeWeight(final CollectionStatistics collectionStats, final TermStatistics... termStats) {
        return simWeight;
    }

    @Override
    public SimScorer simScorer(final SimWeight weight, final LeafReaderContext context) throws IOException {
        return simScorer;
    }

    private static class RandomizedSimWeight extends SimWeight {
        @Override
        public float getValueForNormalization() {
            return 0;
        }

        @Override
        public void normalize(final float queryNorm, final float boost) {
        }
    }

    private static class RandomizedSimScorer extends SimScorer {
        @Override
        public float score(final int doc, final float freq) {
            return RANDOM.nextFloat() * UPPER_BOUND;
        }

        @Override
        public float computeSlopFactor(final int distance) {
            return 0;
        }

        @Override
        public float computePayloadFactor(final int doc, final int start, final int end, final BytesRef payload) {
            return 0;
        }
    }
}
