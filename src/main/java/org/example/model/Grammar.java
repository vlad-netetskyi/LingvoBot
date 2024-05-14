package org.example.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "grammars")
@Data
public class Grammar {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String sentence;
    private String correctSentence;
    private String explanation;


    public Grammar(String sentence, String correctSentence, String explanation) {
        this.sentence = sentence;
        this.correctSentence = correctSentence;
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "Grammar{" +
                "sentence='" + sentence + '\'' +
                ", correctSentence='" + correctSentence + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }

    public Grammar() {
    }
}
