package org.trofiv.labs.search;

import org.trofiv.labs.search.document.DocumentModel;
import org.trofiv.labs.search.index.IndexBuilder;
import org.trofiv.labs.search.service.DocumentLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@SuppressWarnings("UtilityClassCanBeEnum")
public final class IndexBuilderDemo {
    private IndexBuilderDemo() {
    }

    public static void main(final String[] args) {
        final String indexedFile = args[0];
        final String indexLocation = args[1];

        final List<DocumentModel> docs;
        try (final InputStream stream = new FileInputStream(indexedFile)) {
            docs = DocumentLoader.loadDocuments(stream);
        } catch (IOException e) {
            throw new IllegalStateException("Can't load documents from " + indexedFile, e);
        }
        try (IndexBuilder indexBuilder = new IndexBuilder(indexLocation)) {
            docs.stream()
                    .map(DocumentModel::toLuceneDocument)
                    .forEach(docList -> {
                        try {
                            indexBuilder.addDocuments(docList);
                        } catch (IOException e) {
                            throw new IllegalStateException("Can't store documents: " + docList, e);
                        }
                    });
        } catch (Exception e) {
            throw new IllegalStateException("Can't create an index!", e);
        }
    }
}
