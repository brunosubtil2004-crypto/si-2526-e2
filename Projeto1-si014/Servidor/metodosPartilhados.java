import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

public class metodosPartilhados {

    private static final String MAC_FILE = "mySaude.mac";
    private static final String KEYSTORE_USERS = "keystore.users";
    private static final String KS_PASS = "123456";
    private static final String USERS_FILE = "users";


    // Métodos auxiliares

    public static void validarIntegridade(String macPass) throws Exception {
        byte[] content = Files.readAllBytes(Paths.get(USERS_FILE));
        // Ler a String Base64 do ficheiro e remover possíveis espaços/quebras de linha
        String macBase64 = new String(Files.readAllBytes(Paths.get(MAC_FILE))).trim();

        // Decodificar de Base64 para os bytes originais
        byte[] macGravado = Base64.getDecoder().decode(macBase64);
        byte[] macCalculado = calcularHMAC(content, macPass);
        if (!MessageDigest.isEqual(macGravado, macCalculado)) {
            System.err.println("Erro: Integridade do ficheiro de passwords comprometida!");
            System.exit(1);
        }

    }

    public static void atualizarMAC(String macPass) throws Exception {
        byte[] content = Files.readAllBytes(Paths.get(USERS_FILE));
        byte[] novoMac = calcularHMAC(content, macPass);
        String macBase64 = Base64.getEncoder().encodeToString(novoMac);
        Files.write(Paths.get(MAC_FILE), macBase64.getBytes());
    }

    public static byte[] calcularHMAC(byte[] data, String key) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        hmac.init(secretKey);
        return hmac.doFinal(data);
    }

    public static String calcularHash(String pass, byte[] salt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(salt);
        return Base64.getEncoder().encodeToString(md.digest(pass.getBytes()));
    }

    public static boolean utilizadorExiste(String username) throws IOException {
        return Files.lines(Paths.get(USERS_FILE))
                .anyMatch(line -> line.split(":")[0].equals(username));
    }

    public static void adicionarCertificado(String alias, String path) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        File ksFile = new File(KEYSTORE_USERS);

        // Carregar a Keystore existente
        if (ksFile.exists()) {
            try (FileInputStream fis = new FileInputStream(ksFile)) {
                ks.load(fis, KS_PASS.toCharArray());
            }
        } else {
            ks.load(null, KS_PASS.toCharArray());
        }

        // Ler o novo certificado do ficheiro
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate novoCert;
        try (FileInputStream fis = new FileInputStream(path)) {
            novoCert = (X509Certificate) cf.generateCertificate(fis);
        }

        // VERIFICAÇÃO DE DUPLICADOS: Percorrer todos os aliases existentes
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String existingAlias = aliases.nextElement();
            java.security.cert.Certificate certExistente = ks.getCertificate(existingAlias);

            // Compara os certificados (o method equals compara o conteúdo binário)
            if (novoCert.equals(certExistente)) {
                System.err.println("Erro: Este certificado já está a ser utilizado pelo utilizador '" + existingAlias + "'.");
                System.err.println("Cada utilizador deve ter o seu próprio certificado único.");
                System.exit(1); // Interrompe a execução
            }
        }

        // Se chegou aqui, é porque é único. Pode guardar.
        ks.setCertificateEntry(alias, novoCert);

        try (FileOutputStream fos = new FileOutputStream(ksFile)) {
            ks.store(fos, KS_PASS.toCharArray());
        }
    }



}
