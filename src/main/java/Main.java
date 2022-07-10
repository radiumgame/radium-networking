import Networking.Callbacks.ClientCallback;
import Networking.Callbacks.ServerCallback;
import Networking.Client.Client;
import Networking.Server.Server;

import java.util.Scanner;

public class Main extends ClientCallback {

    private static Client client;

    public static void main(String[] args) {
        try {
            new Main().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start() throws Exception {
        Server server = new Server(8080, 10);
        client = new Client("127.0.0.1", server.getPort());
        client.registerCallback(this);
        client.connect();

        String line;
        Scanner scanner = new Scanner(System.in);
        while (!(line = scanner.nextLine()).equals("exit")) {
            if (line.equals("connect")) {
                client.connect();
            } else if (line.equals("disconnect")) {
                client.disconnect();
            }
        }

        server.close();
    }

}
