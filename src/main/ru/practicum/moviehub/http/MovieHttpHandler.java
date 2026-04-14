package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.exception.BadRequestException;
import ru.practicum.moviehub.exception.NotFoundException;
import ru.practicum.moviehub.exception.ValidationException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieHttpHandler extends BaseHttpHandler {

    private enum Endpoint {
        GET_MOVIES,
        GET_MOVIE_BY_ID,
        POST_MOVIE,
        DELETE_MOVIE,
        UNKNOWN,
    }

    private final MoviesStore moviesStore;
    private final Gson gson;

    public MovieHttpHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Endpoint endpoint = getEndpoint(exchange.getRequestURI().getPath(), exchange.getRequestMethod());

        switch (endpoint) {
            case GET_MOVIES -> handleGetMovies(exchange);
            case GET_MOVIE_BY_ID -> handleGetMoviesById(exchange);
            case POST_MOVIE -> handlePostMovie(exchange);
            case DELETE_MOVIE -> handleDeleteMovie(exchange);
            case UNKNOWN -> handleMethodNotAllowed(exchange);
        }
    }

    private Endpoint getEndpoint(String requestPath, String requestMethod) {
        String[] pathParts = requestPath.split("/");

        if (requestMethod.equals("GET")) {
            if (pathParts.length == 2 && pathParts[1].equals("movies")) {
                return Endpoint.GET_MOVIES;
            }

            if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                return Endpoint.GET_MOVIE_BY_ID;
            }
        }

        if (requestMethod.equals("POST")) {
            if (pathParts.length == 2 && pathParts[1].equals("movies")) {
                return Endpoint.POST_MOVIE;
            }
        }

        if (requestMethod.equals("DELETE")) {
            if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                return Endpoint.DELETE_MOVIE;
            }
        }

        return Endpoint.UNKNOWN;
    }

    private void handleGetMovies(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();

        Map<String, String> params = parseQuery(query);
        String yearStr = params.get("year");

        String response;
        int responseCode;

        if (yearStr != null) {
            try {
                int year = Integer.parseInt(yearStr);
                List<Movie> movies = moviesStore.getMoviesByYear(year);
                response = gson.toJson(movies);
                responseCode = 200;
            } catch (NumberFormatException exception) {
                response = gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'", null));
                responseCode = 400;
            }  catch (BadRequestException exception) {
                response = gson.toJson(new ErrorResponse(exception.getMessage(), null));
                responseCode = 400;
            }
        } else {
            List<Movie> movies = moviesStore.getAllMovies();
            response = gson.toJson(movies);
            responseCode = 200;
        }

        writeResponse(exchange, response, responseCode);
    }

    private void handleGetMoviesById(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String movieId = requestPath.split("/")[2];

        String response;
        int responseCode;

        try {
            int id = Integer.parseInt(movieId);
            Movie movie = moviesStore.getMovieById(id);
            response = gson.toJson(movie);
            responseCode = 200;
        } catch (NumberFormatException exception) {
            response = gson.toJson(new ErrorResponse("Некорректный ID", null));
            responseCode = 400;
        } catch (BadRequestException exception) {
            response = gson.toJson(new ErrorResponse(exception.getMessage(), null));
            responseCode = 400;
        } catch (NotFoundException exception) {
            response = gson.toJson(new ErrorResponse(exception.getMessage(), null));
            responseCode = 404;
        }

        writeResponse(exchange, response, responseCode);
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String response;
        int responseCode;

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            writeResponse(exchange,
                    gson.toJson(new ErrorResponse("Неподдерживаемый Content-Type", null)),
                    415);
            return;
        }

        try {
            Movie movie = parseMovie(exchange.getRequestBody());
            Movie createdMovie = moviesStore.addMovie(movie);

            response = gson.toJson(createdMovie);
            responseCode = 201;

        } catch (ValidationException exception) {
            response = gson.toJson(new ErrorResponse(exception.getMessage(), exception.getDetails()));
            responseCode = 422;
        } catch (Exception exception) {
            response = gson.toJson(new ErrorResponse("Некорректный JSON", null));
            responseCode = 400;
        }

        writeResponse(exchange, response, responseCode);
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String movieId = requestPath.split("/")[2];

        String response;
        int responseCode;

        try {
            int id = Integer.parseInt(movieId);
            moviesStore.deleteMovieById(id);
            writeEmptyResponse(exchange, 204);
            return;
        } catch (NumberFormatException exception) {
            response = gson.toJson(new ErrorResponse("Некорректный ID", null));
            responseCode = 400;
        } catch (BadRequestException exception) {
            response = gson.toJson(new ErrorResponse(exception.getMessage(), null));
            responseCode = 400;
        } catch (NotFoundException exception) {
            response = gson.toJson(new ErrorResponse(exception.getMessage(), null));
            responseCode = 404;
        }

        writeResponse(exchange, response, responseCode);
    }

    private Movie parseMovie(InputStream bodyInputStream) throws IOException {
        String body = new String(bodyInputStream.readAllBytes(), DEFAULT_CHARSET);
        JsonObject movieObject = JsonParser.parseString(body).getAsJsonObject();

        String title = movieObject.get("title").getAsString();
        int year = movieObject.get("year").getAsInt();

        return new Movie(0, title, year);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }

        return params;
    }

    private void handleMethodNotAllowed(HttpExchange exchange) throws IOException {
        String response = gson.toJson(new ErrorResponse("Метод не поддерживается", null));
        writeResponse(exchange, response, 405);
    }
}
