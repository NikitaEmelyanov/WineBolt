package com.javarush.telegram;

import com.plexpt.chatgpt.ChatGPT;
import com.plexpt.chatgpt.entity.chat.ChatCompletion;
import com.plexpt.chatgpt.entity.chat.ChatCompletionResponse;
import com.plexpt.chatgpt.entity.chat.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;

@Component
public class ChatGPTService {

    private String tokenGpt;
    private ChatGPT chatGPT;

    private Map<String, List<Message>> messageHistoryByChatId = new HashMap();
    private List<Message> messageHistory = new ArrayList<>(); //История переписки с ChatGPT - нужна для диалогов

    @Autowired
    public ChatGPTService(@Value("${gpt.token}") String token) {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("18.199.183.77", 49232));
        if (token.startsWith("gpt:")) {
            token = "sk-proj-" + new StringBuilder(token.substring(4)).reverse();
        }

        this.chatGPT = ChatGPT.builder()
                .apiKey(token)
                .apiHost("https://api.openai.com/")
                .proxy(proxy)
                .build()
                .init();
    }

    /**
     * Одиночный запрос к ChatGPT по формату "запрос"-> "ответ".
     * Запрос состоит из двух частей:
     * prompt - контектс вопроса
     * question - сам запрос
     */
    public String sendMessage(String prompt, String question, Map<String, List<Message>> messageHistoryByChatId) {
        Message system = Message.ofSystem(prompt);
        Message message = Message.of(question);
//        List<Message> messages = messageHistoryByChatId.computeIfAbsent(chatId, (Function<String, List<Message>>) s -> new ArrayList<>(Arrays.asList(system, message)));

        messageHistory = new ArrayList<>(Arrays.asList(system, message));

        System.out.println(messageHistory);
        return sendMessagesToChatGPT();
    }

    /**
     * Запросы к ChatGPT с сохранением истории сообщений.
     * Метод setPrompt() задает контект запроса
     */
    public void setPrompt(String prompt) {
        Message system = Message.ofSystem(prompt);
        messageHistory = new ArrayList<>(List.of(system));
    }

    /**
     * Запросы к ChatGPT с сохранением истории сообщений.
     * Метод addMessage() добавляет новый вопрос (сообщение) в чат.
     */
    public String addMessage(String question) {
        Message message = Message.of(question);
        messageHistory.add(message);

        return sendMessagesToChatGPT();
    }

    /**
     * Отправляем ChatGPT серию сообщений: prompt, message1, answer1, message2, answer2, ..., messageN
     * Ответ ChatGPT добавляется в конец messageHistory для последующейго использования
     */
    private String sendMessagesToChatGPT() {
        ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(ChatCompletion.Model.GPT_3_5_TURBO.getName()) // GPT4Turbo or GPT_3_5_TURBO
                .messages(messageHistory)
                .maxTokens(3000)
                .temperature(0.2)
                .build();

        ChatCompletionResponse response = chatGPT.chatCompletion(chatCompletion);
        Message res = response.getChoices().get(0).getMessage();
        messageHistory.add(res);

        return res.getContent();
    }
}
