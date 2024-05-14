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
    private String translation;


    public Word(String word, String explanation, String translation) {
        this.word = word;
        this.explanation = explanation;
        this.translation = translation;
    }

    public Word() {

    }

    @Override
    public String toString() {
        return "Word{" +
                "word='" + word + '\'' +
                ", explanation='" + explanation + '\'' +
                ", translation='" + translation + '\'' +
                '}';
    }
}
