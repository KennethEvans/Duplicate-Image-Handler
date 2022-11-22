package net.kenevans.android.duplicateimagehandler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.kenevans.android.duplicateimagehandler.databinding.ActivityMainBinding;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements IConstants {
    private String mDirectory;
    private ActivityMainBinding mBinding;
    private Button mButtonGetDirectory;

    // Launcher for PREF_TREE_URI
    private final ActivityResultLauncher<Intent> openDocumentTreeLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "openDocumentTreeLauncher: result" +
                                ".getResultCode()=" + result.getResultCode());
                        // Find the UID for this application
                        Log.d(TAG, "URI=" + UriUtils.getApplicationUid(this));
                        Log.d(TAG,
                                "Current permissions (initial): "
                                        + UriUtils.getNPersistedPermissions(this));
                        try {
                            if (result.getResultCode() == RESULT_OK &&
                                    result.getData() != null) {
                                // Get Uri from Storage Access Framework.
                                Uri treeUri = result.getData().getData();
                                SharedPreferences.Editor editor =
                                        getSharedPreferences(MAIN_ACTIVITY,
                                                MODE_PRIVATE).edit();
                                if (treeUri == null) {
                                    editor.putString(PREF_TREE_URI, null);
                                    editor.apply();
                                    Utils.errMsg(this, "Failed to get " +
                                            "persistent access permissions");
                                    return;
                                }
                                // Persist access permissions.
                                try {
                                    this.getContentResolver().takePersistableUriPermission(treeUri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    // Save the current treeUri as PREF_TREE_URI
                                    editor.putString(PREF_TREE_URI,
                                            treeUri.toString());
                                    editor.apply();
                                    // Trim the persisted permissions
                                    UriUtils.trimPermissions(this, 1);
                                } catch (Exception ex) {
                                    String msg = "Failed to " +
                                            "takePersistableUriPermission for "
                                            + treeUri.getPath();
                                    Utils.excMsg(this, msg, ex);
                                }
                                Log.d(TAG,
                                        "Current permissions (final): "
                                                + UriUtils.getNPersistedPermissions(this));
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error in openDocumentTreeLauncher: " +
                                    "startActivityForResult", ex);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, this.getClass().getSimpleName() + ".onCreate:");
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);
        mButtonGetDirectory = findViewById(R.id.change_directory_button);
        mButtonGetDirectory.setOnClickListener(v -> chooseDataDirectory());

        Log.d(TAG, "mDirectory=" + mDirectory);
        mDirectory = getDirectoryFromTreeUri();
        updateDirectory();

        if (mDirectory == null) {
            mBinding.directoryName.setText(R.string.default_directory_name);
        } else {
            mBinding.directoryName.setText(mDirectory);
        }

        mBinding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, this.getClass().getSimpleName() + ".find:");
                Intent intent = new Intent(MainActivity.this,
                        GroupActivity.class);
                intent.putExtra(DIRECTORY_CODE, mDirectory);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onPause() {
        Log.v(TAG, this.getClass().getSimpleName() + " onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + " onResume:");
        updateDirectory();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.info) {
            info();
            return true;
        } else if (id == R.id.choose_data_directory) {
            chooseDataDirectory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Displays info about the current configuration
     */
    private void info() {
        try {
            StringBuilder info = new StringBuilder();
            info.append("Directory: ").append(mDirectory).append("\n");
//            if (mSessionListAdapter != null) {
//                info.append("Number of sessions: ")
//                        .append(mSessionListAdapter.getCount()).append("\n");
//            } else {
//                info.append("Number of sessions: NA").append("\n");
//            }
            SharedPreferences prefs = getSharedPreferences(MAIN_ACTIVITY,
                    MODE_PRIVATE);
            info.append(UriUtils.getRequestedPermissionsInfo(this));
            String treeUriStr = prefs.getString(PREF_TREE_URI, null);
            if (treeUriStr == null) {
                info.append("Directory URI: Not set");
            } else {
                Uri treeUri = Uri.parse(treeUriStr);
                if (treeUri == null) {
                    info.append("Directory URI: Not set");
                } else {
                    info.append("Directory URI: ").append(treeUri.getPath());
                }
            }
            Utils.infoMsg(this, info.toString());
        } catch (Throwable t) {
            Utils.excMsg(this, "Error showing info", t);
            Log.e(TAG, "Error showing info", t);
        }
    }

    /**
     * Sets the current data directory
     */
    private void chooseDataDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION &
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        openDocumentTreeLauncher.launch(intent);
    }

    private String getDirectoryFromTreeUri() {
        SharedPreferences prefs = getSharedPreferences(MAIN_ACTIVITY,
                MODE_PRIVATE);
        String treeUriStr = prefs.getString(PREF_TREE_URI, null);
        if (treeUriStr == null) {
            return null;
        } else {
            Uri treeUri = Uri.parse(treeUriStr);
            String directoryName = UriUtils.getPathFromUri(treeUri);
            if (directoryName == null) {
                return null;
            } else {
                if (!directoryName.endsWith("/")) {
                    directoryName += "/";
                }
                return directoryName;
            }
        }
    }

    private void updateDirectory() {
        Log.d(TAG, this.getClass().getSimpleName() + ".updateDirectory:");
        mDirectory = getDirectoryFromTreeUri();
        if (mBinding == null) {
            Log.d(TAG, "mBinding=null");
            return;
        }
        if (mDirectory == null) {
            mBinding.directoryName.setText(R.string.default_directory_name);
        } else {
            mBinding.directoryName.setText(mDirectory);
        }


    }

}