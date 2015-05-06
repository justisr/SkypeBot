package io.mazenmc.skypebot.modules;

import io.mazenmc.skypebot.SkypeBot;
import io.mazenmc.skypebot.engine.bot.*;
import io.mazenmc.skypebot.utils.Resource;
import io.mazenmc.skypebot.utils.Utils;

import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONObject;

import com.google.code.chatterbotapi.ChatterBotSession;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class General implements Module {

    private static ChatterBotSession cleverBot;
    private static ChatterBotSession jabberWacky;
    private static Thread rantThread;
    private static boolean ranting = false;

    @Command(name = "8ball")
    public static String cmd8Ball(String question) {
        String[] options = new String[]{"It is certain", "It is decidedly so", "Without a doubt", "Yes definitely", "You may rely on it", "As I see it, yes", "Most likely", "Outlook good", "Yes", "Signs point to yes", "Reply hazy try again", "Ask again later", "Better not tell you now", "Cannot predict now", "Concentrate and ask again", "Don't count on it", "My reply is no", "My sources say no", "Outlook not so good", "Very doubtful"};
        int chosen = ThreadLocalRandom.current().nextInt(options.length);

        return options[chosen];
    }

    @Command(name = "about")
    public static String cmdAbout(String message) {
        return "Originally created by Vilsol, reincarnated and improved by MazenMC and stuntguy3000\n" +
                "Version: " + Resource.VERSION;
    }

    @Command(name = "bot")
    public static String cmdBot(String message, String msg) {
        return msg;
    }

    @Command(name = "choice")
    public static String choice(String m, String message) {
        String[] choices = message.trim().split(",");

        if (choices.length == 1) {
            return "Give me choices";
        }

        return "I say " + choices[ThreadLocalRandom.current().nextInt(choices.length)].trim();
    }

    @Command(name = "c")
    public static String cmdC(String message, String question) {
        return SkypeBot.getInstance().askQuestion(question);
    }

    @Command(name = "git", alias = {"repo", "repository", "source"})
    public static String cmdGit(String message) {
        return "Git Repository: https://github.com/MazenMC/SkypeBot/tree/web-port";
    }

    @Command(name = "help", alias = {"commands"})
    public static String cmdHelp(String message) {
        String commands = "";

        for (Map.Entry<String, CommandData> data : ModuleManager.getCommands().entrySet()) {
            if (!data.getValue().getCommand().exact()) {
                continue;
            }

            if (!commands.equals("")) {
                commands += ", ";
            }

            commands += Resource.COMMAND_PREFIX + data.getKey();

            if (!data.getValue().getParameterNames().equals("")) {
                commands += " " + data.getValue().getParameterNames();
            }
        }

        return "Available Commands: " + Utils.upload(commands);
    }

    @Command(name = "lmgtfy")
    public static String cmdLmgtfy(String question) {
        return "http://lmgtfy.com/?q=" + URLEncoder.encode(question);
    }

    @Command(name = "md5")
    public static String cmdMd5(String message) {
        String s = "md_1 = 1% of devs (people who know their shit)\n" +
                "md_2 = uses one class for everything\n" +
                "md_3 = true == true, yoo!\n" +
                "md_4 = New instance to call static methods\n" +
                "md_5 = reflects his own classes\n" +
                "md_6 = return this; on everything\n" +
                "md_7 = abstract? never heard of it\n" +
                "md_8 = interface? never heard of it\n" +
                "md_9 = enum? never heard of it\n" +
                "md_10 = java? never heard of it";
        return s;
    }

    @Command(name = "ping")
    public static String cmdPing(String message, @Optional String ip) throws Exception {
        if (ip == null) {
            return "Pong!";
        } else {
            try {
                HttpResponse<JsonNode> response = Unirest.get("https://igor-zachetly-ping-uin.p.mashape.com/pinguin.php?address=" + URLEncoder.encode(ip))
                        .header("X-Mashape-Key", "sHb3a6jczqmshcYqUEwQq3ZZR3BVp18NqaAjsnIYFvVNHMqvCb")
                        .asJson();
                if (response.getBody().getObject().get("result").equals(false)) {
                    return "Invalid hostname!";
                } else {
                    Object timeLeftObject = response.getBody().getObject().get("time");
                    if (timeLeftObject != null) {
                        String timeLeft = timeLeftObject.toString();
                        if (!timeLeft.isEmpty()) {
                            return ip + " - Response took " + timeLeft + "ms";
                        }
                    }

                    return ip + " - No response received!";
                }
            } catch (UnirestException e) {
                return "Error: " + Utils.upload(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    @Command(name = "random")
    public static String cmdRandom(String message, int number1, int number2) {
        int high = Math.max(number1, number2);
        int low = Math.min(number1, number2);

        if (high == low) {
            return "The numbers cannot be the same!";
        }

        return String.valueOf(ThreadLocalRandom.current().nextInt(high - low) + low);
    }

    @Command(name = "sql", cooldown = 30)
    public static String cmdSQL(String msg, String query) throws SQLException {
        if (SkypeBot.getInstance().getDatabase() == null) {
            return "Connection is down!";
        }

        if (query.toUpperCase().contains("DROP DATABASE") || query.toUpperCase().contains("CREATE DATABASE") || query.toUpperCase().contains("USE") || query.toUpperCase().contains("CREATE PROCEDURE")) {
            return "Do not touch the databases!";
        }

        if (query.toUpperCase().contains("INFORMATION_SCHEMA")) {
            return "Not that fast!";
        }

        Statement stmt = null;

        try {
            stmt = SkypeBot.getInstance().getDatabase().createStatement();
            if (query.toLowerCase().startsWith("select") || query.toLowerCase().startsWith("show")) {
                ResultSet result = stmt.executeQuery(query);
                String parsed = Utils.parseResult(result);
                parsed = query + "\n\n" + parsed;
                return "SQL Query Successful: " + Utils.upload(parsed);
            } else {
                return "SQL Query Successful!";
            }
        } catch (SQLException e) {
            String message = e.getMessage();
            message = message.replace("You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near", "");
            return "Error executing SQL: " + message;
        } catch (Exception e) {
            return "Error: " + Utils.upload(ExceptionUtils.getStackTrace(e));
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    @Command(name = "topkek")
    public static String cmdTopKek(String message) {
        return "https://topkek.mazenmc.io/ Gotta be safe while keking!";
    }

    @Command(name = "define")
    public static String cmddefine(String message, String word) throws Exception {
        HttpResponse<String> response = Unirest.get("https://mashape-community-urban-dictionary.p.mashape.com/define?term=" + word.replace(" ", "+"))
                .header("X-Mashape-Key", Resource.KEY_URBAND)
                .header("Accept", "text/plain")
                .asString();
        JSONObject object = new JSONObject(response.getBody());

        if (object.getJSONArray("list").length() == 0) {
            return "No definition found for " + word + "!";
        }

        JSONObject definition = object.getJSONArray("list").getJSONObject(0);

        return "Definition of " + word + ": " + definition.getString("definition") + "\n" +
                definition.getString("example") +
                definition.getInt("thumbs_up") + " thumbs ups, " + definition.getInt("thumbs_down") + " thumbs down" +
                "Definition by " + definition.getString("author");
    }

    @Command(name = "dreamincode", alias = {"whatwouldmazensay"})
    public static String cmddreamincode(String message) {
        String[] options = new String[]{"No, I'm not interested in having a girlfriend I find it a tremendous waste of time.",
                "Hi, my name is Santiago Gonzalez and I'm 14 and I like to program.",
                "I'm fluent in a dozen different programming languages.",
                "Thousands of people have downloaded my apps for the Mac, iPhone, and iPad.",
                "I will be 16 when I graduate college and 17 when I finish my masters.",
                "I really like learning, I find it as essential as eating.",
                "Dr. Bakos: I often have this disease which I call long line-itus.",
                "Dr. Bakos: Are you eager enough just to write down a slump of code, or is the code itself a artistic medium?",
                "Beautiful code is short and concise.",
                "Sometimes when I go to sleep I'm stuck with that annoying bug I cannot fix, and in my dreams I see myself programming. " +
                        "When I wake up I have the solution!",
                        "One of the main reasons I started developing apps was to help people what they want to do like decorate a christmas tree.",
                        "I really like to crochet.",
        "I made a good website http://slgonzalez.com/"};
        int chosen = ThreadLocalRandom.current().nextInt(options.length);

        return options[chosen];
    }

    @Command(name = "(?i)nice", command = false)
    public static String nice(String message) {
        return "https://www.youtube.com/watch?v=zYt0WbDjJ4E";
    }

    @Command(name = "(?i)ayy", exact = false, command = false)
    public static String ayy(String message) {
        if (message.contains("lmao"))
            return "ayy lmao";

        return "lmao";
    }

    @Command(name = "(?i)alien", exact = false, command = false)
    public static String ayyLmao(String message) {
        return "ayy lmao";
    }

    @Command(name = "whatwouldrandomsay")
    public static String cmdRandomSay(String msg) {
        List<Message> messages = SkypeBot.getInstance().messages();
        Message message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));

        while (message.contents().startsWith("/")) {
            message = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        }

        return message.sender() + " says: \"" + message.contents() + "\" ";
    }

    @Command(name = "roflcopter", alias = {"rofl"})
    public static String cmdRofl(String message) {
        return "ROFL all day long! http://goo.gl/pCIqXv";
    }

    @Command(name = "lenny")
    public static String cmdLenny(String message) {
        return "( ͡° ͜ʖ ͡°)";
    }

    @Command(name = "phallusexercise", alias = {"whatwouldjustissay"})
    public static String cmdphallusexercise(String message) {
        String[] options = new String[]{
        		"Guys, Can confirm. Penis exersizes DO work.",
        		"It's only been a week and there is a noticable difference.",
        		"Excersizing my phallus.",
        		"Any lady of mine is gunna feel real lucky.",
        		"If only you guys were as passionate about giving your women pleasure.",
        		"FYI, I have plenty of ladies.",
        		"Any women will apreciate your effort. Knowing you care.",
        		"I'm good at what I do. ;)",
        		"I expect lots of cake!",
        		"We could be sex buddies!!! Makin porn together!!",
        		"All our sex toys are made from 100% ultra-premium custom forumulated silicone; garenteed to last a lifetime.",
        		"Easy To Clean, Eco-Friendly, Hypoallergenic, Hygienic, Boilable, Bleachable and Dishwasher Safe. ;D",
        		"Just in case you ever wanted to wash your vibrator with your eating utensils. I know I do.",
                "When it comes to being pathetic, I'm rank #1.",
                "Me, my best friend, and my second best friend are all moving to oregon when we older. We're gunna have a farm. ;)",
                "Oi, I fell in love with a non-existant girl from a lucid dream.",
                "What are arrays?",
                "So, arrays are like worse Lists!?!?!?"
        };
        int chosen = ThreadLocalRandom.current().nextInt(options.length);

        return options[chosen];
    }
}

