package Networking.Server;

import Networking.Callbacks.ServerCallback;
import Networking.DisconnectReason;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

public class Server {

    private ServerSocket serverSocket;
    private final HashMap<String, ServerClient> clients = new HashMap<>();

    private final int MAX_CLIENTS;

    private final int port;
    private boolean open;

    private Thread acceptThread;

    private final List<ServerCallback> callbacks = new ArrayList<>();

    public Server(int port, int maxClients) throws Exception {
        this.port = port;
        this.MAX_CLIENTS = maxClients;

        create();
    }

    private void create() throws Exception {
        serverSocket = new ServerSocket(port);

        acceptThread = new Thread(() -> {
            try {
                while (open) {
                    acceptClients();
                }
            } catch (Exception e) {
                if (open) e.printStackTrace();
            }
        }, "SERVER-ACCEPT-" + hashCode());

        open = true;
        acceptThread.start();
    }

    public void close() throws Exception {
        for (ServerClient client : getClients()) {
            client.disconnect(true, DisconnectReason.ServerClose);
        }

        open = false;
        acceptThread.stop();
        serverSocket.close();
    }

    public void registerCallback(ServerCallback callback) {
        callbacks.add(callback);
    }

    private void acceptClients() throws Exception {
        Socket newClient = serverSocket.accept();

        String id = generateId();
        ServerClient client = new ServerClient(id, newClient, this);
        if (clients.size() >= MAX_CLIENTS) {
            client.disconnect(true, DisconnectReason.ServerFull);
            return;
        }

        ServerSend.assignId(client);
        clients.put(id, client);

        call((c) -> c.onClientConnect(client));
    }

    private String generateId() {
        String id = UUID.randomUUID().toString();
        while (clients.containsKey(id)) {
            id = UUID.randomUUID().toString();
        }

        return id;
    }

    public void call(Consumer<ServerCallback> callback) {
        for (ServerCallback c : callbacks) {
            callback.accept(c);
        }
    }

    public Collection<ServerClient> getClients() {
        return clients.values();
    }

    public void removeClient(String id) {
        clients.remove(id);
    }

    public int getMaxClients() {
        return MAX_CLIENTS;
    }

    public int getPort() {
        return port;
    }

    public boolean isOpen() {
        return open;
    }

}
