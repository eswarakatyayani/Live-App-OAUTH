package com.katy.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A lightweight view over documents in the Atlas {@code sample_mflix.movies} collection.
 *
 * <p>Only a handful of fields are mapped; Spring Data simply ignores the rest of the
 * (large) source document on read. {@code year} is declared as {@link Object} because
 * the sample data stores it as either an int or a string depending on the record.
 */
@Document(collection = "movies")
public class Movie {

    @Id
    private String id;
    private String title;
    private Object year;
    private String plot;
    private List<String> genres;
    private Integer runtime;
    private String rated;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Object getYear() {
        return year;
    }

    public void setYear(Object year) {
        this.year = year;
    }

    public String getPlot() {
        return plot;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }

    public String getRated() {
        return rated;
    }

    public void setRated(String rated) {
        this.rated = rated;
    }
}
