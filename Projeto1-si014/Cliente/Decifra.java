import java.io.*;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */


public class Decifra {

    // Decifra
    public static void executar(String username, String password, List<String> files) throws Exception {

        // Carregar a Keystore do utilizador (keystore.username)
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream kfile = new FileInputStream("keystore." + username)) {
            ks.load(kfile, password.toCharArray());
        }

        // Obter a Chave do utilizador para fazer o "Unwrap"
        PrivateKey privKey = (PrivateKey) ks.getKey(username, password.toCharArray());
        if (privKey == null) {
            throw new Exception("Erro: Não foi possível obter a chave privada para o utilizador: " + username);
        }

        for (String encryptedFileName : files) {

            if (!encryptedFileName.endsWith(".cifrado") && !encryptedFileName.endsWith(".envelope")) {
                continue;
            }

            String baseName = encryptedFileName.replace(".envelope", "").replace(".cifrado", "");

            // Localizar o ficheiro da chave usando o nome base
            File keyFile = new File(baseName + ".chave." + username);

            if (!keyFile.exists()) {
                System.out.println("Erro: Ficheiro de chave não encontrado: " + keyFile.getName());
                continue;
            }

            byte[] wrappedKey = Files.readAllBytes(keyFile.toPath());
            Cipher rsaCipher = Cipher.getInstance("RSA"); // RSA 2048 bits
            rsaCipher.init(Cipher.UNWRAP_MODE, privKey);

            // Reconstroi a chave AES de 128 bits
            SecretKey aesKey = (SecretKey) rsaCipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);

            String outputName = baseName;

            Cipher aesCipher = Cipher.getInstance("AES"); // AES 128 bits
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey);

            try (FileInputStream fis = new FileInputStream(encryptedFileName);
                 CipherInputStream cis = new CipherInputStream(fis, aesCipher);
                 FileOutputStream fos = new FileOutputStream(outputName)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Ficheiro decifrado com sucesso: " + outputName);
        }
    }
}