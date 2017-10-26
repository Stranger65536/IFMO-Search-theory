package org.trofiv.labs.search.search.random;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;

import java.io.IOException;

@SuppressWarnings("PublicMethodNotExposedInInterface")
public class RandomizedScoreQuery extends Query {
    @Override
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public String toString(final String field) {
        return "Even query";
    }

    @Override
    public Weight createWeight(final IndexSearcher searcher, final boolean needsScores) throws IOException {
        return new RandomizedWeight(this);
    }

    @Override
    public Query rewrite(final IndexReader reader) throws IOException {
        return this;
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(final Object obj) {
        return true;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}