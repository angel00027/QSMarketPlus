package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {

    private static final File logFile = new File(
            QSMarketPlus.getInstance().getDataFolder() + "/logs/transactions.log"
    );

    public static void log(String text) {
        try {
            logFile.getParentFile().mkdirs();
            if (!logFile.exists()) logFile.createNewFile();

            FileWriter fw = new FileWriter(logFile, true);

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            fw.write("[" + time + "] " + text + "\n");
            fw.close();

        } catch (Exception ignored) {}
    }
}
