package com.nita.infotech.tracker.pst;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaos.view.PinView;
import com.firebase.client.Firebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class ActivityForgotPassword extends AppCompatActivity {
    private Button btnSubmit, btnVerify;
    private String newPassword;
    private String reEnterPassword;
    private EditText etNewPassword;
    private EditText etReEnterPassword;
    private SweetAlertDialog pDialog;
    private User loggedInUser;
    private String loggedInUserId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userCollectionRef = db.collection("User");
    private FirebaseAuth mAuth;
    private String verificationCode;
    private Gson gson;
    private PinView pinView;
    private LinearLayout llOTP, llResetPassword;
    private SessionManager sessionManager;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();//Authentication for sending verification code

        sessionManager = new SessionManager(getApplicationContext());
        gson = Utility.getGson();
        String loggedInUserJson = sessionManager.getString("loggedInUser");
        loggedInUser = gson.fromJson(loggedInUserJson, User.class);
        loggedInUserId = sessionManager.getString("loggedInUserId");
        sendOTP();
        llResetPassword = findViewById(R.id.llResetPassword);
        llOTP = findViewById(R.id.llOTP);

        Firebase.setAndroidContext(ActivityForgotPassword.this);
        pinView = findViewById(R.id.pinview);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        etReEnterPassword = (EditText) findViewById(R.id.etReEnterPassword);

        final TextView reSendOTP = (TextView) findViewById(R.id.reSendOTP);
        reSendOTP.setPaintFlags(reSendOTP.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        btnVerify = findViewById(R.id.btnVerify);

        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {
                String content = pinView.getText().toString();
                verifyVerificationCode(content);
            }
        });

        reSendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOTP();
            }
        });

        btnSubmit = (Button) findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View view) {
                newPassword = etNewPassword.getText().toString();
                reEnterPassword = etReEnterPassword.getText().toString();
                if (newPassword.equals(reEnterPassword)) {
                    loggedInUser.setPassword(newPassword);
                    loggedInUser.setStatus("A");
                    loggedInUser.setModifiedDate(new Date());

                    userCollectionRef.document(loggedInUserId).set(loggedInUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ActivityForgotPassword.this, "Updated Successfully",
                                        Toast.LENGTH_SHORT).show();
                                sessionManager.remove("loggedInUser");
                                sessionManager.remove("loggedInUserId");
                                Intent intent = new Intent(ActivityForgotPassword.this, ActivityLogin.class);
                                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                startActivity(intent);
                                finish();
                            } else {

                            }
                        }
                    });

                } else {
                    Toast.makeText(getApplicationContext(), "Passwords don't match! please enter again!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        });

    }

    private void sendOTP() {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                "+91" + loggedInUser.getMobileNumber(),                     // Phone number to verify
                60,                           // Timeout duration
                TimeUnit.SECONDS,                // Unit of timeout
                ActivityForgotPassword.this,        // Activity (for callback binding)
                mCallback);  // OnVerificationStateChangedCallbacks
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        @Override
        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
            //Getting the code sent by SMS
            verificationCode = phoneAuthCredential.getSmsCode();
            System.out.println("verificationCode "+verificationCode);
        }

        @Override
        public void onVerificationFailed(FirebaseException e) {
            System.out.println("e.getMessage() "+e.getMessage());
            Toast.makeText(ActivityForgotPassword.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
            super.onCodeSent(s, forceResendingToken);
            mVerificationId = s;
        }
    };

    private void verifyVerificationCode(String code) {
        //creating the credential
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);

        //signing the user
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(ActivityForgotPassword.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            llOTP.setVisibility(View.GONE);
                            llResetPassword.setVisibility(View.VISIBLE);
                        } else {

                            //verification unsuccessful.. display an error message

                            String message = "Something is wrong, we will fix it soon.";

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                message = "Invalid verification code entered.";
                            }
                            Toast.makeText(ActivityForgotPassword.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
