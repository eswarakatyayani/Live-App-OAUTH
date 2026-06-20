package com.katy.demo.controller;

import com.katy.demo.model.Movie;
import com.katy.demo.repository.MovieRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Protected, read-only API. Requires a valid Bearer JWT.
 */
@RestController
@RequestMapping("/api")
public class MovieController {

    private final MovieRepository movieRepository;

    public MovieController(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Returns a page of sample movies from MongoDB Atlas.
     *
     * @param limit number of documents to return (1-100, default 10)
     */
    @GetMapping("/movies")
    public List<Movie> getMovies(@RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return movieRepository.findAll(PageRequest.of(0, safeLimit)).getContent();
    }
}
