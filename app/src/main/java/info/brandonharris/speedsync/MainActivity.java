package info.brandonharris.speedsync;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import static info.brandonharris.speedsync.Constants.*;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_REQUEST = 5128;
    private Intent pendingIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);

        String address = sharedPreferences.getString(COMPUTER_ADDRESS, "");
        String backupDirectory = sharedPreferences.getString(BACKUP_DIRECTORY, "");

        if (address.isEmpty()) {
            launchSettings();
        } else if (backupDirectory.isEmpty()) {
            launchSettings();
        }

        ((Button) findViewById(R.id.settingsButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSettings();
            }
        });

        ((Button) findViewById(R.id.sendButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SyncActivity.class);
                intent.putExtra(FUNCTION_EXTRA, Functions.SEND_FILE);
                testConnectionStartActivity(intent);
            }
        });

        ((Button) findViewById(R.id.receiveButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SyncActivity.class);
                intent.putExtra(FUNCTION_EXTRA, Functions.RECEIVE_FILE);
                testConnectionStartActivity(intent);
            }
        });

        ((Button) findViewById(R.id.syncButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SyncActivity.class);
                intent.putExtra(FUNCTION_EXTRA, Functions.LIST_FILES); //Function used to sync files
                testConnectionStartActivity(intent);
            }
        });
    }

    private void launchSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private Runnable connectionFailureCallback = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Failed to connect to server. Check settings.", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void testConnectionStartActivity(final Intent intent) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
//                            testConnectionStartActivity(intent);
                            pendingIntent = intent;
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            finish();
                            break;
                    }
                }
            };

            builder.setMessage("Storage permissions are required to send and receive files").setPositiveButton("Enable", dialogClickListener).setNegativeButton("Exit", dialogClickListener).show();
            return;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
        }

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        String address = sharedPreferences.getString(COMPUTER_ADDRESS, "");
        if (address.isEmpty()) {
            connectionFailureCallback.run();
            return;
        }
        Sync.testConnect(address, new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
            }
        }, connectionFailureCallback);
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] grantResults) {
        switch (request) {
            case STORAGE_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    launchSettings();
                    if (pendingIntent != null) {
                        testConnectionStartActivity(pendingIntent);
                        pendingIntent = null;
                    }
                } else {
//                    finish();
                }
            }
        }
    }
}
