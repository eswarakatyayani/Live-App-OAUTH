package com.katy.demo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.katy.demo.model.Movie;

/**
 * Read-only access is all this demo needs, but MongoRepository gives us paging
 * helpers (findAll(Pageable)) used by the controller.
 */
public interface MovieRepository extends MongoRepository<Movie, String> {
}
