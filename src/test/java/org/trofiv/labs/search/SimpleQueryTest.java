package org.trofiv.labs.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.search.join.ToParentBlockJoinQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.trofiv.labs.search.document.DocumentModel;
import org.trofiv.labs.search.document.PriceInfoModel;
import org.trofiv.labs.search.document.SKUModel;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.trofiv.labs.search.util.DocumentUtils.toDocumentIds;

public class SimpleQueryTest extends BaseSearchTest {
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
    @Ignore
    public void testQueryAdidasProductsWithXLSkusWithPriceFrom100to200() {
        final Query childFilter = new TermQuery(new Term("scope", "sku"));
        final Query grandChildQuery = FloatPoint.newRangeQuery("price", 100.0f, 200.0f);
        final Query childJoinQuery = new ToParentBlockJoinQuery(grandChildQuery,
                new QueryBitSetProducer(childFilter), ScoreMode.None);

        final Query parentFilter = new Builder()
                .add(new TermQuery(new Term("brand", "Adidas")), Occur.MUST)
                .build();
        final Query childQuery = new Builder()
                .add(new TermQuery(new Term("size", "XL")), Occur.MUST)
                .add(childJoinQuery, Occur.MUST)
                .build();
        final Query parentJoinQuery = new ToParentBlockJoinQuery(childQuery,
                new QueryBitSetProducer(parentFilter), ScoreMode.None);

        final Set<String> actual = toDocumentIds(indexSearcher.search(parentJoinQuery));
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
