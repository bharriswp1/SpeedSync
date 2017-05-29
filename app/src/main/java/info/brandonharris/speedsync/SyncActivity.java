package info.brandonharris.speedsync;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.DecimalFormat;

import static info.brandonharris.speedsync.Constants.BACKUP_DIRECTORY;
import static info.brandonharris.speedsync.Constants.COMPUTER_ADDRESS;
import static info.brandonharris.speedsync.Constants.FUNCTION_EXTRA;
import static info.brandonharris.speedsync.Constants.PREFERENCES;

public class SyncActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView speedText;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("progress")) {
                progressBar.setProgress(intent.getIntExtra("progress", 0));
                Log.d("Progress", Integer.toString(intent.getIntExtra("progress", -1)));
            }
            if (intent.hasExtra("speed")) {
                DecimalFormat decimalFormat = new DecimalFormat("#.##");
                speedText.setText(decimalFormat.format(intent.getDoubleExtra("speed", 0)) + " mbps");
            }
            if (intent.hasExtra("finish")) {
                if (intent.getBooleanExtra("finish", false)) {
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        registerReceiver(broadcastReceiver, new IntentFilter("syncCallback"));

        int function = getIntent().getIntExtra(FUNCTION_EXTRA, 0);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(1000);
        speedText = (TextView) findViewById(R.id.speedText);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);

        String address = sharedPreferences.getString(COMPUTER_ADDRESS, "127.0.0.1");
        String rootDirectory = sharedPreferences.getString(BACKUP_DIRECTORY, "");
        if (address.equals("127.0.0.1")) {
            return;
        }
        if (rootDirectory.equals("")) {
            return;
        }

        if (savedInstanceState == null) {
            Intent intent = new Intent(this, SyncService.class);
            intent.putExtra(FUNCTION_EXTRA, function);
            startService(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(broadcastReceiver, new IntentFilter("syncActivity"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(broadcastReceiver);
    }
}
