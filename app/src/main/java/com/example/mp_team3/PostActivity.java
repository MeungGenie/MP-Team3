package com.example.mp_team3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.checkerframework.checker.units.qual.C;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PostActivity extends AppCompatActivity {

    private static final String TAG = "PostActivity";
    ArrayList<Uri> uriList = new ArrayList<>();     // ???????????? uri??? ?????? ArrayList ??????

    FirebaseAuth mAuth;
    Button btnPicture;
    ImageButton btnPostCancel;
    Spinner spnCategory;
    ImageButton btnTime;
    TextView tvTime;
    EditText etExplain;
    EditText etTitle;
    EditText etPrice;
    Button btnPostClear;
    String category;
    RecyclerView recyclerView;  // ???????????? ????????? ??????????????????
    MultiImageAdapter adapter;  // ????????????????????? ???????????? ?????????
    //postModel ??????
    String uid;
    String title;
    String price;
    String postingTime;
    String endTime;
    String detail;
    int postNum;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    Uri imgUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creat_post);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // ?????????????????? ????????? ?????? ????????????
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        btnPicture = (Button) findViewById(R.id.btnPicture);
        btnPostCancel = (ImageButton) findViewById(R.id.btnPostCancel);
        btnPicture = (Button) findViewById(R.id.btnPicture);
        spnCategory = (Spinner) findViewById(R.id.spnCategory);
        btnTime = (ImageButton) findViewById(R.id.btnTime);
        tvTime = (TextView) findViewById(R.id.tvTime);
        etExplain = (EditText) findViewById(R.id.etExplain);
        etTitle = (EditText) findViewById(R.id.etTitle);
        etPrice = (EditText) findViewById(R.id.etPrice);
        btnPostClear = (Button) findViewById(R.id.btnPostClear);
        recyclerView = findViewById(R.id.recyclerView);

        // ????????? ?????? ????????????
        if (user == null) {
            Intent intent = new Intent(PostActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        //post ?????? ????????????
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("posts").document("postNum");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document != null) {
                        if (document.exists()) {
                            Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                            if (document.getData() != null) {
                                postNum = Integer.parseInt(document.getData().get("count").toString());
                            }
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    }

                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        // ???????????? ??????
        btnPostCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postCancel();
            }
        });

        // ???????????? ???????????? ??????
        btnPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 1111);
            }
        });

        // ????????? X ?????? ????????? ?????? ?????????
        adapter = new MultiImageAdapter(uriList, PostActivity.this);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new MultiImageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                uriList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, uriList.size());
            }
        });

        // ???????????? ???????????? (???????????? ?????? ??? ??????)
        ArrayAdapter spnAdapter = ArrayAdapter.createFromResource(this, R.array.spnCategory
                , android.R.layout.simple_spinner_dropdown_item);
        spnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategory.setAdapter(spnAdapter);
        spnCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                category = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                category = "????????????";
            }
        });

        // ?????? ???????????? (?????? ??????)
        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DatePickerDialog(PostActivity.this, myDatePicker, myCalendar.get(Calendar.YEAR)
                        , myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // ????????? ?????? ?????? ?????? ??? ??????
        btnPostClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(PostActivity.this, category, Toast.LENGTH_SHORT).show();
                String str1 = etTitle.getText().toString();
                String str2 = etPrice.getText().toString();
                String str3 = tvTime.getText().toString();
                String str4 = etExplain.getText().toString();
                String str5 = category;

                //?????????????????? ??????
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseFirestore db = FirebaseFirestore.getInstance();

                uid = user.getUid();
                title = etTitle.getText().toString();
                price = etPrice.getText().toString();
                postingTime = new Date().toString();
                Date end = new Date(myCalendar.getTimeInMillis());
                endTime = end.toString();
                detail = etExplain.getText().toString();

                docRef.update("count",postNum+1 ).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "postNum successfully updated!");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });

               // ??????????????? ?????? ????????????
                FirebaseStorage storage = FirebaseStorage.getInstance();

                for (int i = 0; i < uriList.size(); i++) {
                    StorageReference storageRef = storage.getReference()
                            .child("postImages/" + "POST_" + postNum + "_" + i + ".jpg");

                    int getI = i;
                    storageRef.putFile(uriList.get(i)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            ////??? ?????? ???????????? ??? database ??????
                            if (getI == 0) {
                                storageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        imgUri = uri;
                                        PostModel postModel = new PostModel(user.getUid(), imgUri.toString(), title, price, category, postingTime, endTime, detail, postNum);
                                        mDatabase.child("posts").child("POST" + "_" + postNum).setValue(postModel);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                    }
                                });
                            }
                        }
                    });
                }

                Intent intent = new Intent(PostActivity.this, MainActivity.class);
                startActivity(intent);
                //finish();
            }
        });
    }

    // ????????? ????????? ????????? ??????
    Calendar myCalendar = Calendar.getInstance();
    DatePickerDialog.OnDateSetListener myDatePicker = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            String myFormat = "yyyy??? MM??? dd??? ??????";
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.KOREA);
            tvTime.setText(sdf.format(myCalendar.getTime()));
        }
    };

    // ???????????? ?????? ??????
    void postCancel() {
        AlertDialog.Builder msgBuilder = new AlertDialog.Builder(PostActivity.this)
                .setMessage("???????????? ???????????? ????????????.\n????????? ?????????????????????????")
                .setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        AlertDialog msgDlg = msgBuilder.create();
        msgDlg.show();
    }

    // ???????????? ??????????????? ????????? ??? ???????????? ?????????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1111) {  // ????????? ?????? ?????? 1111
            if (data == null) {   // ?????? ???????????? ???????????? ?????? ?????? (?????? ?????? X)
            } else {   // ???????????? ???????????? ????????? ??????
                if (data.getClipData() == null) {     // ???????????? ????????? ????????? ??????
                    Uri imageUri = data.getData();
                    uriList.add(imageUri);
                    adapter = new MultiImageAdapter(uriList, getApplicationContext());
                    recyclerView.setAdapter(adapter);
                    recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));
                } else {      // ???????????? ????????? ????????? ??????
                    ClipData clipData = data.getClipData();
                    if (clipData.getItemCount() > 10) {   // ????????? ???????????? 11??? ????????? ??????
                        Toast.makeText(getApplicationContext(), "????????? 10????????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
                    } else {   // ????????? ???????????? 1??? ?????? 10??? ????????? ??????
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri imageUri = clipData.getItemAt(i).getUri();  // ????????? ??????????????? uri??? ????????????.
                            try {
                                uriList.add(imageUri);  //uri??? list??? ?????????.
                            } catch (Exception e) {
                                Log.e(TAG, "?????? ?????? ??????", e);
                            }
                        }
                        adapter = new MultiImageAdapter(uriList, getApplicationContext());
                        recyclerView.setAdapter(adapter);   // ????????????????????? ????????? ??????
                        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, true));     // ?????????????????? ?????? ????????? ??????
                    }
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            postCancel();
            return true;
        }
        return false;
    }

}