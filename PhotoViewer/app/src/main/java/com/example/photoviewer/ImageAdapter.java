package com.example.photoviewer;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private static final String TAG = "ImageAdapter";
    private List<Post> postList;
    private OnPostClickListener clickListener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public ImageAdapter(List<Post> postList, OnPostClickListener clickListener) {
        this.postList = postList;
        this.clickListener = clickListener;
        Log.d(TAG, "ImageAdapter created with " + postList.size() + " posts");
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        Log.d(TAG, "onCreateViewHolder called");
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.imageView.setImageBitmap(post.getImageBitmap());
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPostClick(post);
            }
        });
        Log.d(TAG, "onBindViewHolder: position=" + position + ", title=" + post.getTitle());
    }

    @Override
    public int getItemCount() {
        int count = postList.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewItem);
        }
    }
}
