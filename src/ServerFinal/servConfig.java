/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ServerFinal;

import csc3055.json.JSONSerializable;
import csc3055.json.types.JSONObject;
import csc3055.json.types.JSONObject;
import csc3055.json.types.JSONType;
import csc3055.json.JSONSerializable;
import java.io.InvalidObjectException;

/**
 *
 * @author burli
 */
public class servConfig implements JSONSerializable {
    

/**
 * This class represents the configuration data for the server.
 * @author Zach Kissel
 */

   private String passFile;
   private int port;
   private boolean doDebug;
   private String keyStore;
   private String keyStorePass;

   /**
    * Constructs a configuration object from the appropriate JSON Object.
    * @param config the JSON formatted configuration object.
    * @throws InvalidObjectException if the config object is not valid.
    */
   public servConfig(JSONObject config) throws InvalidObjectException
   {
     deserialize(config);
   }

   /**
    * Gets the port number from the configuration file.
    * @return the port number the server should bind to.
    */
   public int getPort()
   {
     return this.port;
   }

   /**
    * Get the file associated with the secrets.
    * @return the string representing the secret file.
    */
    public String getPassFile()
    {
      return this.passFile;
    }

    /**
     * Get the keystore file name.
     * @return the keystore file name as a string.
     */
    public String getKeyStore()
    {
      return "test/" + this.keyStore;
    }

    /**
     * Gets the keystore password used to protect the keystore.
     */
    public String getKeyStorePass()
    {
      return this.keyStorePass;
    }

    /**
     * Check if debugging is turned on.
     * @return true if debugging is requested; otherwise, false.
     */
    public boolean doDebug()
    {
      return this.doDebug;
    }

   /**
    * Serializes the object into a JSON encoded string.
    * @return a string representing the JSON form of the object.
    */
   public String serialize()
   {
     return toJSONType().getFormattedJSON();
   }

   /**
    * Coverts json data to an object of this type.
    * @param obj a JSON type to deserialize.
    * @throws InvalidObjectException the type does not match this object.
    */
   public void deserialize(JSONType obj) throws InvalidObjectException
   {
     JSONObject config;
     if (obj instanceof JSONObject)
     {
       config = (JSONObject) obj;

       // Get the path to the secrets file.
       if (!config.containsKey("password-file"))
         throw new InvalidObjectException(
            "Invalid configuration file -- no password-file.");
       else
         passFile = config.getString("password-file");

       // Get the port to bind to.
       if (!config.containsKey("port"))
         throw new InvalidObjectException(
            "Invalid configuration file -- no port.");
       else
         port = config.getInt("port");

       // Get the keystore.
       if (!config.containsKey("keystore-file"))
         throw new InvalidObjectException(
            "Invalid configuration file -- no keystore-file.");
       else
         keyStore = config.getString("keystore-file");

       // Get the keystore password
       if (!config.containsKey("keystore-pass"))
         throw new InvalidObjectException(
            "Invalid configuration file -- no keystore-pass.");
       else
         keyStorePass = config.getString("keystore-pass");

      // There is an option debug flag that turns on debugging.
      if (config.containsKey("debug"))
        doDebug = config.getBoolean("debug");
      else
        doDebug = false;


     }
     else
       throw new InvalidObjectException(
          "Configuration -- recieved array, expected Object.");
   }

   /**
    * Converts the object to a JSON type.
    * @return a JSON type either JSONObject or JSONArray.
    */
   public JSONType toJSONType()
   {
     JSONObject obj = new JSONObject();

     obj.put("password-file", passFile);
     obj.put("port", port);
     obj.put("debug", doDebug);
     obj.put("keystore-file", keyStore);
     obj.put("keystore-pass", keyStorePass);

     return obj;
   }

 }
    

