import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */

public class mySaudeServer {
    public static void main(String[] args) {

        System.setProperty("javax.net.ssl.keyStore", "keystore.server");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

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

        ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();

        // Criamos o socket do servidor para aceitar ligações
        try (ServerSocket ss = ssf.createServerSocket(port)) {
            System.out.println("Servidor mySaude à escuta no porto " + port);
            while (true) {
                // Aceita ligações de clientes
                Socket inSoc = ss.accept();
                // Lança uma thread para cada cliente
                new ServerThread(inSoc).start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }
}