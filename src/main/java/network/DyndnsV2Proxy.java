package network;

import fi.iki.elonen.NanoHTTPD;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DyndnsV2Proxy extends NanoHTTPD {

    static Response responseAuthError = newFixedLengthResponse(Response.Status.OK, "text/plain", "badauth");
    static Response responseNoHosts = newFixedLengthResponse(Response.Status.OK, "text/plain", "notfqdn");
    static Response responseGoodLocalHostAndMalformedRequest = newFixedLengthResponse(Response.Status.OK, "text/plain", "good 127.0.0.1");
    static Response response911 = newFixedLengthResponse(Response.Status.OK, "text/plain", "911");

    Logger logger = Logger.getLogger("Log");
    BasicAuth basicAuth;
    String cloudflareZoneID, cloudflareEmail,cloudflareApiKey;
    boolean logTime2Comments;

    public DyndnsV2Proxy(String ip, int port, BasicAuth basicAuth, boolean logTime2Comments, String cloudflareZoneID, String cloudflareEmail, String cloudflareApiKey) throws IOException {
        super(ip, port);
        this.basicAuth = basicAuth;
        this.cloudflareZoneID = cloudflareZoneID;
        this.cloudflareEmail = cloudflareEmail;
        this.cloudflareApiKey = cloudflareApiKey;
        this.logTime2Comments = logTime2Comments;
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
    }

    /**
     * create HTTPS server
     * */
    public DyndnsV2Proxy(String ip, int port, String path2KeyStore, String passphrase, BasicAuth basicAuth, boolean logTime2Comments, String cloudflareZoneID, String cloudflareEmail, String cloudflareApiKey) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        super(ip, port);
        this.basicAuth = basicAuth;
        this.cloudflareZoneID = cloudflareZoneID;
        this.cloudflareEmail = cloudflareEmail;
        this.cloudflareApiKey = cloudflareApiKey;
        this.logTime2Comments = logTime2Comments;

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = Files.newInputStream(Paths.get(path2KeyStore));
        keyStore.load(in, passphrase.toCharArray());

        if(keyStore.size() == 0){
            throw new RuntimeException("could not load keyStore");
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passphrase.toCharArray());
        makeSecure(makeSSLSocketFactory(keyStore, keyManagerFactory), null);

        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

    }



    @Override
    public Response serve(IHTTPSession session){

        //ignore uri

        //--------------------------
        //------authentication------
        //--------------------------

        if(basicAuth == null){
            if(session.getHeaders().get("authorization") != null){
                logger.warning("Request with authorization, even though it is disabled");
            }
        }else{
            String auth = session.getHeaders().get("authorization");


            if (auth == null){
                    logger.warning("Request without authorization, even though enabled");
                    return responseAuthError;
            }

            String[] authSplit = auth.split(" ");

            if(authSplit.length > 0 && !authSplit[0].equals("Basic")){
                logger.warning("Request without non-Basic authorization format");
                return responseAuthError;
            }

            if(authSplit.length != 2){
                logger.warning("Request without bad authorization format");
                return responseAuthError;
            }

            if(!basicAuth.validate(authSplit[1].getBytes(StandardCharsets.UTF_8))){
                logger.warning("authorization failed. Wrong username or password");
                return responseAuthError;
            }
        }

        //------------------------------
        //------extract parameters------
        //------------------------------

        //ip:
        String ipString = session.getParms().get("myip");

        InetAddress ip = extractInetAddress(ipString);

        if(ip == null){
            String xForwardedFor = session.getHeaders().get("X-Forwarded-For");

            if(xForwardedFor != null){
                ip = extractInetAddress(xForwardedFor.split(",")[0]);
            }else{
                ip = extractInetAddress(session.getHeaders().get("http-client-ip"));
            }
        }

        if(ip == null){
            logger.warning("cant access header back-up-header http-client-ip or it isnt correctly formatted");
            return responseGoodLocalHostAndMalformedRequest;
        }


        //hostnames:
        String hostnameString = session.getParms().get("hostname");

        if(hostnameString == null){
            return responseNoHosts;
        }

        String[] hostNameSplit = hostnameString.split(",");
        List<String> hostnames = new ArrayList<>(hostNameSplit.length);

        //preserves order
        for (int i = 0; i < hostNameSplit.length; i++) {
            hostnames.add(hostNameSplit[i].trim());
        }

        // others parameters will be ignored (wildcard, mx, backmx, system, url and offline (offline is the only one with use)

        if(hostnames.size() == 0){
            return responseNoHosts;
        }

        //----------------------------------
        //------Get Cloudflare Records------
        //----------------------------------

        CloudflareRecord.RecordType type;

        if(ip instanceof Inet4Address){
            type = CloudflareRecord.RecordType.A;
        }else if (ip instanceof Inet6Address){
            type = CloudflareRecord.RecordType.AAAA;
        }else{
            logger.warning("Unknown Type of ip Address: " + ip);
            return responseGoodLocalHostAndMalformedRequest;
        }

        List<CloudflareRecord> records = getRecords(type, hostnames, cloudflareZoneID, cloudflareEmail, cloudflareApiKey);

        if(records == null){
            return response911;
        }

        //-------------------------------------
        //------Update Cloudflare Records------
        //-------------------------------------



        //TODO

        //TODO LogTime2Comments optional nicht vergessen


        return newFixedLengthResponse("test");
    }

    public List<CloudflareRecord> getRecords(CloudflareRecord.RecordType type, List<String> hostnames, String cloudflareZoneID, String cloudflareEmail, String cloudflareApiKey) {
        //request Data
        //build hostname query
        URLQueryBuilder queryBuilder = new URLQueryBuilder();
        queryBuilder.appendQueryParameter("match", "any");
        queryBuilder.appendQueryParameter("per_page", "5000000"); //maximum of records... idk how to go to next side...

        //"@abc" is a wildcard for ....abc (@ is the only thing my router accepts)
        //weird escape thingies:
        //      use n times the character 'X' followed by '@' for escaping and n-1 times the character 'X':
        //          "@bla"          ->  wildcard on .... ending with "bla"
        //          "XXX@bla"       ->  concrete address "XX@bla"
        //          "XXtest@bla"    ->  concrete address "XXtest@bla"

        for (String hostname : hostnames) {
            if (hostname.startsWith("@")){
                //wildcard
                queryBuilder.appendQueryParameter("name.endswith", hostname.substring(1));
            } else if (hostname.matches("X+@.*")) { //one or more X at the beginning, followed by @ and any sequence of characters
                //escaped hostname
                queryBuilder.appendQueryParameter("name", hostname.substring(1));
            }else{
                //normal hostname
                queryBuilder.appendQueryParameter("name", hostname);
            }
        }

        //HTTPS Request
        try {
            String cloudFlareApiURL = String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records%s", cloudflareZoneID, queryBuilder.toString());
            HttpsURLConnection httpsConnection;
            httpsConnection = (HttpsURLConnection) new URL(cloudFlareApiURL).openConnection();
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setRequestProperty("X-Auth-Email", String.format("%s", cloudflareEmail));
            httpsConnection.setRequestProperty("X-Auth-Key", String.format("%s", cloudflareApiKey));

            int responseCode = httpsConnection.getResponseCode();

            if(responseCode != 200){
                logger.severe("error code from Cloudflare: " + responseCode + ", " + DyndnsV2Proxy.getRequestStreamString(httpsConnection.getErrorStream()));
                return null;
            }
            String responseString = DyndnsV2Proxy.getRequestStreamString(httpsConnection.getInputStream());

            //parse JSON to CloudflareRecord
            try {
                JSONObject responseJSON = new JSONObject(responseString);

                if(!responseJSON.getBoolean("success")) {
                    logger.severe("success:false? " + ", " + responseString);
                    return null;
                }
                JSONArray results = responseJSON.getJSONArray("result");

                List<CloudflareRecord> records = new ArrayList<>(results.length());

                for (int i = 0; i < results.length(); i++) {
                       CloudflareRecord r = CloudflareRecord.parseFromJSON((JSONObject) results.get(i));
                       if(r != null && r.type == type){
                           records.add(r);
                       }
                }

                return records;

            }catch (JSONException e){
                logger.log(Level.SEVERE, e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null; //return null to indicate critical error (Response 911)
        }
    }

    public static String getRequestStreamString(InputStream inputStream) throws IOException {
        //get response text
        String response;
        {
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader in = new BufferedReader(reader);
            StringBuilder responseBuffer = new StringBuilder();

            char[] buf = new char[1024];
            int length = 0;
            while((length = in.read(buf, 0, 1024)) != -1){
                responseBuffer.append(buf, 0, length);
            }
            in.close();
            response = responseBuffer.toString();
        }
        return response;
    }

    public static InetAddress extractInetAddress(String ip){
        if(ip == null) return null;
        try {
            final InetAddress inet = InetAddress.getByName(ip);
            if(inet.getHostAddress().equalsIgnoreCase(ip)){
                return inet;
            }else{
                return null;
            }
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
