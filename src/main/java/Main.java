import Networking.Callbacks.ClientCallback;
import Networking.Callbacks.ServerCallback;
import Networking.Client.Client;
import Networking.DisconnectReason;
import Networking.Packet.Packet;
import Networking.Packet.ServerPacket;
import Networking.Server.Server;
import Networking.Server.ServerClient;

import java.util.Scanner;

public class Main implements ServerCallback, ClientCallback {

    public static void main(String[] args) {
        try {
            new Main().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start() throws Exception {
        Server server = new Server(8080, 10);
        server.registerCallback(this);
        Client client = new Client("127.0.0.1", server.getPort());
        client.registerCallback(this);
        client.connect();

        String line;
        Scanner scanner = new Scanner(System.in);
        while (!(line = scanner.next()).equals("exit")) {
            if (line.equals("connect")) {
                client.connect();
            } else if (line.equals("disconnect")) {
                client.disconnect();
            }
        }

        server.close();
    }

    @Override
    public void onConnect() {
        System.out.println("Connected!");
    }

    @Override
    public void onDisconnect(DisconnectReason reason) {
        System.out.println("Client: " + reason);
    }

    @Override
    public void onPacket(Packet packet, int type) {
        System.out.println("Packet: " + ServerPacket.values()[type]);
    }

    @Override
    public void onClientConnect(ServerClient client) {

    }

    @Override
    public void onClientDisconnect(ServerClient client, DisconnectReason reason) {

    }

    @Override
    public void onPacket(ServerClient client, Packet packet, int type) {

    }
}
