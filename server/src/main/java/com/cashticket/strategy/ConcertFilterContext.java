package com.cashticket.strategy;

import com.cashticket.entity.Concert;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class ConcertFilterContext {
    private final Map<String, ConcertFilterStrategy> strategies;

    public ConcertFilterContext(List<ConcertFilterStrategy> strategyList) {
        this.strategies = Map.of(
            "artist", strategyList.stream()
                .filter(s -> s instanceof ArtistFilterStrategy)
                .findFirst()
                .orElseThrow(),
            "date", strategyList.stream()
                .filter(s -> s instanceof DateFilterStrategy)
                .findFirst()
                .orElseThrow(),
            "category", strategyList.stream()
                .filter(s -> s instanceof GenreFilterStrategy)
                .findFirst()
                .orElseThrow()
        );
    }

    public List<Concert> applyFilter(String filterType, List<Concert> concerts, String value) {
        ConcertFilterStrategy strategy = strategies.get(filterType);
        if (strategy == null) {
            return concerts;
        }
        return strategy.filter(concerts, value);
    }
} 