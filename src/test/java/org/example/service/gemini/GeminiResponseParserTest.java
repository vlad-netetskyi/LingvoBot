package org.example.service.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.model.Grammar;
import org.example.model.Word;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiResponseParserTest {
    GeminiResponseParser geminiResponseParser = new GeminiResponseParser();

    @Test
    public void parseFiveWordsWithExplanationAndTranslation() throws Exception {
        String geminiResponse = """
                [
                {
                "word": "sightseeing",
                "explanation": "The activity of visiting places of interest",
                "translation": "Огляд визначних пам'яток (Ohliad vyznachnykh pam'yatok)"
                },
                {
                "word": "adventure travel",
                "explanation": "Travel that involves exciting or risky activities",
                "explanation_ua": "Подорожі пригодами (Podorozhi pryhodami)",
                "translation": "Екстремальний туризм (Ekstremalnyi turizm)"
                },
                {
                "word": "cultural heritage",
                "explanation": "The customs, arts, and social institutions of a particular nation or people",
                "translation": "Культурна спадщина (Kulturnа spadshchyna)"
                },
                {
                "word": "souvenir",
                "explanation": "An object that is kept as a reminder of a place or event",
                "translation": "Сувенір (Suvenyr)"
                },
                {
                "word": "ecotourism",
                "explanation": "Tourism that is environmentally responsible",
                "translation": "Екотуризм (Ekoturizm)"
                }
                ]""";

        List<Word> actualWords = geminiResponseParser.parse(geminiResponse);
        List<Word> expectedWords = List.of(
                new Word("sightseeing", "The activity of visiting places of interest", "Огляд визначних пам'яток (Ohliad vyznachnykh pam'yatok)"),
                new Word("adventure travel", "Travel that involves exciting or risky activities", "Екстремальний туризм (Ekstremalnyi turizm)"),
                new Word("itinerary", "a plan of your journey, showing the places you will visit and the dates you will be there", "Маршрут"),
                new Word("passport", "an official document that allows you to travel to other countries", "Паспорт"),
                new Word("visa", "an official document that allows you to enter and stay in a country for a specific period of time", "Віза")
        );
        assertEquals(expectedWords, actualWords);
    }
    @Test
    public void parseGrammarWithExplanationAndTranslation() throws Exception {
        String geminiResponse = """
                  {
                    "sentence": "i would like to order a pizza with pepperoni and extra cheese",
                    "correctSentence": "i would like to order a pizza with pepperoni and extra cheese",
                    "explanation": "The sentence is incorrect because it does not start with a capital letter. The corrected sentence starts with a capital letter and has a period at the end."
                  }
                """;

        Grammar actualGrammar = geminiResponseParser.parseGrammar(geminiResponse);
        Grammar expectedGrammar =   new Grammar("i would like to order a pizza with pepperoni and extra cheese","i would like to order a pizza with pepperoni and extra cheese",
                "The sentence is incorrect because it does not start with a capital letter. The corrected sentence starts with a capital letter and has a period at the end." );
        assertEquals(expectedGrammar, actualGrammar);
    }
}