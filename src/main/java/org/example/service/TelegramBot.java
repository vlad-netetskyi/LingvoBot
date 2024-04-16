package org.example.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.example.config.BotConfig;
import org.example.model.Word;
import org.example.model.WordRepository;
import org.example.model.User;
import org.example.model.UserRepository;

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WordRepository wordRepository;
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
            } else {
                switch (messageText) {
                    case "/start" -> {
                        registerUser(update.getMessage());
                        showStart(chatId, update.getMessage().getChat().getFirstName());
                    }
                    case "/help" -> sendMessage(chatId, HELP_TEXT);
                    case "/about" -> sendMessage(chatId, ABOUT_TEXT);
                    case "/stats" -> showStats(chatId);
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
    }

    private Optional<Word> getRandomWord() {
        var r = new Random();
        var randomId = r.nextLong(MAX_WORD_ID_MINUS_ONE) + 1;
        return wordRepository.findById(randomId);
    }

    private void addButtonAndSendMessage(long chatId, Optional<Word> word) {
        SendMessage message = new SendMessage();
        message.setText(word.get().getEngName() + " - " + word.get().getUaName());
        message.setChatId(chatId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton inLineKeyboardButton = new InlineKeyboardButton();
        inLineKeyboardButton.setCallbackData(NEXT_WORD);
        inLineKeyboardButton.setText(EmojiParser.parseToUnicode("Next word :book:"));
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
        executeMessage(message);
    }

    private void addButtonAndEditMessage(long chatId, Optional<Word> word, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(word.get().getEngName() + " - " + word.get().getUaName());
        message.setMessageId(messageId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton inLineKeyboardButton = new InlineKeyboardButton();
        inLineKeyboardButton.setCallbackData(NEXT_WORD);
        inLineKeyboardButton.setText(EmojiParser.parseToUnicode("Next word :book:"));
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
                var translation = word.getEngName() + " - " + word.getUaName();
                sendMessage(user.getChatId(), translation);

            }
        }
    }

    private void sendWord(long chatId, Word wordToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(wordToSend.getEngName() + " - " + wordToSend.getUaName());

        executeMessage(message);
    }

    private void showStart(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + "! :smile:, nice to meet you!" + " :blush:");
        sendMessage(chatId, answer);
        log.info("Replied to user " + name);
    }

    private void showStats(long chatId) {
        String stats = EmojiParser.parseToUnicode(":book: Vocabulary: " + userRepository.findById(chatId).get().getStatistic());
        sendMessage(chatId, stats);

    }
}
