import Networking.Client.Client;
import Networking.Server.Server;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try {
            new Main().start(args[0].equals("true"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void start(boolean server) throws Exception {
        Server s = null;
        if (server) {
            s = new Server(8080, 10);
        }

        Client client = new Client("localhost", 8080);
        client.connect();

        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            }

            if (input.equals("ping")) {
                System.out.println("Ping: " + client.getPing() + "ms");
                System.out.println("Status: " + client.getPingStatus());
            }
        }

        client.disconnect();
        if (server) {
            s.close();
        }
    }

}
