package org.example.service.gemini;

import org.example.model.Word;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiResponseParserTest {
    GeminiResponseParser geminiResponseParser = new GeminiResponseParser();

    @Test
    public void parseFiveWordsWithExplanationAndTranslation() {
        String geminiResponse = "1. **Tourist** - a person who travels for pleasure or recreation - \\320\\242\\321\\203\\321\\200\\320\\270\\321\\201\\321\\202\\n2. **Destination** - a place to which one is going or traveling - \\320\\234\\321\\226\\321\\201\\321\\206\\320\\265 \\320\\277\\321\\200\\320\\270\\320\\267\\320\\275\\320\\260\\321\\207\\320\\265\\320\\275\\320\\275\\321\\217\\n3. **Attraction** - something that draws people to a place - \\320\\242\\321\\203\\321\\200\\320\\270\\321\\201\\321\\202\\320\\270\\321\\207\\320\\275\\320\\260 \\320\\277\\320\\260\\320\\274\\'\\321\\217\\321\\202\\320\\272\\320\\260\\n4. **Accommodation** - a place to stay overnight - \\320\\240\\320\\276\\320\\267\\320\\274\\321\\226\\321\\211\\320\\265\\320\\275\\320\\275\\321\\217\\n5. **Transport** - a means of traveling from one place to another - \\320\\242\\321\\200\\320\\260\\320\\275\\321\\201\\320\\277\\320\\276\\321\\200\\321\\202\"\n";
        List<Word> actualWords = geminiResponseParser.parse(geminiResponse);
        List<Word> expectedWords = List.of(
                new Word("Tourist", "a person who travels for pleasure or recreation", "Турист"),
                new Word("Destination", "a place to which one is going or traveling", "Місце призначення"),
                new Word("Attraction", "something that draws people to a place", "Туристична пам'ятка"),
                new Word("Accommodation", "a place to stay overnight", "Розміщення"),
                new Word("Transport", "a means of traveling from one place to another", "Транспорт")
        );
        assertEquals(expectedWords,actualWords);
    }
}