package info.brandonharris.speedsync;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static info.brandonharris.speedsync.Constants.*;

/**
 * Created by brandon on 5/22/17.
 */

public class Sync {

    public static void send(final String address, final String directory, final Runnable finishedCallback, final SyncCallback syncCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendPath(new File(directory), address, new File(directory), syncCallback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (finishedCallback != null) {
                    finishedCallback.run();
                }
            }
        }).start();
    }

    public static void receive(final String address, final String baseDirectory, final Runnable finishedCallback, final SyncCallback syncCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sizeRemote(address, syncCallback);
                    String[] fileList = receiveFileList(address);
                    for (String file: fileList) {
                        receiveFile(address, file, baseDirectory, syncCallback);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (finishedCallback != null) {
                    finishedCallback.run();
                }
            }
        }).start();
    }

    public static void sync(final String address, final String baseDirectory, final Runnable finishedCallback, final SyncCallback syncCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] split = getActionList(address, baseDirectory).split("\n\n");

                    String[] fileList = split[0].split("\n");
                    String[] actionList = split[1].split("\n");
                    long remoteSize = Long.valueOf(split[2]);

                    long localSize = 0;

                    for (int i = 0; i < fileList.length; i++) {
                        String filename = fileList[i];
                        String action = actionList[i];

                        if (action.equals("u")) {
                            localSize += new File(baseDirectory, filename).length();
                        }
                    }

                    if (syncCallback != null) {
                        syncCallback.updateTotalSize(localSize + remoteSize);
                    }

                    for (int i = 0; i < fileList.length; i++ ) {
                        String filename = fileList[i];
                        String action = actionList[i];

                        if (action.equals("u")) {
                            sendFile(new File(baseDirectory, filename), address, new File(baseDirectory), syncCallback);
                        } else if (action.equals("d")) {
                            receiveFile(address, filename, baseDirectory, syncCallback);
                        }
                    }

                } catch (ArrayIndexOutOfBoundsException e) {
                    //No files to be synced
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (finishedCallback != null) {
                    finishedCallback.run();
                }
            }
        }).start();
    }



    public static void receiveFile(String address, String path, String baseDirectory, SyncCallback syncCallback) throws IOException {
        Log.d("FILE", path);
        Socket socket = new Socket(address, SERVICE_PORT);

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        outputStream.write(Functions.RECEIVE_FILE);
        outputStream.write(path.length());
        outputStream.write(path.getBytes());

        File file = new File(baseDirectory, path);
        if (file.getParentFile() != null) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        }

        if (!file.exists()) {
            file.createNewFile();
        }

        FileOutputStream fileOutputStream = new FileOutputStream(file);

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;
        int totalBytesRead = 0;
        int lastBytesRead = 0;
        long lastTime = System.currentTimeMillis();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
            if (syncCallback != null) {
                syncCallback.updateProgress(bytesRead);
            }
            if (System.currentTimeMillis() - lastTime > 100) {
                double megabitsPerSecond = (((totalBytesRead - lastBytesRead)/1048576.0)*8)/((System.currentTimeMillis() - lastTime)/1000.0);
                Log.d("Speed", Double.toString(megabitsPerSecond));
                if (syncCallback != null) {
                    syncCallback.updateSpeed(megabitsPerSecond);
                }
                lastTime = System.currentTimeMillis();
                lastBytesRead = totalBytesRead;
            }
        }

        inputStream.close();
        outputStream.close();
        fileOutputStream.close();
    }

    public static String[] receiveFileList(String address) throws IOException {
        Socket socket = new Socket(address, SERVICE_PORT);

        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        outputStream.write(Functions.LIST_FILES);

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        inputStream.close();
        outputStream.close();
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toString().split("\n");
    }

    public static long sizeRemote(String address, SyncCallback syncCallback) throws IOException {
        Socket socket = new Socket(address, SERVICE_PORT);

        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        outputStream.write(Functions.GET_SIZE);

        byte[] sizeBytes = new byte[8];
        int bytesRead = 0;

        while (bytesRead < sizeBytes.length) {
            bytesRead += inputStream.read(sizeBytes, bytesRead, sizeBytes.length - bytesRead);
        }

        long totalSize = ByteBuffer.wrap(sizeBytes).getLong();

        if (syncCallback != null) {
            syncCallback.updateTotalSize(totalSize);
        }

        return totalSize;
    }

    public static long sizeLocalPath(File path) {
        long size = 0;
        if (path.isDirectory()) {
            for (File file: path.listFiles()) {
                size += sizeLocalPath(file);
            }
        } else {
            size += path.length();
        }
        return size;
    }

    private static String listPath(File path) {
        StringBuilder stringBuilder = new StringBuilder();

        for (File subPath: path.listFiles()) {
            if (subPath.isDirectory()) {
                String name = subPath.getName();
                String[] pathList = listPath(subPath).split("\n");
                for (String item: pathList) {
                    if (!item.isEmpty()) {
                        stringBuilder.append(name);
                        stringBuilder.append("/");
                        stringBuilder.append(item);
                        stringBuilder.append("\n");
                    }
                }
            } else {
                stringBuilder.append(subPath.getName());
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    public static String getLocalPathLastModified(String baseDirectory) throws NoSuchAlgorithmException, IOException {
        StringBuilder stringBuilder = new StringBuilder();

        String pathList = listPath(new File(baseDirectory));
        stringBuilder.append(pathList);
        stringBuilder.append("\n");

        String[] files = pathList.split("\n");
        for (String fileName: files) {
            File file = new File(baseDirectory, fileName);
            if (!file.isDirectory()) { //Added for case where there are no files on client
                stringBuilder.append(file.lastModified());
                stringBuilder.append(",");
                stringBuilder.append(getChecksum(file));
                stringBuilder.append("\n");
            }
        }

        return stringBuilder.toString();
    }

    public static String getChecksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");

        FileInputStream fileInputStream = new FileInputStream(file);
        messageDigest.reset();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;

        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            messageDigest.update(buffer, 0, bytesRead);
        }
        byte[] digest = messageDigest.digest();
        return toHex(digest);
    }

    public static String getActionList(String address, String baseDirectory) throws IOException, NoSuchAlgorithmException {
        Socket socket = new Socket(address, SERVICE_PORT);

        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        outputStream.write(Functions.GET_ACTION_LIST);

        String lastModifiedList = getLocalPathLastModified(baseDirectory);

        Log.d("LAST MODIFIED", lastModifiedList);

        outputStream.write(ByteBuffer.allocate(4).putInt(lastModifiedList.length()).array());
        outputStream.write(lastModifiedList.getBytes());
//        outputStream.close();

        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead = 0;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        return byteArrayOutputStream.toString();
    }

    public static void sendPath(File path, String address, File rootDirectory, SyncCallback syncCallback) throws IOException {
        if (path.isDirectory()) {
            for (File file: path.listFiles()) {
                if (file.isDirectory()) {
                    sendPath(file, address, rootDirectory, syncCallback);
                } else {
                    sendFile(file, address, rootDirectory, syncCallback);
                }
            }
        } else {
            sendFile(path, address, rootDirectory, syncCallback);
        }
    }

    public static void sendFile(File file, String address, File rootDirectory, SyncCallback syncCallback) throws IOException {
        Socket socket = new Socket(address, SERVICE_PORT);
        String name = rootDirectory.toURI().relativize(file.toURI()).getPath();
        Log.d("name", name);
        int nameSize = name.length();
        //byte[] sizeBytes = ByteBuffer.allocate(4).putInt(nameSize).array();

        OutputStream outputStream = socket.getOutputStream();

        outputStream.write(Functions.SEND_FILE);
        outputStream.write(nameSize);
        outputStream.write(name.getBytes());

        FileInputStream fileInputStream = new FileInputStream(file);
        int bytesRead = 0;
        int totalBytesRead = 0;
        int lastBytesRead = 0;
        long lastTime = System.currentTimeMillis();
        byte[] buffer = new byte[BUFFER_SIZE];

        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
            if (syncCallback != null) {
                syncCallback.updateProgress(bytesRead);
            }
            if (System.currentTimeMillis() - lastTime > 100) {
                double megabitsPerSecond = (((totalBytesRead - lastBytesRead)/1048576.0)*8)/((System.currentTimeMillis() - lastTime)/1000.0);
                Log.d("Speed", Double.toString(megabitsPerSecond));
                if (syncCallback != null) {
                    syncCallback.updateSpeed(megabitsPerSecond);
                }
                lastTime = System.currentTimeMillis();
                lastBytesRead = totalBytesRead;
            }
        }

        fileInputStream.close();
        outputStream.close();
    }

    public static String toHex(byte[] bytes) {
        return String.format("%x", new BigInteger(1, bytes));
    }

    public static void testConnect(final String address, final Runnable successCallback, final Runnable failureCallback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(address, SERVICE_PORT);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(Functions.TEST);
                    outputStream.close();
                    socket.close();
                    successCallback.run();
                } catch (Exception e) {
                    e.printStackTrace();
                    failureCallback.run();
                }
            }
        }).start();
    }
}
