package com.cashticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

<<<<<<< HEAD
    @Column(length = 100, nullable = false, unique = true)
=======
    @Column(length = 50)
>>>>>>> b80605ab2163a406be438e20585c45723658b4e7
    private String userId;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 100, nullable = false)
    private String password;

    @Column(length = 100, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private LocalDate birthDay;

    @Column(length = 15)
    private String phoneNumber;
}