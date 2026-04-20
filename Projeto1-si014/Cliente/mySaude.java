import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class mySaude {
    public static void main(String[] args) {
        System.setProperty("javax.net.ssl.trustStore", "truststore.cliente");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        String serverAddress = "";
        int serverPort = 0;
        String username = "";
        String password = "";
        String targetUser = "";
        String operation = "";
        List<String> files = new ArrayList<>();

          for (int i = 0; i < args.length; i++) {
            boolean hasNext = (i + 1 < args.length);
            switch (args[i]) {
                case "-s":
                    if (!hasNext) { System.err.println("O comando é inválido."); return; }
                    String[] parts = args[++i].split(":");
                    if (parts.length < 2) { System.err.println("O comando é inválido."); return; }
                    serverAddress = parts[0];
                    serverPort = Integer.parseInt(parts[1]);
                    break;
                case "-u":
                    if (!hasNext) { System.err.println("O comando é inválido."); return; }
                    username = args[++i];
                    break;
                case "-p":
                    if (!hasNext) { System.err.println("O comando é inválido."); return; }
                    password = args[++i];
                    break;
                case "-t":
                    if (!hasNext) { System.err.println("O comando é inválido."); return; }
                    targetUser = args[++i];
                    break;
                case "-e": case "-r": case "-c": case "-d": case "-ce":
                case "-rd": case "-a": case "-v": case "-ae": case "-rv":
                case "-ace": case "-rdv":
                    operation = args[i];
                    while (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                        files.add(args[++i]);
                    }

                    if (files.isEmpty()) {
                        System.err.println("O comando é inválido.");
                        return;
                    }

                    break;
                default:
                    // Se começar por "-" e não for um dos acima, ou se for lixo
                    System.err.println("O comando é inválido.");
                    return;
            }
        }

        // Se o loop acabar e não houver operação definida
        if (operation.isEmpty()) {
            System.err.println("O comando é inválido.");
            return;
        }

        boolean precisaDeKeyStore = operation.contains("c") || operation.contains("d") || 
                             operation.contains("a") || operation.contains("v");

		if (precisaDeKeyStore && password.isEmpty()) {
			System.err.println("O comando é inválido.");
			return;
		}

		// O alvo (-t) é obrigatório para Enviar (e) ou Cifrar (c)
		if ((operation.contains("e") || operation.contains("c")) && targetUser.isEmpty()) {
			System.err.println("O comando é inválido.");
			return;
		}

        try {
            // filtrar
            // Enviar, Cifrar ou Assinar (precisam de ficheiros)
            // OU (Decifrar ou Validar e não  é download)
            boolean localPuro = (operation.contains("d") || operation.contains("v")) && !operation.contains("r");
            boolean precisaDeFicheiro = operation.contains("e") || operation.contains("c") || operation.contains("a") || localPuro;

            if (precisaDeFicheiro) {
                List<String> listaLimpa = new ArrayList<>();
                for (String f : files) {
                    if (new File(f).exists()) {
                        listaLimpa.add(f);
                    } else {
                        System.err.println("Erro Local: O ficheiro '" + f + "' não existe para esta operação.");
                    }
                }

                // Se a lista ficar vazia e não for um download, abortamos
                if (listaLimpa.isEmpty() && !operation.contains("r")) return;

                files = listaLimpa;
            }

            // AÇÕES LOCAIS PRÉ-ENVIO
            if (operation.contains("c") || operation.contains("a")) {
                executaLocalmente(operation, username, password, targetUser, files);
            }

            // REDE (DOWNLOAD/UPLOAD)
            if (operation.contains("e") || operation.contains("r")) {
                List<String> filesParaRede = new ArrayList<>(files);
                if (operation.contains("e")) {
                    filesParaRede = prepararFicheirosParaEnvio(operation, username, targetUser, files);
                } else if (operation.contains("r") && operation.length() > 2) {
                    filesParaRede = prepararFicheirosParaReceber(operation, username, targetUser, files);
                }
                executaRemotamente(serverAddress, serverPort, operation, username, password, targetUser, filesParaRede);
            }

            // PÓS-RECEBER (DECIFRA/VALIDA)
            if (operation.contains("d") || operation.contains("v")) {
                List<String> filesBaixados = ajustarNomesParaProcessamentoLocal(operation, username, targetUser, files);
                List<String> paraProcessar = new ArrayList<>();
                for (String f : filesBaixados) {
                    if (new File(f).exists()) paraProcessar.add(f);
                }

                if (!paraProcessar.isEmpty()) {
                    executaLocalmente(operation, username, password, targetUser, paraProcessar);
                }
            }

        } catch (Exception e) {
            System.err.println("\n[ERRO CRÍTICO] " + e.getMessage());
        }
    }

    private static void executaRemotamente(String host, int port, String op, String user, String pass, String target, List<String> files) throws Exception {
        System.out.println("A tentar ligar a " + host + ":" + port + "...");
        SocketFactory sf = SSLSocketFactory.getDefault( );
        try (Socket s = sf.createSocket(host,port);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            String serverOp = (op.equals("-e") || op.contains("e")) ? "ENVIAR" : "RECEBER";
            String destination = (serverOp.equals("ENVIAR")) ? target : user;

            out.writeObject(serverOp);
            out.writeObject(destination);
            out.flush();

            String response = (String) in.readObject();
            if ("DIR_NAO_EXISTE".equals(response)) {
                System.out.println("Erro: Diretoria '" + destination + "' não existe no servidor.");
                return;
            }

            if (serverOp.equals("ENVIAR")) upload(out, in, files);
            else download(out, in, files);
        }
    }

   private static void upload(ObjectOutputStream out, ObjectInputStream in, List<String> files) throws Exception {
		out.writeInt(files.size());
		for (String fileName : files) {
			out.writeObject(new File(fileName).getName());
		}
		out.flush();

		boolean[] canUpload = (boolean[]) in.readObject();

		for (int i = 0; i < files.size(); i++) {
			String currentPath = files.get(i);
			String nameOnly = new File(currentPath).getName();

			if (!canUpload[i]) {
				System.err.println("Erro: O ficheiro (ou variante) '" + nameOnly + "' já existe no servidor.");
				continue;
			}

			File f = new File(currentPath);
			out.writeLong(f.length());
			try (FileInputStream fis = new FileInputStream(f)) {
				byte[] buf = new byte[4096];
				int read;
				while ((read = fis.read(buf)) != -1) {
					out.write(buf, 0, read);
				}
			}
			out.flush();
			System.out.println("Enviado: " + nameOnly);
		}
	}

    private static void download(ObjectOutputStream out, ObjectInputStream in, List<String> files) throws Exception {
        out.writeInt(files.size());
        for (String f : files) out.writeObject(f);
        out.flush();

        for (String f : files) {
            Object res = in.readObject();
            if ("FICHEIRO_NAO_ENCONTRADO".equals(res)) {
                System.err.println("Erro Servidor: O ficheiro '" + f + "' não existe.");
                continue;
            }
            long size = in.readLong();
            try (FileOutputStream fos = new FileOutputStream(f)) {
                byte[] buf = new byte[4096];
                long readTotal = 0;
                while (readTotal < size) {
                    int r = in.read(buf, 0, (int) Math.min(buf.length, size - readTotal));
                    fos.write(buf, 0, r);
                    readTotal += r;
                }
                System.out.println("Recebido: " + f);
            }
        }
    }

    private static void executaLocalmente(String op, String user, String pass, String target, List<String> files) {
        try {
            if (op.contains("c")) Cifra.executar(user, pass, target, files);
            if (op.contains("a")) Assina.executar(user, pass, files);
            if (op.contains("d")) {
                Decifra.executar(user, pass, files);
                if (op.contains("v")) {
                    List<String> dec = new ArrayList<>();
                    for (String f : files) dec.add(f.replace(".envelope", "").replace(".cifrado", ""));
                    Valida.executar(user, pass, target, dec);
                }
            } else if (op.contains("v")) {
                Valida.executar(user, pass, target, files);
            }
        } catch (Exception e) {
            System.err.println("Erro no processamento local: " + e.getMessage());
        }
    }

    private static List<String> prepararFicheirosParaEnvio(String op, String user, String target, List<String> originalFiles) throws IOException {
        List<String> listaFinal = new ArrayList<>();
        for (String f : originalFiles) {
            if (op.contains("ace")) {
                Files.copy(new File(f + ".cifrado").toPath(), new File(f + ".envelope").toPath(), StandardCopyOption.REPLACE_EXISTING);
                listaFinal.add(f + ".envelope");
                listaFinal.add(f + ".chave." + target);
                listaFinal.add(f + ".assinatura." + user);
            } else if (op.contains("ae")) {
                Files.copy(new File(f).toPath(), new File(f + ".assinado").toPath(), StandardCopyOption.REPLACE_EXISTING);
                listaFinal.add(f + ".assinado");
                listaFinal.add(f + ".assinatura." + user);
            } else if (op.contains("ce")) {
                listaFinal.add(f + ".cifrado");
                listaFinal.add(f + ".chave." + target);
            } else {
                listaFinal.add(f);
            }
        }
        return listaFinal;
    }

    private static List<String> prepararFicheirosParaReceber(String op, String user, String target, List<String> originalFiles) {
        List<String> listaFinal = new ArrayList<>();
        for (String f : originalFiles) {
            if (op.contains("rdv")) {
                listaFinal.add(f + ".envelope");
                listaFinal.add(f + ".chave." + user);
                listaFinal.add(f + ".assinatura." + target);
            } else if (op.contains("rv")) {
                listaFinal.add(f + ".assinado");
                listaFinal.add(f + ".assinatura." + target);
            } else if (op.contains("rd")) {
                listaFinal.add(f + ".cifrado");
                listaFinal.add(f + ".chave." + user);
            } else {
                listaFinal.add(f);
            }
        }
        return listaFinal;
    }

    private static List<String> ajustarNomesParaProcessamentoLocal(String op, String user, String target, List<String> originalFiles) {
        List<String> listaFinal = new ArrayList<>();
        for (String f : originalFiles) {
            if (op.contains("rdv")) listaFinal.add(f + ".envelope");
            else if (op.contains("rv")) listaFinal.add(f + ".assinado");
            else if (op.contains("rd")) listaFinal.add(f + ".cifrado");
            else listaFinal.add(f);
        }
        return listaFinal;
    }
}