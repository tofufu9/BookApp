package com.example.bookapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    //view binding
    private ActivityRegisterBinding binding;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, begin register
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String name = "", email = "", password = "";

    private void validateData() {
        /*Before creating account, lets do some data validation*/

        //get data
        name = binding.nameEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();
        String cPassword = binding.cPasswordEt.getText().toString().trim();

        //validate data
        if (TextUtils.isEmpty(name)){
            //name edit text is empty, must enter name
            Toast.makeText(this,"Enter your name...", Toast.LENGTH_SHORT).show();
        }
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //email is either not entered or email pattern is invalid, don't allow to continue in that case
            Toast.makeText(this,"Invalid email pattern...!", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(password)){
            //password edit text is empty, must enter password
            Toast.makeText(this,"Enter password...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(cPassword)){
            //confirm password edit text is empty, must enter confirm password
            Toast.makeText(this,"Confirm Password...", Toast.LENGTH_SHORT).show();
        }
        else if (!password.equals(cPassword)){
            //password and confirm password doesn't match, don't allow to continue
            Toast.makeText(this,"Password doesn't match...", Toast.LENGTH_SHORT).show();
        }
        else {
            // all data is validated, begin creating account
            createUserAccount();
        }
    }

    private void createUserAccount() {
        //show progress
        progressDialog.setMessage("Creating account...");
        progressDialog.show();

        //create user in firebase auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //account creation success
                        updateUserInfo();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        //account creating failed
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUserInfo() {
        progressDialog.setMessage("Saving user info...");

        //timestamp
        long timestamp = System.currentTimeMillis();

        //get current user uid, since user is registered so we can get now
        String uid = firebaseAuth.getUid();

        //setup data to add in db
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", uid);
        hashMap.put("email", email);
        hashMap.put("name", name);
        hashMap.put("profileImage", ""); //add empty, will do later
        hashMap.put("userType", "user"); //possible values are user, admin: will make admin manually in firebase realtime database by changing the balue
        hashMap.put("timestamp", timestamp);

        //set data to db
        DatabaseReference ref = FirebaseDatabase.getInstance("https://book-app-b55d2-default-rtdb.firebaseio.com/").getReference("Users");
        ref.child(uid)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                    //data added to db
                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this,"Account created...", Toast.LENGTH_SHORT).show();
                        //since user account is created so start dashboard of user
                        startActivity(new Intent(RegisterActivity.this, DashboardUserActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                    //data failed adding to db
                   progressDialog.dismiss();
                   Toast.makeText(RegisterActivity.this,""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}