package com.cashticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(length = 30, nullable = false)
    private String artist;

    @Column(length = 1000)
    private String posterImgURL;

    @Column(length = 100, nullable = false)
    private String place;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Boolean isAuction = false;
} 