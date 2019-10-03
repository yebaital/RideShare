package studio.redrim.rideshare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends Permissions {

    private Button driverBtn, customerBtn;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        mAuth = FirebaseAuth.getInstance();

        driverBtn = findViewById(R.id.driver_button);
        customerBtn = findViewById(R.id.customer_button);

        if (checkPermissions()) {
//            if (mAuth.getCurrentUser() != null) {
//                startActivity(new Intent(WelcomeActivity.this, DriversMapActivity.class));
//            }

            driverBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(WelcomeActivity.this, DriverLoginActivity.class));
                }
            });

            customerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(WelcomeActivity.this, CustomerLoginActivity.class));
                }
            });
        }
    }
}
