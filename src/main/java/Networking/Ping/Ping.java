package Networking.Ping;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Ping {

    public static final float MAX_PING_TIME = 5000;

    protected Ping() {}

    public static float ping(String url) throws Exception {
        Process process = Runtime.getRuntime().exec("ping " + url);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        int i;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((i = reader.read(buffer)) > 0)
            output.append(buffer, 0, i);
        reader.close();
        String pingStr = output.toString().split("Average = ")[1].split("ms")[0];
        return Float.parseFloat(pingStr);
    }

    public static PingStatus getConnectionStatus(float ping) {
        if (ping <= 30) return PingStatus.Great;
        else if (ping <= 60) return PingStatus.Good;
        else if (ping <= 100) return PingStatus.Average;
        else if (ping < MAX_PING_TIME) return PingStatus.Bad;
        else return PingStatus.TimedOut;
    }

}
