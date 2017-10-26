package org.trofiv.labs.search.search;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import java.io.IOException;

public class EvenScorer extends Scorer {
    private final DocIdSetIterator iterator;

    @SuppressWarnings("WeakerAccess")
    public EvenScorer(final LeafReaderContext context, final Weight w) {
        super(w);
        iterator = new EvenDocIdSetIterator(context.reader().maxDoc());
    }

    @Override
    public int docID() {
        return iterator.docID();
    }

    @Override
    public float score() throws IOException {
        return 1;
    }

    @Override
    public int freq() throws IOException {
        return 1;
    }

    @Override
    public DocIdSetIterator iterator() {
        return iterator;
    }

    private static final class EvenDocIdSetIterator extends DocIdSetIterator {
        final int maxDoc;
        int doc = -1;

        private EvenDocIdSetIterator(final int maxDoc) {
            this.maxDoc = maxDoc;
        }

        @Override
        public int docID() {
            return doc;
        }

        @Override
        public int nextDoc() {
            return advance(doc + 2);
        }

        @Override
        public int advance(final int target) {
            doc = target;
            if (doc % 2 == 0) {
                doc++;
            }
            if (doc >= maxDoc) {
                doc = NO_MORE_DOCS;
            }
            return doc;
        }

        @Override
        public long cost() {
            return maxDoc / 2;
        }
    }
}
