package com.nita.infotech.tracker.pst;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import cn.pedant.SweetAlert.SweetAlertDialog;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ActivitySignUp extends AppCompatActivity {

    private EditText etName,etMobileNumber,etDob;
    private TextView tvError;
    private RadioGroup rBtnGender;
    private RadioButton rMale,rFemale;
    private ImageView ivDob;
    private Button btnSubmit;
    private String name,mobileNumber,gender="M",dobStr;
    private Date dob;
    private DatePickerDialog dobPicker;
    private SweetAlertDialog pDialog;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userCollectionRef = db.collection("User");
    private SessionManager sessionManager;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        etName=findViewById(R.id.etName);
        etMobileNumber=findViewById(R.id.etMobileNumber);
        rBtnGender=findViewById(R.id.rBtnGender);
        rMale=findViewById(R.id.rMale);
        rFemale=findViewById(R.id.rFemale);
        etDob=findViewById(R.id.etDob);
        ivDob=findViewById(R.id.ivDob);
        tvError=findViewById(R.id.tvError);
        btnSubmit=findViewById(R.id.btnSubmit);

        rBtnGender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton rb = (RadioButton) group.findViewById(checkedId);
                if (rMale.isChecked()) {
                    gender="M";
                } else {
                    if (rFemale.isChecked()) {
                        gender="F";
                    }
                }
            }
        });


        sessionManager = new SessionManager(ActivitySignUp.this);
        gson = Utility.getGson();
        final java.util.Calendar instance = java.util.Calendar.getInstance();
        final int day = instance.get(java.util.Calendar.DAY_OF_MONTH);
        final int month = instance.get(java.util.Calendar.MONTH);
        final int year = instance.get(java.util.Calendar.YEAR);

        ivDob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //etFromDate.setEnabled(true);
                dobPicker = new DatePickerDialog(view.getRootView().getContext(), R.style.CalendarDatePicker,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                etDob.setText(String.format("%02d", dayOfMonth) + "/" + (String.format("%02d", (monthOfYear + 1))) + "/" + year);
                            }
                        }, year, month, day);
                dobPicker.getDatePicker().setMaxDate(System.currentTimeMillis() - 1000);
                dobPicker.setTitle("Select DOB");
                dobPicker.show();
            }
        });
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvError.setVisibility(View.GONE);
                name = etName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    etName.setError("Enter the full name");
                    etName.requestFocus();
                    return;
                } else {
                    if (Utility.isNumericWithSpace(name)) {
                        etName.setError("Invalid full name");
                        etName.requestFocus();
                        return;
                    }
                }

                mobileNumber = etMobileNumber.getText().toString().trim();
                if (!Utility.isValidPhone(mobileNumber)) {
                    etMobileNumber.setError("Invalid mobile number");
                    etMobileNumber.requestFocus();
                    return;
                }

                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                Date fromDt = null;
                Date toDayDate = new Date();
                toDayDate.setTime(toDayDate.getTime() - 24 * 60 * 60 * 1000);
                System.out.println("toDayDate " + toDayDate);
                dobStr = etDob.getText().toString().trim();
                if (TextUtils.isEmpty(dobStr)) {
                    etDob.setError("Select the from date");
                    etDob.requestFocus();
                    return;
                } else {
                    try {
                        dob = dateFormat.parse(dobStr);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        etDob.setError("DOB format should be in dd/MM/yyyy");
                        etDob.requestFocus();
                        return;
                    }
                }
                if (pDialog != null) {
                    pDialog.show();
                }
                User user = new User();
                user.setCreatedDate(new Date());
                user.setCreatorId("SELF");
                user.setCreatorType("A");
                user.setModifierId("SELF");
                user.setModifierType("A");
                user.setName(name);
                user.setMobileNumber(mobileNumber);
                user.setGender(gender);
                user.setDob(dob);
                user.setStatus("F");
                userCollectionRef
                        .add(user)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                if (pDialog != null && pDialog.isShowing()) {
                                    pDialog.dismiss();
                                }
                                sessionManager.putString("loggedInUser", gson.toJson(user));
                                sessionManager.putString("loggedInUserId", documentReference.getId());
                                Intent intent = new Intent(ActivitySignUp.this, ActivityForgotPassword.class);
                                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
            }
        });

    }
}
