package org.example;

import org.example.controller.Bot;
import org.example.dao.DaoImpl;
import org.example.controller.users.UsersController;
import org.example.services.exchangeRates.CbrConnector;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException, TelegramApiException {
        String token = Files.readString(Paths.get("src/main/resources/token.config"));
        String myId = Files.readString(Paths.get("src/main/resources/myId.txt"));

        UsersController usersController = new UsersController(new DaoImpl());
        Bot bot = new Bot(token, new CbrConnector(), usersController);
        registration(bot);

        bot.sendMessage(Long.parseLong(myId), "Bot is active now!");
    }

    /**
     * Регистрация бота в API Telegram
     */
    private static void registration(Bot bot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);
    }
}