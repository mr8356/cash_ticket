package com.cashticket.strategy;

import com.cashticket.entity.Concert;
import java.util.List;

public interface ConcertFilterStrategy {
    List<Concert> filter(List<Concert> concerts, String value);
} 