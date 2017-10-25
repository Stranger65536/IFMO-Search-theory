package org.trofiv.labs.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.trofiv.labs.search.document.DocumentModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Arrays.asList;

public enum DocumentLoader {
    ;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public static List<DocumentModel> loadDocuments(final InputStream inputStream) throws IOException {
        return asList(JSON_MAPPER.readValue(inputStream, DocumentModel[].class));
    }

}
