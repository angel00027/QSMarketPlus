package mp.quesito.qSMarketPlus.economia;

import java.util.HashMap;
import java.util.Map;

public class EconomyManager {

    private final Map<String, EconomyProvider> economies = new HashMap<>();

    public void register(String id, EconomyProvider provider) {
        economies.put(id.toLowerCase(), provider);
    }

    public EconomyProvider get(String id) {

        if (id == null) return economies.get("vault");

        EconomyProvider eco = economies.get(id.toLowerCase());

        if (eco == null) {
            eco = economies.get("vault"); // fallback seguro
        }

        return eco;
    }

    public boolean exists(String id) {
        return economies.containsKey(id.toLowerCase());
    }
}