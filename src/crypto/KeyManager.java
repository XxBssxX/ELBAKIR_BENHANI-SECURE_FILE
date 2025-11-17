package crypto;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


public class KeyManager {
    
    private static final String KEY_FILE = "shared_key.txt";
    
    public static SecretKey loadOrGenerateKey() throws Exception {
        File keyFile = new File(KEY_FILE);
        
        if (keyFile.exists()) {
            
            String keyString = new String(Files.readAllBytes(Paths.get(KEY_FILE)));
            keyString = keyString.trim();
            return CryptoUtils.stringToKey(keyString);
        } else {
           
            SecretKey key = CryptoUtils.generateKey();
            saveKey(key);
            return key;
        }
    }
    
    public static void saveKey(SecretKey key) throws Exception {
        String keyString = CryptoUtils.keyToString(key);
        try (PrintWriter writer = new PrintWriter(new FileWriter(KEY_FILE))) {
            writer.print(keyString);
        }
    }
    
    public static SecretKey loadKey() throws Exception {
        if (!new File(KEY_FILE).exists()) {
            throw new FileNotFoundException("Le fichier de clé n'existe pas: " + KEY_FILE);
        }
        String keyString = new String(Files.readAllBytes(Paths.get(KEY_FILE)));
        keyString = keyString.trim();
        return CryptoUtils.stringToKey(keyString);
    }
}


