package br.com.smartlog.application.port;
public interface LanguageModel {
    String answer(String instructions, String context, String question);
}
