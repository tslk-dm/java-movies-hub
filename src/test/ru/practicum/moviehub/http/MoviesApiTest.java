package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static MoviesServer server;
    private static  MoviesStore store;
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @BeforeAll
    static void beforeAll() {

    }

    @BeforeEach
    void beforeEach() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
    }

    @AfterAll
    static void afterAll() {

    }

    @AfterEach
    void afterEach() {
        server.stop();
    }

    private HttpResponse<String> sendGetMovie() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    private HttpResponse<String> sendGetMovieById(String id) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + id))
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    private HttpResponse<String> sendPostMovies(String body, String contentType) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(body, DEFAULT_CHARSET));

        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }

        HttpRequest request = builder.build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    private HttpResponse<String> sendDeleteMovieById(String id) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies/" + id))
                .DELETE()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    private HttpResponse<String> sendGetMoviesByYear(String year) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies?year=" + year))
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = sendGetMovie();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenStoreHasMovies_returnsMoviesList() throws Exception {
        Movie movie1 = store.addMovie(new Movie(0, "Movie1", 2010));
        Movie movie2 = store.addMovie(new Movie(0, "Movie2", 2014));

        HttpResponse<String> resp = sendGetMovie();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(resp.body(),  new ListOfMoviesTypeToken().getType());

        assertEquals(2, movies.size(), "Должны вернуться все добавленные фильмы");

        assertEquals(movie1.getId(), movies.get(0).getId(), "Должен совпадать id первого фильма");
        assertEquals(movie1.getTitle(), movies.get(0).getTitle(), "Должен совпадать title первого фильма");
        assertEquals(movie1.getYear(), movies.get(0).getYear(), "Должен совпадать year первого фильма");

        assertEquals(movie2.getId(), movies.get(1).getId(), "Должен совпадать id второго фильма");
        assertEquals(movie2.getTitle(), movies.get(1).getTitle(), "Должен совпадать title второго фильма");
        assertEquals(movie2.getYear(), movies.get(1).getYear(), "Должен совпадать year второго фильма");
    }


    // POST: Добавляет фильм при корректных данных
    @Test
    void postMovie_whenValidData_addsMovie() throws Exception {
        String requestBody = "{\"title\":\"Movie1\",\"year\":2010}";

        HttpResponse<String> resp = sendPostMovies(requestBody,  "application/json");

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        Movie createdMovie = gson.fromJson(resp.body(), Movie.class);

        assertTrue(createdMovie.getId() == 1, "У созданного фильма должен быть id=1");
        assertEquals("Movie1", createdMovie.getTitle(), "Назавние фильма должно быть Movie1");
        assertEquals(2010, createdMovie.getYear(), "Год фильма должен быть 2010");

        assertEquals(1, store.getAllMovies().size(), "Фильм должен сохраниться в store");
    }

    //POST: Возвращает ошибку при пустом title
    @Test
    void postMovie_whenTitleIsEmpty_returnsValidationError() throws Exception {
        String requestBody = "{\"title\":\"\",\"year\":2010}";

        HttpResponse<String> resp = sendPostMovies(requestBody, "application/json");

        assertEquals(422, resp.statusCode(), "При пустом title статус должен быть 422");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
        assertEquals(0, store.getAllMovies().size(), "Фильм не должен добавиться");
    }

    //POST: Возвращает ошибку при длинном title
    @Test
    void postMovie_whenTitleTooLong_returnsValidationError() throws Exception {
        String longTitle = "a".repeat(101);

        String requestBody = "{\"title\":\"%s\",\"year\":2010}".formatted(longTitle);

        HttpResponse<String> resp = sendPostMovies(requestBody, "application/json");

        assertEquals(422, resp.statusCode(), "При title длиннее 100 статус должен быть  422");

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
        assertEquals(0, store.getAllMovies().size());
    }

    //POST: Возвращает ошибку при неверном year: Год меньше 1888
    @Test
    void postMovie_whenYearLessThan1888_returnsValidationError() throws Exception {
        String requestBody = "{\"title\":\"Movie1\",\"year\":1800}";

        HttpResponse<String> resp = sendPostMovies(requestBody, "application/json");

        assertEquals(422, resp.statusCode(), "При year < 1888 статус должен быть 422");

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
        assertEquals(0, store.getAllMovies().size());
    }

    //POST: Возвращает ошибку при неверном year: Год больше текущего + 1
    @Test
    void postMovie_whenYearTooLarge_returnsValidationError() throws Exception {
        int invalidYear = java.time.Year.now().getValue() + 2;

        String requestBody = "{\"title\":\"Future Movie\",\"year\":%d}".formatted(invalidYear);

        HttpResponse<String> resp = sendPostMovies(requestBody, "application/json");

        assertEquals(422, resp.statusCode(), "При year > current year + 1 статус должен быть 422");

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
        assertEquals(0, store.getAllMovies().size());
    }

    //POST: Возвращает ошибку при неправильном Content-Type
    @Test
    void postMovie_whenContentTypeIsInvalid_returnsError() throws Exception {
        String requestBody = "{\"title\":\"Movie1\",\"year\":2010}";

        HttpResponse<String> resp = sendPostMovies(requestBody, "text/plain");

        assertEquals(415, resp.statusCode(), "При неверном Content-Type статус должен быть 415");

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
        assertEquals(0, store.getAllMovies().size());
    }

    //POST: Возвращает ошибку при некорректном JSON
    @Test
    void postMovie_whenJsonIsInvalid_returnsBadRequest() throws Exception {
        String requestBody = "{\"title\":\"Movie1\",\"year\":2010";

        HttpResponse<String> resp = sendPostMovies(requestBody, "application/json");

        assertEquals(400, resp.statusCode(), "При некорректном JSON статус должен быть 400");

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
        assertEquals(0, store.getAllMovies().size());
    }

    //GET /movies/{id}: Возвращает фильм по существующему id
    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        Movie createdMovie = store.addMovie(new Movie(0, "Movie1", 2010));

        HttpResponse<String> resp = sendGetMovieById(Integer.toString(createdMovie.getId()));

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть статус 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        Movie movieFromResponse = gson.fromJson(resp.body(), Movie.class);

        assertEquals(createdMovie.getId(), movieFromResponse.getId());
        assertEquals(createdMovie.getTitle(), movieFromResponse.getTitle());
        assertEquals(createdMovie.getYear(), movieFromResponse.getYear());
    }

    //GET /movies/{id}: Возвращает ошибку, если фильм не найден
    @Test
    void getMovieById_whenMovieNotFound_returnsNotFound() throws Exception {
        HttpResponse<String> resp = sendGetMovieById(Integer.toString(9999));

        assertEquals(404, resp.statusCode(), "Если фильм не найден, статус должен быть 404");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
    }

    //GET /movies/{id}: Возвращает ошибку, если id не число
    @Test
    void getMovieById_whenIdIsNotNumber_returnsBadRequest() throws Exception {
        HttpResponse<String> resp = sendGetMovieById("abc");

        assertEquals(400, resp.statusCode(), "Если id не число, должен вернуться статус 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
    }

    //DELETE: Удаляет фильм по существующему id
    @Test
    void deleteMovie_whenMovieExists_removesMovie() throws Exception {
        Movie movie = store.addMovie(new Movie(0, "Movie1", 2010));

        HttpResponse<String> resp = sendDeleteMovieById(String.valueOf(movie.getId()));

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть статус 204");

        // Проверяем, что фильм реально удалился
        assertEquals(0, store.getAllMovies().size(), "Фильм должен быть удалён из store");
    }

    //DELETE: Возвращает ошибку, если фильм не найден
    @Test
    void deleteMovie_whenMovieNotFound_returnsNotFound() throws Exception {
        HttpResponse<String> resp = sendDeleteMovieById("9999");

        assertEquals(404, resp.statusCode(), "Если фильм не найден, должен вернуться статус 404");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
    }

    //DELETE: Возвращает ошибку, если id не число
    @Test
    void deleteMovie_whenIdIsNotNumber_returnsBadRequest() throws Exception {
        HttpResponse<String> resp =  sendDeleteMovieById("abc");

        assertEquals(400, resp.statusCode(), "Если id не число, должен вернуться статус 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
    }

    //GET /movies?year=YYYY: Возвращает фильмы указанного года
    @Test
    void getMoviesByYear_whenMoviesExist_returnsMoviesOfSpecifiedYear() throws Exception {
        Movie movie1 = store.addMovie(new Movie(0, "Movie1", 2010));
        Movie movie2 = store.addMovie(new Movie(0, "Movie2", 2010));
        store.addMovie(new Movie(0,"Movie3", 2014));

        HttpResponse<String> resp = sendGetMoviesByYear("2010");

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть статус 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertEquals(2, movies.size(), "Должны вернуться только фильмы указанного года");

        assertTrue(movies.stream().anyMatch(m ->
                m.getId() == movie1.getId()
                        && m.getTitle().equals(movie1.getTitle())
                        && m.getYear() == movie1.getYear()));

        assertTrue(movies.stream().anyMatch(m ->
                m.getId() == movie2.getId()
                        && m.getTitle().equals(movie2.getTitle())
                        && m.getYear() == movie2.getYear()));
    }

    //GET /movies?year=YYYY: Возвращает пустой список, если фильмов с таким годом нет
    @Test
    void getMoviesByYear_whenNoMoviesForYear_returnsEmptyList() throws Exception {
        store.addMovie(new Movie(0, "Movie1", 2010));
        store.addMovie(new Movie(0, "Movie2", 2014));

        HttpResponse<String> resp = sendGetMoviesByYear("2020");

        assertEquals(200, resp.statusCode(), "Если фильмов нет, должен вернуться статус 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(
                resp.body(),
                new ListOfMoviesTypeToken().getType()
        );

        assertTrue(movies.isEmpty(), "Должен вернуться пустой список");
    }

    //GET /movies?year=YYYY: Возвращает ошибку, если параметр year не число

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returnsBadRequest() throws Exception {
        HttpResponse<String> resp = sendGetMoviesByYear("abc");

        assertEquals(400, resp.statusCode(), "Если year не число, должен вернуться статус 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
    }

    // При неподдерживаемом HTTP-методе возвращается 405 Method Not Allowed.
    @Test
    void whenMethodNotAllowed_returns405() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));

        assertEquals(405, resp.statusCode(), "Неподдерживаемый метод должен вернуть статус 405");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        Gson gson = new Gson();
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertTrue(error.getError() != null && !error.getError().isBlank());
    }
}