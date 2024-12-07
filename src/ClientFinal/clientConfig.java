/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientFinal;

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
public class clientConfig implements JSONSerializable {
    private String host;
    private int port;
    private String trustStore;
    private String trustStorePass;

    /**
     * Constructs a configuration object from the appropriate JSON Object.
     *
     * @param config the JSON formatted configuration object.
     * @throws InvalidObjectException if the config object is not valid.
     */
    public clientConfig(JSONObject config) throws InvalidObjectException {
        deserialize(config);
    }

    /**
     * Gets the port number from the configuration file.
     *
     * @return the port number the server should bind to.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Get the file associated with the secrets.
     *
     * @return the string representing the secret file.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Get the Truststore file name.
     *
     * @return the Truststore file name as a string.
     */
    public String getTrustStore() {
        return "test/" + this.trustStore;
    }

    /**
     * Gets the Truststore password used to protect the Truststore.
     */
    public String getTrustStorePass() {
        return this.trustStorePass;
    }

    /**
     * Serializes the object into a JSON encoded string.
     *
     * @return a string representing the JSON form of the object.
     */
    public String serialize() {
        return toJSONType().getFormattedJSON();
    }

    /**
     * Coverts json data to an object of this type.
     *
     * @param obj a JSON type to deserialize.
     * @throws InvalidObjectException the type does not match this object.
     */
    public void deserialize(JSONType obj) throws InvalidObjectException {
        JSONObject config;
        if (obj instanceof JSONObject) {
            config = (JSONObject) obj;

            // Get the path to the secrets file.
            if (!config.containsKey("host")) {
                throw new InvalidObjectException(
                        "Invalid host");
            } else {
                host = config.getString("host");
            }

            // Get the port to bind to.
            if (!config.containsKey("port")) {
                throw new InvalidObjectException(
                        "Invalid configuration file -- no port.");
            } else {
                port = config.getInt("port");
            }

            // Get the truststore.
            if (!config.containsKey("truststore-file")) {
                throw new InvalidObjectException(
                        "Invalid configuration file -- no truststore-file.");
            } else {
                trustStore = config.getString("truststore-file");
            }

            // Get the truststore password
            if (!config.containsKey("truststore-pass")) {
                throw new InvalidObjectException(
                        "Invalid configuration file -- no truststore-pass.");
            } else {
                trustStorePass = config.getString("truststore-pass");
            }
        }
    }

    /**
     * Converts the object to a JSON type.
     *
     * @return a JSON type either JSONObject or JSONArray.
     */
    public JSONType toJSONType() {
        JSONObject obj = new JSONObject();

        obj.put("host", host);
        obj.put("port", port);
        obj.put("truststore-file", trustStore);
        obj.put("truststore-pass", trustStorePass);

        return obj;
    }

}
