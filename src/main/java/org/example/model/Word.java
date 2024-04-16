package org.example.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity(name = "words")
@Data
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String engName;
    private String uaName;
}
