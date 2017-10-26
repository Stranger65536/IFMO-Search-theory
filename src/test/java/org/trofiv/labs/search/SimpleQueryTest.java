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
import org.apache.lucene.search.spans.SpanContainingQuery;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanNotQuery;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanPositionRangeQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.trofiv.labs.search.document.DocumentModel;
import org.trofiv.labs.search.document.PriceInfoModel;
import org.trofiv.labs.search.document.SKUModel;
import org.trofiv.labs.search.search.even.EvenQuery;
import org.trofiv.labs.search.search.random.RandomizedCustomScoreQuery;
import org.trofiv.labs.search.search.random.RandomizedScoreQuery;
import org.trofiv.labs.search.search.random.RandomizedSimilarity;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.trofiv.labs.search.util.DocumentUtils.areEqual;
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
        //searches for terms with the specified similarity level
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
        //makes wildcard prefix
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
        //performs term expansion
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

    @Test
    public void testSpanQuery() {
        //words are located in the specific (or not) order at the specific maximum distance
        final Query query = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "commodo")),
                new SpanTermQuery(new Term("description", "nulla"))},
                2,
                true);
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern pattern = Pattern.compile(".*commodo\\s+(?<term>[a-zA-Z\\d]+\\s+){0,2}nulla.*");
        final Pattern filter = Pattern.compile("(?<nonterm>[^a-zA-Z\\d\\s]|[\r\n])+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> {
                    final String prepared = filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                            .replaceAll(" ");
                    return pattern.matcher(prepared).matches();
                })
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testSpanContainingQuery() {
        //one span is included into another
        final SpanNearQuery spanBigQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "commodo")),
                new SpanTermQuery(new Term("description", "nulla"))},
                2,
                true);
        final SpanNearQuery spanLittleQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "sint")),
                new SpanTermQuery(new Term("description", "nulla"))},
                1,
                true);
        final Query query = new SpanContainingQuery(spanBigQuery, spanLittleQuery);
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern pattern = Pattern.compile(".*commodo\\s+sint(?<nulla>\\s+nulla){2}.*");
        final Pattern filter = Pattern.compile("(?<nonterm>[^a-zA-Z\\d\\s]|[\r\n])+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> {
                    final String prepared = filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                            .replaceAll(" ");
                    return pattern.matcher(prepared).matches();
                })
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testSpanPositionCheckQuery() {
        //span found at the specific positions range
        final SpanNearQuery spanQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "commodo")),
                new SpanTermQuery(new Term("description", "nulla"))},
                2,
                true);
        final Query query = new SpanPositionRangeQuery(spanQuery, 3, 6);
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern pattern = Pattern.compile(".*commodo\\s+(?<term>[a-zA-Z\\d]+\\s+){0,2}nulla.*");
        final Pattern filter = Pattern.compile("(?<nonterm>[^a-zA-Z\\d\\s]|[\r\n])+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> {
                    final String prepared = filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                            .replaceAll(" ");
                    if (pattern.matcher(prepared).matches()) {
                        final List<String> terms = Stream.of(prepared.split(" "))
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                        return terms.size() >= 5 && "commodo".equals(terms.get(3)) && "nulla".equals(terms.get(5));
                    } else {
                        return false;
                    }
                })
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testSpanOrQuery() {
        //at least one of spans matches
        final SpanNearQuery firstQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "commodo")),
                new SpanTermQuery(new Term("description", "nulla"))},
                2,
                true);
        final SpanNearQuery secondQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "tempor")),
                new SpanTermQuery(new Term("description", "ipsum"))},
                2,
                true);
        final Query query = new SpanOrQuery(firstQuery, secondQuery);
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern firstPattern = Pattern.compile(".*commodo\\s+(?<term>[a-zA-Z\\d]+\\s+){0,2}nulla.*");
        final Pattern secondPattern = Pattern.compile(".*tempor\\s+(?<term>[a-zA-Z\\d]+\\s+){0,2}ipsum.*");
        final Pattern filter = Pattern.compile("(?<nonterm>[^a-zA-Z\\d\\s]|[\r\n])+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> {
                    final String prepared = filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                            .replaceAll(" ");
                    return firstPattern.matcher(prepared).matches() || secondPattern.matcher(prepared).matches();
                })
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void testSpanNotQuery() {
        //Should have no overlappings
        final SpanNearQuery includeQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "commodo")),
                new SpanTermQuery(new Term("description", "nulla"))},
                2,
                true);
        final SpanNearQuery excludeQuery = new SpanNearQuery(new SpanQuery[]{
                new SpanTermQuery(new Term("description", "sint")),
                new SpanTermQuery(new Term("description", "nulla"))},
                1,
                true);
        final Query query = new SpanNotQuery(includeQuery, excludeQuery);
        final Set<String> actual = toDocumentIds(indexSearcher.search(query));
        final Pattern includePattern = Pattern.compile(".*commodo\\s+(?<term>[a-zA-Z\\d]+\\s+){0,2}nulla.*");
        final Pattern excludePattern = Pattern.compile(".*commodo\\s+sint(?<nulla>\\s+nulla){2}.*");
        final Pattern filter = Pattern.compile("(?<nonterm>[^a-zA-Z\\d\\s]|[\r\n])+");
        final Set<String> expected = documentModels.stream()
                .filter(documentModel -> {
                    final String prepared = filter.matcher(documentModel.getDescription().toLowerCase(Locale.US))
                            .replaceAll(" ");
                    return includePattern.matcher(prepared).matches() && !excludePattern.matcher(prepared).matches();
                })
                .map(DocumentModel::getId)
                .collect(Collectors.toSet());
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void customQueryTest() {
        final Query query = new EvenQuery();
        final List<Document> actual = indexSearcher.search(query);
        final List<Document> all = indexSearcher.search(new MatchAllDocsQuery());
        final Iterator<Boolean> matcher =
                IntStream.iterate(0, i -> (i + 1) % 2)
                        .mapToObj(i -> i != 0)
                        .iterator();
        final List<Document> expected = all.stream().filter(i -> matcher.next()).collect(Collectors.toList());
        assertTrue(areEqual(actual, expected));
    }

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

    @Test
    @SuppressWarnings("MagicNumber")
    public void testCustomSimilarityScores() {
        final Map<Document, Float> result = indexSearcher.scoredSearch(new RandomizedScoreQuery());
        result.forEach((k, v) -> MatcherAssert.assertThat(v,
                is(both(greaterThanOrEqualTo(RandomizedSimilarity.LOWER_BOUND))
                        .and(lessThan(RandomizedSimilarity.UPPER_BOUND)))));
    }

    @Test
    @SuppressWarnings("MagicNumber")
    public void customScoreQueryTest() {
        final Map<Document, Float> result = indexSearcher.scoredSearch(
                new RandomizedCustomScoreQuery(new MatchAllDocsQuery()));
        result.forEach((k, v) -> MatcherAssert.assertThat(v,
                is(both(greaterThanOrEqualTo(RandomizedCustomScoreQuery.LOWER_BOUND))
                        .and(lessThan(RandomizedCustomScoreQuery.UPPER_BOUND)))));
    }
}
