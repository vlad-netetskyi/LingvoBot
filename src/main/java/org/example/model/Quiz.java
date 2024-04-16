package org.example.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity(name = "quizzes")
@Data
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String quiz;
}
