package com.bigdaddyscreations;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceToken {
    public static List<DeviceToken> createDeviceTokens(File profilesDir) {
        List<DeviceToken> retVal = new ArrayList<DeviceToken>();

        File[] files = profilesDir.listFiles();

        for (File userDir : files) {
            if (! userDir.isDirectory())
                continue;

            File tokenFile = new File(userDir, "deviceToken.json");

            if(! tokenFile.exists() || ! tokenFile.isFile())
                continue;

            String token = null;

            try {
                JSONParser parser = new JSONParser();
                JSONObject object = null;
                try {
                    object = (JSONObject) parser.parse(new FileReader(tokenFile));
                }
                catch (IOException ignored) {}
                catch (ParseException ignored) {}

                if(object == null)
                    continue;

                token = (String) object.get("token");
            }
            catch (ClassCastException ignored) {}

            if(token == null)
                continue;

            DeviceToken dt = new DeviceToken(userDir.getName(), token);
            retVal.add(dt);
        }

        return retVal;
    }

    public String userID;
    public String deviceToken;

    public DeviceToken(String userID, String deviceToken) {
        this.userID = userID;
        this.deviceToken = deviceToken;
    }

    @Override
    public String toString() {
        return "DeviceToken{" +
                "userID='" + userID + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                '}';
    }
}
