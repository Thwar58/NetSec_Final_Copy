/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientFinal;

import com.google.zxing.WriterException;
import csc3055.json.JsonIO;
import csc3055.json.types.JSONObject;
import merrimackutil.util.NonceCache;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

/**
 *
 * @author burli
 */
public class ClientDriver {

    private static clientConfig config = null;
    private static LoginController loginControl;
    private static mainController mainControl;
    private static PrintWriter out;
    private static Scanner in;
    private static String username;
    private static ArrayList<String> usersAndKeys;
    private static ArrayList<String> secKeys;
    private static SecObj secObj;
    //the list of file names
    private static ArrayList<String> fNameList;
    //the list of files
    private static ArrayList<byte[]> fileList;
    //the nonce cache
    private static NonceCache cache;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws UnsupportedEncodingException, WriterException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        fNameList = new ArrayList<>();
        fileList = new ArrayList<>();
        cache = new NonceCache(32, 60);
        SSLSocketFactory fac;
        SSLSocket sock = null;
        secObj = new SecObj();
        //for users and public keys
        usersAndKeys = new ArrayList<String>();
        //for users and associated shared secrets
        secKeys = new ArrayList<String>();

        //load the client configuration
        loadConfig("clientConfig.json");
        // Set up the trust store.
        System.setProperty("javax.net.ssl.trustStore", config.getTrustStore());
        System.setProperty("javax.net.ssl.trustStorePassword", config.getTrustStorePass());
        loginControl = new LoginController();
        loginControl.runGUI();
        try {
            // Set up an SSL connection to the requested server.
            fac = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sock = (SSLSocket) fac.createSocket(config.getHost(), config.getPort());
            sock.setEnabledProtocols(new String[]{"TLSv1.3"});
            sock.startHandshake();
            out = new PrintWriter(sock.getOutputStream(), true);
            in = new Scanner(sock.getInputStream());
        } catch (UnknownHostException ex) {
            System.out.println("Host is unknown.");
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println("Could not create socket. " + ioe);
            System.exit(1);
        }

