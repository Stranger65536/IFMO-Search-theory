package org.trofiv.labs.search.search.even;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import java.io.IOException;
import java.util.Set;

public class EvenWeight extends Weight {
    @SuppressWarnings("WeakerAccess")
    public EvenWeight(final Query query) {
        super(query);
    }

    @Override
    public void extractTerms(final Set<Term> terms) {

    }

    @Override
    @SuppressWarnings("ReturnOfNull")
    public Explanation explain(final LeafReaderContext context, final int doc) throws IOException {
        return null;
    }

    @Override
    public float getValueForNormalization() throws IOException {
        return 0;
    }

    @Override
    public void normalize(final float norm, final float boost) {

    }

    @Override
    public Scorer scorer(final LeafReaderContext context) throws IOException {
        return new EvenScorer(context, this);
    }
}
