import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class criarUser {

    private static final String USERS_FILE = "users";


    public static void main(String[] args) {
        // Validação de Argumentos: criarUser <user> <role> <pass> -f <cert>
        if (args.length != 5 || !args[3].equals("-f")) {
            System.out.println("Uso: java criarUser <username> <função> <password> -f <cert_file>");
            return;
        }

        String username = args[0];
        String role = args[1]; // medico ou utente
        String password = args[2];
        String certPath = args[4];

        // Validação da Função
        if (!role.equals("medico") && !role.equals("utente")) {
            System.err.println("Erro: Função inválida. Use 'medico' ou 'utente'.");
            System.exit(1);
        }

        try {
            // Pedir password de MAC via Consola (segurança)
            Console console = System.console();
            if (console == null) {
                System.err.println("Erro: Não foi possível aceder à consola.");
                return;
            }
            char[] macPassChars = console.readPassword("Introduza a password de MAC do servidor: ");
            String macPass = new String(macPassChars);

            // Verificar integridade do ficheiro atual (se existir)
            if (Files.exists(Paths.get(USERS_FILE))) {
                metodosPartilhados.validarIntegridade(macPass);

                // Verificar se utilizador já existe
                if (metodosPartilhados.utilizadorExiste(username)) {
                    System.err.println("Erro: O utilizador '" + username + "' já existe.");
                    System.exit(1);
                }
            }

            // Gerar Salt e Hash da password do utilizador
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] salt = new byte[16];
            sr.nextBytes(salt);
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = metodosPartilhados.calcularHash(password, salt);

            // Escrever no ficheiro 'users'
            String novaLinha = username + ":" + role + ":" + saltBase64 + ":" + hashBase64;
            Files.write(Paths.get(USERS_FILE), (novaLinha + System.lineSeparator()).getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            // Atualizar o ficheiro de MAC
            metodosPartilhados.atualizarMAC(macPass);

            // Criar diretoria do utilizador
            File userDir = new File(username);
            if (!userDir.exists()) {
                userDir.mkdir();
                System.out.println("Diretoria '" + username + "' criada.");
            }

            // Adicionar certificado à keystore.users
            metodosPartilhados.adicionarCertificado(username, certPath);

            System.out.println("Utilizador '" + username + "' criado com sucesso.");

        } catch (Exception e) {
            System.err.println("Erro crítico: " + e.getMessage());
        }
    }


}
