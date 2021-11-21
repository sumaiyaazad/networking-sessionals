import java.io.*;
import java.net.Socket;

public class UploadThread extends Thread {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public Integer id = 0;
    private Integer requestId = 0;
    private String userCommand = "";

    public UploadThread(Socket clientSocket, Integer studentId, String command) throws IOException {
        socket = clientSocket;
        id = studentId;
        userCommand = command;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void run() {
        try {
                    String command = userCommand.split("-")[0];
                    int fileSize = Integer.parseInt(userCommand.split("-")[2]);
                    if (Server.currentBufferSize + fileSize > Server.bufferSize) {
                        out.writeObject("maximum buffer size overflow");
                        return;
                    }
                    Server.currentBufferSize += fileSize;
                    String fileName = userCommand.split("-")[1];
                    //last arg for upload request is public or private and for response is request id
                    String lastArg = userCommand.split("-")[3];
                    String fileType = command.equals("response") ? "public" : lastArg;
                    String fileId = "src\\files\\" + id + "\\" + fileType + "\\" + fileName;
                    int selectedChunkSize = 100;
//                    int selectedChunkSize = ThreadLocalRandom.current().nextInt(Server.minSize, Server.maxSize + 1);
                    out.writeObject("parameter-" + fileName + "-" + fileSize + "-" + selectedChunkSize);
                    uploadFile(fileId, fileSize, selectedChunkSize);
                    if (command.equals("response")) {
                        notifyRequestSender(lastArg, fileName);
                    }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void notifyRequestSender(String lastArg, String fileName) {
        String requestedUser = lastArg.split(":")[0];
        for (int i = 0; i < Server.threadArrayList.size(); i++) {
            if (Server.threadArrayList.get(i).id.equals(Integer.parseInt(requestedUser))) {
                Server.threadArrayList.get(i).messageArray.add(id + " uploaded file : " + fileName + " in response to your file request no : " + lastArg);
            }
        }
    }

    private void uploadFile(String fileId, int fileSize, int selectedChunkSize) throws IOException, ClassNotFoundException {
        FileOutputStream fos = new FileOutputStream(fileId);
        byte[] bytes = new byte[(int) selectedChunkSize];
        int numberOfChunk = (fileSize % selectedChunkSize == 0) ? (fileSize / selectedChunkSize) : (fileSize / selectedChunkSize + 1);
        System.out.println("fileId : " + fileId + " fileSize : " + fileSize + " selectedChunkSize : " + selectedChunkSize + " noOfChunk : " + numberOfChunk);
        int count = 0;
        while (count < numberOfChunk) {
            count += 1;
            System.out.println("received chunk no : " + count);
            out.writeObject("successfully received chunk no : " + count);
            socket.getInputStream().read(bytes);
            fos.write(bytes);
            System.out.println("wrote chunk no : " + count + " to file");
        }
        fos.close();
        String completionMessage = (String) in.readObject();
        System.out.println("client message : " + completionMessage);
        if (!completionMessage.contains("complete") || count != numberOfChunk) {
            // delete that file from server
            File file = new File(fileId);
            file.delete();
            System.out.println("transmission failure, file deleted");
            out.writeObject("transmission cancelled");
        } else {
            out.writeObject("transmission completed successfully");
        }
        Server.currentBufferSize -= fileSize;
    }

}


