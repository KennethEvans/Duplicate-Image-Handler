package net.kenevans.android.duplicateimagehandler;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.documentfile.provider.DocumentFile;


public class GroupActivity extends AppCompatActivity implements IConstants {
    private Handler mHandler;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private List<Image> mImages;
    private List<Image> mInvalidImages;
    private List<Group> mGroups;
    private boolean mHandleDelete;
    private String mDirectory;
    private boolean mUseSubdirectories;

    ActivityResultLauncher<IntentSenderRequest> deleteResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            Log.d(TAG, "deleteResultLauncher: "
                            + ActivityResult.resultCodeToString(result.getResultCode()));
                            mHandleDelete = false;
                            if (result.getResultCode() == RESULT_OK) {
                                Log.d(TAG, "deleteResultLauncher succeeded");
                                mHandleDelete = true;
                            } else if (result.getResultCode() == RESULT_CANCELED) {
                                Log.d(TAG, "deleteResultLauncher cancelled");
                                mHandleDelete = false;
                            }
                        }
                    }
            );


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, this.getClass().getSimpleName() + ".onCreate:");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        mListView = findViewById(R.id.list);
        mDirectory = null;
        mUseSubdirectories = true;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDirectory = extras.getString(DIRECTORY_CODE, null);
            mUseSubdirectories = extras.getBoolean(USE_SUBDIRECTORIES_CODE,
                    true);
        }
        mImages = ImageRepository.getImages(this, mDirectory,
                mUseSubdirectories);

        mProgressBar = findViewById(R.id.progressBar);
        mHandler = new Handler(getMainLooper());
        startFind();
    }

    @Override
    public void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + " onResume:"
                + "mHandleDelete=" + mHandleDelete);
        super.onResume();
        if (mHandleDelete) {
            mImages = ImageRepository.getImages(this, mDirectory,
                    mUseSubdirectories);
            startFind();
        }
    }

    @Override
    protected void onPause() {
        Log.v(TAG, this.getClass().getSimpleName() + " onPause");
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group, menu);
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
        } else if (id == R.id.remove_checked) {
            removeCheckedImages();
            return true;
        } else if (id == R.id.problem_images) {
            showProblemImages();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent
//            data) {
//        mHandleDelete = false;
//        if (requestCode == REQ_DELETE_IMAGES) {
//            if (resultCode == Activity.RESULT_OK) {
//                Log.d(TAG, "REQ_DELETE_IMAGES succeeded");
//                mHandleDelete = true;
//            } else if (resultCode == Activity.RESULT_CANCELED) {
//                Log.d(TAG, "REQ_DELETE_IMAGES cancelled");
//            }
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }

    private void startFind() {
        Log.d(TAG, this.getClass().getSimpleName() + ".startFind:");
        new Thread(() -> {
            Log.d(TAG, "GroupActivity: run: images=" + mImages.size());
            mGroups = new ArrayList<>();
            runOnUiThread(() ->
                    mListView.setAdapter(new GroupAdapter(GroupActivity.this)
                    ));
            final List<Group> groups =
                    SimilarImage.find(GroupActivity.this, mImages,
                            mProgressBar);
            int nMultiple = 0;
            List<Group> prunedGroups = new ArrayList<>();
            for (Group group : groups) {
                if (group.getImages().size() > 1) {
                    nMultiple++;
                    prunedGroups.add(group);
                }
            }
            Log.d(TAG, "Found " + nMultiple + " multiple-image groups out" +
                    " of "
                    + groups.size() + " groups");

            String msg = "Found " + mImages.size() + " images in "
                    + prunedGroups.size() + " multiple-image groups";
            mHandler.post(() -> {
                mProgressBar.setVisibility(View.GONE);
//                        Utils.infoMsg(GroupActivity.this, msg);
            });

            mHandler.post(() -> {
                Log.d(TAG, msg);
                // Find the ones that are invalid (no thumbnail)
                mInvalidImages = new ArrayList<>();
                for (Image image : mImages) {
                    if (!image.isValid()) {
                        mInvalidImages.add(image);
                    }
                }
                Log.d(TAG,
                        mInvalidImages.size() + " invalid of " + mImages.size() +
                                " images");
                mGroups = prunedGroups;
                // Find the number of images that in groups
                int nGroupImages = 0;
                for (Group group : mGroups) {
                    nGroupImages += group.getImages().size();
                }
                Log.d(TAG, "Final: images=" + mImages.size() + " groups="
                        + mGroups.size() + " with " + nGroupImages
                        + " group images");
                mListView.setAdapter(new GroupAdapter(GroupActivity.this));
            });
        }).start();
    }

    /**
     * Displays info about the current configuration
     */
    private void info() {
        try {
            StringBuilder info = new StringBuilder();
            if (mGroups == null) {
                info.append("There are no groups").append("\n");
            } else {
                info.append(mImages.size()).
                        append(" images in ").append(mGroups.size())
                        .append(" multiple-image groups").append("\n");
            }
            int nChecked = 0;
            for (Image image : mImages) {
                if (image.isChecked()) nChecked++;
            }
            info.append("Number of images checked: ").append(nChecked);
            Utils.infoMsg(this, info.toString());
        } catch (Throwable t) {
            Utils.excMsg(this, "Error showing info", t);
            Log.e(TAG, "Error showing info", t);
        }
    }

    /**
     * Removes the checked images. Runs in a thread as it can take some
     * time.
     * Calls startFind when done, which also runs in a thread. Each of these
     * threads will have a new progress bar.
     */
    private void removeCheckedImages() {
        Log.d(TAG, this.getClass().getSimpleName() +
                ".removeCheckedImages:");
        List<Image> checkedImages = new ArrayList<>();
        for (Image image : mImages) {
            if (image.isChecked()) {
                checkedImages.add(image);
            }
        }
        if (checkedImages.size() == 0) {
            Utils.warnMsg(this, "There are no checked images to remove");
            return;
        }
        List<Uri> uriList = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        for (Image image : checkedImages) {
            uriList.add(ContentUris.withAppendedId(uri, image.getId()));
        }
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            Log.d(TAG, "removeCheckedImages: Launching PendingIntent"
                    + " nImages=" + uriList.size());
            PendingIntent pi =
                    MediaStore.createDeleteRequest(this.getContentResolver(),
                            uriList);
            try {
                IntentSenderRequest senderRequest =
                        new IntentSenderRequest.Builder(pi)
                                .setFillInIntent(null)
                                .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, 0)
                                .build();
                deleteResultLauncher.launch(senderRequest);
            } catch (Exception ex) {
                Log.d(TAG, "removeCheckedImages: Error launching " +
                        "PendingIntent");
            }
        } else {
            Log.d(TAG, "removeCheckedImages: Build < 30");
            ContentResolver resolver = this.getContentResolver();
            for (Uri uri1 : uriList) {
                try {
                    resolver.delete(uri1, null, null);
                } catch (SecurityException securityException) {
                    RecoverableSecurityException recoverableSecurityException =
                            (RecoverableSecurityException) securityException;
                    IntentSenderRequest senderRequest =
                            new IntentSenderRequest.Builder(
                                    recoverableSecurityException.getUserAction()
                                            .getActionIntent().getIntentSender()).build();
                    deleteResultLauncher.launch(senderRequest);
                }
            }
        }
    }

    /**
     * Removes the checked images. Runs in a thread as it can take some
     * time.
     * Calls startFind when done, which also runs in a thread. Each of these
     * threads will have a new progress bar.
     */
    private void removeCheckedImages0() {
        Log.d(TAG, this.getClass().getSimpleName() +
                ".removeCheckedImages:");
        List<Image> checkedImages = new ArrayList<>();
        for (Image image : mImages) {
            if (image.isChecked()) {
                checkedImages.add(image);
            }
        }
        if (checkedImages.size() == 0) {
            Utils.warnMsg(this, "There are no checked images to remove");
            return;
        }
        mListView.setAdapter(new GroupAdapter(GroupActivity.this));
        if (mProgressBar != null) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setMax(checkedImages.size());
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.VISIBLE);
        }
        new Thread(() -> {
            mGroups = new ArrayList<>();
            runOnUiThread(() ->
                    mListView.setAdapter(new GroupAdapter(GroupActivity.this)
                    ));
            Uri persistedUri =
                    getContentResolver().getPersistedUriPermissions().get(0).getUri();
            DocumentFile documentFile = DocumentFile.fromTreeUri(this,
                    persistedUri);
            List<Image> errorImages = new ArrayList<>();
            boolean res;
            int nImages = 0;
            for (Image image : checkedImages) {
                nImages++;
                File file = new File(image.getPath());
                DocumentFile nextDocument =
                        documentFile.findFile(file.getName());
                try {
                    res = DocumentsContract.deleteDocument(getContentResolver(),
                            nextDocument.getUri());
                    if (!res) {
                        errorImages.add(image);
                    } else {
                        mImages.remove(image);
                    }
                } catch (Exception ex) {
                    Utils.excMsg(this, "Error deleting " + file, ex);
                }
                int finalNImages = nImages;
                GroupActivity.this.runOnUiThread(() ->
                        mProgressBar.setProgress(finalNImages)
                );
            }
            if (errorImages.size() > 0) {
                StringBuilder msg = new StringBuilder();
                msg.append("Errors deleting:").append("\n");
                for (Image image2 : errorImages) {
                    msg.append(image2.getPath()).append("\n");
                }
                Utils.errMsg(this, msg.toString());
            }
            startFind();
        }).start();
    }

    /**
     * Show invalid images.
     */
    private void showProblemImages() {
        // Create the info message
        StringBuilder info = new StringBuilder();
        if (mInvalidImages.size() == 0) {
            info.append("There are no invalid images");
        } else {
            info.append("\n");
            for (Image image : mInvalidImages) {
                info.append(image.toString()).append("\n");
            }
        }
        TextView textView = new TextView(this);
        textView.setText(info.toString());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.invalid_images_title);
        builder.setView(textView);
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> dialog.dismiss());
//        builder.setNegativeButton(android.R.string.cancel,
//                (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private class GroupAdapter extends ArrayAdapter<Group> {

        public GroupAdapter(@NonNull Context context) {
            super(context, 0, mGroups);
        }

        @Override
        public int getCount() {
            return mGroups == null ? 0 : mGroups.size();
        }

        @Override
        public Group getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        /**
         * Resets the contents of the View.
         *
         * @param position The position.
         * @param holder   The ViewHolder.
         * @param parent   The parent.
         */
        public void resetConvertView(int position, ViewHolder holder,
                                     ViewGroup parent) {
            holder.name.setText(GroupActivity.this.getString(
                    R.string.group_title, position));
            holder.linearLayout.removeAllViews();
            // Get the images for this group
            List<Image> groupImages = mGroups.get(position).getImages();
//            Log.d(TAG, "    resetConvertView: position=" + position);
            for (Image image : groupImages) {
                ConstraintLayout constraintLayout =
                        (ConstraintLayout) LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.image_info_item, parent
                                        , false);
                LinearLayout.LayoutParams param =
                        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                holder.linearLayout.addView(constraintLayout, param);

                ImageView imageView =
                        (ImageView) constraintLayout.getViewById(R.id.image);
                Glide.with(GroupActivity.this)
                        .load(image.getPath())
                        .error(R.drawable.invalid_image)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(imageView);
                imageView.setOnClickListener(v ->
                        Utils.infoMsg(GroupActivity.this,
                                image.toString()));

                TextView imageName =
                        (TextView) constraintLayout.getViewById(R.id.image_name);
                imageName.setText(image.getName());

                TextView imageSize =
                        (TextView) constraintLayout.getViewById(R.id.image_size);
                String readableSize =
                        Utils.humanReadableByteCountBin(image.getSize());
                imageSize.setText(readableSize);

                TextView imagePath =
                        (TextView) constraintLayout.getViewById(R.id.image_path);
                imagePath.setText(image.getPath());

                CheckBox checkBox =
                        (CheckBox) constraintLayout.getViewById(R.id.checkBox);
                checkBox.setOnCheckedChangeListener((buttonView,
                                                     isChecked) ->
                        image.setChecked(isChecked));
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
//            Log.d(TAG, "getView: position=" + position + " convertView=null: "
//                    + (convertView == null));
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_list_group, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.group_name);
                holder.linearLayout = convertView.findViewById(R.id.images);
                resetConvertView(position, holder, parent);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                resetConvertView(position, holder, parent);
            }
            return convertView;
        }

        private class ViewHolder {
            TextView name;
            LinearLayout linearLayout;
        }
    }
}
