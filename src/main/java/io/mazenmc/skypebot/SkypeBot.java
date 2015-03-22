package io.mazenmc.skypebot;

import static spark.Spark.*;

import com.google.code.chatterbotapi.ChatterBotFactory;
import com.google.code.chatterbotapi.ChatterBotSession;
import com.google.code.chatterbotapi.ChatterBotType;
import io.mazenmc.skypebot.engine.bot.ModuleManager;
import io.mazenmc.skypebot.handler.CooldownHandler;
import io.mazenmc.skypebot.utils.UpdateChecker;
import io.mazenmc.skypebot.utils.Utils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONObject;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class SkypeBot {

    private static SkypeBot instance;
    Connection database;
    private ChatterBotSession bot;
    private twitter4j.Twitter twitter;
    private boolean locked = false;
    private UpdateChecker updateChecker;
    private CooldownHandler cooldownHandler;

    public SkypeBot() {
        instance = this;

        try {
            bot = new ChatterBotFactory().create(ChatterBotType.CLEVERBOT).createSession();
        } catch (Exception ignored) {
        }

        ModuleManager.loadModules("io.mazenmc.skypebot.modules");

        Properties connectionProps = new Properties();
        connectionProps.put("user", "skype_bot");
        connectionProps.put("password", "skype_bot");

        try {
            database = DriverManager.getConnection("jdbc:mysql://localhost:3306/skype_bot", connectionProps);
        } catch (SQLException e) {
        }

        List<String> twitterInfo = Utils.readAllLines("twitter_auth");

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(twitterInfo.get(0))
                .setOAuthConsumerSecret(twitterInfo.get(1))
                .setOAuthAccessToken(twitterInfo.get(2))
                .setOAuthAccessTokenSecret(twitterInfo.get(3));
        twitter = new TwitterFactory(cb.build()).getInstance();

        cooldownHandler = new CooldownHandler();

        setPort(80);

        get("/bot/:message", (req, res) -> {
            JSONObject response = new JSONObject();

            String data = ModuleManager.parseText(URLDecoder.decode(req.params("message")));

            if (data == null) {
                response.put("type", -1);
            } else if (data.startsWith("http")) {
                response.put("type", 2);
            } else {
                response.put("type", 1);
            }

            response.put("data", data);
            return response.toString();
        });

        new UpdateChecker().start();
    }

    public static SkypeBot getInstance() {
        if (instance == null) {
            new SkypeBot();
        }

        return instance;
    }

    public String askQuestion(String question) {
        if (bot == null) {
            return "ChatterBot Died";
        }

        try {
            return bot.think(question);
        } catch (Exception ignored) {
            return "I am overthinking... (" + ExceptionUtils.getStackTrace(ignored) + ")";
        }
    }

    public Connection getDatabase() {
        return database;
    }

    public Twitter getTwitter() {
        return twitter;
    }

    public CooldownHandler getCooldownHandler() {
        return cooldownHandler;
    }
}
