import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */

public class ServerThread extends Thread {
    private Socket socket;
    private String macPass;
    private String userAtual;
    private String userFuncao;
    private static final String USERS_FILE = "users";
    private static final String MAC_FILE = "mySaude.mac";
    private static final String KEYSTORE_USERS = "keystore.users";
    private static final String KS_PASS = "123456";

    public ServerThread(Socket inSoc, String macPass) {
        this.socket = inSoc;
        this.macPass = macPass;
    }


    @Override
    public void run() {
        try (
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream())
        ) {
            // AUTENTICAÇÃO
            String user = (String) inStream.readObject();
            String pass = (String) inStream.readObject();

            if (!autenticar(user, pass, this.macPass)) {
                outStream.writeObject("AUTH_FAIL");
                return;
            }
            outStream.writeObject("AUTH_OK");

            // RECEBER OPERAÇÃO
            String op = (String) inStream.readObject();

            // CERTIFICADOS
            // Se o cliente pedir um certificado que não tem localmente
            if (op.equals("GET_CERT")) {
                String target = (String) inStream.readObject();
                enviarCertificado(target, outStream);
                return;
            }

            // CONTROLO DE ACESSO
            // Apenas médicos podem enviar ficheiros para o servidor
            if (op.equals("ENVIAR") && !this.userFuncao.equals("medico")) {
                outStream.writeObject("ERRO_PERMISSAO");
                return;
            }

            String targetUser = (String) inStream.readObject();
            File userDir = new File(targetUser);

            if (!userDir.exists() || !userDir.isDirectory()) {
                outStream.writeObject("DIR_NAO_EXISTE");
                return;
            }
            outStream.writeObject("READY");

            if (op.equals("ENVIAR")) {
                Upload(inStream, outStream, userDir);
            } else if (op.equals("RECEBER")) {
                Download(inStream, outStream, userDir);
            }

        } catch (Exception e) {
            System.err.println("Erro na thread: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException e) {}
        }
    }


  private void Upload(ObjectInputStream in, ObjectOutputStream out, File userDir) throws Exception {
		int numFiles = in.readInt();
		String[] fileNames = new String[numFiles];
		boolean[] canAccept = new boolean[numFiles];

		for (int i = 0; i < numFiles; i++) {
			fileNames[i] = (String) in.readObject();
			canAccept[i] = !Dupli(userDir, fileNames[i]);
		}

		out.writeObject(canAccept);
		out.flush();

		for (int i = 0; i < numFiles; i++) {
			if (!canAccept[i]) continue;

			long fileSize = in.readLong();
			File newFile = new File(userDir, fileNames[i]);
			try (FileOutputStream fos = new FileOutputStream(newFile)) {
				byte[] buffer = new byte[4096];
				long bytesRead = 0;
				while (bytesRead < fileSize) {
					int read = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - bytesRead));
					fos.write(buffer, 0, read);
					bytesRead += read;
				}
			}
		}
	}

    private void Download(ObjectInputStream in, ObjectOutputStream out, File userDir) throws Exception {
        int numFiles = in.readInt();
        for (int i = 0; i < numFiles; i++) {
            String fileName = (String) in.readObject();
            File file = new File(userDir, fileName);

            if (!file.exists()) {
                out.writeObject("FICHEIRO_NAO_ENCONTRADO");
                continue;
            }
            out.writeObject("OK");
            out.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            out.flush();
        }
    }

    // Verifica se já existe um ficheiro para evitar duplicados
	private boolean Dupli(File dir, String fileName) {
		String baseName = fileName.split("\\.")[0];
		File[] list = dir.listFiles();
		if (list == null) return false;

		for (File f : list) {
			String existingBase = f.getName().split("\\.")[0];
			if (existingBase.equalsIgnoreCase(baseName)) {
				return true;
			}
		}
		return false;
	}





    // Novas cenas

    private void enviarCertificado(String alias, ObjectOutputStream out) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        File ksFile = new File(KEYSTORE_USERS);

        if (!ksFile.exists()) {
            out.writeObject("KS_NOT_FOUND");
            return;
        }

        try (FileInputStream fis = new FileInputStream(ksFile)) {
            ks.load(fis, KS_PASS.toCharArray());
        }

        Certificate cert = ks.getCertificate(alias);
        if (cert == null) {
            out.writeObject("CERT_NOT_FOUND");
        } else {
            out.writeObject("OK");
            out.writeObject(cert.getEncoded()); // Envia os bytes do certificado
        }
        out.flush();
    }


    // Validar autenticacao
    private boolean autenticar(String user, String pass, String macPass) throws Exception {
        metodosPartilhados.validarIntegridade(macPass);

        // Verificar password
        if (!Files.exists(Paths.get(USERS_FILE))) return false;

        List<String> linhas = Files.readAllLines(Paths.get(USERS_FILE));
        for (String linha : linhas) {
            String[] p = linha.split(":");
            if (p[0].equals(user)) {
                byte[] salt = Base64.getDecoder().decode(p[2]);
                String hashCalculado = metodosPartilhados.calcularHash(pass, salt);

                if (hashCalculado.equals(p[3])) {
                    this.userAtual = user;
                    this.userFuncao = p[1];
                    return true;
                }
            }
        }
        return false;
    }



}