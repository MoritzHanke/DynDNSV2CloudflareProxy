package network;

import org.json.JSONException;
import org.json.JSONObject;

public class CloudflareRecord {
    enum RecordType {
        A, AAAA;
    };

    String id;
    String name;
    RecordType type;
    String content;
    //boolean proxiable;
    //boolean proxied;
    //int ttl;
    //String comment;


    private CloudflareRecord() {
    }

    public CloudflareRecord(String id, String name, RecordType type, String content) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.content = content;
    }

    public static CloudflareRecord parseFromJSON(JSONObject json){
        try {
            CloudflareRecord r = new CloudflareRecord();
            String type = json.getString("type");
            r.id = json.getString("id");
            r.name = json.getString("name");
            if(type.equalsIgnoreCase("A")){
                r.type = RecordType.A;
            }else if (type.equalsIgnoreCase("AAAA")){
                r.type = RecordType.AAAA;
            }else{
                return null;
            }
            r.content = json.getString("content");
            return r;
        }catch (JSONException e){
            return null;
        }
    }
}
