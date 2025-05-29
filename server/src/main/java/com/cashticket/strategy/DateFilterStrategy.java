package com.cashticket.strategy;

import com.cashticket.entity.Concert;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DateFilterStrategy implements ConcertFilterStrategy {
    @Override
    public List<Concert> filter(List<Concert> concerts, String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return concerts;
        }
        LocalDate filterDate = LocalDate.parse(dateStr);
        return concerts.stream()
                .filter(concert -> concert.getDate().equals(filterDate))
                .collect(Collectors.toList());
    }
} 