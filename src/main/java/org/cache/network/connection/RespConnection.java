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
import static org.cache.protocol.commands.ResponseConstants.ERROR;
import static org.cache.protocol.commands.ResponseConstants.METRICS;
import static org.cache.protocol.commands.ResponseConstants.NOT_FOUND;
import static org.cache.protocol.commands.ResponseConstants.OK;
import static org.cache.protocol.commands.ResponseConstants.SIZE;
import static org.cache.protocol.commands.ResponseConstants.VALUE;

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
        writeArray(responseParts(value));
    }

    @Override
    public String send(String value) throws IOException {
        return sendCommand(List.of(value.trim().split("\\s+")));
    }

    public String sendCommand(List<String> commandParts) throws IOException {
        List<String> response = sendCommandForResponse(commandParts);
        return String.join(" ", response);
    }

    public List<String> sendCommandForResponse(List<String> commandParts) throws IOException {
        writeArray(commandParts);
        List<String> response = readResponse();

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
        List<String> response = readResponse();

        if (response == null) {
            return null;
        }

        return String.join(" ", response);
    }

    private List<String> readResponse() throws IOException {
        int type = input.read();

        if (type == -1) {
            return null;
        }

        return switch (type) {
            case ARRAY_PREFIX -> readArrayAfterType();
            case SIMPLE_STRING_PREFIX, INTEGER_PREFIX -> List.of(lines.readLine());
            case ERROR_PREFIX -> List.of(ERROR.name(), lines.readLine());
            case BULK_STRING_PREFIX -> List.of(readBulkStringAfterType());
            default -> throw new IOException("Unsupported RESP response type: " + (char) type);
        };
    }

    private List<String> readArrayAfterType() throws IOException {
        int itemCount = parseInteger(lines.readLine(), "array length");
        List<String> values = new ArrayList<>(itemCount);

        for (int i = 0; i < itemCount; i++) {
            values.add(readBulkString());
        }

        return values;
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

    private List<String> responseParts(String response) {
        if (response.equals(OK.name()) || response.equals(NOT_FOUND.name())) {
            return List.of(response);
        }

        if (response.startsWith(ERROR.name() + " ")) {
            return List.of(ERROR.name(), response.substring((ERROR.name() + " ").length()));
        }

        if (response.startsWith(VALUE.name() + " ")) {
            return List.of(VALUE.name(), response.substring((VALUE.name() + " ").length()));
        }

        if (response.startsWith(SIZE.name() + " ")) {
            return List.of(SIZE.name(), response.substring((SIZE.name() + " ").length()));
        }

        if (response.startsWith(METRICS.name() + " ")) {
            return metricParts(response);
        }

        return List.of(response);
    }

    private List<String> metricParts(String response) {
        List<String> parts = new ArrayList<>();
        parts.add(METRICS.name());

        String metrics = response.substring((METRICS.name() + " ").length());
        for (String metric : metrics.split("\\s+")) {
            String[] nameAndValue = metric.split("=", 2);
            if (nameAndValue.length == 2) {
                parts.add(nameAndValue[0]);
                parts.add(nameAndValue[1]);
            }
        }

        return parts;
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
