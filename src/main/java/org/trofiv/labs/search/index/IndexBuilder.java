package org.trofiv.labs.search.index;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;

public class IndexBuilder implements Closeable {
    private final Directory directory;
    private final IndexWriter indexWriter;

    public IndexBuilder(final String indexLocation) throws IOException {
        directory = FSDirectory.open(Paths.get(indexLocation));
        final IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
        iwc.setOpenMode(OpenMode.CREATE);
        indexWriter = new IndexWriter(directory, iwc);
    }

    public void addDocuments(final Iterable<Document> documents) throws IOException {
        indexWriter.addDocuments(documents);
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
        directory.close();
    }
}
