package com.chihyao.bingo;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 100;
    private static final String URL_RTDB = "https://bingo-e33cc-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private FirebaseAuth auth;
    private TextView nickText;
    private ImageView avatar;
    private Group groupAvatars;
    int[] avatarIds = {R.drawable.avatar_0, R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3, R.drawable.avatar_4, R.drawable.avatar_5, R.drawable.avatar_6 };
    private Member member;
    private FirebaseRecyclerAdapter<GameRoom, RoomHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        findViews();
    }

    private void findViews() {
        nickText = findViewById(R.id.nickname);
        avatar = findViewById(R.id.avatar);
        nickText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNickDialog(nickText.getText().toString());
            }
        });
        groupAvatars = findViewById(R.id.group_avatars);
        groupAvatars.setVisibility(View.GONE);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupAvatars.setVisibility(
                        groupAvatars.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });
        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
        findViewById(R.id.avatar_5).setOnClickListener(this);
        findViewById(R.id.avatar_6).setOnClickListener(this);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText roomEdit = new EditText(MainActivity.this);
                roomEdit.setText("Welcome");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Game Room")
                        .setMessage("Please input your Room Title")
                        .setView(roomEdit)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String roomTitle = roomEdit.getText().toString();
                                GameRoom room = new GameRoom(roomTitle, member);
                                FirebaseDatabase.getInstance(URL_RTDB).getReference("rooms")
                                        .push().setValue(room);
                            }
                        }).show();
            }
        });
        //Recycler
        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Query query = FirebaseDatabase.getInstance(URL_RTDB)
                .getReference("rooms")
                .limitToLast(30);
        FirebaseRecyclerOptions<GameRoom> options =
                new FirebaseRecyclerOptions.Builder<GameRoom>()
                    .setQuery(query, GameRoom.class)
                    .build();
        adapter = new FirebaseRecyclerAdapter<GameRoom, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull @NotNull MainActivity.RoomHolder holder, int position, @NonNull @NotNull GameRoom model) {
                holder.image.setImageResource(avatarIds[model.init.avatarId]);
                holder.titleText.setText(model.title);
            }

            @NonNull
            @NotNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.room_row, parent, false);
                return new RoomHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    public class RoomHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView titleText;

        public RoomHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.room_image);
            titleText = itemView.findViewById(R.id.room_title);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
        adapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_menu_signout:
                FirebaseAuth.getInstance().signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull @NotNull FirebaseAuth auth) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.EmailBuilder().build(),
                                    new AuthUI.IdpConfig.GoogleBuilder().build()
                            ))
                            .setIsSmartLockEnabled(false)
                            .build()
                    ,RC_SIGN_IN);
        } else {
            Log.d(TAG, "onAuthStateChanged: " + auth.getCurrentUser().getEmail() + "/" + auth.getCurrentUser().getUid());
            String displayName = user.getDisplayName();
            FirebaseDatabase.getInstance(URL_RTDB)
                    .getReference("users")
                    .child(user.getUid())
                    .child("displayName")
                    .setValue(user.getDisplayName());
            FirebaseDatabase.getInstance(URL_RTDB)
                    .getReference("users")
                    .child(user.getUid())
                    .child("uid")
                    .setValue(user.getUid());
            FirebaseDatabase.getInstance(URL_RTDB)
                    .getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            member = snapshot.getValue(Member.class);
                            if (member != null) {
                                if (member.nickname != null) {
                                    nickText.setText(member.nickname);
                                } else {
                                    showNickDialog(displayName);
                                }
                                avatar.setImageResource(avatarIds[member.avatarId]);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {

                        }
                    });
/*            FirebaseDatabase.getInstance(URL_RTDB)
                    .getReference("users")
                    .child(user.getUid())
                    .child("nickname")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                            if (snapshot.getValue() != null) {
                                String nickname = (String) snapshot.getValue();
                            } else {
                                showNickDialog(displayName);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull @NotNull DatabaseError error) {

                        }
                    });*/
        }

    }

    private void showNickDialog(String displayName) {
        EditText nickEdit = new EditText(this);
        nickEdit.setText(displayName);
        new AlertDialog.Builder(this)
                .setTitle("your nickname")
                .setMessage("Please input your nickname")
                .setView(nickEdit)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nickname = nickEdit.getText().toString();
                        FirebaseDatabase.getInstance(URL_RTDB).getReference("users")
                                .child(auth.getUid())
                                .child("nickname")
                                .setValue(nickname);
                    }
                }).show();
    }

    @Override
    public void onClick(View view) {
        if (view instanceof ImageView) {
            int selectedId = 0;
            switch (view.getId()) {
                case R.id.avatar_0:
                    selectedId = 0;
                    break;
                case R.id.avatar_1:
                    selectedId = 1;
                    break;
                case R.id.avatar_2:
                    selectedId = 2;
                    break;
                case R.id.avatar_3:
                    selectedId = 3;
                    break;
                case R.id.avatar_4:
                    selectedId = 4;
                    break;
                case R.id.avatar_5:
                    selectedId = 5;
                    break;
                case R.id.avatar_6:
                    selectedId = 6;
                    break;
            }
            groupAvatars.setVisibility(View.GONE);
            FirebaseDatabase.getInstance(URL_RTDB).getReference("users")
                    .child(auth.getUid())
                    .child("avatarId")
                    .setValue(selectedId);
        }
    }
}