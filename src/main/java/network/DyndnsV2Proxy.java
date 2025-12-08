package network;

import fi.iki.elonen.NanoHTTPD;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DyndnsV2Proxy extends NanoHTTPD {

    static Response responseAuthError(){ return newFixedLengthResponse(Response.Status.OK, "text/plain", "badauth");}
    static Response responseNoHosts(){ return newFixedLengthResponse(Response.Status.OK, "text/plain", "notfqdn");}
    static Response responseGoodLocalHostAndMalformedRequest(){ return newFixedLengthResponse(Response.Status.OK, "text/plain", "good 127.0.0.1");}
    static Response response911(){ return newFixedLengthResponse(Response.Status.OK, "text/plain", "911");}

    Logger logger = Logger.getLogger("Log");
    BasicAuth basicAuth;
    String cloudflareZoneID, cloudflareToken;
    boolean logTime2Comments;

    List<Pair<Boolean, String>> evaluated_hostname_filter;

    private DyndnsV2Proxy(String hostname, int port) {
        super(hostname, port);
    }

    public static DyndnsV2Proxy genHTTPDyndnsV2Proxy(String ip, int port, BasicAuth basicAuth, boolean logTime2Comments, String cloudflareZoneID, String cloudflareToken, List<String> hostname_filter) throws IOException {
        DyndnsV2Proxy proxy = new DyndnsV2Proxy(ip, port);
        proxy.basicAuth = basicAuth;
        proxy.cloudflareZoneID = cloudflareZoneID;
        proxy.cloudflareToken = cloudflareToken;
        proxy.logTime2Comments = logTime2Comments;
        proxy.evaluated_hostname_filter = hostname_filter.stream().map(DyndnsV2Proxy::evaluateHostname).collect(Collectors.toList());
        proxy.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        return proxy;
    }

    /**
     * create HTTPS server
     * */
    public static DyndnsV2Proxy genHTTPSDyndnsV2Proxy(String ip, int port, String path2KeyStore, String passphrase, BasicAuth basicAuth, boolean logTime2Comments, String cloudflareZoneID, String cloudflareToken, List<String> hostname_filter) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        DyndnsV2Proxy proxy = new DyndnsV2Proxy(ip, port);
        proxy.basicAuth = basicAuth;
        proxy.cloudflareZoneID = cloudflareZoneID;
        proxy.cloudflareToken = cloudflareToken;
        proxy.logTime2Comments = logTime2Comments;
        proxy.evaluated_hostname_filter = hostname_filter.stream().map(DyndnsV2Proxy::evaluateHostname).collect(Collectors.toList());

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = Files.newInputStream(Paths.get(path2KeyStore));
        keyStore.load(in, passphrase.toCharArray());

        if(keyStore.size() == 0){
            throw new RuntimeException("could not load keyStore");
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passphrase.toCharArray());
        proxy.makeSecure(makeSSLSocketFactory(keyStore, keyManagerFactory), null);

        proxy.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        return proxy;
    }

    public static DyndnsV2Proxy genHTTPSDyndnsV2Proxy_GenKeyStore(String ip, int port, BasicAuth basicAuth, boolean logTime2Comments, String cloudflareZoneID, String cloudflareToken, List<String> hostname_filter) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, NoSuchProviderException, InvalidKeyException, SignatureException {
        DyndnsV2Proxy proxy = new DyndnsV2Proxy(ip, port);
        proxy.basicAuth = basicAuth;
        proxy.cloudflareZoneID = cloudflareZoneID;
        proxy.cloudflareToken = cloudflareToken;
        proxy.logTime2Comments = logTime2Comments;
        proxy.evaluated_hostname_filter = hostname_filter.stream().map(DyndnsV2Proxy::evaluateHostname).collect(Collectors.toList());

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null); //keystore will only exist at runtime in java memory. no need for password

        CertAndKeyGen gen = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        gen.generate(2048);
        PrivateKey privKey = gen.getPrivateKey();

        X509Certificate[] chain=new X509Certificate[1];
        chain[0]=gen.getSelfCertificate(new X500Name("CN=ROOT"), (long)1000*365*24*3600);
        //valid for 1000 years... bit overkill, but idk what happens if you dont restart the application in 3 years... maybe a bit unsafe, but who cares

        String password = UUID.randomUUID().toString();
        ks.setKeyEntry("temp", privKey, password.toCharArray(), chain);

        if(ks.size() == 0){
            throw new RuntimeException("could not load keyStore");
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, password.toCharArray());
        proxy.makeSecure(makeSSLSocketFactory(ks, keyManagerFactory), null);

        proxy.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        return proxy;
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
                    return responseAuthError();
            }

            String[] authSplit = auth.split(" ");

            if(authSplit.length > 0 && !authSplit[0].equalsIgnoreCase("Basic")){
                logger.warning("Request without non-Basic authorization format");
                return responseAuthError();
            }

            if(authSplit.length != 2){
                logger.warning("Request without bad authorization format");
                return responseAuthError();
            }

            if(!basicAuth.validate(authSplit[1].getBytes(StandardCharsets.UTF_8))){
                logger.warning("authorization failed. Wrong username or password");
                return responseAuthError();
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
            return responseGoodLocalHostAndMalformedRequest();
        }


        //hostnames:
        String hostnameString = session.getParms().get("hostname");

        if(hostnameString == null){
            return responseNoHosts();
        }

        String[] hostNameSplit = hostnameString.split(",");
        List<String> hostnames = new ArrayList<>(hostNameSplit.length);

        //preserves order (not guaranteed for streams)
        for (int i = 0; i < hostNameSplit.length; i++) {
            hostnames.add(hostNameSplit[i].trim());
        }

        // others parameters will be ignored (wildcard, mx, backmx, system, url and offline (offline is the only one with a use case)

        if(hostnames.size() == 0){
            return responseNoHosts();
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
            return responseGoodLocalHostAndMalformedRequest();
        }

        List<CloudflareRecord> records = getRecords(type, hostnames, cloudflareZoneID, cloudflareToken);

        if(records == null){
            return response911();
        }

        //-------------------------------------
        //------Update Cloudflare Records------
        //-------------------------------------

        //updates all or none
        List<CloudflareRecord> updatedRecords = updateRecords(ip, records, logTime2Comments, cloudflareZoneID, cloudflareToken);

        if (updatedRecords == null){
            return response911();
        }

        //--------------------------------
        //------Check Update Success------
        //--------------------------------

        boolean[] hostnameFoundSuccess = new boolean[hostnames.size()]; //is included in records
        boolean[] hostnameUpdateSuccess = new boolean[hostnames.size()]; //is included in updatedRecords

        //check for each hostname, if at least one matching updated Record was found

        for(int i = 0; i < hostnames.size(); i++){
            String hostname = hostnames.get(i);
            Pair<Boolean, String> evaluated_hostname = evaluateHostname(hostname); //(is_wildcard, hostname-truncated)

            //wildcard shit. see getRecords()
            //"@abc" is a wildcard for ....abc (@ is the only thing my router accepts)
            //weird escape thingies:
            //      use n times the character 'X' followed by '@' for escaping and n-1 times the character 'X':
            //          "@bla"          ->  wildcard on .... ending with "bla"
            //          "XXX@bla"       ->  concrete address "XX@bla"
            //          "XXtest@bla"    ->  concrete address "XXtest@bla"

            if (evaluated_hostname.getKey()){ //wildcard
                //wildcard
                for (CloudflareRecord updatedRecord : updatedRecords){
                    if(updatedRecord.name.toLowerCase().endsWith(evaluated_hostname.getValue().toLowerCase())){
                        hostnameUpdateSuccess[i] = true;
                        break;
                    }
                }
                for (CloudflareRecord r : records){
                    if(r.name.toLowerCase().endsWith(evaluated_hostname.getValue().toLowerCase())){
                        hostnameFoundSuccess[i] = true;
                        break;
                    }
                }
            } else {

                //escaped hostname or regular hostname
                for (CloudflareRecord updatedRecord : updatedRecords){
                    if(updatedRecord.name.equalsIgnoreCase(evaluated_hostname.getValue())){
                        hostnameUpdateSuccess[i] = true;
                        break;
                    }
                }
                for (CloudflareRecord r : records){
                    if(r.name.equalsIgnoreCase(evaluated_hostname.getValue())){
                        hostnameFoundSuccess[i] = true;
                        break;
                    }
                }
            }
        }

        //--------------------------
        //------Build Response------
        //--------------------------

        StringBuilder response = new StringBuilder();
        for (int i = 0; i < hostnames.size(); i++) {
            if(hostnameUpdateSuccess[i]){
                response.append("good ");
                response.append(hostnames.get(i));
                response.append("\n");
            }else {
                if(hostnameFoundSuccess[i]){
                    response.append("nochg ");
                    response.append(hostnames.get(i));
                    response.append("\n");
                }else{
                    response.append("nohost ");
                    response.append(hostnames.get(i));
                    response.append("\n");
                }
            }
        }

        return newFixedLengthResponse(response.toString());
    }

    public List<CloudflareRecord> updateRecords(InetAddress ipAddress, List<CloudflareRecord> toUpdate, boolean logTime2Comments, String cloudflareZoneID, String cloudflareToken){
        List<CloudflareRecord> updatedRecords = new ArrayList<>(toUpdate.size());
        try {
            String cloudFlareApiURL = String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records/batch", cloudflareZoneID);
            HttpsURLConnection httpsConnection;
            httpsConnection = (HttpsURLConnection) new URL(cloudFlareApiURL).openConnection();
            httpsConnection.setRequestMethod("POST");
            httpsConnection.setRequestProperty("Content-Type", "application/json");
            httpsConnection.setRequestProperty("Authorization", "Bearer " + cloudflareToken);

            //body
            JSONArray toModifyJSON = new JSONArray();
            JSONObject updateJSON = new JSONObject();
            //check indentfactro
            boolean sendsmth = false;

            for (CloudflareRecord record : toUpdate) {
                JSONObject obj = new JSONObject();
                obj.put("id", record.id);

                if (record.content.equalsIgnoreCase(ipAddress.getHostAddress())){
                    continue; //dont update, not changed (abusive behaviour)
                }else{
                    obj.put("content", ipAddress.getHostAddress());
                    if(logTime2Comments){
                        obj.put("comment", "Updated on: " + LocalDateTime.now().toString());
                    }
                }
                sendsmth = true;
                toModifyJSON.put(obj);
                obj.toString();
            }
            updateJSON.put("patches", toModifyJSON);

            if(!sendsmth){
                return updatedRecords;
            }

            httpsConnection.setDoOutput(true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(httpsConnection.getOutputStream()));
            writer.write(JSONWriter.valueToString(updateJSON));
            writer.close();

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
                JSONObject results = responseJSON.getJSONObject("result");
                JSONArray patches = results.getJSONArray("patches");

                for (int i = 0; i < patches.length(); i++) {
                    CloudflareRecord r = CloudflareRecord.parseFromJSON((JSONObject) patches.get(i));
                    updatedRecords.add(r);
                }

                return updatedRecords;

            }catch (JSONException e){
                logger.log(Level.SEVERE, e.getMessage(), e);
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null; //return null to indicate critical error (Response 911)
        }
    }

    public List<CloudflareRecord> getRecords(CloudflareRecord.RecordType type, List<String> hostnames, String cloudflareZoneID, String cloudflareToken) {
        //request Data
        //build hostname query
        URLQueryBuilder queryBuilder = new URLQueryBuilder();
        queryBuilder.appendQueryParameter("match", "any");
        queryBuilder.appendQueryParameter("per_page", "5000000"); //maximum of records... idk how to go to next side...

        //"@abc" is a wildcard for ....abc (@ is the only thing my router accepts)
        //weird escape thingies:
        //      use n times the character 'X' followed by '@' for escaping and n-1 times the character 'X':
        //          "@bla"          ->  wildcard on .... ending with "bla"
        //          "XXX@bla"       ->  address "XX@bla"
        //          "XXtest@bla"    ->  address "XXtest@bla"

        for (String hostname : hostnames) {
            Pair<Boolean, String> evaluatedHostname = evaluateHostname(hostname);
            if(evaluatedHostname.getKey()) { //wildcard
                queryBuilder.appendQueryParameter("name.endswith", evaluatedHostname.getValue());
            }else{
                queryBuilder.appendQueryParameter("name", evaluatedHostname.getValue());
            }
        }

        //HTTPS Request
        try {
            String cloudFlareApiURL = String.format("https://api.cloudflare.com/client/v4/zones/%s/dns_records%s", cloudflareZoneID, queryBuilder.toString());
            HttpsURLConnection httpsConnection;
            httpsConnection = (HttpsURLConnection) new URL(cloudFlareApiURL).openConnection();
            httpsConnection.setRequestMethod("GET");
            httpsConnection.setRequestProperty("Authorization", "Bearer " + cloudflareToken);

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
                           //check if filtered out
                           boolean filtered_out = false;
                           for (Pair<Boolean, String> hostnameFilter : evaluated_hostname_filter){
                               if (hostnameFilter.getKey() && r.name.endsWith(hostnameFilter.getValue())){
                                   filtered_out = true; //filtered out by wildcard
                                   break;
                               }
                               if (!hostnameFilter.getKey() && r.name.equalsIgnoreCase(hostnameFilter.getValue())){
                                   filtered_out = true; //filtered out
                                   break;
                               }
                           }
                            if(!filtered_out){
                                records.add(r);
                            }
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

    public static Pair<Boolean, String> evaluateHostname(String hostname){
        //returns true + suffix for wildcard
        //and false + hostname for normal hostnames
        if (hostname.startsWith("@")){
            //wildcard
            return new Pair( true, hostname.substring(1));
        } else if (hostname.matches("X+@.*")) { //one or more X at the beginning, followed by @ and any sequence of characters
            //escaped hostname
            return new Pair( false, hostname.substring(1));
        }else{
            //normal hostname
            return new Pair( false, hostname);
        }
    }
}
