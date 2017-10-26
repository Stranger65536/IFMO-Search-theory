package org.trofiv.labs.search.search.random;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.Similarity.SimWeight;

import java.io.IOException;

public class RandomizedScorer extends Scorer {
    private final DocIdSetIterator iterator;
    private final Similarity similarity;
    private final LeafReaderContext context;

    @SuppressWarnings("WeakerAccess")
    public RandomizedScorer(final LeafReaderContext context, final Similarity similarity, final Weight w) {
        super(w);
        this.iterator = DocIdSetIterator.all(context.reader().maxDoc());
        this.similarity = similarity;
        this.context = context;
    }

    @Override
    public int docID() {
        return iterator.docID();
    }

    @Override
    public float score() throws IOException {
        final SimWeight simWeight = similarity.computeWeight(null);
        return similarity.simScorer(simWeight, context).score(docID(), freq());
    }

    @Override
    public int freq() throws IOException {
        return 1;
    }

    @Override
    public DocIdSetIterator iterator() {
        return iterator;
    }
}
