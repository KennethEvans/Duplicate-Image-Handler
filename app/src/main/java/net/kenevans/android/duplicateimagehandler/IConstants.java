package net.kenevans.android.duplicateimagehandler;

public interface IConstants {
    /**
     * Tag to associate with log messages.
     */
    String TAG = "DuplicateImageHandler";

    /**
     * Name of the package for this application.
     */
    String PACKAGE_NAME = "net.kenevans.android.duplicateimagehandler";

    /**
     * Used for SharedPreferences
     */
    String MAIN_ACTIVITY = "MainActivity";

    /**
     * Intent code for the directory.
     */
    String DIRECTORY_CODE = PACKAGE_NAME
            + ".directory";

    /**
     * Key for information URL sent to InfoActivity.
     */
    String INFO_URL = "InformationURL";

    /**
     * Intent code for using subdirectories.
     */
    String USE_SUBDIRECTORIES_CODE = PACKAGE_NAME
            + ".useSubdirectories";

    // Preferences
    String PREF_TREE_URI = "tree_uri";

    // Preferences
    String PREF_DIRECTORY= "directory";

    // Requests
    int REQ_READ_MEDIA_PERMISSION = 1;
}

