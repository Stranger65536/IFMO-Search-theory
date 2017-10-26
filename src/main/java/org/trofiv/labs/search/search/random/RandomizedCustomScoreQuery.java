package org.trofiv.labs.search.search.random;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.Random;

public class RandomizedCustomScoreQuery extends CustomScoreQuery {
    public static final float LOWER_BOUND = 0.0f;
    public static final float UPPER_BOUND = 10.0f;

    public RandomizedCustomScoreQuery(final Query subQuery) {
        super(subQuery);
    }

    @Override
    protected CustomScoreProvider getCustomScoreProvider(final LeafReaderContext context) throws IOException {
        return new RandomizedCustomScoreProvider(context);
    }

    @SuppressWarnings("WeakerAccess")
    private static class RandomizedCustomScoreProvider extends CustomScoreProvider {
        private static final Random RANDOM = new Random(172488);

        public RandomizedCustomScoreProvider(final LeafReaderContext context) {
            super(context);
        }

        @Override
        public float customScore(final int doc, final float subQueryScore, final float valSrcScore) throws IOException {
            return RANDOM.nextFloat() * UPPER_BOUND;
        }
    }
}
