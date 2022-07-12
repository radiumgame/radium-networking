package Networking.Sync;

import java.util.HashMap;

public class NetworkSync {

    public HashMap<String, HashMap<String, Object>> syncData = new HashMap<>();
    private final HashMap<String, Object> baseData;
    private final SyncCallback callback;

    public NetworkSync(HashMap<String, Object> baseData, SyncCallback callback) {
        this.baseData = baseData;
        this.callback = callback;
    }

    public void updateProperty(String clientId, String propertyName, Object data) {
        if (!syncData.containsKey(clientId)) {
            return;
        }

        syncData.get(clientId).put(propertyName, data);
    }

    public void addClient(String clientId) {
        syncData.put(clientId, (HashMap)baseData.clone());
    }

    public void removeClient(String clientId) {
        syncData.remove(clientId);
    }

    public SyncCallback getCallback() {
        return callback;
    }

}
