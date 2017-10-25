package org.trofiv.labs.search;

import lombok.extern.log4j.Log4j2;
import org.junit.BeforeClass;
import org.trofiv.labs.search.document.DocumentModel;
import org.trofiv.labs.search.index.IndexBuilder;
import org.trofiv.labs.search.index.IndexSearcher;
import org.trofiv.labs.search.service.DocumentLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Log4j2
public abstract class BaseSearchTest {
    private static final String INDEX_DATA_LOCATION = "product_data.json";
    private static final String INDEX_DIR_NAME = "idx";
    protected static IndexSearcher indexSearcher;
    protected static List<DocumentModel> documentModels;
    private static Path indexPath;

    @BeforeClass
    public static void setUp() {
        try {
            log.info("Loading documents from {}", INDEX_DATA_LOCATION);
            documentModels = DocumentLoader.loadDocuments(ClassLoader.getSystemResourceAsStream(INDEX_DATA_LOCATION));
            try {
                log.info("Creating temp directory for index: {}", INDEX_DIR_NAME);
                indexPath = Files.createTempDirectory(INDEX_DIR_NAME);
            } catch (IOException e) {
                throw new IllegalStateException("Can't create temp directory for index: " + INDEX_DIR_NAME, e);
            }
            log.info("Indexing documents into {}", indexPath);
            try (IndexBuilder indexBuilder = new IndexBuilder(indexPath.toString())) {
                documentModels.stream()
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
            indexSearcher = new IndexSearcher(indexPath.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Can't find classpath resource " + INDEX_DATA_LOCATION, e);
        }
    }
}
