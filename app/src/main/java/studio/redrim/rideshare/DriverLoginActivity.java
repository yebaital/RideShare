package studio.redrim.rideshare;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private Button loginBtn, registerBtn;
    private TextView registerPrompt, loginPrompt, label;
    private EditText emailText, passwordText;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userId;
    private FirebaseDatabase db;
    private DatabaseReference driverRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();

        registerBtn = findViewById(R.id.driver_register_button);
        loginBtn = findViewById(R.id.driver_login_button);
        label = findViewById(R.id.driver_label);
        registerPrompt = findViewById(R.id.driver_register_prompt);
        loginPrompt = findViewById(R.id.driver_login_prompt);
        emailText = findViewById(R.id.driver_email);
        passwordText = findViewById(R.id.driver_password);
        progressBar = findViewById(R.id.driver_progress);

        registerPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                label.setText(getString(R.string.driver_registration));
                registerBtn.setVisibility(View.VISIBLE);
                loginPrompt.setVisibility(View.VISIBLE);
                loginBtn.setVisibility(View.GONE);
                registerPrompt.setVisibility(View.GONE);
            }
        });

        loginPrompt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                label.setText(getString(R.string.driver_login));
                registerBtn.setVisibility(View.GONE);
                loginPrompt.setVisibility(View.GONE);
                loginBtn.setVisibility(View.VISIBLE);
                registerPrompt.setVisibility(View.VISIBLE);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailText.getText().toString().trim();
                String password = passwordText.getText().toString();
                LoginDriver(email, password);
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailText.getText().toString().trim();
                String password = passwordText.getText().toString();
                RegisterDriver(email, password);
            }
        });

    }

    private void RegisterDriver(String email, String password) {
        if (isDataValid(email, password)) {
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.GONE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                currentUser = mAuth.getCurrentUser();
                                userId = currentUser.getUid();
                                driverRef = db.getReference().child("users").child("drivers").child(userId);
                                driverRef.setValue(true);
                                Toast.makeText(getApplicationContext(), "Registration Successful!", Toast.LENGTH_LONG).show();
                                goHome();
                            } else {
                                progressBar.setVisibility(View.GONE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(getApplicationContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void LoginDriver(String email, String password) {
        if (isDataValid(email, password)) {
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.GONE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(getApplicationContext(), "Login Successful!", Toast.LENGTH_LONG).show();
                                goHome();
                            } else {
                                progressBar.setVisibility(View.GONE);
                                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                                Toast.makeText(getApplicationContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private boolean isDataValid(String email, String password) {
        if (email.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Email cannot be empty.", Toast.LENGTH_LONG).show();
            return false;
        } else if (password.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Password cannot be empty.", Toast.LENGTH_LONG).show();
            return false;
        } else if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password must be more than characters long", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return true;
        }
    }

    private void goHome() {
        startActivity(new Intent(DriverLoginActivity.this, DriversMapActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null){
//            startActivity(new Intent(DriverLoginActivity.this, DriversMapActivity.class));
        }
    }
}
