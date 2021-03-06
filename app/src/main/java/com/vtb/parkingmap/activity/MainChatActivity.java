package com.vtb.parkingmap.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.vtb.parkingmap.R;

public final class MainChatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        EditText editText = findViewById(R.id.editText);
        findViewById(R.id.enterBtn).setOnClickListener(view -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("firstName", editText.getText().toString());
            startActivity(intent);
        });
    }
}