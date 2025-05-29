package com.cashticket.strategy;

import com.cashticket.entity.Concert;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GenreFilterStrategy implements ConcertFilterStrategy {
    @Override
    public List<Concert> filter(List<Concert> concerts, String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            return concerts;
        }
        return concerts.stream()
                .filter(concert -> concert.getCategory().equals(genre))
                .collect(Collectors.toList());
    }
} 