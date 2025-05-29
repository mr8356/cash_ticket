package com.cashticket.strategy;

import com.cashticket.entity.Concert;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ArtistFilterStrategy implements ConcertFilterStrategy {
    @Override
    public List<Concert> filter(List<Concert> concerts, String artist) {
        if (artist == null || artist.trim().isEmpty()) {
            return concerts;
        }
        return concerts.stream()
                .filter(concert -> concert.getArtist().toLowerCase().contains(artist.toLowerCase()))
                .collect(Collectors.toList());
    }
} 