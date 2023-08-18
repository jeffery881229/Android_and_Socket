package com.example.android_and_socket;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private EditText nameEditText;
    private Button connectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        nameEditText = findViewById(R.id.NameText);
        connectButton = findViewById(R.id.ConnectButton);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToServer();
            }
        });
    }

    private void connectToServer() {
        String name = nameEditText.getText().toString();

        Intent chatIntent = new Intent(MainActivity.this, Chat.class);
        chatIntent.putExtra("name", name); // Pass the name to the Chat activity
        startActivity(chatIntent);
    }
}

