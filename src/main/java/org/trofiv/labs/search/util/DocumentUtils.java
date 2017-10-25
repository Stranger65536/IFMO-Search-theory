package org.trofiv.labs.search.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DocumentUtils {
    ;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String dumpDocument(final Document document) {
        final ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
        document.getFields().forEach(field -> objectNode.put(field.name(), field.stringValue()));
        return objectNode.toString();
    }

    public static Set<String> toDocumentIds(final Collection<Document> documents) {
        return documents.stream().map(doc -> doc.getFields().get(0).stringValue()).collect(Collectors.toSet());
    }

    public static Map<String, Document> indexDocuments(
            final Collection<Document> documents,
            final String fieldName) {
        return indexDocuments(documents, fieldName, i -> true);
    }

    public static String getFieldValue(final Document document, final String fieldName) {
        final IndexableField field = document.getField(fieldName);
        return field == null ? "" : field.stringValue();
    }

    public static Map<String, Document> indexDocuments(
            final Collection<Document> documents,
            final String fieldName,
            final Function<Document, Boolean> documentFilter) {
        return documents.stream()
                .filter(documentFilter::apply)
                .filter(doc -> doc.getField(fieldName) != null)
                .collect(Collectors.toMap(doc -> getFieldValue(doc, fieldName), Function.identity()));
    }
}
