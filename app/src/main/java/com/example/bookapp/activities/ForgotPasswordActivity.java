package com.example.bookapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bookapp.databinding.ActivityForgotPasswordBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    //View binding
    private ActivityForgotPasswordBinding binding;

    //firebase auth
    private FirebaseAuth firebaseAuth;

    //progress dialog
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        //init/setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onBackPressed();
            }
        });

        //handle click, begin recovery password
        binding.submitBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                validateData();
            }
        });
    }

    private String email = "";
    private void validateData(){
        //get data i.e. email
        email = binding.emailEt.getText().toString().trim();

        //validate data e.g. shouldn't empty and should be valid format
        if(email.isEmpty()){
            Toast.makeText(this, "Enter email...", Toast.LENGTH_SHORT).show();
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Toast.makeText(this, "Invalid enter email...", Toast.LENGTH_SHORT).show();
        }
        else {
            recoverPassword();
        }
    }

    private void recoverPassword(){
        //show progress
        progressDialog.setMessage("Sending password recovery instructions to "+email);
        progressDialog.show();

        //begin sending recovery
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(new OnSuccessListener<Void>(){
                    @Override
                    public void onSuccess(Void unused){
                        //sent
                        progressDialog.dismiss();
                        Toast.makeText(ForgotPasswordActivity.this, "Instructions to", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener(){
                    @Override
                    public void onFailure(@NonNull Exception e){
                //failed to send
                progressDialog.dismiss();
                Toast.makeText(ForgotPasswordActivity.this,"Failed to send due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}