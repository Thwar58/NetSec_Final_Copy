package ServerFinal;

import csc3055.json.JSONSerializable;
import csc3055.json.types.JSONArray;
import csc3055.json.types.JSONObject;
import csc3055.json.types.JSONType;
import java.io.InvalidObjectException;


/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author burli
 */
public class accEntries implements JSONSerializable{
    public String salt;
    public String pass;
    public String totp;
    public String user;
    
    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public String getTotp() {
        return totp;
    }

    public void setTotp(String totp) {
        this.totp = totp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String serialize() {
        return toJSONType().getFormattedJSON();
    }

    @Override
    public void deserialize(JSONType obj) throws InvalidObjectException {
        JSONObject tmp;
        JSONArray block = new JSONArray();
        if(obj instanceof JSONObject){
            tmp = (JSONObject)obj;
            if (tmp.containsKey("salt")){
                this.salt = tmp.getString("salt");
            }
            if (tmp.containsKey("pass")){
                this.pass = tmp.getString("pass");
            }
            if (tmp.containsKey("totp-key")){
                this.totp = tmp.getString("totp-key");
                
            }
            if (tmp.containsKey("user")){
                this.user = tmp.getString("user");
            }
        }
    }

    @Override
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();
        obj.put("salt", salt);
        obj.put("pass", pass);
        obj.put("totp-key", totp);
        obj.put("user", user);
        
        return obj;
    }
    
}
