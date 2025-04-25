import com.sun.security.auth.login.ConfigFile;
import config.Config;
import server.DyndnsV2Proxy;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {

    static String[] configDefaults = {
        "runHTTP", "false",
        "HTTPPort", "80",
        /*"runHTTPS", "false",
        "HTTPsPort", "443",
        "keyStoreFilePath", "",
        "keyStorePassPhrase", "",
        "IPAddress", "127.0.0.1",*/ //HTTPS not supported
    };

    static String defConfigPath = "./proxy.conf";

    public static void main(String[] args) throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

        String configPath = System.getenv("CONFIG_PATH");
        Logger logger = Logger.getLogger("Log");

        try {
            FileHandler fh;
            // This block configure the logger with handler and formatter
            fh = new FileHandler("./dyndnsproxy.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }

        if(configPath == null){
            configPath = defConfigPath;
        }

        Properties properties;
        try {
            properties = Config.loadPropertiesFillDefaults(new File(configPath), configDefaults);
        }catch (Exception e){
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage(), e);
            System.err.println("error with Config. See StackTrace");
            return;
        }

        DyndnsV2Proxy httpServer = null;
        DyndnsV2Proxy httpsServer = null;

        String ip = properties.getProperty("IPAddress");

        if(ip == null){
            logger.log(Level.SEVERE, "missing IPAddress Field");
            System.err.println("missing IPAddress Field");
        }

        if(Boolean.parseBoolean(properties.getProperty("runHTTP"))){
            try {
                int port = Integer.parseInt(properties.getProperty("HTTPPort"));
                httpServer = new DyndnsV2Proxy(ip, port);
                System.out.printf("HTTPs-Proxy started. Listening on %s:%d",ip, port);
            }catch (Exception e){
                e.printStackTrace();
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        //HTTPS not supported (cant test this shit... )
        /*
        if(Boolean.parseBoolean(properties.getProperty("runHTTPS"))){
            try {
                int port = Integer.parseInt(properties.getProperty("HTTPsPort"));
                String keyStoreFilePath = properties.getProperty("keyStoreFilePath");
                String keyStorePassPhrase = properties.getProperty("keyStorePassPhrase");
                httpsServer = new DyndnsV2Proxy(ip, port, keyStoreFilePath, keyStorePassPhrase);
                System.out.printf("HTTPs-Proxy started. Listening on %s:%d",ip, port);
            }catch (Exception e){
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        */

    }


}
