package org.example.service.gemini;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import org.example.model.Grammar;
import org.example.model.Word;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class GeminiResponseParser {
    public List<Word> parse(String geminiText) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return objectMapper.readValue(geminiText, new TypeReference<List<Word>>() {});
    }

    public Grammar parseGrammar(String geminiText) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        //objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        System.out.println(geminiText);
        System.out.println(objectMapper.readValue(geminiText, Grammar.class));
        return objectMapper.readValue(geminiText,Grammar.class);
    }
}





