package io.mazenmc.skypebot.utils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import net.lingala.zip4j.core.ZipFile;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class UpdateChecker extends Thread {

    private String lastSha = "--";
    private String accessToken;

    public UpdateChecker() {
        accessToken = Utils.readFirstLine("key_github");
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }

            try {
                HttpResponse<JsonNode> response = Unirest.get("https://api.github.com/repos/MazenMC/SkypeBot/commits?sha=web-port" +
                        "&access_token=" + accessToken)
                        .header("User-Agent", "Mazen-SkypeBot")
                        .header("Content-Type", "application/json")
                        .asJson();

                JsonNode node = response.getBody();
                JSONObject recentCommit = node.getArray().getJSONObject(0);
                String sha = recentCommit.getString("sha");

                System.out.println("looked for commits");

                if (!lastSha.equals(sha) && !lastSha.equals("--")) {
                    URL url = new URL("https://github.com/MazenMC/SkypeBot/archive/web-port.zip");
                    HttpsURLConnection c = (HttpsURLConnection) url.openConnection();

                    System.out.println("Found new commit! Downloading source...");

                    try (InputStream stream = c.getInputStream()) {
                        File f = new File("web-port.zip");

                        if (f.exists())
                            f.delete();

                        Files.copy(stream, Paths.get("web-port.zip"));
                        stream.close();
                    }

                    System.out.println("Downloaded source! extracting jar...");

                    File output = new File("SkypeBot-web-port");

                    if (output.exists())
                        output.delete();

                    ZipFile zip = new ZipFile(new File("web-port.zip"));

                    zip.extractAll(System.getProperty("user.dir"));

                    System.out.println("Extracted jar! Compiling source...");

                    ProcessBuilder builder = new ProcessBuilder("/usr/bin/mvn", "clean", "compile", "assembly:single")
                            .redirectErrorStream(true).directory(output);
                    Process process = builder.start();

                    process.waitFor();

                    System.out.println("Compiled source!");

                    File compiled = new File(output, "target/skypebot-1.0-SNAPSHOT-jar-with-dependencies.jar");
                    File current = new File("skypebot-1.0-SNAPSHOT-jar-with-dependencies.jar");

                    if (!compiled.exists()) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String tmp;
                        List<String> lines = new ArrayList<>();

                        while ((tmp = in.readLine()) != null) {
                            lines.add(tmp);
                        }

                        in.close();

                        System.out.println("error! printing stacktrace...");
                        lines.forEach(System.out::println);
                        lastSha = sha;
                        continue;
                    }

                    current.delete();
                    current.createNewFile();

                    FileOutputStream fos = new FileOutputStream(current);
                    FileInputStream fis = new FileInputStream(compiled);
                    byte[] buffer = new byte[1024];
                    int i;

                    while ((i = fis.read(buffer)) > 0) {
                        fos.write(buffer, 0, i);
                    }

                    fis.close();
                    fos.close();
                    process.destroy();

                    System.out.println("bye bye...");
                    System.exit(0);
                } else {
                    lastSha = sha;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
