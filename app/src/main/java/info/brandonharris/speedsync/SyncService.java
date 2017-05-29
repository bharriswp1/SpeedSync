package info.brandonharris.speedsync;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

import java.io.File;
import java.text.DecimalFormat;

import static info.brandonharris.speedsync.Constants.BACKUP_DIRECTORY;
import static info.brandonharris.speedsync.Constants.COMPUTER_ADDRESS;
import static info.brandonharris.speedsync.Constants.FUNCTION_EXTRA;
import static info.brandonharris.speedsync.Constants.PREFERENCES;

public class SyncService extends IntentService {
    private static int NOTIFICATION_ID = 12548;  //COMPLETELY RANDOM NUMBER

    public SyncService() {
        super("SyncService");
    }

    long readBytes = 0;
    long lastTime = 0;
    long totalBytes = 0;

    private NotificationCompat.Builder mBuilder;

    SyncCallback syncCallback = new SyncCallback() {
        @Override
        void updateSpeed(final double megabitsPerSecond) {
            Intent broadcastIntent = new Intent("syncCallback");
            broadcastIntent.putExtra("speed", megabitsPerSecond);
            sendBroadcast(broadcastIntent);
        }

        @Override
        void updateProgress(long bytes) {
            readBytes += bytes;
            if (System.currentTimeMillis() - lastTime > 250 || readBytes == totalBytes) {
                lastTime = System.currentTimeMillis();
                int progress = (int) (1000 * ((double) readBytes / (double) totalBytes));
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                String percentage = decimalFormat.format(progress / 10.0);
                mBuilder.setContentText(percentage + "% complete");
                mBuilder.setProgress(1000, progress, false);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, mBuilder.build());

                Intent broadcastIntent = new Intent("syncCallback");
                broadcastIntent.putExtra("progress", progress);
                sendBroadcast(broadcastIntent);
            }
        }

        @Override
        void updateTotalSize(final long totalSize) {
            totalBytes = totalSize;
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            int function = intent.getIntExtra(FUNCTION_EXTRA, 0);

            int icon = R.drawable.ic_sync_black_24dp;
            String title = "";

            switch (function) {
                case Constants.Functions.SEND_FILE:
                    icon = R.drawable.ic_file_upload_black_24dp;
                    title = "Sending files";
                    break;
                case Constants.Functions.RECEIVE_FILE:
                    icon = R.drawable.ic_file_download_black_24dp;
                    title = "Receiving files";
                    break;
                case Constants.Functions.GET_ACTION_LIST:
                    icon = R.drawable.ic_sync_black_24dp;
                    title = "Syncing files";
                    break;
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(icon)
                        .setContentTitle(title)
                        .setAutoCancel(false)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setContentText("0% Complete");
            } else {
                mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(icon)
                        .setAutoCancel(false)
                        .setContentTitle(title)
                        .setProgress(1000, 0, false);
            }
            Notification notification = mBuilder.build();

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(NOTIFICATION_ID, notification);

            SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);

            String address = sharedPreferences.getString(COMPUTER_ADDRESS, "127.0.0.1");
            String rootDirectory = sharedPreferences.getString(BACKUP_DIRECTORY, "");
            if (address.equals("127.0.0.1")) {
                return;
            }
            if (rootDirectory.equals("")) {
                return;
            }

            if (function == Constants.Functions.SEND_FILE) {
                totalBytes = Sync.sizeLocalPath(new File(rootDirectory));
//                Sync.send(address, rootDirectory, null, syncCallback);
                Sync.sendPath(new File(rootDirectory), address, new File(rootDirectory), syncCallback);
                finishActivity();
            } else if (function == Constants.Functions.RECEIVE_FILE) {
//                Sync.receive(address, rootDirectory, null, syncCallback);
                Sync.sizeRemote(address, syncCallback);
                String[] fileList = Sync.receiveFileList(address);
                for (String file: fileList) {
                    Sync.receiveFile(address, file, rootDirectory, syncCallback);
                }
                finishActivity();
            } else if (function == Constants.Functions.LIST_FILES) { //Function used to sync files
//                Sync.sync(address, rootDirectory, null, syncCallback);
                String[] split = Sync.getActionList(address, rootDirectory).split("\n\n");

                if (split.length < 3) { //This means there are no files to sync
                    finishActivity();
                    return;
                }

                String[] fileList = split[0].split("\n");
                String[] actionList = split[1].split("\n");
                long remoteSize = Long.valueOf(split[2]);

                long localSize = 0;

                for (int i = 0; i < fileList.length; i++) {
                    String filename = fileList[i];
                    String action = actionList[i];

                    if (action.equals("u")) {
                        localSize += new File(rootDirectory, filename).length();
                    }
                }

                if (syncCallback != null) {
                    syncCallback.updateTotalSize(localSize + remoteSize);
                }

                for (int i = 0; i < fileList.length; i++ ) {
                    String filename = fileList[i];
                    String action = actionList[i];

                    if (action.equals("u")) {
                        Sync.sendFile(new File(rootDirectory, filename), address, new File(rootDirectory), syncCallback);
                    } else if (action.equals("d")) {
                        Sync.receiveFile(address, filename, rootDirectory, syncCallback);
                    }
                }
                finishActivity();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void finishActivity() {
        Intent broadcastIntent = new Intent("syncCallback");
        broadcastIntent.putExtra("finish", true);
        sendBroadcast(broadcastIntent);
    }
}
