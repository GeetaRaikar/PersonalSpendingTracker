package com.nita.infotech.tracker.pst;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.nita.infotech.tracker.pst.model.Category;
import com.nita.infotech.tracker.pst.model.Expense;
import com.nita.infotech.tracker.pst.model.MonthWiseSalary;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class FragmentExpense extends Fragment{
    private Gson gson;
    private FirebaseFirestore db=FirebaseFirestore.getInstance();
    private CollectionReference expenseCollectionRef=db.collection("Expense");
    private CollectionReference categoryCollectionRef=db.collection("Category");
    private CollectionReference monthWiseSalaryCollectionRef=db.collection("MonthWiseSalary");
    private User loggedInUser;
    private SessionManager sessionManager;
    private String loggedInUserId;
    private SweetAlertDialog pDialog;
    private ListenerRegistration expenseListener;
    private List<Expense> expenseList=new ArrayList<>();
    private Expense expense;
    private LinearLayout llNoList,llExpenseAmount;
    private RecyclerView rvExpense;
    private RecyclerView.Adapter expenseAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private TextView tvExpenseAmount,tvSalary,tvRemainingAmount,tvError,tvMonthYear;
    private MonthWiseSalary monthWiseSalary;
    private Spinner spExpenseCategory,spMonth,spYear;
    private ImageView ivDate;
    private EditText etDate,etAmount,etDesc;
    private Button btnSave;
    private DatePickerDialog picker;
    private Category selectExpenseCategory;
    private Boolean isDate = false;
    private Date date;
    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private String amountStr,desc,dateStr,month,year;
    private List<Category> categoryList=new ArrayList<>();
    private Category category;
    private int monthIndex;
    private Calendar calendar = new GregorianCalendar();
    String[] monthArray;
    private float salary=0,amount=0;
    private ImageView ivAdd;


    @Override
    public void onStart() {
        super.onStart();
        final SweetAlertDialog pDialog;
        pDialog = Utility.createSweetAlertDialog(getContext());
        pDialog.show();

        if(categoryList.size()>0){
            categoryList.clear();
        }
        categoryCollectionRef
                .whereEqualTo("creatorId",loggedInUserId)
                .orderBy("createdDate", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Log.d(TAG, document.getId()document.getId() + " => " + document.getData());
                                category = document.toObject(Category.class);
                                category.setId(document.getId());
                                categoryList.add(category);
                            }
                        } else {
                            //Log.w(TAG, "Error getting documents.", task.getException());
                            // System.out.println("Error getting documents: " + task.getException());
                        }
                    }
                });
        // [END get_all_users]
    }

    @Override
    public void onStop() {
        super.onStop();
        if (expenseListener != null) {
            expenseListener.remove();
        }
    }

    public FragmentExpense() {
        // Required empty public constructor
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ActivityHome)getActivity()).getSupportActionBar().setDisplayShowCustomEnabled(false);
        ((ActivityHome)getActivity()).getSupportActionBar().setTitle(getString(R.string.expense));
        BottomNavigationView navBar = getActivity().findViewById(R.id.nav_view);
        navBar.setVisibility(View.VISIBLE);
        monthArray = getResources().getStringArray(R.array.month_option_array);
        rvExpense = view.findViewById(R.id.rvExpense);
        tvExpenseAmount = view.findViewById(R.id.tvExpenseAmount);
        tvSalary = view.findViewById(R.id.tvSalary);
        tvRemainingAmount = view.findViewById(R.id.tvRemainingAmount);
        spMonth = view.findViewById(R.id.spMonth);
        spYear = view.findViewById(R.id.spYear);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getContext());
        rvExpense.setLayoutManager(layoutManager);
        llNoList = view.findViewById(R.id.llNoList);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),R.array.month_option_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spMonth.setAdapter(adapter);

        Date today = new Date();
        calendar.setTime(today);
        spMonth.setSelection(calendar.get(Calendar.MONTH));
        year=""+calendar.get(Calendar.YEAR);
        //monthIndex=calendar.get(Calendar.MONTH);
        //getExpenses();

        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                monthIndex=i;
                tvMonthYear.setText(""+monthArray[monthIndex]+" "+year);
                monthWiseSalaryCollectionRef
                        .whereEqualTo("month",monthIndex)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    return;
                                }
                                monthWiseSalary=null;
                                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                    // Log.d(TAG, document.getId()document.getId() + " => " + document.getData());
                                    monthWiseSalary = document.toObject(MonthWiseSalary.class);
                                    monthWiseSalary.setId(document.getId());
                                }
                                if(monthWiseSalary==null){
                                    salary=0;
                                    rvExpense.setVisibility(View.GONE);
                                    llNoList.setVisibility(View.VISIBLE);
                                    System.out.println("tvExpenseAmount "+ amount);
                                    System.out.println("tvSalary "+ salary);
                                    System.out.println("tvRemainingAmount "+ (salary-amount));
                                    ivAdd.setVisibility(View.GONE);
                                    tvExpenseAmount.setText("0");
                                    tvSalary.setText("0");
                                    tvRemainingAmount.setText("0");
                                }else{
                                    salary=monthWiseSalary.getSalary();
                                    getExpenses();
                                }
                            }
                        });
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ivAdd= (ImageView) view.findViewById(R.id.addExpense);
        ivAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //addDialogExpense();
                createBottomSheet();
                bottomSheetDialog.show();
            }
        });
    }

    BottomSheetDialog bottomSheetDialog;

    private void createBottomSheet() {
        if (bottomSheetDialog == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_add_expense, null);
            bottomSheetDialog = new BottomSheetDialog(getContext());//new BottomSheetDialog(this,R.style.BottomSheetDialog)
            bottomSheetDialog.setContentView(view);
            spExpenseCategory = view.findViewById(R.id.spExpenseCategory);
            tvError = view.findViewById(R.id.tvError);
            ivDate = view.findViewById(R.id.ivDate);
            etDate = view.findViewById(R.id.etDate);
            etAmount = view.findViewById(R.id.etAmount);
            etDesc = view.findViewById(R.id.etDesc);
            btnSave = view.findViewById(R.id.btnSave);
            etDate.setEnabled(true);
            if(categoryList.size()>0) {
                List<String> nameList = new ArrayList<>();
                for (Category c : categoryList) {
                    nameList.add(c.getCategory());
                }
                ArrayAdapter<String> adaptor = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, nameList);
                adaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spExpenseCategory.setAdapter(adaptor);

                spExpenseCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectExpenseCategory = categoryList.get(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }

            etDate.setText("" + String.format("%02d", calendar.get(Calendar.DATE)) + "/" + String.format("%02d", (calendar.get(Calendar.MONTH) + 1)) + "/" + calendar.get(Calendar.YEAR));
            ivDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    final Calendar cldr = Calendar.getInstance();
                    int day = cldr.get(Calendar.DAY_OF_MONTH);
                    int month = cldr.get(Calendar.MONTH);
                    int year = cldr.get(Calendar.YEAR);
                    // date picker dialog
                    picker = new DatePickerDialog(getContext(), R.style.CalendarDatePicker,
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                    etDate.setText(String.format("%02d", dayOfMonth) + "/" + (String.format("%02d", (monthOfYear + 1))) + "/" + year);
                                }
                            }, year, month, day);
                    picker.setTitle("Select Expense Date");
                    picker.show();
                }
            });
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    tvError.setVisibility(View.GONE);
                    if(selectExpenseCategory==null){
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText("Select the expense category");
                        return;
                    }
                    amountStr = etAmount.getText().toString().trim();
                    if (TextUtils.isEmpty(amountStr)) {
                        etAmount.setError("Enter amount");
                        etAmount.requestFocus();
                        return;
                    }
                    dateStr = etDate.getText().toString().trim();
                    if (TextUtils.isEmpty(dateStr)) {
                        etDate.setError("Enter Date in DD/MM/YYYY");
                        etDate.requestFocus();
                        return;
                    }
                    if (Utility.isDateValid(dateStr)) {
                        isDate = true;
                    } else {
                        isDate = false;
                        etDate.setError("InCorrect Date");
                        etDate.requestFocus();
                        return;
                    }
                    desc = etDesc.getText().toString().trim();
                    if (isDate == true && selectExpenseCategory != null) {
                        try {
                            date = dateFormat.parse(dateStr);
                        } catch (ParseException e) {
                            etDate.setError("DD/MM/YYYY");
                            etDate.requestFocus();
                            e.printStackTrace();
                        }
                        String str[]=dateStr.split("/");
                        float amountInFloat = Float.parseFloat(amountStr);
                        expense = new Expense();
                        expense.setExpenseDate(date);
                        expense.setMonth(Integer.parseInt(str[1])-1);
                        expense.setMonthWiseSalaryId(monthWiseSalary.getId());
                        expense.setDetails(desc);
                        expense.setAmount(amountInFloat);
                        expense.setCategoryId(selectExpenseCategory.getId());
                        expense.setCreatorId(loggedInUserId);
                        expense.setModifierId(loggedInUserId);
                        expense.setCreatorType("A");
                        expense.setModifierType("A");
                        addExpense();
                    }
                }
            });
        }
    }

    private void getExpenses() {
        System.out.println("monthWiseSalary.getId() "+monthWiseSalary.getId());
        if (expenseList.size() != 0) {
            expenseList.clear();
        }
        final SweetAlertDialog pDialog;
        pDialog = Utility.createSweetAlertDialog(getContext());
        pDialog.show();
        // System.out.println("Expense -");
        expenseListener = expenseCollectionRef
                .whereEqualTo("creatorId", loggedInUserId)
                .whereEqualTo("monthWiseSalaryId",monthWiseSalary.getId())
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
                        if (expenseList.size() != 0) {
                            expenseList.clear();
                        }
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Log.d(TAG, document.getId()document.getId() + " => " + document.getData());
                            expense = document.toObject(Expense.class);
                            // System.out.println("Expense Category Name-" + expense.getExpenseCategoryId());
                            expenseList.add(expense);
                        }
                        if (expenseList.size() != 0) {
                            llNoList.setVisibility(View.GONE);
                            rvExpense.setVisibility(View.VISIBLE);
                            amount = 0;
                            for (Expense expense : expenseList) {
                                amount = amount + expense.getAmount();
                            }
                            expenseAdapter = new ExpenseAdapter(expenseList);
                            rvExpense.setAdapter(expenseAdapter);
                        } else {
                            rvExpense.setVisibility(View.GONE);
                            llNoList.setVisibility(View.VISIBLE);
                        }
                        System.out.println("tvExpenseAmount "+ String.format("%.2f",amount));
                        System.out.println("tvSalary "+ String.format("%.2f",salary));
                        System.out.println("tvRemainingAmount "+ String.format("%.2f",(salary-amount)));
                        tvExpenseAmount.setText("" + String.format("%.2f",amount));
                        tvSalary.setText("" + String.format("%.2f",salary));
                        tvRemainingAmount.setText("" + String.format("%.2f",(salary-amount)));
                    }
                });
        // System.out.println("Expense  datapath-" + db);


    }

    private void addExpense() {
        final SweetAlertDialog pDialog;
        pDialog = Utility.createSweetAlertDialog(getContext());
        pDialog.show();
        expenseCollectionRef
                .add(expense)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        bottomSheetDialog.dismiss();
                        SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Success")
                                .setContentText("Successfully added")
                                .setConfirmText("Ok")
                                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                                    @Override
                                    public void onClick(SweetAlertDialog sDialog) {
                                        sDialog.dismissWithAnimation();
                                        getExpenses();
                                    }
                                });
                        dialog.setCancelable(false);
                        dialog.show();

                        //Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                        //Toast.makeText(getContext(), "Success", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //Log.w(TAG, "Error adding document", e);
                        Toast.makeText(getContext(), "Error", Toast.LENGTH_LONG).show();
                    }
                });
        // [END add_document]

    }

    class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.MyViewHolder> {
        private List<Expense> expenseList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView tvAmount, tvDate, tvExpenseCategory, tvDescription;

            public MyViewHolder(View view) {
                super(view);
                tvExpenseCategory = view.findViewById(R.id.tvExpenseCategory);
                tvAmount = (TextView) view.findViewById(R.id.tvAmount);
                tvDate = (TextView) view.findViewById(R.id.tvDate);
                tvDescription = view.findViewById(R.id.tvDescription);
            }
        }


        public ExpenseAdapter(List<Expense> expenseList) {
            this.expenseList = expenseList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_expense, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            final Expense expense = expenseList.get(position);
            holder.tvAmount.setText("" + expense.getAmount());
            String expenseDate = null;
            if (expense.getExpenseDate() != null) {
                expenseDate = Utility.formatDateToString(expense.getExpenseDate().getTime());
            }
            if (expenseDate != null) {
                holder.tvDate.setText("" + expenseDate);
            }
            holder.tvDescription.setText("" + expense.getDetails());

            for (Category c:categoryList){
                if(c.getId().equalsIgnoreCase(expense.getCategoryId())){
                    holder.tvExpenseCategory.setText("" + c.getCategory());
                    break;
                }
            }
            /*
            DocumentReference categoryDocRef = db.document("Category/" + expense.getCategoryId());
            categoryDocRef
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            category = documentSnapshot.toObject(Category.class);
                            category.setId(documentSnapshot.getId());
                            // System.out.println("ExpenseCategoryId - "+expenseCategory.getName());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
            */

        }

        @Override
        public int getItemCount() {
            return expenseList.size();
        }
    }


}