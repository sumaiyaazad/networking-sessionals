import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;

public class ServiceProvider extends Thread {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    public Integer id = 0;
    Integer requestId = 0;
    ArrayList<String> messageArray = new ArrayList<String>();

    public ServiceProvider(Socket clientSocket) throws IOException {
        socket = clientSocket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(this.socket.getInputStream());
    }

    public void run() {
        try {
            boolean loginFlag = login();
            while (loginFlag) {
                String userCommand = (String) in.readObject();
                if (userCommand.equalsIgnoreCase("logout")) {
                    logout();
                } else if (userCommand.equalsIgnoreCase("lookup-student-list")) {
                    lookupStudentList();
                } else if (userCommand.equalsIgnoreCase("lookup-own-files")) {
                    lookupOwnFiles();
                } else if (userCommand.equalsIgnoreCase("lookup-other-files")) {
                    lookupOtherFiles();
                } else if (userCommand.contains("upload") || userCommand.contains("response")) {
                    String command = userCommand.split("-")[0];
                    int fileSize = Integer.parseInt(userCommand.split("-")[2]);
//                    if (Server.currentBufferSize + fileSize > Server.bufferSize) {
//                        out.writeObject("maximum buffer size overflow");
//                        continue;
//                    }
                    Server.currentBufferSize += fileSize;
                    String fileName = userCommand.split("-")[1];
                    //last arg for upload request is public or private and for response is request id
                    String lastArg = userCommand.split("-")[3];
                    String fileType = command.equals("response") ? "public" : lastArg;
                    String fileId = "src\\uploads\\" + id + "\\" + fileType + "\\" + fileName;
                    int selectedChunkSize = 100;
//                    int selectedChunkSize = ThreadLocalRandom.current().nextInt(Server.minSize, Server.maxSize + 1);
                    out.writeObject("parameter-" + fileName + "-" + fileSize + "-" + selectedChunkSize );
                    uploadFile(fileId, fileSize, selectedChunkSize);
                    if (command.equals("response")) {
                        notifyRequestSender(lastArg, fileName);
                    }
                } else if (userCommand.contains("download")) {
                    String studentId = userCommand.split("-")[1];
                    String fileName = userCommand.split("-")[2];
                    String fileId = "src\\uploads\\" + studentId + "\\public\\" + fileName;
                    File file = new File(fileId);
                    if (!file.exists() || file.isDirectory()) {
                        out.writeObject("requested file does not exist");
                    } else {
                        Server.currentBufferSize += file.length();
                        out.writeObject("download-" + id + "-" + fileName + "-" + file.length());
                        downloadFile(file);
                    }
                } else if (userCommand.contains("request")) {
                    String description = userCommand.split("-")[1];
                    out.writeObject("stored your request");
                    notifyAll(description);
                } else if (userCommand.contains("view-message")) {
                    out.writeObject("view-message request processing ....");
                    viewMessage();
                }
            }
        } catch (Exception e) {
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

    private void viewMessage() throws IOException {
        String sendObject;
        if (messageArray.size() == 0) {
            sendObject = "You have no new message \n";
        } else {
            sendObject = "Your unread messages : \n";
        }
        for (int i = 0; i < messageArray.size(); i++) {
            sendObject += messageArray.get(i) + "\n";
        }
        messageArray = new ArrayList<String>();
        out.writeObject(sendObject);
    }

    private void notifyAll(String description) {
        requestId += 1;
        String rId = id.toString() + ":" + requestId.toString();
        for (int i = 0; i < Server.threadArrayList.size(); i++) {
            if (!Server.currentUserArray.contains(Server.threadArrayList.get(i).id) || Server.threadArrayList.get(i).id.equals(id)) {
                continue;
            }
            Server.threadArrayList.get(i).messageArray.add("requested file id : " + rId + " description : " + description);
        }
    }

    private void downloadFile( File file) throws IOException {
        out.writeObject("download will start in few seconds...");
        FileInputStream fin = new FileInputStream(file);
        int bufferSize = Server.bufferSize>65500 ? 65500 : Server.bufferSize;
        byte[] bytes = new byte[(int) bufferSize];
        while (fin.read(bytes) > 0) {
            socket.getOutputStream().write(bytes);
        }
        out.writeObject("file sent successfully");
        fin.close();
    }


    private void uploadFile(String fileId, int fileSize, int selectedChunkSize) throws IOException, ClassNotFoundException, InterruptedException {
        FileOutputStream fos = new FileOutputStream(fileId);
        byte[] bytes = new byte[(int) selectedChunkSize];
        int numberOfChunk = (fileSize % selectedChunkSize == 0) ? (fileSize / selectedChunkSize) : (fileSize / selectedChunkSize + 1);
        System.out.println("fileId : " + fileId + " fileSize : " + fileSize + " selectedChunkSize : " + selectedChunkSize + " noOfChunk : " + numberOfChunk);
        int count = 0;
        while (count < numberOfChunk) {
            if(socket.getInputStream().read(bytes)>0){
                count += 1;
//                System.out.println("received chunk no : " + count );
                out.writeObject("successfully received chunk no : " + count);
                fos.write(bytes);
//                System.out.println("wrote chunk no : " + count + " to file");
            };
        }
        String completionMessage = (String) in.readObject();
//        System.out.println("client message : " + completionMessage);
        fos.close();
//        simulation file delete
//        count=0;
        if (!completionMessage.contains("complete") || count != numberOfChunk) {
            File file = new File(fileId);
            file.delete();
//            System.out.println("transmission failure, file deleted");
            out.writeObject("transmission cancelled");
        } else {
            out.writeObject("transmission completed successfully");
        }
        Server.currentBufferSize -= fileSize;
    }

    private void lookupOtherFiles() throws IOException {
        String otherPublicList;
        if (Server.userArray.size() == 1) {
            otherPublicList = "none uploaded public file \n";
        } else {
            otherPublicList = "other public file list : \n";
        }
        for (int i = 0; i < Server.userArray.size(); i++) {
            if (Server.userArray.get(i).equals(id)) {
                continue;
            }

            File folder = new File("src\\uploads\\" + Server.userArray.get(i) + "\\public");
            String[] listOfPublicFiles = folder.list();
            if (listOfPublicFiles.length != 0) {
                otherPublicList += Server.userArray.get(i) + " public file list : \n";
            }
            for (int j = 0; j < listOfPublicFiles.length; j++) {
                otherPublicList += listOfPublicFiles[j] + "\n";
            }
        }
        out.writeObject(otherPublicList);
    }

    private void lookupOwnFiles() throws IOException {
        String fileList;
        File publicFolder = new File("src\\uploads\\" + id + "\\public");
        File privateFolder = new File("src\\uploads\\" + id + "\\private");
        File downloadFolder = new File("src\\downloads\\"+id);
        String[] listOfPublicFiles = publicFolder.list();
        String[] listOfPrivateFiles = privateFolder.list();
        String[] listOfDownloadedFiles = downloadFolder.list();
        if (listOfPrivateFiles.length == 0 && listOfPublicFiles.length == 0) {
            fileList = "you haven't uploaded any file\n";
        } else {
            fileList = "your uploaded file list : \n";
        }
        for (int i = 0; i < listOfPublicFiles.length; i++) {
            fileList += listOfPublicFiles[i] + " --> public \n";
        }
        for (int i = 0; i < listOfPrivateFiles.length; i++) {
            fileList += listOfPrivateFiles[i] + " --> private \n";
        }
        if (listOfDownloadedFiles.length == 0) {
            fileList += "you haven't dwnloaded any file\n";
        } else {
            fileList += "your dwnloaded file list : \n";
        }
        for (int i = 0; i < listOfDownloadedFiles.length; i++) {
            fileList += listOfDownloadedFiles[i] + " --> public \n";
        }
        out.writeObject(fileList);
    }

    private void lookupStudentList() throws IOException {
        String studentList = "student list : \n";
        studentList += id + "\n";
        for (int i = 0; i < Server.userArray.size(); i++) {
            if (Server.userArray.get(i) == id) {
                continue;
            }
            if (Server.currentUserArray.contains(Server.userArray.get(i))) {
                studentList += Server.userArray.get(i) + " --> online \n";
            } else {
                studentList += Server.userArray.get(i) + " --> offline \n";
            }
        }
        out.writeObject(studentList);
    }

    private void logout() throws IOException {
        Server.currentUserArray.remove(id);
        out.writeObject("logout successful");
        socket.close();
    }

    private boolean login() throws IOException, ClassNotFoundException {
        out.writeObject("login ID :");
        String sUserId = (String) in.readObject();
        Integer userId = Integer.parseInt(sUserId);
        if (Server.currentUserArray.contains(userId)) {
            out.writeObject("login denied, " + userId + " active");
            socket.close();
            return false;
        } else if (Server.userArray.contains(userId)) {
            out.writeObject("welcome again " + userId);
            Server.currentUserArray.add(userId);
            id = userId;
            return true;
        } else {
            out.writeObject("welcome " + userId);
            //add create new folder functionality
            Server.currentUserArray.add(userId);
            Server.userArray.add(userId);
            id = userId;
            new File("src\\uploads\\" + sUserId).mkdir();
            new File("src\\downloads\\" + sUserId).mkdir();
            new File("src\\uploads\\" + sUserId + "\\public").mkdir();
            new File("src\\uploads\\" + sUserId + "\\private").mkdir();
            return true;
        }

    }
}
