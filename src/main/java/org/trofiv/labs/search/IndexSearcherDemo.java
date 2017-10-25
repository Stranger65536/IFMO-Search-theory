package org.trofiv.labs.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.trofiv.labs.search.document.DocumentModel;
import org.trofiv.labs.search.index.IndexSearcher;
import org.trofiv.labs.search.service.DocumentLoader;
import org.trofiv.labs.search.util.DocumentUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UtilityClassCanBeEnum")
public final class IndexSearcherDemo {
    private IndexSearcherDemo() {
    }

    public static void main(final String[] args) throws IOException {
        final String dataLocation = args[0];
        final String indexLocation = args[1];
        final List<Document> docModels;
        try (final InputStream stream = new FileInputStream(dataLocation)) {
            docModels = DocumentLoader.loadDocuments(stream).stream()
                    .map(DocumentModel::toLuceneDocument).flatMap(List::stream).collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Can't load documents from " + dataLocation, e);
        }
        final Map<String, Document> rootIndex = DocumentUtils.indexDocuments(docModels, "id");
        final Map<String, Document> skuIndex = DocumentUtils.indexDocuments(
                docModels, "skuId", doc -> doc.getField("productId") != null);
        final IndexSearcher indexSearcher = new IndexSearcher(indexLocation);

        final Query query1 = new PhraseQuery("color", "black");
        final Query query2 = new PhraseQuery("size", "S");
        final Query query = new Builder()
                .add(query1, Occur.MUST)
                .add(query2, Occur.MUST)
                .build();

        indexSearcher.search(query).forEach(doc -> {
            final Document rootDoc = rootIndex.get(DocumentUtils.getFieldValue(doc, "id"));
            final Document skuDoc = skuIndex.get(DocumentUtils.getFieldValue(doc, "skuId"));
            Stream.of(doc, rootDoc, skuDoc)
                    .filter(Objects::nonNull)
                    .map(DocumentUtils::dumpDocument)
                    .forEach(System.out::print);
            System.out.println();
        });
    }
}
