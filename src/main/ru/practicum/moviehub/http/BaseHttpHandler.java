package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    protected void writeResponse(HttpExchange exchange,
                               String responseString,
                               int responseCode) throws IOException {

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json; charset=UTF-8"
        );

        try (OutputStream os = exchange.getResponseBody()) {
            exchange.sendResponseHeaders(responseCode, 0);
            os.write(responseString.getBytes(DEFAULT_CHARSET));
        }
        exchange.close();
    }

    protected void writeEmptyResponse(HttpExchange exchange, int responseCode) throws IOException {
        exchange.sendResponseHeaders(responseCode, -1);
    }
}