package com.javarush.telegram;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Log4j
public class WineBotApp extends MultiSessionTelegramBot {


    @Autowired
    private ChatGPTService chatGPT;
    private DialogMode currentMode = null;
    private ArrayList<String> list = new ArrayList<>();
    private Map<String, List<com.plexpt.chatgpt.entity.chat.Message>> messageHistoryByChatId = new HashMap();

    @Autowired
    public WineBotApp(@Value("${bot.name}") String botName, @Value("${bot.token}") String botToken) {
        super(botName, botToken);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
//        var originalMessage = update.getMessage();
//        log.debug(originalMessage).getText();

        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        //Command START
        if (message.equals("/start")) {
            currentMode = DialogMode.MAIN;
            String text = loadMessage("main");
            sendTextMessage(text);
            sendPhotoMessage("main");

            showMainMenu("Главное меню бота \uD83D\uDCCB", "/start",
                    "Определенная позиция \uD83E\uDD14", "/fact",
                    "Вино по Вашим предпочтениям \uD83C\uDF77 ", "/preference",
                    "Вино для определенных мероприятий. \uD83C\uDF82", "/event",
                    "Вино по сорту винограда \uD83C\uDF47", "/variety",
                    "Вопрос Виртуальному сомелье\uD83E\uDDE0", "/sommelier");

            return;
        }
        //Command FACT
        if (message.equals("/fact")) {
            currentMode = DialogMode.FACT;
            String fact = loadMessage("fact");
            sendPhotoMessage("fact");
            sendTextMessage(fact);
            return;
        }
        if (currentMode == DialogMode.FACT && !isMessageCommand()) {

            String fact = message;
            String promt = loadPrompt("fact");
            String answer = chatGPT.sendMessage(promt, fact, messageHistoryByChatId);
            sendTextMessage(answer);
            return;
        }
        //Command PREFERENCE
        if (message.equals("/preference")) {
            currentMode = DialogMode.PREFERENCE;
            sendPhotoMessage("preference");
            String text = loadMessage("preference");
            sendTextButtonsMessage(text,
                    "Выбрать исходя из предпочтений", "message_preference",
                    "Подобрать случайное, но достойное вино", "message_random");
            return;
        }
        if (currentMode == DialogMode.PREFERENCE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String promt = loadPrompt(query);
                String userChatHistory = String.join(" ", list);
                Message msg = sendTextMessage("Секундочку, нужно подумать...");
                String answer = chatGPT.sendMessage(promt, userChatHistory, messageHistoryByChatId);
                updateTextMessage(msg, answer);
            }
            list.add(message);
            return;
        }
        //Command EVENT
        if (message.equals("/event")) {
            currentMode = DialogMode.EVENT;
            sendPhotoMessage("event");

            sendTextButtonsMessage("Выберете мероприятие под которое нужно подобрать напитки",
                    "Свадьба", "date_wedding",
                    "Корпоративная вечеринка", "date_corporate",
                    "Рождение ребенка ", "date_birth_of_a_child",
                    "Юбилей", "date_anniversary",
                    "День рождения", "date_birthday",
                    "Другое мероприятие", "date_other");
            return;
        }
        if (currentMode == DialogMode.EVENT && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                String text = loadMessage("message_of_event");
                sendTextMessage(text);

                String promt = loadPrompt(query);
                chatGPT.setPrompt(promt);
                return;
            }
            Message msg = sendTextMessage("Секундочку, нужно подумать...");
            String answer = chatGPT.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }
        //Command VARIETY
        if (message.equals("/variety")) {
            currentMode = DialogMode.VARIETY;
            sendPhotoMessage("variety");
            String variety = loadMessage("variety");
            sendTextMessage(variety);
            return;
        }
        if (currentMode == DialogMode.VARIETY && !isMessageCommand()) {
            String sort = message;
            String promt = loadPrompt("variety");
            String answer = chatGPT.sendMessage(promt, sort, messageHistoryByChatId);
            sendTextMessage(answer);
            return;
        }
        //Command SOMMELIER
        if (message.equals("/sommelier")) {
            currentMode = DialogMode.SOMMELIER;
            sendPhotoMessage("sommelier");
            String text = loadMessage("sommelier");
            sendTextMessage(text);
            return;
        }
        if (currentMode == DialogMode.SOMMELIER && !isMessageCommand()) {
            String promt = loadPrompt("sommelier");
            Message msg = sendTextMessage("Секундочку, нужно подумать...");
            String answer = chatGPT.sendMessage(promt, message, messageHistoryByChatId);
            updateTextMessage(msg, answer);
            return;
        }

    }
}
