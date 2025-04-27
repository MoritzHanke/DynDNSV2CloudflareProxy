package network;

import java.util.ArrayList;

public class URLQueryBuilder {

    private char sep = '=';
    private ArrayList<String> query = new ArrayList<>();

    public URLQueryBuilder() {
    }

    public URLQueryBuilder(char sep) {
        this.sep = sep;
    }

    public boolean appendQueryParameter(String name, String value){
        if(name == null || value == null){
            return false;
        }
        query.add(name);
        query.add(value);
        return true;
    }

    @Override
    public String toString() {
        if(query.size() == 0){
            return "";
        }

        StringBuilder s = new StringBuilder();

        String queryParamSep = "?"; //sep = seperator for name and value; queryParamSep is the ? between different parameters
        for (int i = 0; i < query.size()/2; i++){
            s.append(queryParamSep);
            s.append(query.get(i*2));
            s.append(sep);
            s.append(query.get(i*2+1));
            queryParamSep = "&";
        }

        s.append('#');

        return s.toString();
    }
}
