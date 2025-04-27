package network;

import java.util.Base64;

public class BasicAuth {
    String username;
    String password;

    public BasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    //prob not safe for side-channel-attacks, but idc
    public boolean validate(byte[] base64Auth){
        try {
            byte[] s = Base64.getDecoder().decode(base64Auth);
            String[] credentials = new String(s).split(":");

            if(credentials.length != 2){
                return false;
            }

            return credentials[0].equals(username) && credentials[1].equals(password);
        }catch (IllegalArgumentException e){
            return false;
        }
    }
}
