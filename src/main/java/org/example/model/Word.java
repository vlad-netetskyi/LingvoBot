package org.example.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity(name = "words")
@Data
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String word;
    private String explanation;
    private String ukrainianWord;


    public Word(String word, String explanation, String ukrainianWord) {
        this.word = word;
        this.explanation = explanation;
        this.ukrainianWord = ukrainianWord;
    }

    public Word() {

    }
}
