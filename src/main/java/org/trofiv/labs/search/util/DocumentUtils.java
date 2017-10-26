package org.trofiv.labs.search.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public static boolean areEqual(final Document first, final Document second) {
        final List<IndexableField> firstFields = first.getFields();
        final List<IndexableField> secondFields = second.getFields();

        if (firstFields.size() != secondFields.size()) {
            return false;
        }

        final Iterator<IndexableField> firstIt = firstFields.iterator();
        final Iterator<IndexableField> secondIt = secondFields.iterator();

        while (firstIt.hasNext() && secondIt.hasNext()) {
            final IndexableField firstField = firstIt.next();
            final IndexableField secondField = secondIt.next();

            if (!Objects.equals(firstField.name(), secondField.name())
                    || !Objects.equals(firstField.stringValue(), secondField.stringValue())) {
                return false;
            }
        }

        return true;
    }

    public static boolean areEqual(final Collection<Document> first, final Collection<Document> second) {
        if (first.size() != second.size()) {
            return false;
        }

        final Iterator<Document> firstIt = first.iterator();
        final Iterator<Document> secondIt = second.iterator();

        while (firstIt.hasNext() && secondIt.hasNext()) {
            final Document firstDoc = firstIt.next();
            final Document secondDoc = secondIt.next();

            if (!areEqual(firstDoc, secondDoc)) {
                return false;
            }
        }

        return true;
    }
}
