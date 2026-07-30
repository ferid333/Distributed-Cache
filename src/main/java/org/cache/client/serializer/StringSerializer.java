package org.cache.client.serializer;

public class StringSerializer implements Serializer<String> {

    @Override
    public String encode(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    @Override
    public String decode(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }
}
