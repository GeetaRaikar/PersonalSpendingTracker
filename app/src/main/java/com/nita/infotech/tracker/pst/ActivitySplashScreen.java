package com.nita.infotech.tracker.pst;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class ActivitySplashScreen extends AppCompatActivity {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Gson gson = Utility.getGson();
    private SessionManager sessionManager;
    private DocumentReference staffDocRef;
    private User loggedInUser;
    private String loggedInUserId,staffId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        sessionManager = new SessionManager(ActivitySplashScreen.this);
        staffId = sessionManager.getString("loggedInUserId");

        // System.out.println("adminJson - " + adminId);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {

                if (!TextUtils.isEmpty(staffId)) {
                    validateAdmin(staffId);
                }
                else {
                    // This method will be executed once the timer is over
                    Intent i = new Intent(ActivitySplashScreen.this, ActivityLogin.class);
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    startActivity(i);
                    finish();
                }
            }
        }, 1000);
    }

    private void validateAdmin(String documentId) {

        staffDocRef = db.document("User/" + documentId);
        staffDocRef.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        //System.out.println("Data -key -" + documentSnapshot.getId() + " value -" + documentSnapshot.getData());
                        loggedInUserId = documentSnapshot.getId();
                        loggedInUser = documentSnapshot.toObject(User.class);
                        loggedInUser.setId(documentSnapshot.getId());
                        if (loggedInUser != null && loggedInUser.getStatus().equals("A")) {
                            sessionManager.putString("loggedInUser", gson.toJson(loggedInUser));
                            sessionManager.putString("loggedInUserId", loggedInUserId);
                            Intent intent = new Intent(ActivitySplashScreen.this, ActivityHome.class);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                            startActivity(intent);
                            finish();
                        } else {
                            Intent i = new Intent(ActivitySplashScreen.this, ActivityLogin.class);
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                            startActivity(i);
                            finish();
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Intent i = new Intent(ActivitySplashScreen.this, ActivityLogin.class);
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        startActivity(i);
                        finish();
                    }
                });
    }
}
