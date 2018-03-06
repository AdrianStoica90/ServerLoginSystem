import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Created by David on 25/02/2018.
 */
public class Password {

    private static final int desiredKeyLen =2048; //Something else.
    private static final byte[] salt = {-51, -81, -89, -43, -73, -4, 57, 18, -124, -79, 38, -87, -118, 74, -33, 42, -86, -35, -86, 35,
            -96, -74, 43, -88, -49, 84, 6, -73, -40, 78, 115, -4, 76 -115};
    private static final int iterations = 20 * 1000; // As value increases, so does cost to bruteforce.

    public static byte[] getSaltedHash(char[] password) throws Exception {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, desiredKeyLen);
        SecretKey key = skf.generateSecret(spec);
        byte[] res = key.getEncoded();
        return res;

        //currentSalt = SecureRandom.getInstance("PBKDF2WithHmacSHA512").generateSeed(saltLen);
        //return Base64.encodeBase64String(currentSalt) + "$" + hash(password, currentSalt);
    }
}
