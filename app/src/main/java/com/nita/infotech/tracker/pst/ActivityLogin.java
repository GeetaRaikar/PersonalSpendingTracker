package com.nita.infotech.tracker.pst;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.pedant.SweetAlert.SweetAlertDialog;


public class ActivityLogin extends AppCompatActivity {
    private String mobileNumber, password;
    private EditText etMobileNumber, etPassword;
    private Button btnSubmit, btnLogin;
    private TextView tvName;
    private ImageView appIcon;
    private LinearLayout llLogin;
    private TextView tvFor, tvSignUp;
    private User loggedInUser;
    private String loggedInUserId, academicYearId, userId;
    private Gson gson = Utility.getGson();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userCollectionRef = db.collection("User");
    private DocumentReference instituteDocRef;
    private SweetAlertDialog pDialog;
    private SessionManager sessionManager;
    int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        gson = Utility.getGson();
        sessionManager = new SessionManager(ActivityLogin.this);
        sessionManager.putBoolean("isPromptShowed", false);
        userId = sessionManager.getString("loggedInUserId");
        pDialog = Utility.createSweetAlertDialog(ActivityLogin.this);

        // System.out.println("adminJson - " + adminId);
        tvName = findViewById(R.id.tvName);
        appIcon = findViewById(R.id.appIcon);
        etMobileNumber = (EditText) findViewById(R.id.etMobileNumber);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        llLogin = (LinearLayout) findViewById(R.id.llLogin);
        etPassword = (EditText) findViewById(R.id.etPassword);

        tvFor = (TextView) findViewById(R.id.tvFor);

        tvSignUp = (TextView) findViewById(R.id.tvSignUp);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mobileNumber = etMobileNumber.getText().toString().trim();
                if (Utility.isValidPhone(mobileNumber)) {
                    getAdmin();
                } else {
                    etMobileNumber.setError(getString(R.string.errInvalidMobNum));
                    etMobileNumber.requestFocus();
                }
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                password = etPassword.getText().toString().trim();
                if (loggedInUser.getPassword().equals(password)) {
                    gson = Utility.getGson();
                    String loggedInAdminStr = gson.toJson(loggedInUser);
                    sessionManager.putString("loggedInUser", loggedInAdminStr);
                    sessionManager.putString("loggedInUserId", loggedInUserId);
                    Intent intent = new Intent(ActivityLogin.this, ActivityHome.class);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    startActivity(intent);
                    finish();
                } else {
                    etPassword.setError(getString(R.string.errInvalidPassword));
                    etPassword.requestFocus();
                }

            }
        });
        tvFor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionManager.putString("loggedInUser", gson.toJson(loggedInUser));
                sessionManager.putString("loggedInUserId", loggedInUserId);
                Intent intent = new Intent(ActivityLogin.this, ActivityForgotPassword.class);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                startActivity(intent);
                finish();
            }
        });

        tvSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ActivityLogin.this, ActivitySignUp.class);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                startActivity(intent);
                finish();
            }
        });

    }

    private void getAdmin() {
        if (pDialog != null && !pDialog.isShowing()) {
            pDialog.show();
        }
        userCollectionRef
                .whereEqualTo("mobileNumber", mobileNumber)
                .whereIn("status", Arrays.asList("A", "F"))
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        count++;
                        System.out.println("task -" + task.getResult().size());
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // System.out.println("Data -key -" + document.getId() + " value -" + document.getData());
                                loggedInUserId = document.getId();
                                loggedInUser = document.toObject(User.class);
                                loggedInUser.setId(document.getId());
                            }
                            if (pDialog != null && pDialog.isShowing()) {
                                pDialog.dismiss();
                            }
                            if (loggedInUser == null) {
                                etMobileNumber.setError(getString(R.string.errInvalidMobNum));
                                etMobileNumber.requestFocus();
                                return;
                            } else {
                                tvName.setText("Hi " + loggedInUser.getName() + "!");
                                if (loggedInUser.getStatus().equals("F")) {
                                    sessionManager.putString("loggedInUser", gson.toJson(loggedInUser));
                                    sessionManager.putString("loggedInUserId", loggedInUserId);
                                    Intent intent = new Intent(ActivityLogin.this, ActivityForgotPassword.class);
                                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                    startActivity(intent);
                                    finish();
                                } else if (!loggedInUser.getStatus().equals("A")) {
                                    etMobileNumber.setError("Admin has deactivated your account");
                                    etMobileNumber.requestFocus();
                                    return;
                                } else {
                                    //Toast.makeText(getApplicationContext(), "Please login", Toast.LENGTH_SHORT).show();
                                    btnSubmit.setVisibility(View.GONE);
                                    llLogin.setVisibility(View.VISIBLE);
                                    tvSignUp.setVisibility(View.GONE);
                                }

                            }
                        } else {
                            // Log.d(TAG, "Error getting documents: ", task.getException());
                            //System.out.println("Error getting documents: -" + task.getException());
                        }
                    }
                });

        // [END get_multiple]

    }
}