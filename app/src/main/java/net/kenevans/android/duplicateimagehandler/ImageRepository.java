package net.kenevans.android.duplicateimagehandler;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ImageRepository implements IConstants {

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_TAKEN,
    };

    public static List<Image> getImages(Context context) {
        return getImages(context, null, false);
    }

    public static List<Image> getImages(Context context, String directory,
                                        boolean useSubdirectories) {
        String selection;
        String[] selectionArgs;
        if (directory == null) {
            selection = null;
            selectionArgs = null;
        } else {
            selection = MediaStore.Images.ImageColumns.RELATIVE_PATH
                    + " LIKE?";
            String subDirectoriesMarker = useSubdirectories ? "%" : "";
            selectionArgs = new String[]{directory + subDirectoriesMarker};
        }
        if (selection == null) {
            Log.d(TAG, "getImages: selection=selection"
                    + " selectionArgs=" + selectionArgs[0]);
        } else {
            Log.d(TAG, "getImages: selection=\"" + selection + "\""
                    + " selectionArgs[0]=\"" + selectionArgs[0] + "\"");
        }
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " desc";
        List<Image> images = new ArrayList<>();
        try (Cursor cursor = contentResolver.query(uri, STORE_IMAGES,
                selection, selectionArgs, sortOrder)) {
            while (cursor != null && cursor.moveToNext()) {
                Image image = new Image();

                image.setId(cursor.getLong(0));
                image.setPath(cursor.getString(1));
                image.setName(cursor.getString(2));
                image.setMimetype(cursor.getString(3));
                image.setSize(cursor.getLong(4));
                image.setDateModified(1000 * cursor.getLong(5));
                image.setDateTaken(cursor.getLong(6));

                images.add(image);
            }

        } catch (Exception ex) {
            Utils.excMsg(context, "Error getting images", ex);
        }
        Log.d(TAG, "getImages: size=" + images.size());
        return images;
    }
}
