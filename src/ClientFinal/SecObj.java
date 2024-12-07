/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientFinal;

import csc3055.json.types.JSONArray;
import csc3055.json.types.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author burli
 */
public class SecObj {

    public ArrayList<String> clientsKeys;
    public KeyAgreement ecdhKex;
    public KeyPair pair;
    public SecureRandom rand;

    public SecObj() throws NoSuchAlgorithmException {
        Security.addProvider(new BouncyCastleProvider());
        clientsKeys = new ArrayList<>();
        ecdhKex = KeyAgreement.getInstance("ECDH");
        pair = genKeyPair();
        rand = new SecureRandom();
    }

    //takes an Arraylist of public keys as string and loads them into clientKeys
    //keys will be formatted like: string username, string key, string username, string key
    //returns ArrayList with format: user, sharedSecret, user sharedSecret
    public ArrayList<String> loadSecKeys(ArrayList<String> keys) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        String aUser = "";
        String b64PubValue = "";
        for (int i = 0; i < keys.size(); i++) {
            if (i % 2 == 0) {
                aUser = keys.get(i);
            } else {
                b64PubValue = keys.get(i);
                // Load the public value from the other side.
                X509EncodedKeySpec spec = new X509EncodedKeySpec(
                        Base64.getDecoder().decode(b64PubValue));
                PublicKey pubKey = KeyFactory.getInstance("EC").generatePublic(spec);
                ecdhKex.init(pair.getPrivate());
                ecdhKex.doPhase(pubKey, true);
                byte[] secret = ecdhKex.generateSecret();
                String secret64 = Base64.getEncoder().encodeToString(secret);
                clientsKeys.add(aUser);
                clientsKeys.add(secret64);
            }
        }
        return clientsKeys;
    }

    public KeyPair genKeyPair() throws NoSuchAlgorithmException {
        // Generate a pair of Elliptic Curve keys.
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        pair = generator.generateKeyPair();
        return pair;
    }

    public String getPubKey() {
        return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    }

    public String decryptPayload(String myUser, JSONObject payload) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] sharedSecret = null;
        //receive the payload and extract variables for decryption
        String sender = payload.getString("sender");
        JSONObject msg = (JSONObject) payload.get(myUser);
        String iv64 = msg.getString("iv");
        String iv642 = msg.getString("iv2");
        String encKey64 = msg.getString("key");
        String msgEnc = msg.getString("msg");
        //get the shared secret for with the sender
        for (int i = 0; i < clientsKeys.size(); i++) {
            if (sender.equals(clientsKeys.get(i))) {
                sharedSecret = Base64.getDecoder().decode(clientsKeys.get(i + 1));
            }

        }
        if (sharedSecret == null) {
            System.out.println("We do not share a key with this sender, unknown sender");
        }

        byte[] aesKey = Base64.getDecoder().decode(encKey64);
        Cipher sharedCiph = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParams2 = new GCMParameterSpec(128, Base64.getDecoder().decode(iv642));
        sharedCiph.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), gcmParams2);
        byte[] decKey = sharedCiph.doFinal(aesKey);
        //use the decrypted key to decrypt the message
        SecretKey aKey = new SecretKeySpec(decKey, "AES");
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        //tag size 128 from before and the iv from before
        GCMParameterSpec gcmParams = new GCMParameterSpec(128, Base64.getDecoder().decode(iv64));
        aesCipher.init(Cipher.DECRYPT_MODE, aKey, gcmParams);
        // Finalize the message.
        byte[] plaintext = aesCipher.doFinal(Base64.getDecoder().decode(msgEnc));
        String theMessage = new String(plaintext, StandardCharsets.US_ASCII);

        //format for the gui
        String toReturn = sender + ": " + theMessage;

        return toReturn;
    }

    public String decryptFilePayload(String myUser, JSONObject payload) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        byte[] sharedSecret = null;
        //receive the payload and extract variables for decryption
        String sender = payload.getString("sender");
        JSONObject msg = (JSONObject) payload.get(myUser);
        String iv64 = msg.getString("iv");
        String iv642 = msg.getString("iv2");
        String encKey64 = msg.getString("key");
        String msgEnc = msg.getString("msg");
        //get the shared secret for with the sender
        for (int i = 0; i < clientsKeys.size(); i++) {
            if (sender.equals(clientsKeys.get(i))) {
                sharedSecret = Base64.getDecoder().decode(clientsKeys.get(i + 1));
            }

        }
        if (sharedSecret == null) {
            System.out.println("We do not share a key with this sender, unknown sender");
        }

        byte[] aesKey = Base64.getDecoder().decode(encKey64);
        Cipher sharedCiph = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParams2 = new GCMParameterSpec(128, Base64.getDecoder().decode(iv642));
        sharedCiph.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), gcmParams2);
        byte[] decKey = sharedCiph.doFinal(aesKey);
        //use the decrypted key to decrypt the message
        SecretKey aKey = new SecretKeySpec(decKey, "AES");
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        //tag size 128 from before and the iv from before
        GCMParameterSpec gcmParams = new GCMParameterSpec(128, Base64.getDecoder().decode(iv64));
        aesCipher.init(Cipher.DECRYPT_MODE, aKey, gcmParams);
        // Finalize the message.
        byte[] plaintext = aesCipher.doFinal(Base64.getDecoder().decode(msgEnc));
        String theMessage = new String(plaintext, StandardCharsets.US_ASCII);

        //format for the gui
        String toReturn = theMessage;

        return toReturn;
    }

    public JSONObject encryptedPayload(String myUser, String message, String nonce) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //make the payload object
        JSONObject payload = new JSONObject();
        String aUser = "";

        for (int i = 0; i < clientsKeys.size(); i++) {
            if (i % 2 == 0) {
                aUser = clientsKeys.get(i);
            } else {
                //make an array for each receiver
                JSONObject entry = new JSONObject();

                byte[] sharedSecret = Base64.getDecoder().decode(clientsKeys.get(i));

                //generate a session key that will encrypt the message
                int tagSize = 128;
                // Set up an AES cipher object.
                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");

                // Get a key generator object.
                KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");

                // Set the key size to 128 bits.
                aesKeyGen.init(128);

                // Generate the key.
                Key aesKey = aesKeyGen.generateKey();

                byte[] rawIv = new byte[16];		// Block size of AES.
                rand.nextBytes(rawIv);					// Fill the array with random bytes.
                GCMParameterSpec gcmParams = new GCMParameterSpec(tagSize, rawIv);

                // Put the cipher in encrypt mode with the specified key.
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParams);

                byte[] ciphertext = aesCipher.doFinal(message.getBytes(StandardCharsets.US_ASCII));

                //encrypt the key with the shared secret
                Cipher sharedCiph = Cipher.getInstance("AES/GCM/NoPadding");

                byte[] rawIv2 = new byte[16];		// Block size of AES.
                rand.nextBytes(rawIv2);					// Fill the array with random bytes.
                GCMParameterSpec gcmParams2 = new GCMParameterSpec(128, rawIv2);

                sharedCiph.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), gcmParams2);
                byte[] encKey = sharedCiph.doFinal(aesKey.getEncoded());

                //create variables for the payload
                String iv64 = Base64.getEncoder().encodeToString(rawIv);
                String iv642 = Base64.getEncoder().encodeToString(rawIv2);
                String ciphText = Base64.getEncoder().encodeToString(ciphertext);
                String encKey64 = Base64.getEncoder().encodeToString(encKey);
                //put this entry in the JSONObject
                entry.put("iv", iv64);
                entry.put("iv2", iv642);
                entry.put("msg", ciphText);
                entry.put("key", encKey64);
                payload.put(aUser, entry);

            }
        }
        payload.put("status", "message");
        payload.put("sender", myUser);
        payload.put("nonce",nonce);
        return payload;

    }

    public JSONObject encryptedFilePayload(String myUser, String message, String nonce) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        //make the payload object
        JSONObject payload = new JSONObject();
        String aUser = "";

        for (int i = 0; i < clientsKeys.size(); i++) {
            if (i % 2 == 0) {
                aUser = clientsKeys.get(i);
            } else {
                //make an array for each receiver
                JSONObject entry = new JSONObject();

                byte[] sharedSecret = Base64.getDecoder().decode(clientsKeys.get(i));

                //generate a session key that will encrypt the message
                int tagSize = 128;
                // Set up an AES cipher object.
                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");

                // Get a key generator object.
                KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");

                // Set the key size to 128 bits.
                aesKeyGen.init(128);

                // Generate the key.
                Key aesKey = aesKeyGen.generateKey();

                byte[] rawIv = new byte[16];		// Block size of AES.
                rand.nextBytes(rawIv);					// Fill the array with random bytes.
                GCMParameterSpec gcmParams = new GCMParameterSpec(tagSize, rawIv);

                // Put the cipher in encrypt mode with the specified key.
                aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParams);

                byte[] ciphertext = aesCipher.doFinal(message.getBytes(StandardCharsets.US_ASCII));

                //encrypt the key with the shared secret
                Cipher sharedCiph = Cipher.getInstance("AES/GCM/NoPadding");
                byte[] rawIv2 = new byte[16];		// Block size of AES.
                rand.nextBytes(rawIv2);					// Fill the array with random bytes.
                GCMParameterSpec gcmParams2 = new GCMParameterSpec(128, rawIv2);
                sharedCiph.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), gcmParams2);
                byte[] encKey = sharedCiph.doFinal(aesKey.getEncoded());

                //create variables for the payload
                String iv64 = Base64.getEncoder().encodeToString(rawIv);
                String iv642 = Base64.getEncoder().encodeToString(rawIv2);
                String ciphText = Base64.getEncoder().encodeToString(ciphertext);
                String encKey64 = Base64.getEncoder().encodeToString(encKey);
                //put this entry in the JSONObject
                entry.put("iv", iv64);
                entry.put("iv2", iv642);
                entry.put("msg", ciphText);
                entry.put("key", encKey64);
                payload.put(aUser, entry);

            }
        }
        payload.put("status", "file");
        payload.put("sender", myUser);
        payload.put("nonce", nonce);
        return payload;

    }

}
