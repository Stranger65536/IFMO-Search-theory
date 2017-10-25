package org.trofiv.labs.search.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PriceInfoModel {
    @JsonProperty
    String address;
    @JsonProperty
    float price;

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + address.hashCode();
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

        final PriceInfoModel other = (PriceInfoModel) o;

        return address.equals(other.address);
    }
}
