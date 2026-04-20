import java.io.*;
import java.net.Socket;
import java.util.Arrays;

/**
 * Grupo - si014
 * Bruno Subtil - 62249
 * José Lourenço - 62817
 * Tomás Rodrigues - 60932
 */

public class ServerThread extends Thread {
    private Socket socket;

    public ServerThread(Socket inSoc) {
        this.socket = inSoc;
    }

    @Override
    public void run() {
        try (
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream())
        ) {
            String op = (String) inStream.readObject();
            String targetUser = (String) inStream.readObject(); // username do destinatário

            String clientAddr = socket.getInetAddress().getHostAddress();
            System.out.println("\nPedido recebido de: " + clientAddr);
            System.out.println("Operação: " + op + " | Utilizador Alvo: " + targetUser);

            // A diretoria do utilizador deve estar na mesma pasta do server
            File userDir = new File(targetUser);

            // Se a diretoria do utilizador não existe no servidor, dá erro
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


}