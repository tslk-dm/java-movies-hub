package ru.practicum.moviehub.store;

import ru.practicum.moviehub.exception.BadRequestException;
import ru.practicum.moviehub.exception.NotFoundException;
import ru.practicum.moviehub.exception.ValidationException;
import ru.practicum.moviehub.model.Movie;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    // GET /movies
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    // GET /movies?year=YYYY
    public List<Movie> getMoviesByYear(int year) {
        validateYearQuery(year);

        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                result.add(movie);
            }
        }
        return result;
    }

    // GET /movies/{id}
    public Movie getMovieById(int id) {
        validateId(id);

        Movie movie = movies.get(id);
        if (movie == null) {
            throw new NotFoundException("Фильм с id=%d не найден".formatted(id));
        }

        return movie;
    }

    // POST /movies
    public Movie addMovie(Movie movie) {
        validateMovie(movie);

        Movie storedMovie = new Movie(nextId++, movie.getTitle().trim(), movie.getYear());
        movies.put(storedMovie.getId(), storedMovie);

        return storedMovie;
    }

    // DELETE /movies/{id}
    public void deleteMovieById(int id) {
        validateId(id);

        Movie removed = movies.remove(id);
        if (removed == null) {
            throw new NotFoundException("Фильм с id=%d не найден".formatted(id));
        }
    }

    private void validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie == null) {
            errors.add("тело запроса отсутствует");
            throw new ValidationException("Ошибка валидации", errors);
        }

        String title = movie.getTitle();
        if (title == null || title.isBlank()) {
            errors.add("название не должно быть пустым");
        } else if (title.trim().length() > 100) {
            errors.add("название должно быть не длиннее 100 символов");
        }

        int currentYear = Year.now().getValue();
        int minYear = 1888;
        int maxYear = currentYear + 1;

        if (movie.getYear() < minYear || movie.getYear() > maxYear) {
            errors.add("год должен быть между " + minYear + " и " + maxYear);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Ошибка валидации", errors);
        }
    }

    private void validateId(int id) {
        if (id <= 0) {
            throw new BadRequestException("Некорректный ID");
        }
    }

    private void validateYearQuery(int year) {
        int minYear = 1888;
        int maxYear = Year.now().getValue() + 1;

        if (year < minYear || year > maxYear) {
            throw new BadRequestException("Некорректный параметр запроса — 'year'");
        }
    }
}