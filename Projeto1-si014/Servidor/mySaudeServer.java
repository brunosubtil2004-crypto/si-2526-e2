import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.Console;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class mySaudeServer {
    public static void main(String[] args) {

        System.setProperty("javax.net.ssl.keyStore", "keystore.server");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        System.setProperty("javax.net.ssl.trustStore", "truststore.server");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");

        if (args.length < 1) {
            System.out.println("Erro: Indicar o porto.");
            System.out.println("Uso: java mySaudeServer <porto>");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println("Erro: O porto '" + args[0] + "' não é um número válido.");
            return;
        }


        // Verificar o MAC
        Console console = System.console();
        if (console == null) {
            System.err.println("Erro: Consola não disponível.");
            return;
        }
        char[] macPassChars = console.readPassword("Introduza a password de MAC do servidor: ");
        String macPass = new String(macPassChars);

        // Verificar integridade do ficheiro de passwords no arranque
        try {
            metodosPartilhados.validarIntegridade(macPass);
            System.out.println("Integridade do ficheiro 'users' confirmada.");
        } catch (Exception e) {
            System.err.println("AVISO: " + e.getMessage());
            System.err.println("O servidor vai terminar por segurança.");
            System.exit(1); // terminar se o MAC estiver errado
        }



        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();

        // Criamos o socket do servidor para aceitar ligações
        try (ServerSocket ss = ssf.createServerSocket(port)) {
            System.out.println("Servidor mySaude à escuta no porto " + port);
            while (true) {
                // Aceita ligações de clientes
                Socket inSoc = ss.accept();
                // Lança uma thread para cada cliente
                new ServerThread(inSoc, macPass).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }
}