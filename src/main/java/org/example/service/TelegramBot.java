package org.example.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.example.config.BotConfig;
import org.example.model.*;

import java.io.IOException;
import java.sql.Timestamp;

import org.example.service.gemini.Gemini;
import org.example.service.gemini.GeminiResponseParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WordRepository wordRepository;
    private final Gemini gemini = new Gemini();
    private final GeminiResponseParser parser = new GeminiResponseParser();
    final BotConfig config;
    static final String HELP_TEXT = """
            My goal is to help you learn English.
                                                            
            We can talk, learn NEW WORDS or practice GRAMMAR. I also have daily LESSONS for you. Just write me one of those words to start😉
                                                            
            If you have any questions, feedback or something is not working - contact with me themrwaik@gmail.com
                        
            You can execute commands from the main menu on the left or by typing a command:""";
    static final String ABOUT_TEXT = """
            Feedback
                
            If the bot was useful for you, let me know.
            Write what you lack in the bot, what you would like to change.
                
            I would be grateful if you share the bot with your friends.
                
            📮 Contact: @netetskyi
            📢 Link for friends: @Helping_LingvoBot_bot""";
    static final String ERROR_TEXT = "Error occurred: ";
    static final String NEXT_WORD = "NEXT_WORD";
    static final long MAX_WORD_ID_MINUS_ONE = 23;


    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "restart a bot"));
        listOfCommands.add(new BotCommand("/stats", "get your statistic"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/about", "about a creator"));
        listOfCommands.add(new BotCommand("/word", "learn new word"));
        listOfCommands.add(new BotCommand("/language", "set language for leaning"));
        listOfCommands.add(new BotCommand("/talk", "just talk, like people do"));
        listOfCommands.add(new BotCommand("/grammar", "grammar analysis (/grammar i like pizza)"));
        listOfCommands.add(new BotCommand("/topic", "get 5 words on your topic (/topic cars)"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot`s command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && config.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);
                }
            } else if (messageText.startsWith("/topic")) {
                if (messageText.length() > "/topic ".length()) {
                    try {
                        String topic = messageText.substring(messageText.indexOf("/topic") + "/topic ".length()).trim();
                        topic(chatId, topic);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    sendMessage(update.getMessage().getChatId(), "Error, try to use: /topic + topic for learning(/topic tourism)");
                }
            } else if (messageText.startsWith("/grammar")) {
                if (messageText.length() > "/grammar ".length()) {
                    try {
                        String text = messageText.substring(messageText.indexOf("/grammar") + "/grammar ".length()).trim();
                        grammar(chatId, text);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    sendMessage(update.getMessage().getChatId(), "Error, try to use: /grammar + sentence for grammar analysis(/grammar i like pizza)");
                }
            } else {
                switch (messageText) {
                    case "/start" -> {
                        registerUser(update.getMessage());
                        showStart(chatId, update.getMessage().getChat().getFirstName());
                    }
                    case "/help" -> sendMessage(chatId, HELP_TEXT);
                    case "/about" -> sendMessage(chatId, ABOUT_TEXT);
                    case "/language" -> showLanguage(chatId);
                    case "/stats" -> showStats(chatId);
                    case "/talk" -> talkWithAI(chatId);
                    case "/word" -> {
                        var word = getRandomWord();
                        word.ifPresent(randomWord -> addButtonAndSendMessage(chatId, word));
                    }
                    default -> sendMessage(chatId, "Sorry, command was not recognized");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callbackData.equals(NEXT_WORD)) {
                var word = getRandomWord();

                //word.ifPresent(randomWord -> addButtonAndSendMessage(chatId, getRandomWord()));
                word.ifPresent(randomWord -> addButtonAndEditMessage(chatId, getRandomWord(), update.getCallbackQuery().getMessage().getMessageId()));
            }
        }
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String learningLanguage;

            switch (callbackData) {
                case "SET_LANGUAGE_ENGLISH" -> {
                    learningLanguage = "English";
                    updateUserLearningLanguage(chatId, learningLanguage);
                    sendMessage(chatId, "You have selected English as your learning language.");
                }
                case "SET_LANGUAGE_GERMAN" -> {
                    learningLanguage = "German";
                    updateUserLearningLanguage(chatId, learningLanguage);
                    sendMessage(chatId, "You have selected German as your learning language.");
                }
                case "SET_LANGUAGE_FRENCH" -> {
                    learningLanguage = "French";
                    updateUserLearningLanguage(chatId, learningLanguage);
                    sendMessage(chatId, "You have selected French as your learning language.");
                }
            }
        }
    }

    private void topic(long chatId, String topic) throws IOException {
        User user = userRepository.findById(chatId).orElse(new User());
        String language = user.getLearningLanguage();
        System.out.println(topic);
        String response = gemini.prompt("write 5 " + language + "words with explanations and translation into ukrainian from topic " + topic + " in JSON format. " +
                "Return only JSON array in using next template [ { \"word\": \"some word\", \"explanation\": \"some explanation\", \"translation\": \"some translation\" }]. Start with \"[\" end with \"]\".");
        System.out.println(response);
        List<Word> words = parser.parse(response);
        System.out.println(words);
        System.out.println(words.get(0));

        if (CollectionUtils.isEmpty(words)) {
            sendMessage(chatId, "Some word data is missing. Please try again later.");
            return;
        }
        for (Word word : words) {
            sendWord(chatId, word);
        }
    }

    private void grammar(long chatId, String text) throws IOException {
        User user = userRepository.findById(chatId).orElse(new User());
        String language = user.getLearningLanguage();
        System.out.println(text);
        String response = gemini.prompt("write on " + language + "language, whether the sentence: \" " + text + " is grammatically correct, write the grammatically correct sentence and an explanation" +
                " of why the sentence is correct or incorrect. write answer in JSON format. Return only JSON object in using next template " +
                "[ { \\\"sentence\\\": \\\"some sentence\\\", \\\"correctSentence\\\": \\\"some correctSentence\\\", \\\"explanation\\\": \\\"some explanation\\\" }]. object JSON should only have 3 fields! Start with \"{\" end with \"}\". Use only <> quotation marks in your answers! ");
        System.out.println(response);
        Grammar grammar = parser.parseGrammar(response);
        System.out.println(grammar);

        sendGrammar(chatId, grammar);
    }

    private Optional<Word> getRandomWord() {
        var r = new Random();
        var randomId = r.nextLong(MAX_WORD_ID_MINUS_ONE) + 1;
        return wordRepository.findById(randomId);
    }

    private void addButtonAndSendMessage(long chatId, Optional<Word> word) {
        SendMessage message = new SendMessage();
        message.setText(word.get().getWord() + " - " + word.get().getTranslation());
        message.setChatId(chatId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton inLineKeyboardButton = new InlineKeyboardButton();
        inLineKeyboardButton.setCallbackData(NEXT_WORD);
        inLineKeyboardButton.setText(EmojiParser.parseToUnicode("Next word :smile:"));
        rowInLine.add(inLineKeyboardButton);
        InlineKeyboardButton inLineKeyboardButton1 = new InlineKeyboardButton();
        inLineKeyboardButton1.setCallbackData(NEXT_WORD);
        inLineKeyboardButton1.setText(EmojiParser.parseToUnicode("Add to dictionary :book:"));
        rowInLine.add(inLineKeyboardButton1);
        rowsInline.add(rowInLine);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        User user = userRepository.findById(chatId).orElse(new User());
        if (user.getChatId() == chatId) {
            long currentStatistic = user.getStatistic();
            user.setStatistic(currentStatistic + 1);
        }

        userRepository.save(user);
        executeMessage(message);
    }

    private void addButtonAndEditMessage(long chatId, Optional<Word> word, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(word.get().getWord() + " - " + word.get().getTranslation());
        message.setMessageId(messageId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton inLineKeyboardButton = new InlineKeyboardButton();
        inLineKeyboardButton.setCallbackData(NEXT_WORD);
        inLineKeyboardButton.setText(EmojiParser.parseToUnicode("Next word :smile:"));
        InlineKeyboardButton inLineKeyboardButton1 = new InlineKeyboardButton();
        inLineKeyboardButton1.setCallbackData(NEXT_WORD);
        inLineKeyboardButton1.setText(EmojiParser.parseToUnicode("Add to dictionary :book:"));
        rowInLine.add(inLineKeyboardButton1);
        rowInLine.add(inLineKeyboardButton);
        rowsInline.add(rowInLine);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        User user = userRepository.findById(chatId).orElse(new User());
        if (user.getChatId() == chatId) {
            long currentStatistic = user.getStatistic();
            user.setStatistic(currentStatistic + 1);
        }

        userRepository.save(user);
        executeEditMessage(message);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            user.setLearningLanguage("English");
            user.setStatistic(0);

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

   /* private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }*/

    /*private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
*//*

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);
*//*

        executeMessage(message);
    }*/

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeEditMessage(EditMessageText message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        executeMessage(message);
    }

    //@Scheduled(cron = "${cron.scheduler}")
    private void sendQuizWord() {
        var words = wordRepository.findAll();
        var users = userRepository.findAll();

        for (Word word : words) {
            for (User user : users) {
                var translation = word.getWord() + " - " + word.getTranslation();
                sendMessage(user.getChatId(), translation);

            }
        }
    }

    private void sendWord(long chatId, Word wordToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(wordToSend.getWord() + " - " + wordToSend.getTranslation() + ". " + wordToSend.getExplanation());

        executeMessage(message);
    }

    private void sendGrammar(long chatId, Grammar grammarToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("❓ Sentence: " + grammarToSend.getSentence() + "\n ✅ Correct sentence: " + grammarToSend.getCorrectSentence() + "\n ❗ Explanation: " + grammarToSend.getExplanation());

        executeMessage(message);
    }

    private void showStart(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + "! :smile:, nice to meet you!" + " :blush:");
        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }

    private void showStats(long chatId) {
        String stats = EmojiParser.parseToUnicode(":book: Vocabulary: " + userRepository.findById(chatId).get().getStatistic()) + "\n" +
                "⏰ Practiced: 1 day";
        sendMessage(chatId, stats);

    }

    private void showLanguage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Select the language you want to learn:"
                + "\n1. English 🇬🇧"
                + "\n2. German 🇩🇪"
                + "\n3. French 🇫🇷");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton buttonEnglish = new InlineKeyboardButton();
        buttonEnglish.setText("English 🇬🇧");
        buttonEnglish.setCallbackData("SET_LANGUAGE_ENGLISH");
        row1.add(buttonEnglish);
        rowsInline.add(row1);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton buttonGerman = new InlineKeyboardButton();
        buttonGerman.setText("German 🇩🇪");
        buttonGerman.setCallbackData("SET_LANGUAGE_GERMAN");
        row2.add(buttonGerman);
        rowsInline.add(row2);

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton buttonFrench = new InlineKeyboardButton();
        buttonFrench.setText("French 🇫🇷");
        buttonFrench.setCallbackData("SET_LANGUAGE_FRENCH");
        row3.add(buttonFrench);
        rowsInline.add(row3);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void updateUserLearningLanguage(long chatId, String language) {
        User user = userRepository.findById(chatId).orElse(new User());
        user.setLearningLanguage(language);
        userRepository.save(user);
    }

    private void talkWithAI(long chatId) {


    }
}
