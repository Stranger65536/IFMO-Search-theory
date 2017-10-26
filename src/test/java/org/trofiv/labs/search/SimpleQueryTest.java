package org.trofiv.labs.search;

import info.debatty.java.stringsimilarity.Cosine;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.junit.Test;
import org.trofiv.labs.search.document.DocumentModel;
import org.trofiv.labs.search.document.PriceInfoModel;
import org.trofiv.labs.search.document.SKUModel;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.trofiv.labs.search.util.DocumentUtils.toDocumentIds;

public class SimpleQueryTest extends BaseSearchTest {
    @SuppressWarnings({"resource", "IOResourceOpenedButNotSafelyClosed"})
    private static final Analyzer ANALYZER = new StandardAnalyzer();

    @Test
    public void testAllDocuments() {
        final Query query = new MatchAllDocsQuery();
        final List<Document> actual = indexSearcher.search(query);
        assertThat(actual, hasSize(Math.toIntExact(documentModels.stream().mapToLong(DocumentModel::size).sum())));
    }

    @Test
    public void testQueryBlackSkus() {
        final Query query = new TermQuery(new Term("color", "black"));
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Set<String> expected = documentModels.stream()
                .map(i -> i.getSku().stream()
                        .filter(skuModel -> "black".equals(skuModel.getColor()))
                        .map(SKUModel::getSkuId))
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testAdidasProducts() throws ParseException {
        final Query query = new QueryParser("brand", ANALYZER).parse("Adidas");
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> "Adidas".equals(documentModel.getBrand()))
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testFuzzyQuery() throws ParseException {
        final String word = "incididunt";
        final Query query = new QueryParser("description", ANALYZER).parse(word + "~0.7");
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern filter = Pattern.compile("[^a-zA-Z\\d\\s]+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel ->
                        Stream.of(filter.matcher(documentModel.getDescription()).replaceAll(" ").split(" "))
                                .filter(s -> !s.isEmpty())
                                .anyMatch(s -> new Cosine().similarity(s, word) >= 0.7))
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testPrefixQuery() throws ParseException {
        final String prefix = "incididunt";
        final Query query = new PrefixQuery(new Term("description", prefix));
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern filter = Pattern.compile("[^a-zA-Z\\d\\s]+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel ->
                        Stream.of(filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                                .replaceAll(" ").split(" "))
                                .filter(s -> !s.isEmpty())
                                .anyMatch(s -> s.startsWith(prefix)))
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testWildcardQuery() throws ParseException {
        final String word = "incididunt";
        //noinspection MagicCharacter
        final String wildcard = word + '*';
        final Query query = new WildcardQuery(new Term("description", wildcard));
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern filter = Pattern.compile("[^a-zA-Z\\d\\s]+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel ->
                        Stream.of(filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                                .replaceAll(" ").split(" "))
                                .filter(s -> !s.isEmpty())
                                .anyMatch(s -> s.startsWith(word)))
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    //todo span position check
    //todo span or
    //todo span not
    //todo span near
    //todo span first
    //todo span containing
    //todo span multi
    //todo custom query
    //todo custom weight
    //todo custom scorer
    //todo custom similarity

    @Test
    public void testQueryXLSkus() {
        final Query query = new TermQuery(new Term("size", "XL"));
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Set<String> expected = documentModels.stream()
                .map(i -> i.getSku().stream()
                        .filter(skuModel -> "XL".equals(skuModel.getSize()))
                        .map(SKUModel::getSkuId))
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testQueryBlackAndXLSkus() {
        final Query query1 = new TermQuery(new Term("size", "XL"));
        final Query query2 = new TermQuery(new Term("color", "black"));
        final Query query = new Builder()
                .add(query1, Occur.MUST)
                .add(query2, Occur.MUST)
                .build();
        final Set<String> expected = documentModels.stream()
                .map(i -> i.getSku().stream()
                        .filter(skuModel -> "XL".equals(skuModel.getSize()))
                        .filter(skuModel -> "black".equals(skuModel.getColor()))
                        .map(SKUModel::getSkuId))
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testQueryBlackOrXLSkus() {
        final Query query1 = new TermQuery(new Term("size", "XL"));
        final Query query2 = new TermQuery(new Term("color", "black"));
        final Query query = new Builder()
                .add(query1, Occur.SHOULD)
                .add(query2, Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();
        final Set<String> expected = documentModels.stream()
                .map(i -> i.getSku().stream()
                        .filter(skuModel -> "XL".equals(skuModel.getSize()) || "black".equals(skuModel.getColor()))
                        .map(SKUModel::getSkuId))
                .flatMap(Function.identity())
                .collect(Collectors.toSet());
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testQueryProductsWithBlackSkus() {
        final Query nestedQuery = new TermQuery(new Term("color", "black"));
        final Query parentFilter = new TermQuery(new Term("scope", "product"));
        final Query parentQuery = new ToParentBlockJoinQuery(nestedQuery,
                new QueryBitSetProducer(parentFilter), ScoreMode.None);
        final Set<String> actual = toDocumentIds(indexSearcher.search(parentQuery));
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> documentModel.getSku().stream()
                        .anyMatch(skuModel -> "black".equals(skuModel.getColor())))
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testQueryProductsWithBlackAndXLSkus() {
        final Query query1 = new TermQuery(new Term("size", "XL"));
        final Query query2 = new TermQuery(new Term("color", "black"));
        final Query nestedQuery = new Builder()
                .add(query1, Occur.MUST)
                .add(query2, Occur.MUST)
                .build();
        final Query parentFilter = new TermQuery(new Term("scope", "product"));
        final Query parentQuery = new ToParentBlockJoinQuery(nestedQuery,
                new QueryBitSetProducer(parentFilter), ScoreMode.None);
        final Set<String> actual = toDocumentIds(indexSearcher.search(parentQuery));
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> documentModel.getSku().stream()
                        .anyMatch(skuModel -> "black".equals(skuModel.getColor()) && "XL".equals(skuModel.getSize())))
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testQueryProductsWithBlackOrXLSkus() {
        final Query query1 = new TermQuery(new Term("size", "XL"));
        final Query query2 = new TermQuery(new Term("color", "black"));
        final Query childQuery = new Builder()
                .add(query1, Occur.SHOULD)
                .add(query2, Occur.SHOULD)
                .setMinimumNumberShouldMatch(1)
                .build();
        final Query parentFilter = new TermQuery(new Term("scope", "product"));
        final Query parentQuery = new ToParentBlockJoinQuery(childQuery,
                new QueryBitSetProducer(parentFilter), ScoreMode.None);
        final Set<String> actual = toDocumentIds(indexSearcher.search(parentQuery));
        final Set<String> expected = documentModels.stream().filter(documentModel ->
                documentModel.getSku().stream().anyMatch(skuModel ->
                        "black".equals(skuModel.getColor()) || "XL".equals(skuModel.getSize())))
                .map(DocumentModel::getId).collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testQueryAdidasProductsWithXLSkusWithPriceFrom100to200() throws ParseException {
        final Query childFilter = new TermQuery(new Term("scope", "sku"));
        final Query grandChildQuery = FloatPoint.newRangeQuery("price", 100.0f, 200.0f);
        final Query childJoinQuery = new ToParentBlockJoinQuery(grandChildQuery,
                new QueryBitSetProducer(childFilter), ScoreMode.None);

        final Query parentFilter = new Builder()
                .add(new TermQuery(new Term("scope", "product")), Occur.MUST)
                .build();
        final Query childQuery = new Builder()
                .add(new TermQuery(new Term("size", "XL")), Occur.MUST)
                .add(childJoinQuery, Occur.MUST)
                .build();
        final Query parentJoinQuery = new ToParentBlockJoinQuery(childQuery,
                new QueryBitSetProducer(parentFilter), ScoreMode.None);

        final Query parentQuery = new Builder()
                .add(new QueryParser("brand", ANALYZER).parse("Adidas"), Occur.MUST)
                .add(parentJoinQuery, Occur.MUST)
                .build();

        final Set<String> actual = toDocumentIds(indexSearcher.search(parentQuery));
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> "Adidas".equals(documentModel.getBrand()))
                .filter(documentModel -> documentModel.getSku().stream()
                        .anyMatch(skuModel -> "XL".equals(skuModel.getSize())
                                && skuModel.getPrices().stream()
                                .map(PriceInfoModel::getPrice)
                                .anyMatch(price -> price >= 100.0f && price <= 200.0f)))
                .map(DocumentModel::getId).collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }
}
