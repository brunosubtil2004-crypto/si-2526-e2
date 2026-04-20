import java.io.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */

public class Cifra {

    // Executa a cifra híbrida
    public static void executar(String username, String password, String target, List<String> files) throws Exception {

        // Carregar a Keystore do utilizador (keystore.username)
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream kfile = new FileInputStream("keystore." + username)) {
            ks.load(kfile, password.toCharArray());
        }

        // Obter o certificado do destinatário (target) para cifrar a chave
        Certificate cert = ks.getCertificate(target);
        if (cert == null) {
            throw new Exception("Erro: Certificado para o destinatário '" + target + "' não encontrado na keystore.");
        }

        for (String fileName : files) {
            // Gerar Chave AES de 128 bits (Cifra Simétrica)
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            SecretKey aesKey = kg.generateKey();

            // Cifrar o Conteúdo do Ficheiro com AES
            Cipher cAES = Cipher.getInstance("AES");
            cAES.init(Cipher.ENCRYPT_MODE, aesKey);

            // Nome do ficheiro de saída
            try (FileInputStream fis = new FileInputStream(fileName);
                 FileOutputStream fos = new FileOutputStream(fileName + ".cifrado");
                 CipherOutputStream cos = new CipherOutputStream(fos, cAES)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }

            // Cifrar a Chave AES com a Chave Pública RSA do destinatário (Wrap)
            Cipher cRSA = Cipher.getInstance("RSA");
            cRSA.init(Cipher.WRAP_MODE, cert.getPublicKey());
            byte[] keyCifrada = cRSA.wrap(aesKey);

            // Guardar a chave: nome_de_ficheiro.chave.username_do_destinatario
            String keyFileName = fileName + ".chave." + target;
            try (FileOutputStream kos = new FileOutputStream(keyFileName)) {
                kos.write(keyCifrada);
            }

            System.out.println("Ficheiro cifrado com sucesso: " + fileName + ".cifrado");
            System.out.println("Chave guardada em: " + keyFileName);
        }
    }
}