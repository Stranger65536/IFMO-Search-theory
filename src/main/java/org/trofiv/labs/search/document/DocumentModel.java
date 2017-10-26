package org.trofiv.labs.search.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
public class DocumentModel {
    @JsonProperty
    String id;
    @JsonProperty
    String name;
    @JsonProperty
    String gender;
    @JsonProperty
    String brand;
    @JsonProperty
    String description;
    @JsonProperty
    Set<SKUModel> sku;

    public long size() {
        return 1 + sku.stream().mapToLong(s -> s.getPrices().size()).map(i -> i + 1).sum();
    }

    public List<Document> toLuceneDocument() {
        final Document rootDocument = new Document();
        rootDocument.add(new StringField("id", id, Store.YES));
        rootDocument.add(new StringField("scope", "product", Store.NO));
        rootDocument.add(new TextField("brand", brand, Store.NO));
        rootDocument.add(new TextField("description", description, Store.NO));
        rootDocument.add(new StringField("gender", gender, Store.NO));
        rootDocument.add(new TextField("name", name, Store.NO));

        final Stream<Document> skuDocuments = sku.stream().map(skuModel -> {
            final Document skuDocument = new Document();
            //noinspection TooBroadScope
            skuDocument.add(new StringField("scope", "sku", Store.NO));
            skuDocument.add(new StringField("skuId", skuModel.getSkuId(), Store.YES));
            skuDocument.add(new StringField("color", skuModel.getColor(), Store.NO));
            skuDocument.add(new StringField("size", skuModel.getSize(), Store.NO));

            final Stream<Document> priceDocuments = skuModel.getPrices().stream().map(priceInfoModel -> {
                final Document priceDocument = new Document();
                priceDocument.add(new StringField("scope", "price", Store.NO));
                priceDocument.add(new TextField("address", priceInfoModel.getAddress(), Store.NO));
                priceDocument.add(new FloatPoint("price", priceInfoModel.getPrice()));
                return priceDocument;
            });

            return Stream.concat(priceDocuments, Stream.of(skuDocument));
        }).flatMap(Function.identity());

        return Stream.concat(skuDocuments, Stream.of(rootDocument)).collect(Collectors.toList());
    }
}