        boolean createAndLogin = false;
        while (!createAndLogin) {

            if (loginControl.getCreateAttempt()) {
                String toSend = createMessage(loginControl.getCreateUser(), loginControl.getCreatePass());
                out.println(toSend);
                JSONObject payload = JsonIO.readObject(in.nextLine());
                if (payload.getString("status").equals("true")) {
                    String totp = payload.getString("payload");
                    loginControl.generateQR(loginControl.getCreateUser(), totp);
                    //create a QR on the login control

                } else {
                    System.err.println("That User already exists");
                }
                loginControl.setCreateAttempt(false);
            }

            if (loginControl.getLoginAttempt()) {
                String toSend = loginMessage(loginControl.getLoginUser(), loginControl.getLoginPass(), loginControl.getOTP());
                out.println(toSend);
                JSONObject payload = JsonIO.readObject(in.nextLine());
                if (payload.getString("status").equals("true")) {
                    username = loginControl.getLoginUser();
                    //close the GUI
                    loginControl.closeGUI();
                    //break out of the while loop
                    createAndLogin = true;

                }
                loginControl.setLoginAttempt(false);
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        JSONObject firstConnection = new JSONObject();
        firstConnection.put("user", username);
        firstConnection.put("pubkey", secObj.getPubKey());
        firstConnection.put("status", "exchange");
        String nonceAsString1 = Base64.getEncoder().encodeToString(cache.getNonce());
        firstConnection.put("nonce", nonceAsString1);
        String sendKey = firstConnection.toJSON();
        System.out.println(sendKey);
        out.println(sendKey);

        //open the main client
        mainControl = new mainController();
        //run the main client
        mainControl.runGUI();
        Timer waitTime = new Timer();
        while (true) {

            //timer task code src
            //https://www.studytonight.com/java-examples/java-timer-and-timertask
            TimerTask inputWaiter = new TimerTask() {
                @Override
                public void run() {
                    JSONObject inCheck = JsonIO.readObject(in.nextLine());
                    byte[] nonceToCheck = Base64.getDecoder().decode(inCheck.getString("nonce"));
                    if(cache.containsNonce(nonceToCheck)){
                        return;
                    }else{
                        typeHandler(inCheck);
                        cache.addNonce(nonceToCheck);
                    }
                }

            };
            //wait for 3 seconds on the input, otherwise continue with the loop
            waitTime.schedule(inputWaiter, 3000);
            //wait before cancelling
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            inputWaiter.cancel();
            


            if (mainControl.getHasMessage()) {
                if (!secKeys.isEmpty()){
                    try {
                        String msg = mainControl.getMessage();
                        String localMsg = username + ": " + msg;
                        mainControl.updateMessageBox(localMsg);
                        String nonceAsString = Base64.getEncoder().encodeToString(cache.getNonce());
                        JSONObject aPayload = secObj.encryptedPayload(username, msg, nonceAsString);
                        String aMsgToSend = aPayload.toJSON();
                        out.println(aMsgToSend);
                        
                    } catch (NoSuchPaddingException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvalidAlgorithmParameterException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalBlockSizeException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BadPaddingException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else{
                    System.out.println("Nobody else is connected to the chat");
                }
                System.out.println(username + " said: " + mainControl.getMessage());
                mainControl.setHasMessage(false);
                
            }

            if (mainControl.getHasFile()) {
                if (!secKeys.isEmpty()){
                    try {
                        String msg = Base64.getEncoder().encodeToString(mainControl.getFile());
                        //update list of files here
                        String nonceAsString = Base64.getEncoder().encodeToString(cache.getNonce());
                        JSONObject aPayload = secObj.encryptedFilePayload(username, msg, nonceAsString);
                        aPayload.put("FileName",mainControl.getFileName());
                        String aMsgToSend = aPayload.toJSON();
                        out.println(aMsgToSend);

                    } catch (NoSuchPaddingException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvalidAlgorithmParameterException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IllegalBlockSizeException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BadPaddingException ex) {
                        Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                else{
                    System.out.println("Nobody else is connected to the chat");
                }
                System.out.println(username + " sent file: " + mainControl.getPath());

                fNameList.add(mainControl.getFileName());
                fileList.add(mainControl.getFile());
                mainControl.updateFileBox(fNameList);

                mainControl.setHasFile(false);

            }

            if(mainControl.getWantsToDownload()){
                downloadFile(mainControl.getSelectedPos());
                mainControl.setWantsToDownload(false);
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static void typeHandler(JSONObject inCheck) {
        String status = (String) inCheck.get("status");
        String aUser = (String) inCheck.get("user");
        String aKey = (String) inCheck.get("pubkey");
        if (status.equals("exchange")) {
            //add the new users info
            if (!username.equals(aUser)) {
                usersAndKeys.add(aUser);
                usersAndKeys.add(aKey);
                try {
                    secKeys = secObj.loadSecKeys(usersAndKeys);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvalidKeySpecException ex) {
                    Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvalidKeyException ex) {
                    Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //update the GUI
            mainControl.updateConnectedBox(username, usersAndKeys);
            //respond to the new user with our public key
            JSONObject respondCon = new JSONObject();
            respondCon.put("user", username);
            respondCon.put("pubkey", secObj.getPubKey());
            respondCon.put("status", "exchange2");
            String nonceAsString2 = Base64.getEncoder().encodeToString(cache.getNonce());
            respondCon.put("nonce", nonceAsString2);
            String respondKey = respondCon.toJSON();
            System.out.println("response2?");
            out.println(respondKey);
        }
        if (status.equals("exchange2")) {
            //we don't need to send anything out now
            //because this is the response from the other clients
            //add the new user and their public key
            usersAndKeys.add(inCheck.getString("user"));
            usersAndKeys.add(inCheck.getString("pubkey"));
            //update the GUI
            mainControl.updateConnectedBox(username, usersAndKeys);
            try {
                secKeys = secObj.loadSecKeys(usersAndKeys);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeySpecException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        //help from https://mkyong.com/java/how-to-convert-file-into-an-array-of-bytes/
        if (status.equals("file")) {
            //bFile is the file in byte representation
            byte[] bFile = new byte[0];
            try {
                bFile = Base64.getDecoder().decode(secObj.decryptFilePayload(username, inCheck));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } catch (NoSuchPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidKeyException e) {
                throw new RuntimeException(e);
            } catch (IllegalBlockSizeException e) {
                throw new RuntimeException(e);
            } catch (BadPaddingException e) {
                throw new RuntimeException(e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new RuntimeException(e);
            }
            fileList.add(bFile);
            //gets the name of the file
            fNameList.add(inCheck.getString("FileName"));
            mainControl.updateFileBox(fNameList);
        }
        
        if(status.equals("message")){
            try {
                String forGUI = secObj.decryptPayload(username, inCheck);
                mainControl.updateMessageBox(forGUI);
                
                
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchPaddingException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidAlgorithmParameterException ex) {
                Logger.getLogger(ClientDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

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
            config = new clientConfig(configObj);
        } catch (InvalidObjectException ex) {
            System.out.println("Invalid configuration file.");
            System.exit(1);
        }

    }

    public static String createMessage(String username, String pass) {
        JSONObject payload = new JSONObject();
        payload.put("username", username);
        payload.put("password", pass);
        payload.put("status", "create");
        String nonceAsString = Base64.getEncoder().encodeToString(cache.getNonce());
        payload.put("nonce",nonceAsString);
        return payload.toJSON();

    }

    public static String loginMessage(String username, String pass, String otp) {
        JSONObject payload = new JSONObject();
        payload.put("username", username);
        payload.put("password", pass);
        payload.put("otp", otp);
        payload.put("status", "login");
        String nonceAsString = Base64.getEncoder().encodeToString(cache.getNonce());
        payload.put("nonce", nonceAsString);
        return payload.toJSON();
    }

    /**
     *
     * @param pos the position in the arraylists of the file that we are downloading
     */
    private static void downloadFile(int pos){
        try {
            System.out.print("trying to save file...");
            byte[] bFile = fileList.get(pos);
            //path is the path being saved to
            String path = System.getProperty("user.dir");
            System.out.println(path);
            File outputFile = new File(username+"_Copy_Of_"+fNameList.get(pos));
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                outputStream.write(bFile);
            };
            System.out.println("saved file to: " + path + "\\" + username + "CopyOf" + fNameList.get(pos));
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
