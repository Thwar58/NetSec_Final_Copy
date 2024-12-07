/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ServerFinal;

import csc3055.cli.LongOption;
import csc3055.cli.OptionParser;
import csc3055.json.JsonIO;
import csc3055.json.types.JSONObject;
import csc3055.util.Tuple;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.ServerSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author burli
 */
public class ServerDriver {

    private static boolean doHelp = false;
    private static boolean doConfig = false;
    private static String configName = null;
    private static servConfig config = null;
    private static passwd db = null;
    private static ExecutorService threads;
    private static ServerSocket serverSock;
    private static SSLServerSocketFactory sslFact;
    public static ArrayList<ClientThread> connections;

    /**
     * Process the command line arguments.
     *
     * @param args the array of command line arguments.
     */
    public static boolean processArgs(String[] args) {

        OptionParser parser;

        LongOption[] opts = new LongOption[2];
        opts[0] = new LongOption("help", false, 'h');
        opts[1] = new LongOption("config", true, 'c');

        Tuple<Character, String> currOpt;

        parser = new OptionParser(args);
        parser.setLongOpts(opts);
        parser.setOptString("hc:");

        while (parser.getOptIdx() != args.length) {

            currOpt = parser.getLongOpt(false);

            switch (currOpt.getFirst()) {
                case 'h':
                    doHelp = true;
                    break;
                case 'c':
                    doConfig = true;
                    configName = currOpt.getSecond();
                    break;
                case '?':
                    usage();
                    break;
            }
        }

        // Verify that these options are not conflicting.
        if (doConfig && doHelp) {
            return false;
        }

        if (doConfig) {
            loadConfig(configName);
            return true;
        } else if (doHelp) {
            return false;
        }

        if (!doConfig && !doHelp) {
            loadConfig("config.json");
            return true;
        }
        return false;
    }

    /**
     * Loads the configuration information from the configuration file.
     *
     * @param configName the name of the configuration file.
     */
    public static void loadConfig(String configName) {

        JSONObject configObj = null;
        try {
            String dir = System.getProperty("user.dir");
            configObj = JsonIO.readObject(new File(dir + "/test/" + configName));
        } catch (FileNotFoundException ex) {
            System.out.println("Configruation file not found.");
            System.exit(1);
        }
        try {
            config = new servConfig(configObj);
        } catch (InvalidObjectException ex) {
            System.out.println("Invalid configuration file.");
            System.exit(1);
        }

    }

    /**
     * Prints the help menu.
     */
    public static void usage() {
        System.out.println("usage:");
        System.out.println("  chatserver");
        System.out.println("  chatserver --config <configfile>");
        System.out.println("  chatserver --help");
        System.out.println("options:");
        System.out.println("  -c, --config Set the config file.");
        System.out.println("  -h, --help Display the help.");
        System.exit(1);
    }

    /**
     * Loads the password database file.
     *
     * @param passdbName the name of the password database file.
     */
    public static void loadPasswordDatabase(String passdbName) throws IOException {
        String dir = System.getProperty("user.dir");
        dir = dir + "/test/";
        JSONObject passdbObj = null;
        File passFile = new File(dir + passdbName);

        if (!passFile.exists()) {
            db = new passwd();
            return;
        }

        try {
            passdbObj = JsonIO.readObject(passFile);
        } catch (FileNotFoundException ex) {
            System.out.println("Password database file not found.");
            System.exit(1);
        }
        try {
            db = new passwd(passdbObj);
        } catch (InvalidObjectException ex) {
            System.out.println("Invalid password database file.");
            System.exit(1);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        // Load the server configuration.
        if (!processArgs(args)) {
            usage();
        } else {
            // Set the keystore and keystore password.
            System.setProperty("javax.net.ssl.keyStore", config.getKeyStore());
            System.setProperty("javax.net.ssl.keyStorePassword", config.getKeyStorePass());

            // Load the password database.
            loadPasswordDatabase(config.getPassFile());
            sslFact = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            serverSock = sslFact.createServerSocket(config.getPort());
            threads = Executors.newFixedThreadPool(3);
            


            
            connections = new ArrayList<ClientThread>();
            
            while (true) {
                try {
                    SSLSocket aSocket = (SSLSocket) serverSock.accept();
                    ClientThread newCon = new ClientThread(aSocket, db);
                    connections.add(newCon);
                    threads.submit(newCon);
                    //update each thread with the new connections
                    for (ClientThread thread : connections){
                        thread.updateConnections(connections);
                    }
                    System.out.println("Connection received.");
                    
                    
                } catch (IOException ex) {
                    Logger.getLogger(ServerDriver.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
       
            
            
            
            
        }

    }

}
