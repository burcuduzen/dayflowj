package com.burcuduzen.dayflow.auth;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.*;
public final class PasswordHasher {
    private static final int ITERATIONS=600000;
    private PasswordHasher() {}
    public static String hash(String password) {
        byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
        return ITERATIONS+":"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
    }
    public static boolean matches(String password,String encoded) {
        try {
            String[] parts=encoded.split(":");int iterations=Integer.parseInt(parts[0]);
            if(iterations!=ITERATIONS) return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[2]),derive(password,Base64.getDecoder().decode(parts[1]),iterations));
        } catch(RuntimeException ex) { return false; }
    }
    private static byte[] derive(String password,byte[] salt,int iterations) {
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(GeneralSecurityException ex) { throw new IllegalStateException("Parola işlenemedi."); }
        finally { spec.clearPassword(); }
    }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch(GeneralSecurityException ex) { throw new IllegalStateException("Doğrulama işlenemedi."); }
    }
}
