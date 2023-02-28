package com.example.chatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ListView messageListView;
    private MessageAdapter adapter;
    private ImageButton sendImageButton;
    private Button sendMessageButton;
    private EditText messageEditText;

    private String userName;
    private String recipientUserId;
    private String recipientUserName;

    private static final int RC_IMAGE_PICKER = 1234;

    private SignInClient oneTapClient;

    private FirebaseAuth auth;
    private FirebaseDatabase firebaseDatabase;

    private DatabaseReference messagesDatabaseReference;
    private ChildEventListener messagesChildEventListener;

    private DatabaseReference usersDatabaseReference;
//    private ChildEventListener usersChildEventListener;

    private FirebaseStorage storage;
    private StorageReference chatImagesStorageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar !=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        auth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        if (intent != null){
            userName = intent.getStringExtra("userName");
            recipientUserId = intent.getStringExtra("recipientUserId");
            recipientUserName = intent.getStringExtra("recipientUserName");
        }

        setTitle(recipientUserName);


        firebaseDatabase =FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        oneTapClient = Identity.getSignInClient(this);

        messagesDatabaseReference = firebaseDatabase.getReference().child("messages");
        usersDatabaseReference = firebaseDatabase.getReference().child("users");
        chatImagesStorageReference = storage.getReference().child("chat_images");

        sendImageButton = findViewById(R.id.sendImageButton);
        sendMessageButton = findViewById(R.id.sendMessageButton);
        messageEditText = findViewById(R.id.messageEditText);

        messageListView = findViewById(R.id.messageListView);
        List<ChatMessage> chatMessages = new ArrayList<>();
        adapter = new MessageAdapter(this, R.layout.messege_item, chatMessages);
        messageListView.setAdapter(adapter);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (charSequence.toString().trim().length() > 0){
                    sendMessageButton.setEnabled(true);
                } else {
                    sendMessageButton.setEnabled(false);
                }

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        messageEditText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(500) {
        }});

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ChatMessage message = new ChatMessage();
                message.setText(messageEditText.getText().toString());
                message.setName(userName);
                message.setSender(auth.getCurrentUser().getUid());
                message.setRecipient(recipientUserId);
                message.setImageUrl(null);

                messagesDatabaseReference.push().setValue(message);

                messageEditText.setText("");
            }
        });

        sendImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Choose an image"), RC_IMAGE_PICKER);

            }
        });

        messagesChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);

                if (message.getSender().equals(auth.getCurrentUser().getUid()) && message.getRecipient().equals(recipientUserId)){
                    message.setMine(true);
                    adapter.add(message);
                }else if (message.getRecipient().equals(auth.getCurrentUser().getUid()) && message.getSender().equals(recipientUserId) ){
                    message.setMine(false);
                    adapter.add(message);
                }


            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };

        messagesDatabaseReference.addChildEventListener(messagesChildEventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.log_out:
                FirebaseAuth.getInstance().signOut();
                oneTapClient.signOut();
                startActivity(new Intent(ChatActivity.this, SingInActivity.class));
                return true;

            case android.R.id.home:
                finish();
                NavUtils.navigateUpTo(ChatActivity.this, new Intent());
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_IMAGE_PICKER && resultCode == RESULT_OK){
            Uri selectedImageUri = data.getData();
            StorageReference imageReference = chatImagesStorageReference.child(selectedImageUri.getLastPathSegment());

            UploadTask uploadTask = imageReference.putFile(selectedImageUri);

            uploadTask = imageReference.putFile(selectedImageUri);

            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Continue with the task to get the download URL
                    return imageReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        ChatMessage chatMessage = new ChatMessage();
                        chatMessage.setImageUrl(downloadUri.toString());
                        chatMessage.setName(userName);
                        chatMessage.setSender(auth.getCurrentUser().getUid());
                        chatMessage.setRecipient(recipientUserId);
                        messagesDatabaseReference.push().setValue(chatMessage);
                    } else {
                        // Handle failures
                        // ...
                    }
                }
            });
        }
    }
}