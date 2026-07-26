package org.cache.network.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.cache.protocol.ProtocolConstants.*;

public class RespConnection implements ProtocolConnection, AutoCloseable {

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final LineProtocolConnection lines;

    public RespConnection(Socket socket) throws IOException {
        this(socket, socket.getInputStream(), socket.getOutputStream());
    }

    RespConnection(Socket socket, InputStream input, OutputStream output) throws IOException {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.lines = new LineProtocolConnection(socket, input, output);
    }

    @Override
    public List<String> readCommand() throws IOException {
        int type = input.read();

        if (type == -1) {
            return null;
        }

        if (type != ARRAY_PREFIX) {
            throw new IOException("Expected '" + ARRAY_PREFIX + "' but received '" + (char) type + "'");
        }

        int itemCount = parseInteger(lines.readLine(), "array length");
        List<String> values = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            values.add(readBulkString());
        }

        return values;
    }

    @Override
    public void write(String value) throws IOException {
        byte[] bytes = value.getBytes(UTF_8);
        writeAscii(BULK_STRING_PREFIX + Integer.toString(bytes.length) + CRLF);
        output.write(bytes);
        writeAscii(CRLF);
        output.flush();
    }

    @Override
    public String send(String value) throws IOException {
        return sendCommand(List.of(value.trim().split("\\s+")));
    }

    public String sendCommand(List<String> commandParts) throws IOException {
        writeArray(commandParts);
        String response = readString();

        if (response == null) {
            throw new IOException("Connection closed");
        }

        return response;
    }

    public void writeArray(List<String> values) throws IOException {
        writeAscii(ARRAY_PREFIX + Integer.toString(values.size()) + CRLF);

        for (String value : values) {
            byte[] bytes = value.getBytes(UTF_8);
            writeAscii(BULK_STRING_PREFIX + Integer.toString(bytes.length) + CRLF);
            output.write(bytes);
            writeAscii(CRLF);
        }

        output.flush();
    }

    private String readString() throws IOException {
        int type = input.read();

        if (type == -1) {
            return null;
        }

        return switch (type) {
            case SIMPLE_STRING_PREFIX, INTEGER_PREFIX -> lines.readLine();
            case ERROR_PREFIX -> "ERROR " + lines.readLine();
            case BULK_STRING_PREFIX -> readBulkStringAfterType();
            default -> throw new IOException("Unsupported RESP response type: " + (char) type);
        };
    }

    private String readBulkString() throws IOException {
        expect(BULK_STRING_PREFIX);
        return readBulkStringAfterType();
    }

    private String readBulkStringAfterType() throws IOException {
        int length = parseInteger(lines.readLine(), "bulk string length");

        if (length < 0) {
            return null;
        }

        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Connection closed while reading bulk string");
        }

        expect(CARRIAGE_RETURN);
        expect(NEW_LINE);

        return new String(bytes, UTF_8);
    }

    private int parseInteger(String value, String name) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid RESP " + name + ": " + value, exception);
        }
    }

    private void expect(char expected) throws IOException {
        int actual = input.read();

        if (actual != expected) {
            throw new IOException("Expected '" + expected + "' but received '" + (char) actual + "'");
        }
    }

    private void writeAscii(String value) throws IOException {
        output.write(value.getBytes(US_ASCII));
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
