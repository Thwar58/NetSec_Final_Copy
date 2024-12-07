package ServerFinal;

import csc3055.json.JSONSerializable;
import csc3055.json.types.JSONArray;
import csc3055.json.types.JSONObject;
import csc3055.json.types.JSONType;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidKeyException;
import org.bouncycastle.jcajce.spec.ScryptKeySpec;
import java.io.InvalidObjectException;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author burli
 */
public class passwd implements JSONSerializable {

    private ArrayList<accEntries> entries;
    private FileWriter file;

    //for creating an initial json
    public passwd() throws IOException {
        Security.addProvider(new BouncyCastleProvider());
        this.entries = new ArrayList<accEntries>();

    }

    //for using an existing json
    public passwd(JSONObject obj) throws InvalidObjectException {
        Security.addProvider(new BouncyCastleProvider());
        this.entries = new ArrayList<accEntries>();
        deserialize(obj);
    }

    public boolean isEmpty() {
        if (this.entries.isEmpty()) {
            return true;
        }
        return false;
    }

    public boolean userExists(String user) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getUser().equals(user)) {
                return true;
            }
        }
        return false;
    }

    public void addEntry(accEntries entry) {
        this.entries.add(entry);

    }

    public accEntries getEntryByUser(String user) {
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.entries.get(i).getUser().equals(user)) {
                return this.entries.get(i);
            }
        }
        return null;
    }

    @Override
    public String serialize() {
        return toJSONType().getFormattedJSON();
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        JSONObject tmp;
        JSONArray entriesArray = new JSONArray();

        if (obj instanceof JSONObject) {
            tmp = (JSONObject) obj;
            if (tmp.containsKey("entries")) {
                entriesArray = tmp.getArray("entries");
            }
        } else {
            throw new InvalidObjectException("Passwd object error");
        }
        for (int i = 0; i < entriesArray.size(); i++) {
            JSONObject currEntry = entriesArray.getObject(i);
            accEntries anEntry = new accEntries();
            anEntry.deserialize(currEntry);
            entries.add(anEntry);

        }
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        JSONArray entriesArray = new JSONArray();
        for (accEntries entry : entries) {
            entriesArray.add(entry.toJSONType());
        }

        obj.put("entries", entriesArray);
        return obj;

    }

    /**
     * This method takes a password as a string an derives a hash from it using
     * SCRYPT.
     *
     * @param pass a non-empty string representing the password.
     * @return the Base-64 encoded form of the hash.
     */
    public String deriveHash(String pass, String salt) {
        assert (!pass.isEmpty());
        byte[] hash;

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("SCRYPT");
            ScryptKeySpec spec = new ScryptKeySpec(pass.toCharArray(), Base64.getDecoder().decode(salt), 2048, 8, 1, 128);
            hash = skf.generateSecret(spec).getEncoded();

            // Return the hash to the caller.
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException nae) {
            nae.printStackTrace();
        } catch (InvalidKeySpecException kse) {
            kse.printStackTrace();
        }

        // Should not be reached.
        return null;
    }

}
