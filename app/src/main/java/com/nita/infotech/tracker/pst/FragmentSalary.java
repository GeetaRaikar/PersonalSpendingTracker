package com.nita.infotech.tracker.pst;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.nita.infotech.tracker.pst.model.MonthWiseSalary;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class FragmentSalary extends Fragment {
    private Gson gson;
    private FirebaseFirestore db=FirebaseFirestore.getInstance();
    private CollectionReference monthWiseSalaryCollectionRef=db.collection("MonthWiseSalary");
    private CollectionReference expenseCollectionRef=db.collection("Expense");
    private User loggedInUser;
    private SessionManager sessionManager;
    private String loggedInUserId;
    private SweetAlertDialog pDialog;
    private ListenerRegistration monthWiseSalaryListener;
    private MonthWiseSalary monthWiseSalary;
    private List<MonthWiseSalary> monthWiseSalaryList = new ArrayList<>();
    private List<Integer> alreadyMonthWiseSalaryList = new ArrayList<>();
    private RecyclerView rvMonthWiseSalary;
    private RecyclerView.Adapter monthWiseSalaryAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private LinearLayout llNoList;
    private ImageButton btnSubmit;
    private String salary;
    private EditText etSalary;
    private String[] monthArray;

    public FragmentSalary() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        if (pDialog.isShowing() && pDialog == null) {
            pDialog.show();
        }
        monthWiseSalaryListener = monthWiseSalaryCollectionRef
                .whereEqualTo("creatorId",loggedInUserId)
                .orderBy("createdDate", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        if (monthWiseSalaryList.size() != 0) {
                            monthWiseSalaryList.clear();
                        }
                        if (alreadyMonthWiseSalaryList.size() != 0) {
                            alreadyMonthWiseSalaryList.clear();
                        }
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Log.d(TAG, document.getId()document.getId() + " => " + document.getData());
                            monthWiseSalary = document.toObject(MonthWiseSalary.class);
                            monthWiseSalary.setId(document.getId());
                            alreadyMonthWiseSalaryList.add(monthWiseSalary.getMonth());
                            monthWiseSalaryList.add(monthWiseSalary);
                        }
                        if (monthWiseSalaryList.size() != 0) {
                            rvMonthWiseSalary.setVisibility(View.VISIBLE);
                            llNoList.setVisibility(View.GONE);
                            monthWiseSalaryAdapter = new MonthWiseSalaryAdapter(monthWiseSalaryList);
                            monthWiseSalaryAdapter.notifyDataSetChanged();
                            rvMonthWiseSalary.setAdapter(monthWiseSalaryAdapter);
                        } else {
                            rvMonthWiseSalary.setVisibility(View.GONE);
                            llNoList.setVisibility(View.VISIBLE);
                        }
                    }
                });

    }

    @Override
    public void onStop() {
        super.onStop();
        if (monthWiseSalaryListener != null) {
            monthWiseSalaryListener.remove();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(getContext());
        gson = Utility.getGson();
        String userJson = sessionManager.getString("loggedInUser");
        loggedInUser = gson.fromJson(userJson, User.class);
        loggedInUserId = loggedInUser.getId();
        pDialog = Utility.createSweetAlertDialog(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_salary, container, false);
        ((ActivityHome)getActivity()).getSupportActionBar().setDisplayShowCustomEnabled(false);
        ((ActivityHome)getActivity()).getSupportActionBar().setTitle(getString(R.string.salary));
        BottomNavigationView navBar = getActivity().findViewById(R.id.nav_view);
        navBar.setVisibility(View.VISIBLE);
        monthArray = getResources().getStringArray(R.array.month_option_array);
        llNoList = (LinearLayout) view.findViewById(R.id.llNoList);
        rvMonthWiseSalary = view.findViewById(R.id.rvSalary);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getContext());
        rvMonthWiseSalary.setLayoutManager(layoutManager);
        etSalary = view.findViewById(R.id.etSalary);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Date today = new Date();
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(today);
                int monthIndex=calendar.get(Calendar.MONTH);
                salary = etSalary.getText().toString().trim();
                if (TextUtils.isEmpty(salary)) {
                    etSalary.setError("Enter Salary");
                    etSalary.requestFocus();
                    return;
                } else {
                    if (!Utility.isNumeric(salary)) {
                        etSalary.setError("Invalid Salary");
                        etSalary.requestFocus();
                        return;
                    }else {
                        for (int i = 0; i < alreadyMonthWiseSalaryList.size(); i++) {
                            int month = alreadyMonthWiseSalaryList.get(i);
                            if (monthIndex == month) {
                                System.out.println("equal");
                                etSalary.setError("Already this month salary is saved");
                                etSalary.requestFocus();
                                return;
                            }
                        }
                    }
                }
                if (!pDialog.isShowing() && pDialog == null) {
                    pDialog.show();
                }
                monthWiseSalary = new MonthWiseSalary();
                monthWiseSalary.setMonth(monthIndex);
                monthWiseSalary.setPaymentDate(today);
                monthWiseSalary.setSalary(Float.parseFloat(salary));
                monthWiseSalary.setCreatorId(loggedInUserId);
                monthWiseSalary.setModifierId(loggedInUserId);
                monthWiseSalary.setCreatorType("A");
                monthWiseSalary.setModifierType("A");
                addMonthWiseSalary();
            }
        });
        return view;
    }

    class MonthWiseSalaryAdapter extends RecyclerView.Adapter<MonthWiseSalaryAdapter.MyViewHolder> {
        private List<MonthWiseSalary> monthWiseSalaryList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView tvSalary,tvMonth;
            public ImageView ivDeleteSalary;

            public MyViewHolder(View view) {
                super(view);
                tvSalary = view.findViewById(R.id.tvSalary);
                tvMonth = view.findViewById(R.id.tvMonth);
                ivDeleteSalary = view.findViewById(R.id.ivDeleteSalary);
            }
        }


        public MonthWiseSalaryAdapter(List<MonthWiseSalary> monthWiseSalaryList) {
            this.monthWiseSalaryList = monthWiseSalaryList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_salary, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            final MonthWiseSalary monthWiseSalary = monthWiseSalaryList.get(position);
            holder.tvSalary.setText("" + monthWiseSalary.getSalary());
            holder.tvMonth.setText("" + monthArray[monthWiseSalary.getMonth()]);
            holder.ivDeleteSalary.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    deleteMonthWiseSalary(monthWiseSalary);
                }
            });
        }

        @Override
        public int getItemCount() {
            return monthWiseSalaryList.size();
        }
    }

    private void deleteMonthWiseSalary(MonthWiseSalary monthWiseSalary) {
        if (pDialog == null && !pDialog.isShowing()) {
            pDialog.show();
        }
        expenseCollectionRef
                .whereEqualTo("monthWiseSalaryId", monthWiseSalary.getId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        if (documentSnapshots.size() == 0) {
                            SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.WARNING_TYPE)
                                    .setTitleText("Delete")
                                    .setContentText("Do you want to delete " + monthArray[monthWiseSalary.getMonth()] + " salary?")
                                    .setConfirmText("Confirm")
                                    .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sDialog) {
                                            sDialog.dismissWithAnimation();
                                        }
                                    })
                                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                        @Override
                                        public void onClick(SweetAlertDialog sDialog) {
                                            if (pDialog == null && !pDialog.isShowing()) {
                                                pDialog.show();
                                            }
                                            monthWiseSalaryCollectionRef.document(monthWiseSalary.getId())
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            if (pDialog != null) {
                                                                pDialog.dismiss();
                                                            }
                                                            SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                                                                    .setTitleText("Deleted")
                                                                    .setContentText("Salary has been deleted.")
                                                                    .setConfirmText("Ok");
                                                            dialog.setCancelable(false);
                                                            dialog.show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            if (pDialog != null) {
                                                                pDialog.dismiss();
                                                            }
                                                            SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                                                                    .setTitleText("Unable to delete salary")
                                                                    .setContentText("For some network issue please check it.")
                                                                    .setConfirmText("Ok");
                                                            dialog.setCancelable(false);
                                                            dialog.show();
                                                        }
                                                    });
                                            sDialog.dismissWithAnimation();
                                        }
                                    });
                            dialog.setCancelable(false);
                            dialog.show();
                        } else {
                            if (pDialog != null) {
                                pDialog.dismiss();
                            }
                            SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                                    .setTitleText("Unable to delete salary")
                                    .setContentText("In this Salary some expenses are there.")
                                    .setConfirmText("Ok");
                            dialog.setCancelable(false);
                            dialog.show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Error getting documents:" + e);
                    }
                });
    }

    private void addMonthWiseSalary() {
        if(monthWiseSalaryList.size()>0) {
            MonthWiseSalary mws = monthWiseSalaryList.get(0);
            mws.setStatus("I");
            monthWiseSalaryCollectionRef.document(mws.getId()).set(mws).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (pDialog != null) {
                        pDialog.dismiss();
                    }
                    if (task.isSuccessful()) {
                    } else {
                    }
                }
            });
        }
        monthWiseSalaryCollectionRef
                .add(monthWiseSalary)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Successfully")
                                .setContentText("Salary has been successfully added.")
                                .setConfirmText("Ok");
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Unable to add salary")
                                .setContentText("Network issue, please check it.")
                                .setConfirmText("Ok");
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                });
        etSalary.setText("");
    }

}