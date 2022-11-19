package net.kenevans.android.duplicateimagehandler;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gavin on 2017/3/27.
 */

public class ImageRepository implements IConstants {

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.ImageColumns.RELATIVE_PATH,
    };

    public static List<Image> getImages(Context context) {
        return getImages(context, null);
    }

    public static List<Image> getImages(Context context, String directory) {
        String selection;
        String[] selectionArgs;
        if (directory == null) {
            selection = null;
            selectionArgs = null;
        } else {
            selection = MediaStore.Images.ImageColumns.RELATIVE_PATH
                    + " LIKE?";
            selectionArgs = new String[] {directory + "%"};
        }
        if(selection == null) {
            Log.d(TAG, "getImages: selection=selection"
                    + " selectionArgs=" + selectionArgs);
        } else {
            Log.d(TAG, "getImages: selection=\"" + selection + "\""
                    + " selectionArgs[0]=\"" + selectionArgs[0] + "\"");
        }
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = context.getContentResolver();
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " desc";
        Cursor cursor = contentResolver.query(uri, STORE_IMAGES,
                selection, selectionArgs, sortOrder);

        List<Image> result = new ArrayList<>();

        while (cursor != null && cursor.moveToNext()) {
            Image image = new Image();

            image.setId(cursor.getLong(0));
            image.setPath(cursor.getString(1));
            image.setName(cursor.getString(2));
            image.setMimetype(cursor.getString(3));
            image.setSize(cursor.getLong(4));
            image.setRelativePath(cursor.getString(5));

            result.add(image);
        }

        if (cursor != null) cursor.close();

        Log.d(TAG, "getImages: size=" + result.size());

        return result;
    }

}
