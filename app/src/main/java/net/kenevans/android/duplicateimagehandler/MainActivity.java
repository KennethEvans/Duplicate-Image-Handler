package net.kenevans.android.duplicateimagehandler;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;

import net.kenevans.android.duplicateimagehandler.databinding.ActivityMainBinding;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements IConstants {
    private String mDirectory;
    private ActivityMainBinding mBinding;
    private boolean mUseSubdirectories;
    private boolean mReadMediaImagesAsked;
    private final String mReadImagePermission = (Build.VERSION.SDK_INT >= 33) ?
            Manifest.permission.READ_MEDIA_IMAGES :
            Manifest.permission.READ_EXTERNAL_STORAGE;

    // Launcher for PREF_TREE_URI
    private final ActivityResultLauncher<Intent> openDocumentTreeLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
//                        Log.d(TAG, "openDocumentTreeLauncher: result" +
//                                ".getResultCode()=" + result.getResultCode());
                        // Find the UID for this application
//                        Log.d(TAG, "UID=" + UriUtils.getApplicationUid(this));
//                        Log.d(TAG,
//                                "Current permissions (initial): "
//                                        + UriUtils.getNPersistedPermissions
//                                        (this));
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
                                    mDirectory = getDirectoryFromTreeUri();
                                } catch (Exception ex) {
                                    String msg = "Failed to " +
                                            "takePersistableUriPermission for "
                                            + treeUri.getPath();
                                    Utils.excMsg(this, msg, ex);
                                }
//                                Log.d(TAG,
//                                        "Current permissions (final): "
//                                                + UriUtils
//                                                .getNPersistedPermissions
//                                                (this));
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error in openDocumentTreeLauncher", ex);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, this.getClass().getSimpleName() + ".onCreate:");
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);
        Button buttonAllMedia = findViewById(R.id.all_media_button);
        buttonAllMedia.setOnClickListener(v ->
        {
            Log.d(TAG, "buttonAllMedia.setOnClickListener");
            mDirectory = null;
            updateDirectory();
        });

        Button buttonGetDirectory = findViewById(R.id.change_directory_button);
        buttonGetDirectory.setOnClickListener(v -> chooseDataDirectory());

        CheckBox subdirectoriesCheckBox =
                findViewById(R.id.subdirectories_check_box);
        subdirectoriesCheckBox.setOnCheckedChangeListener((buttonView,
                                                           isChecked) -> {
            mUseSubdirectories = isChecked;
            SharedPreferences.Editor editor =
                    getSharedPreferences(MAIN_ACTIVITY,
                            MODE_PRIVATE).edit();
            editor.putBoolean(USE_SUBDIRECTORIES_CODE, mUseSubdirectories);
            editor.apply();
        });

        SharedPreferences prefs = getSharedPreferences(MAIN_ACTIVITY,
                MODE_PRIVATE);
        mUseSubdirectories = prefs.getBoolean(USE_SUBDIRECTORIES_CODE, true);
        mDirectory = prefs.getString(PREF_DIRECTORY, null);

        mBinding.fab.setOnClickListener(view -> find());
    }

    @Override
    protected void onPause() {
        Log.v(TAG, this.getClass().getSimpleName() + " onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + " onResume:");
        super.onResume();
        updateDirectory();
        // Check permissions (Will prompt user if either not granted)
        if (!isReadMediaImagesGranted()) {
            Log.d(TAG, "onResume: isReadMediaImagesGranted()=false");
            if (!mReadMediaImagesAsked) {
                requestLocationPermission();
                mReadMediaImagesAsked = true;
            } else {
                Utils.warnMsg(this,
                        "The necessary permission is not granted. "
                                + "No images will be found. You can fix this"
                                + " in Settings|Apps for Duplicate Image " +
                                "Handler.");
            }
        } else {
            Log.d(TAG, "onResume: isReadMediaImagesGranted()=true");
        }
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
        } else if (item.getItemId() == R.id.help) {
            showHelp();
            return true;
        } else if (id == R.id.choose_data_directory) {
            chooseDataDirectory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void find() {
//        Log.d(TAG, this.getClass().getSimpleName() + " find:");
        Intent intent = new Intent(MainActivity.this,
                GroupActivity.class);
        intent.putExtra(DIRECTORY_CODE, mDirectory);
        intent.putExtra(USE_SUBDIRECTORIES_CODE, mUseSubdirectories);
        startActivity(intent);
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
     * Show the help.
     */
    private void showHelp() {
        Log.v(TAG, this.getClass().getSimpleName() + " showHelp");
        try {
            // Start the InfoActivity
            Intent intent = new Intent();
            intent.setClass(this, InfoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(INFO_URL, "file:///android_asset" +
                    "/DuplicateImageHandler.html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, getString(R.string.help_show_error), ex);
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
//        Log.d(TAG, this.getClass().getSimpleName() + ".updateDirectory:"
//                + " mDirectory=" + mDirectory);
        if (mBinding == null) {
            Log.d(TAG, "mBinding=null");
            return;
        }
        SharedPreferences.Editor editor =
                getSharedPreferences(MAIN_ACTIVITY,
                        MODE_PRIVATE).edit();
        editor.putString(PREF_DIRECTORY, mDirectory);
        editor.apply();
        if (mDirectory == null) {
            mBinding.directoryName.setText(R.string.default_directory_name);
        } else {
            mBinding.directoryName.setText(mDirectory);
        }
    }

    /**
     * Determines if the needed permission is granted.
     *
     * @return If granted.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean isReadMediaImagesGranted() {
        return ContextCompat.checkSelfPermission(this, mReadImagePermission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Determines if the needed permission is granted and calls
     * requestPermissions if it has not been previously denied.
     */
    protected void requestLocationPermission() {
        Log.d(TAG, this.getClass().getSimpleName()
                + " mReadMediaImagesAsked=" + mReadMediaImagesAsked);
        // Check location
        if (!isReadMediaImagesGranted() && !mReadMediaImagesAsked) {
            // One or both location permissions are not granted
            Log.d(TAG, "    Calling requestPermissions");
            requestPermissions(new String[]{
                            mReadImagePermission},
                    REQ_READ_MEDIA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[]
                                                   permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: mReadMediaImagesAsked="
                + mReadMediaImagesAsked);
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
        if (requestCode == REQ_READ_MEDIA_PERMISSION) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(mReadImagePermission)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "mReadImagePermission granted");
                    } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Log.d(TAG, "mReadImagePermission denied");
                    }
                }
            }
        }
    }
}