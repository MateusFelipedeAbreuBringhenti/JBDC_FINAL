package com.unimater.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.unimater.dao.SaleItemDAO;
import com.unimater.model.SaleItem;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

public class SaleItemHandler implements HttpHandler {

    private SaleItemDAO saleItemDAO;
    private Connection connection;

    public SaleItemHandler(Connection connection) {
        this.connection = connection;
    }

    private Gson gson = new Gson();

    public void handle(HttpExchange exchange) throws IOException {
        saleItemDAO = new SaleItemDAO(connection);

        if (isGet(exchange)) {
            handleGet(exchange);
        } else if (isPost(exchange)) {
            handlePost(exchange);
        } else if (isPut(exchange)) {
            handlePut(exchange);
        } else if (isDelete(exchange)) {
            handleDelete(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String[] segments = segmentPath(exchange);
        if (segments.length > 2) {
            try {
                int id = Integer.parseInt(segments[2]);
                SaleItem saleItem = saleItemDAO.getById(id);
                if (saleItem != null) {
                    String jsonResponse = gson.toJson(saleItem);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                    sendOutputJson(exchange, jsonResponse);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, -1);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        SaleItem saleItem = parseRequestBody(exchange, SaleItem.class);
        if (saleItem != null && saleItem.getId() == 0) {
            saleItemDAO.upsert(saleItem);
            exchange.sendResponseHeaders(201, -1);
        } else {
            exchange.sendResponseHeaders(400, -1);
        }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        SaleItem saleItem = parseRequestBody(exchange, SaleItem.class);
        if (saleItem != null && saleItem.getId() > 0) {
            saleItemDAO.upsert(saleItem);
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(400, -1);
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        String[] segments = segmentPath(exchange);
        if (segments.length > 2) {
            try {
                int id = Integer.parseInt(segments[2]);
                saleItemDAO.delete(id);
                exchange.sendResponseHeaders(204, -1);
            } catch (NumberFormatException e) {
                exchange.sendResponseHeaders(400, -1);
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private <T> T parseRequestBody(HttpExchange exchange, Class<T> clazz) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        return gson.fromJson(reader, clazz);
    }

    private static void sendOutputJson(HttpExchange exchange, String jsonResponse) throws IOException {
        OutputStream output = exchange.getResponseBody();
        output.write(jsonResponse.getBytes());
        output.flush();
        output.close();
    }

    private static String[] segmentPath(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        String[] segments = path.split("/");
        return segments;
    }

    private static boolean isGet(HttpExchange exchange) {
        return exchange.getRequestMethod().equals("GET");
    }

    private static boolean isPost(HttpExchange exchange) {
        return exchange.getRequestMethod().equals("POST");
    }

    private static boolean isPut(HttpExchange exchange) {
        return exchange.getRequestMethod().equals("PUT");
    }

    private static boolean isDelete(HttpExchange exchange) {
        return exchange.getRequestMethod().equals("DELETE");
    }
}