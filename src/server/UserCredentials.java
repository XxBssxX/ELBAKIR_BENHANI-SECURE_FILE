package server;

import java.util.HashMap;
import java.util.Map;


public class UserCredentials {
    
    private static final Map<String, String> credentials = new HashMap<>();
    
    static {
        
        credentials.put("bassam", "bassam123");
        credentials.put("mohammed", "mohammed123");
    }
    
    /**
     
     * @param login 
     * @param password 
     * @return 
     */
    public static boolean validate(String login, String password) {
        String storedPassword = credentials.get(login);
        return storedPassword != null && storedPassword.equals(password);
    }
       
    public static void addUser(String login, String password) {
        credentials.put(login, password);
    }
}


