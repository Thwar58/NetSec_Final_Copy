/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ServerFinal;

import csc3055.json.JsonIO;
import csc3055.json.types.JSONArray;
import csc3055.json.types.JSONObject;
import merrimackutil.util.NonceCache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;

/**
 *
 * @author burli
 */
public class ClientThread implements Runnable {

    private SSLSocket aSocket;
    private passwd passLocker;
    private boolean isLoggedIn = false;
    private boolean loginAttempt = false;
    private boolean createAttempt = false;
    private PrintWriter send;
    private Scanner receive;
    private String user;
    public ArrayList<ClientThread> currentConnections;
    public static boolean notAuthed;
    //the noncecache of the thread
    private static NonceCache cache;

    public boolean isLoginAttempt() {
        return loginAttempt;
    }

    public String getUser() {
        return this.user;
    }

    public void setLoginAttempt(boolean loginAttempt) {
        this.loginAttempt = loginAttempt;
    }

    public boolean isCreateAttempt() {
        return createAttempt;
    }

    public void setCreateAttempt(boolean createAttempt) {
        this.createAttempt = createAttempt;
    }

    public ClientThread(SSLSocket socket, passwd locker) {
        this.aSocket = socket;
        this.passLocker = locker;
        cache = new NonceCache(32, 60);

    }

    public void updateConnections(ArrayList<ClientThread> connections) {
        this.currentConnections = connections;
    }

    public boolean isIsLoggedIn() {
        return isLoggedIn;
    }

    public void setIsLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public String createAccount(String username, String password) throws NoSuchAlgorithmException, InvalidKeyException {
        if (!passLocker.userExists(username)) {
            byte[] salt = new byte[16];
            SecureRandom rand = new SecureRandom();
            // Get the random salt.
            rand.nextBytes(salt);
            String salt64 = Base64.getEncoder().encodeToString(salt);
            String hashedPass = passLocker.deriveHash(password, salt64);
            //now we have the salt and the hashed pass

            //generate a timeOTP object for this user
            TimeOTP creator = new TimeOTP();
            //create an entry
            accEntries entry = new accEntries();
            entry.setUser(username);
            entry.setTotp(creator.get64Key());
            entry.setPass(hashedPass);
            entry.setSalt(salt64);

            //add it to the db
            passLocker.addEntry(entry);
            //update the locker
            saveOut(passLocker);

            //Base32 for the construction of the QR on the client side
            String totp = creator.get32Key();
            JSONObject payload = new JSONObject();
            payload.put("status", "true");
            payload.put("payload", totp);
            String returnMe = payload.toJSON();
            return returnMe;
        } else {
            JSONObject payload = new JSONObject();
            payload.put("status", "false");
            payload.put("payload", "User already Exists.");
            String returnMe = payload.toJSON();
            return returnMe;
        }
    }

    public String loginToAccount(String aUser, String pass, String otp) {
        if (passLocker.userExists(aUser)) {
            try {

                accEntries entry = passLocker.getEntryByUser(aUser);

                TimeOTP totp;
                totp = new TimeOTP(0, entry.getTotp());
                JSONObject payload = new JSONObject();
                if (entry.getPass().equals(passLocker.deriveHash(pass, entry.getSalt())) && totp.verify(otp)) {
                    payload.put("status", "true");
                    String returnMe = payload.toJSON();
                    this.user = aUser;
                    return returnMe;
                } else {
                    payload.put("status", "false");
                    String returnMe = payload.toJSON();
                    return returnMe;
                }

            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            System.err.println("The user does not exist");
        }

        return null;
    }

    public String typeHandler(JSONObject message) {
        String status = (String) message.get("status");
        if (status.equals("create")) {
            try {
                String aUser = (String) message.get("username");
                String pass = (String) message.get("password");
                String payload = createAccount(aUser, pass);
                System.out.println("Client thread says create");

                return payload;
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        if (status.equals("login")) {
            String aUser = (String) message.get("username");
            String pass = (String) message.get("password");
            String otp = (String) message.get("otp");
            String payload = loginToAccount(aUser, pass, otp);
            System.out.println("Client thread says login");
            return payload;

        }

        if (status.equals("exchange")) {
            String payload = keyExchange(message);
            return payload;
        }

        if (status.equals("exchange2")) {
            String payload = keyExchange2(message);
            return payload;

        }

        if (status.equals("file")){
            System.out.println("Got file, sending off now...");
            return message.toJSON();

        }

        if (status.equals("message")){
            return message.toJSON();
            
        }
        return null;
    }

    //other users respond with their keys
    public static String keyExchange2(JSONObject message) {
        JSONObject keyAndUser = new JSONObject();
        String aUser = (String) message.get("user");
        String aKey = (String) message.get("pubkey");
        keyAndUser.put("user", aUser);
        keyAndUser.put("pubkey", aKey);
        keyAndUser.put("status", "exchange2");
        String nonceAsString = Base64.getEncoder().encodeToString(cache.getNonce());
        keyAndUser.put("nonce",nonceAsString);
        String toSend = keyAndUser.toJSON();
        return toSend;
    }

    //first part, I send my key to the other users
    public static String keyExchange(JSONObject message) {     
        String aKey = (String) message.get("pubkey");       
        String myUser = (String) message.get("user");
        JSONObject keyAndUser = new JSONObject();
        keyAndUser.put("user", myUser);
        keyAndUser.put("pubkey", aKey);
        keyAndUser.put("status", "exchange");
        String nonceAsString = Base64.getEncoder().encodeToString(cache.getNonce());
        keyAndUser.put("nonce",nonceAsString);
        String toSend = keyAndUser.toJSON();
        return toSend;
    }

    public void saveOut(passwd passwdLocker) {
        try {
            String dir = System.getProperty("user.dir");
            dir = dir + "/test/";
            JsonIO.writeSerializedObject(passwdLocker, new File(dir + "passwd.json"));
            passLocker = passwdLocker;
        } catch (FileNotFoundException ex) {
            System.out.println("Could not save passLocker to disk.");
            System.out.println(ex);
        }
    }

    public boolean getAuthed() {
        return notAuthed;
    }

    public PrintWriter getSock() {
        return this.send;
    }

    @Override
    public void run() {
        try {
            send = new PrintWriter(aSocket.getOutputStream(), true);
            receive = new Scanner(aSocket.getInputStream());

        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        notAuthed = true;
        while (notAuthed) {
            JSONObject aMessage = JsonIO.readObject(receive.nextLine());
            String toSend = typeHandler(aMessage);
            if (toSend.contains("true")) {
                notAuthed = false;
                user = aMessage.getString("username");
            }
            send.println(toSend);
        }

        while (true) {

            JSONObject message = JsonIO.readObject(receive.nextLine());
            String toSend = typeHandler(message);

            for (ClientThread cons : currentConnections) {
                if (!cons.getUser().equals(user)) {
                    cons.getSock().println(toSend);
                }

            }
        }

    }
}
