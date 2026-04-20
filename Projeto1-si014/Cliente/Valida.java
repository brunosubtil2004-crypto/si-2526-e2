import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.List;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */

public class Valida {

    public static void executar(String username, String password, String targetUser, List<String> files) throws Exception {

        // Carregar a Keystore
        FileInputStream kfile = new FileInputStream("keystore." + username);
        KeyStore kstore = KeyStore.getInstance("PKCS12");
        kstore.load(kfile, password.toCharArray());

        // Obter certificado
        Certificate cert = kstore.getCertificate(targetUser);

        if (cert == null) {
            throw new Exception("Erro: Certificado de '" + targetUser + "' não encontrado na sua keystore.");
        }

        Signature s = Signature.getInstance("SHA256withRSA");

        for (String fileName : files) {

            String baseName = fileName.replace(".envelope", "").replace(".assinado", "").replace(".cifrado", "");
            String sigName = baseName + ".assinatura." + targetUser;
            File signatureFile = new File(sigName);

            if (!signatureFile.exists()) {
                System.out.println("Aviso: Não foi encontrada assinatura para o ficheiro: " + fileName);
                continue;
            }

            // Inicializar a verificação
            s.initVerify(cert);

            // Ler o ficheiro original
            try (FileInputStream myfile = new FileInputStream(fileName)) {
                byte[] buffer = new byte[2048];
                int read;
                while ((read = myfile.read(buffer)) != -1) {
                    s.update(buffer, 0, read);
                }
            }

            // Ler a assinatura do ficheiro .assinado
            byte[] assinatura = new byte[(int) signatureFile.length()];
            try (FileInputStream fis = new FileInputStream(signatureFile)) {
                fis.read(assinatura);
            }

            // Verificar
            boolean isValid = s.verify(assinatura);

            if (isValid) {
                System.out.println("SUCESSO: A assinatura de '" + targetUser + "' para o ficheiro '" + fileName + "' é VÁLIDA.");
            } else {
                System.err.println("ALERTA: A assinatura para o ficheiro '" + fileName + "' é INVÁLIDA!");
            }
        }
    }
}