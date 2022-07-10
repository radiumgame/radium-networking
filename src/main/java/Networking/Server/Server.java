package Networking.Server;

import Networking.Packet.Packet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    private ServerSocket serverSocket;
    private final HashMap<String, ServerClient> clients = new HashMap<>();

    private final int MAX_CLIENTS;

    private final int port;
    private boolean open;

    private Thread acceptThread;

    public Server(int port, int maxClients) throws IOException {
        this.port = port;
        this.MAX_CLIENTS = maxClients;

        create();
    }

    private void create() throws IOException {
        serverSocket = new ServerSocket(port);

        acceptThread = new Thread(() -> {
            try {
                while (open) {
                    acceptClients();
                }
            } catch (IOException e) {
                if (open) e.printStackTrace();
            }
        }, "SERVER-ACCEPT-" + hashCode());

        open = true;
        acceptThread.start();
    }

    public void close() throws IOException {
        for (ServerClient client : getClients()) {
            client.disconnect(true);
        }

        open = false;
        acceptThread.stop();
        serverSocket.close();
    }

    private void acceptClients() throws IOException {
        Socket newClient = serverSocket.accept();
        String id = generateId();
        ServerClient client = new ServerClient(id, newClient, this);

        ServerSend.assignId(client);

        clients.put(id, client);
    }

    private String generateId() {
        String id = UUID.randomUUID().toString();
        while (clients.containsKey(id)) {
            id = UUID.randomUUID().toString();
        }

        return id;
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
