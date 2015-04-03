package com.bigdaddyscreations;

import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;
import javapns.notification.ResponsePacket;
import org.apache.commons.cli.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure(); // log4j stuff

        final Options options = new Options();
        options.addOption("h", "help", false, "extended help");
        options.addOption("D", true, "player profiles directory");
        options.addOption("K", true, "p12 keystore file");
        options.addOption("P", true, "keystore password");
        options.addOption("M", true, "message to be sent; put it in \"\"");
        options.addOption(null, "debuglog", false, "enable debug logging");
        options.addOption(null, "production", false, "send using production server (otherwise sandbox is used)");
        options.addOption(null, "skipCount", true, "skip first N users");
        options.addOption(null, "sendCount", true, "process only N users");

        final CommandLineParser parser = new PosixParser();
        final CommandLine cli = parser.parse(options, args);

        if (cli.hasOption('h')) {
            final HelpFormatter help = new HelpFormatter();
            help.setWidth(Integer.MAX_VALUE);
            help.printHelp("java -jar mass-pusher.jar [OPTIONS]...", options);
            System.exit(0);
        }

        if (!cli.hasOption("D"))
            throw new MissingArgumentException(options.getOption("D"));

        if (!cli.hasOption("K"))
            throw new MissingArgumentException(options.getOption("K"));

        if (!cli.hasOption("P"))
            throw new MissingArgumentException(options.getOption("P"));

        if(! cli.hasOption("M"))
            throw new MissingArgumentException(options.getOption("M"));

        if(! cli.hasOption("debuglog")) {
            List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
            loggers.add(LogManager.getRootLogger());
            for (Logger logger : loggers) {
                logger.setLevel(Level.OFF);
            }
        }

        File profilesDir = new File(cli.getOptionValue("D"));

        if (!profilesDir.exists() || !profilesDir.isDirectory())
            throw new IllegalArgumentException("'" + profilesDir.getAbsolutePath() + "' does not exists or is not a directory");

        File keystoreFile = new File(cli.getOptionValue("K"));

        if (!keystoreFile.exists() || !keystoreFile.isFile())
            throw new IllegalArgumentException("'" + profilesDir.getAbsolutePath() + "' does not exists or is not a file");

        String password = cli.getOptionValue("P");

        String message = cli.getOptionValue("M");

        List<DeviceToken> tokens = DeviceToken.createDeviceTokens(profilesDir);

        // deviceID -> userID
        Map<String, String> tokenMap = new HashMap<String, String>();
        for(DeviceToken t : tokens)
            tokenMap.put(t.deviceToken, t.userID);

        boolean production = false;
        if(cli.hasOption("production"))
            production = true;
        
        int skipCount = 0;
        if(cli.hasOption("skipCount"))
            skipCount = Integer.parseInt(cli.getOptionValue("skipCount"));
        
        int sendCount = tokens.size() - skipCount;
        if(cli.hasOption("sendCount"))
            sendCount = Integer.parseInt(cli.getOptionValue("sendCount"));
        
        PushNotificationPayload payload = (PushNotificationPayload) PushNotificationPayload.combined(message, -1, "default");

        System.out.println("Sending...");

        SenderThread thread = new SenderThread(tokens, payload, keystoreFile, password, production, 50, skipCount, skipCount + sendCount);
        thread.start();
        thread.join();

        System.out.println("... done.");
        System.out.println();

        int index = thread.getSentCount() - 1;
        if(index >= 0)
            System.out.println("Last processed user #" + index + " " + tokens.get(index).userID + " (" + tokens.get(index).deviceToken + ")");
        else
            System.out.println("No users processed, check your 'skipCount' and 'sendCount' settings.");

        System.out.println();

        for(PushedNotification success : thread.getSuccessfulNotifications()) {
            String t = success.getDevice().getToken();
            System.out.println("Successfully pushed to " + tokenMap.get(t) + " (" + t + ")");
        }

        if(thread.getSuccessfulNotifications().size() > 0)
            System.out.println();

        for(PushedNotification fail : thread.getFailedNotifications()) {
            String t = fail.getDevice().getToken();
            ResponsePacket r = fail.getResponse();

            String msg = "Failed to push to " + tokenMap.get(t) + " (" + t + ")";

            if(r != null)
                msg += ": " + r.getMessage();

            System.out.println(msg);
        }
    }
}
