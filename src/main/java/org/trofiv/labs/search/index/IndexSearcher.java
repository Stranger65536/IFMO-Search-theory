package org.trofiv.labs.search.index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IndexSearcher {
    private final org.apache.lucene.search.IndexSearcher indexSearcher;
    private final int size;

    public IndexSearcher(final String indexLocation) throws IOException {
        final Directory dir = FSDirectory.open(Paths.get(indexLocation));
        final IndexReader indexReader = DirectoryReader.open(dir);
        indexSearcher = new org.apache.lucene.search.IndexSearcher(indexReader);
        size = indexSearcher.count(new MatchAllDocsQuery());
    }

    public List<Document> search(final Query query) {
        final TopDocs topDocs;
        try {
            topDocs = indexSearcher.search(query, size);
        } catch (IOException e) {
            throw new IllegalStateException("Can't perform index search!", e);
        }
        return Arrays.stream(topDocs.scoreDocs).map(i -> {
            try {
                return indexSearcher.doc(i.doc);
            } catch (IOException e) {
                throw new IllegalStateException("Can't extract document by id: " + i.doc, e);
            }
        }).collect(Collectors.toList());
    }
}
