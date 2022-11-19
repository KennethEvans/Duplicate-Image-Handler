package net.kenevans.android.duplicateimagehandler;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * Created by gavin on 2017/3/27.
 */

public class GroupActivity extends AppCompatActivity implements IConstants {

    private Handler mHandler;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, this.getClass().getSimpleName() + ".onCreate:");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        PermissionsUtils.getPermissions(this);

        final ListView listView = (ListView) findViewById(R.id.list);
        String directory = null;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            directory = extras.getString(DIRECTORY_CODE, null);
        }
        final List<Image> images = ImageRepository.getImages(this, directory);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        mHandler = new Handler(getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "GroupActivity: run:");
                final List<Group> groups =
                        SimilarImage.find(GroupActivity.this, images);
                int nMultiple = 0;
                List<Group> prunedGroups = new ArrayList<>();
                for (Group group : groups) {
                    if (group.getImages().size() > 1) {
                        nMultiple++;
                        prunedGroups.add(group);
                    }
                }
                Log.d(TAG, "GroupActivity: mHandler.post: size > 1: " +
                        +nMultiple + "/" + groups.size());
                String msg = "Found " + images.size() + " images in "
                        + prunedGroups.size() + " groups";
                mHandler.post(new Runnable() {
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Utils.infoMsg(GroupActivity.this, msg);
                    }
                });

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, msg);
                        listView.setAdapter(new GroupAdapter(GroupActivity.this,
                                prunedGroups));
                    }
                });
            }
        }).start();
    }

    private class GroupAdapter extends ArrayAdapter<Group> {
        private List<Group> groups;
        private Context mCtx;

        public GroupAdapter(@NonNull Context context, List<Group> groups) {
            super(context, 0, groups);
            this.mCtx = context;
            this.groups = groups;
        }

        @Override
        public int getCount() {
            return groups == null ? 0 : groups.size();
        }

        @Override
        public Group getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_list_group, parent, false);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.group_name);
                holder.name.setText("Group " + position);
                holder.linearLayout = convertView.findViewById(R.id.images);
                holder.linearLayout.removeAllViews();
                // Get the images for this group
                List<Image> images = groups.get(position).getImages();
                for (Image image : images) {
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
                            .centerCrop()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView);
                    TextView textViewName =
                            (TextView) constraintLayout.getViewById(R.id.image_name);
                    textViewName.setText(image.getName());
                    TextView textViewSize =
                            (TextView) constraintLayout.getViewById(R.id.image_size);
                    String readableSize =
                            Utils.humanReadableByteCountBin(image.getSize());
                    textViewSize.setText(readableSize);
                    TextView textViewPath =
                            (TextView) constraintLayout.getViewById(R.id.image_path);
                    textViewPath.setText(image.getPath());
                }
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
                holder.name.setText("Group: " + position);
            }
            return convertView;
        }

        private class ViewHolder {
            TextView name;
            LinearLayout linearLayout;
        }
    }
}
