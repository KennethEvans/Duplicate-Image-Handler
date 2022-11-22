package net.kenevans.android.duplicateimagehandler;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to find similar images. Each image has a fingerprint and a finger.
 * <br><br>
 * The fingerprint is is a String derived from a 96 x96 thumbnail of the
 * image scaled to 8 x 8 then converted to grayscale.
 * <br><br>
 * The finger is a 256 character String with values 0 or 1 depending on if
 * the corresponding pixel in the fingerprint is less than the average pixel
 * gray value or not.
 */
public class SimilarImage implements IConstants {
    // The condition for a similar image is if the Hamming distance is less
    // than this value.
    public static final int SIMILARITY_CONDITION_DEFAULT = 5;

    /**
     * Loops over the given images arranging the similar ones into groups. This
     * version has no progress bar to update.
     *
     * @param activity The Activity calling this method.
     * @param images   The images.
     * @return The list of groups.
     */
    @SuppressWarnings("unused")
    public static List<Group> find(Activity activity, List<Image> images) {
        return find(activity, images, null);
    }

    /**
     * Loops over the given images arranging the similar ones into groups. If
     * the progressBar is not null, it is updated using runOnUiThread.
     *
     * @param activity    The Activity calling this method.
     * @param images      The images.
     * @param progressBar A progress bar.
     * @return The list of groups.
     */
    public static List<Group> find(Activity activity, List<Image> images,
                                   ProgressBar progressBar) {
        // Calculate all the fingerprints
        calculateFingerPrint(activity, images, progressBar);

        // Find duplicates
        List<Group> groups = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            if (!image.isValid()) continue;
            List<Image> temp = new ArrayList<>();
            temp.add(image);
            for (int j = i + 1; j < images.size(); j++) {
                Image image2 = images.get(j);
                if (!image.isValid()) continue;
                if(image.isSimilar()) continue;
                int dist = hammingDist(image.getFinger(), image2.getFinger());
                if (dist < SIMILARITY_CONDITION_DEFAULT) {
                    temp.add(image2);
//                    images.remove(image2);
//                    j--;
                }
            }
            Group group = new Group();
            group.setImages(temp);
            groups.add(group);
        }
        return groups;
    }

    /**
     * Calculates the finger prints for all images. This can take some time.
     * This version has no progress bar to update.
     *
     * @param activity The Activity calling this method.
     * @param images   The images.
     */
    @SuppressWarnings("unused")
    private static void calculateFingerPrint(Activity activity,
                                             List<Image> images) {
        calculateFingerPrint(activity, images, null);
    }

    /**
     * Calculates the finger prints for all images. This can take some time.
     * If the progressBar is not null, it is updated using runOnUiThread.
     *
     * @param activity    The Activity calling this method.
     * @param images      The images.
     * @param progressBar A progress bar.
     */
    private static void calculateFingerPrint(Activity activity,
                                             List<Image> images,
                                             ProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setMax(images.size());
            progressBar.setProgress(0);
        }
        float scale_width, scale_height;
        Log.d(TAG, "calculateFingerPrint: images: " + images.size());
        Bitmap bitmap;
        int nNull = 0;
        int nProcessed = 0;
        try {
            for (Image image : images) {
                nProcessed++;
                if (progressBar != null && nProcessed % 10 == 0) {
                    int finalNProcessed = nProcessed;
                    activity.runOnUiThread(() ->
                            progressBar.setProgress(finalNProcessed)
                    );
                }
                // MICRO_KIND is 96 x 96
                bitmap =
                        MediaStore.Images.Thumbnails
                                .getThumbnail(activity.getContentResolver(),
                                        image.getId(),
                                        MediaStore.Images.Thumbnails.MICRO_KIND,
                                        null);
                if (bitmap == null) {
                    nNull++;
                    Log.d(TAG, "Null bitmap for " + image.getPath());
                    image.setValid(false);
                    continue;
                }
                scale_width = 8.0f / bitmap.getWidth();
                scale_height = 8.0f / bitmap.getHeight();
                Matrix matrix = new Matrix();
                matrix.postScale(scale_width, scale_height);

                Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                image.setFinger(getFingerPrint(scaledBitmap));

                bitmap.recycle();
                scaledBitmap.recycle();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Exception in calculateFingerPrint", ex);
        }
        Log.d(TAG, "End calculateFingerPrint: nNull="
                + nNull + "/" + images.size());
    }

    private static long getFingerPrint(Bitmap bitmap) {
        double[][] grayPixels = getGrayPixels(bitmap);
        double grayAvg = getGrayAvg(grayPixels);
        return getFingerPrint(grayPixels, grayAvg);
    }

    /**
     * Calculates the fingerprint by setting characters to 0 or 1 depending
     * on if the corresponding pixel in the fingerprint is less than the average
     * pixel gray value or not. Then calculates the finger
     *
     * @param pixels The array of pixels in the fingerprint.
     * @param avg    The average gray value.
     * @return The finger.
     */
    private static long getFingerPrint(double[][] pixels, double avg) {
        int width = pixels[0].length;
        int height = pixels.length;
        byte[] bytes = new byte[height * width];
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (pixels[i][j] >= avg) {
                    bytes[i * height + j] = 1;
                    stringBuilder.append("1");
                } else {
                    bytes[i * height + j] = 0;
                    stringBuilder.append("0");
                }
            }
        }
        long fingerprint1 = 0;
        long fingerprint2 = 0;
        for (int i = 0; i < 64; i++) {
            if (i < 32) {
                fingerprint1 += (bytes[63 - i] << i);
            } else {
                fingerprint2 += (bytes[63 - i] << (i - 31));
            }
        }
        return (fingerprint2 << 32) + fingerprint1;
    }

    private static double getGrayAvg(double[][] pixels) {
        int width = pixels[0].length;
        int height = pixels.length;
        int count = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                count += pixels[i][j];
            }
        }
        return (double)count / (double)(width * height);
    }

    private static double[][] getGrayPixels(Bitmap bitmap) {
        int width = 8;
        int height = 8;
        double[][] pixels = new double[height][width];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[i][j] = computeGrayValue(bitmap.getPixel(i, j));
            }
        }
        return pixels;
    }

    /**
     * Computes the gray value of the given RGB pixel using 0.3 red, 0.59 green,
     * and 0.11 blue.
     *
     * @param pixel The RGB pixel.
     * @return The gray value.
     */
    private static double computeGrayValue(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = (pixel) & 255;
        return 0.3 * red + 0.59 * green + 0.11 * blue;
    }

    /**
     * Calculates the Hamming distance between the two given fingerprints.
     * The Hamming distance is the number of positions at which the
     * corresponding symbols are different.
     *
     * @param finger1 The first fingerprint.
     * @param finger2 The second fingerprint.
     * @return The hamming distance.
     */
    private static int hammingDist(long finger1, long finger2) {
        int dist = 0;
        long result = finger1 ^ finger2;
        while (result != 0) {
            ++dist;
            result &= result - 1;
        }
        return dist;
    }
}
