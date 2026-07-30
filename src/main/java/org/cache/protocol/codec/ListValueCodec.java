package org.cache.protocol.codec;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ListValueCodec implements ValueCodec<List<String>> {

    @Override
    public byte[] encode(List<String> value) {

        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            DataOutputStream dao = new DataOutputStream(byteArrayOutputStream);

            for(String element : value) {
                dao.writeUTF(element);
            }

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new CodecConversionException("Something went wrong in list to byte encoding");
        }
    }

    @Override
    public List<String> decode(byte[] value) {
        List<String> list = new ArrayList<>();

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(value);
             DataInputStream dis = new DataInputStream(byteArrayInputStream)) {

            while (dis.available() > 0) {
                list.add(dis.readUTF());
            }
        } catch (IOException e) {
            throw new CodecConversionException("Something went wrong in byte to list decoding");
        }
        return list;
    }

    @Override
    public String toString(byte[] value) {
        List<String> list = decode(value);

        return String.join(", ", list);
    }
}
