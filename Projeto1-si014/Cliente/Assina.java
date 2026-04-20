import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.List;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */

public class Assina {

    public static void executar(String username, String password, List<String> files) throws Exception {

        // Carregar a Keystore do utilizador
        FileInputStream kfile = new FileInputStream("keystore." + username);
        KeyStore kstore = KeyStore.getInstance("PKCS12");
        kstore.load(kfile, password.toCharArray());

        // Obter a Chave usando o nome e a password passados pelo terminal
        PrivateKey pkey = (PrivateKey) kstore.getKey(username, password.toCharArray());

        if (pkey == null) {
            throw new Exception("Erro: Não foi possível encontrar a chave privada para o utilizador " + username);
        }

        // inicializar a assinatura com SHA256 e RSA
        Signature s = Signature.getInstance("SHA256withRSA");

        // Processar cada ficheiro da lista
        for (String fileName : files) {
            s.initSign(pkey);

            // Ler o ficheiro original para assinar
            try (FileInputStream myfile = new FileInputStream(fileName)) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = myfile.read(buffer)) != -1) {
                    s.update(buffer, 0, read);
                }
            }

            // Gerar assinatura
            byte[] assinatura = s.sign();

            // Guardar a assinatura num ficheiro .assinado
            String outputName = fileName + ".assinatura." + username;
            try (FileOutputStream filesign = new FileOutputStream(outputName)) {
                filesign.write(assinatura);
            }
            System.out.println("Assinatura criada: " + outputName);

        }
    }
}