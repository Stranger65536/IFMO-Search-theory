package org.trofiv.labs.search.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.Set;

@Value
public class SKUModel {
    @JsonProperty
    String skuId;
    @JsonProperty
    String size;
    @JsonProperty
    String color;
    @JsonProperty
    Set<PriceInfoModel> prices;

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + size.hashCode();
        result = 31 * result + color.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final SKUModel skuModel = (SKUModel) o;

        return size.equals(skuModel.size) && color.equals(skuModel.color);
    }
}
