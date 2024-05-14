package org.example;

import org.example.service.gemini.Gemini;

import java.io.IOException;

public class Vertex {
    public static void main(String[] args) throws IOException {
        Gemini gemini = new Gemini();
        System.out.println(gemini.prompt("write 5 english words with explanations and translation into ukrainian from topic tourism in JSON format. " +
                "Return only JSON array in using next template [ { \"word\": \"some word\", \"explanation\": \"some explanation\", \"translation\": \"some translation\" }]. Start with \"[\" end with \"]\"."));
    }
}
