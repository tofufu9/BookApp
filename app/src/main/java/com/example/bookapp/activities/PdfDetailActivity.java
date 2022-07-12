package com.example.bookapp.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapters.AdapterComment;
import com.example.bookapp.adapters.AdapterPdfFavorite;
import com.example.bookapp.databinding.ActivityPdfDetailBinding;
import com.example.bookapp.databinding.DialogCommentAddBinding;
import com.example.bookapp.models.ModelComment;
import com.example.bookapp.models.ModelPdf;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfDetailActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfDetailBinding binding;

    //pdf id, get from intent
    String bookId, bookTitle, bookUrl;

    boolean isInMyFavorite = false;

    private FirebaseAuth firebaseAuth;

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";

    private ProgressDialog progressDialog;
    // Array List to hold Books
    private ArrayList<ModelPdf> pdfArrayList;
    // ArrayList to hold comment
    private ArrayList<ModelComment> commentArrayList;
    // Adapter to set to RecyclerView
    private AdapterComment adapterComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get data from intent e.g. bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");

        //at start hide download button, because we need book url that will load later in functions loadBookDetails();
        binding.downloadBookBtn.setVisibility(View.GONE);

        // init progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite();
        }

        loadBookDetails();
        // increment book view count , whenever this page starts
        MyApplication.incrementBookViewCount(bookId);
        loadComments();
        //handle click, goto previous activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, open to view pdf
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);
            }
        });
        //handle click, open download pdf
        binding.downloadBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG_DOWNLOAD, "onClick: Checking permission");
                if (ContextCompat.checkSelfPermission(PdfDetailActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG_DOWNLOAD, "onclick: Permission already granted, can download book");
                    MyApplication.downloadBook(PdfDetailActivity.this, "" + bookId, "" + bookTitle, "" + bookUrl);
                } else {
                    Log.d(TAG_DOWNLOAD, "onClick: Permission was not granted, request permission...");
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        //handle click, add/remove favorite
        binding.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this, "You're not logged in ", Toast.LENGTH_SHORT).show();
                } else {
                    if (isInMyFavorite) {
                        //in favorite, remove from favorite
                        MyApplication.removeFromFavorite(PdfDetailActivity.this, bookId);
                    } else {
                        //not in favorite, add to favorite
                        MyApplication.addToFavorite(PdfDetailActivity.this, bookId);
                    }
                }
            }
        });

        // handle click, show comment add dialog
        binding.addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Requirements: User must be logged in to add comment
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this, "You're not logged in...", Toast.LENGTH_SHORT).show();
                } else {
                    addCommentDialog();
                }
            }
        });

    }


    private void loadComments() {
        // init ArrayList before adding data into it
        commentArrayList = new ArrayList<>();

        // db path to load comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).child("Comments")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // clear arraylist before adding data into it
                        commentArrayList.clear();
                        for (DataSnapshot ds: snapshot.getChildren()) {
                            // get data as model, spelling of variables in model must be as same as in firebase
                            ModelComment model = ds.getValue(ModelComment.class);
                            // add to arraylist
                            commentArrayList.add(model);
                        }
                        // setup adapter
                        adapterComment = new AdapterComment(PdfDetailActivity.this, commentArrayList);
                        // set adapter to recyclerview
                        binding.commentsRv.setAdapter(adapterComment);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String comment = "";

    private void addCommentDialog() {
        // inflate bind view for dialog
        DialogCommentAddBinding commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this));

        // setup alert dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);
        builder.setView(commentAddBinding.getRoot());

        // create and show alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        // handle click, dismiss dialog
        commentAddBinding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        // handle click, add comment
        commentAddBinding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get data
                comment = commentAddBinding.commentEt.getText().toString().trim();
                // validate data
                if (TextUtils.isEmpty(comment)) {
                    Toast.makeText(PdfDetailActivity.this, "Enter your comment...", Toast.LENGTH_SHORT).show();
                } else {
                    alertDialog.dismiss();
                    addComment();
                }
            }
        });
    }

    private void addComment() {
        // show progress dialog
        progressDialog.setMessage("Adding comment...");
        progressDialog.show();

        // timestamp for comment id, comment time
        String timestamp = "" + System.currentTimeMillis();

        // setup data to add in db for comment
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", "" + timestamp);
        hashMap.put("bookId", "" + bookId);
        hashMap.put("timestamp", "" + timestamp);
        hashMap.put("comment", "" + comment);
        hashMap.put("uid", "" + firebaseAuth.getUid());

        // DB path to add data into it
        // Books > bookId > Comments > commentId > commentData
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).child("Comments").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(PdfDetailActivity.this, "Comment added", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(PdfDetailActivity.this, "Failed to add comment due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //request storage permission
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG_DOWNLOAD, "Permission Granted");
                    MyApplication.downloadBook(this, "" + bookId, "" + bookTitle, "" +bookUrl);
                } else {
                    Log.d(TAG_DOWNLOAD, "Permission was denied...: ");
                    Toast.makeText(this, "Permission was denied...", Toast.LENGTH_SHORT).show();
                }
            });

    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle = "" + snapshot.child("title").getValue();
                        String description = "" + snapshot.child("description").getValue();
                        String categoryId = "" + snapshot.child("categoryId").getValue();
                        String viewsCount = "" + snapshot.child("viewsCount").getValue();
                        String downloadsCount = "" + snapshot.child("downloadsCount").getValue();
                        bookUrl = "" + snapshot.child("url").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();

                        //required data is loaded, shown download button
                        binding.downloadBookBtn.setVisibility(View.VISIBLE);

                        //format date
                        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        MyApplication.loadCategory("" + categoryId, binding.categoryTv);
                        MyApplication.loadPdfFromUrlSinglePage("" + bookUrl, "" + bookTitle, binding.pdfView, binding.progressBar, binding.pagesTv);
                        MyApplication.loadPdfSize("" + bookUrl, "" + bookTitle, binding.sizeTv);
                        MyApplication.loadPdfPageCount(PdfDetailActivity.this,""+bookUrl, binding.pagesTv);

                        //set data
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.viewsTv.setText(viewsCount.replace("null", "N/A"));
                        binding.downloadsTv.setText(downloadsCount.replace("null", "N/A"));
                        binding.dateTv.setText(date);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkIsFavorite() {
        //logged in check if its in favorite list or not
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavorite = snapshot.exists(); //true: if exists, false if not exists
                        if (isInMyFavorite) {
                            //exists in favorite
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_white, 0, 0);
                            binding.favoriteBtn.setText("Remove Favorite");
                        } else {
                            //not exists in favorite
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white, 0, 0);
                            binding.favoriteBtn.setText("Add Favorite");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}