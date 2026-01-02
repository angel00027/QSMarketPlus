package mp.quesito.qSMarketPlus.utils;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DiscordWebhook {

    public static void send(String webhookUrl, String message) {

        try {

            // Evitar mensajes vacíos
            if (message == null || message.isEmpty()) return;

            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // ESCAPAR JSON correctamente
            String safe = message
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            String json = "{\"content\":\"" + safe + "\"}";

            OutputStream os = connection.getOutputStream();
            os.write(json.getBytes());
            os.flush();
            os.close();

            connection.getInputStream().close();

        } catch (Exception ex) {
            System.out.println("Error al enviar webhook Discord: " + ex.getMessage());
        }
    }
}
