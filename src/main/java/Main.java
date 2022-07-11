import Networking.Callbacks.ClientCallback;
import Networking.Callbacks.ServerCallback;
import Networking.Client.Client;
import Networking.Server.Server;
import Networking.Server.ServerClient;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            new Main().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start() throws Exception {
        String line;
        Scanner scanner = new Scanner(System.in);
        while (!(line = scanner.nextLine()).equals("exit")) {
            if (line.equals("server")) {
                Server server = new Server(8080, 10);
                server.registerCallback(new ServerCallback() {
                    @Override
                    public void onClientConnect(ServerClient client) {
                        System.out.println("Client connected: " + client.getIp());
                    }
                });
            } else if (line.equals("client")) {
                Client client = new Client("localhost", 8080);
                client.connect();
            }
        }
    }

}
