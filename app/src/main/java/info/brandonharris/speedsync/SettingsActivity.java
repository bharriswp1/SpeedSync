package info.brandonharris.speedsync;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static info.brandonharris.speedsync.Constants.*;

public class SettingsActivity extends AppCompatActivity {
    private final static int CODE = 51524;

    private MenuParameterFragment backupDirectoryParameterFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        TextParameterFragment computerAddressFragment = TextParameterFragment.newInstance(this, "IP Address", COMPUTER_ADDRESS, "None");
        fragmentTransaction.add(R.id.settingsLayout, computerAddressFragment);

        backupDirectoryParameterFragment = MenuParameterFragment.newInstance("Backup Directory", sharedPreferences.getString(BACKUP_DIRECTORY, Environment.getExternalStorageDirectory().getAbsolutePath()));
        backupDirectoryParameterFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(SettingsActivity.this, FilePickerActivity.class);

                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);

                i.putExtra(FilePickerActivity.EXTRA_START_PATH, backupDirectoryParameterFragment.getParameterValue());

                startActivityForResult(i, CODE);
            }
        });
        fragmentTransaction.add(R.id.settingsLayout, backupDirectoryParameterFragment);

        fragmentTransaction.commit();
    }

    private void updateBackupDirectory(String path) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(BACKUP_DIRECTORY, path);
        editor.apply();

        backupDirectoryParameterFragment.setParameterValue(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE && resultCode == Activity.RESULT_OK) {
            List<Uri> files = Utils.getSelectedFilesFromResult(data);
            for (Uri uri: files) {
                updateBackupDirectory(Utils.getFileForUri(uri).getAbsolutePath());
            }
        }
    }
}
