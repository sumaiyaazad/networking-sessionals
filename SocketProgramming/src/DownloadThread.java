import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class DownloadThread extends Thread {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public Integer id=0;
    private String userCommand = "";

    public DownloadThread(Socket clientSocket, Integer studentId, String command) throws IOException {
        socket = clientSocket;
        id = studentId;
        userCommand = command;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run(String userCommand) {
        try {
                if(userCommand.contains("download")){
                    String studentId = userCommand.split("-")[1];
                    String fileName = userCommand.split("-")[2];
                    String fileId = "src\\files\\" + studentId + "\\public\\" + fileName;
                    File file = new File(fileId);
                    if (!file.exists() || file.isDirectory()) {
                        out.writeObject("requested file does not exist");
                    } else {
                    Server.currentBufferSize += file.length();
                    out.writeObject("download-" + id + "-" + fileName + "-" + file.length());
                    downloadFile(file);
                    }
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(File file) throws IOException {
        out.writeObject("download will start in few seconds...");
        FileInputStream fin = new FileInputStream(file);
        byte[] bytes = new byte[(int) Server.bufferSize];
        while(fin.read(bytes)>0) {
            socket.getOutputStream().write(bytes);
        }
        out.writeObject("file sent successfully");
        fin.close();
    }
}
