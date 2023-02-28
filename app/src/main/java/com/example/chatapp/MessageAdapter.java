package com.example.chatapp;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import java.util.List;


public class MessageAdapter extends ArrayAdapter<ChatMessage> {

    private List<ChatMessage> messages;
    private Activity activity;

    public MessageAdapter(Activity context, int resource, List<ChatMessage> messages) {
        super(context, resource, messages);

        this.messages = messages;
        this.activity = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        LayoutInflater layoutInflater = (LayoutInflater) activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        ChatMessage chatMessage = getItem(position);
        int layoutResource = 0;
        int viewType = getItemViewType(position);

        if (viewType == 0) {
            layoutResource = R.layout.my_message_item;
        }else {
            layoutResource = R.layout.your_message_item;
        }

        if (convertView != null){

            viewHolder = (ViewHolder) convertView.getTag();
        }else {
            convertView = layoutInflater.inflate(layoutResource, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        }

        boolean isText = chatMessage.getImageUrl() == null;

        if (isText){
            viewHolder.messageTextView.setVisibility(View.VISIBLE);
            viewHolder.photoImageView.setVisibility(View.GONE);
            viewHolder.messageTextView.setText(chatMessage.getText());
        }else {
            viewHolder.messageTextView.setVisibility(View.GONE);
            viewHolder.photoImageView.setVisibility(View.VISIBLE);
            Glide.with(viewHolder.photoImageView.getContext()).load(chatMessage.getImageUrl()).into(viewHolder.photoImageView);
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {

        int flag;
        ChatMessage chatMessage = messages.get(position);

        if (chatMessage.isMine()){
            flag = 0;
        }else {
            flag = 1;
        }

        return flag;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    private class ViewHolder {

        private ImageView photoImageView;
        private TextView messageTextView;

        public ViewHolder(View view){
            photoImageView = view.findViewById(R.id.photoImageView);
            messageTextView = view.findViewById(R.id.messageTextView);

        }

    }
}
