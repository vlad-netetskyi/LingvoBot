package org.example.service.gemini;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

import javax.annotation.PreDestroy;
import java.io.IOException;

public class Gemini {
    public static final String COMPACT_NIRVANA_420519 = "compact-nirvana-420519";
    private String projectId = COMPACT_NIRVANA_420519;
    private String location = "us-central1";
    private String modelName = "gemini-1.0-pro-vision";
    private VertexAI vertexAI;

    public Gemini() {
        this.vertexAI = new VertexAI(projectId, location);
    }

    @PreDestroy
    private void close() {
        this.vertexAI.close();
    }

    public String prompt(String text) throws IOException {
        GenerativeModel model = new GenerativeModel(modelName, vertexAI);
        // GenerateContentResponse response = model.generateContent(text);
        String fullResponse = model.generateContent(text).getCandidates(0).getContent().getParts(0).getText();
        String response = fullResponse.substring(8, fullResponse.length() - 3);
        //response.getCandidates(0).getContent().getParts(1).get
        return fullResponse;
    }
}
