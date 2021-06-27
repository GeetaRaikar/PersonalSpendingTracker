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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.nita.infotech.tracker.pst.model.Category;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.pedant.SweetAlert.SweetAlertDialog;

public class FragmentCategory extends Fragment {
    private Gson gson;
    private FirebaseFirestore db=FirebaseFirestore.getInstance();
    private CollectionReference categoryCollectionRef=db.collection("Category");
    private CollectionReference expenseCategoryCollectionRef=db.collection("ExpenseCategory");
    private User loggedInUser;
    private SessionManager sessionManager;
    private String loggedInUserId;
    private SweetAlertDialog pDialog;
    private ListenerRegistration categoryListener;
    private Category category;
    private List<Category> categoryList = new ArrayList<>();
    private List<String> alreadyCategoryList = new ArrayList<>();
    private RecyclerView rvCategory;
    private RecyclerView.Adapter categoryAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private LinearLayout llNoList;
    private ImageButton btnSubmit;
    private String name;
    private EditText etExpenseCategoryName;

    public FragmentCategory() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        if (pDialog.isShowing() && pDialog == null) {
            pDialog.show();
        }
        categoryListener = categoryCollectionRef
                .whereEqualTo("creatorId",loggedInUserId)
                .orderBy("createdDate", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        if (categoryList.size() != 0) {
                            categoryList.clear();
                        }
                        if (alreadyCategoryList.size() != 0) {
                            alreadyCategoryList.clear();
                        }
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            // Log.d(TAG, document.getId()document.getId() + " => " + document.getData());
                            category = document.toObject(Category.class);
                            category.setId(document.getId());
                            alreadyCategoryList.add(category.getCategory());
                            categoryList.add(category);
                        }
                        if (categoryList.size() != 0) {
                            rvCategory.setVisibility(View.VISIBLE);
                            llNoList.setVisibility(View.GONE);
                            categoryAdapter = new CategoryAdapter(categoryList);
                            categoryAdapter.notifyDataSetChanged();
                            rvCategory.setAdapter(categoryAdapter);
                        } else {
                            rvCategory.setVisibility(View.GONE);
                            llNoList.setVisibility(View.VISIBLE);
                        }
                    }
                });

    }

    @Override
    public void onStop() {
        super.onStop();
        if (categoryListener != null) {
            categoryListener.remove();
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

        View view = inflater.inflate(R.layout.fragment_category, container, false);
        ((ActivityHome)getActivity()).getSupportActionBar().setDisplayShowCustomEnabled(false);
        ((ActivityHome)getActivity()).getSupportActionBar().setTitle(getString(R.string.category));
        BottomNavigationView navBar = getActivity().findViewById(R.id.nav_view);
        navBar.setVisibility(View.VISIBLE);

        llNoList = (LinearLayout) view.findViewById(R.id.llNoList);
        rvCategory = view.findViewById(R.id.rvCategory);
        // use a linear layout manager
        layoutManager = new LinearLayoutManager(getContext());
        rvCategory.setLayoutManager(layoutManager);
        etExpenseCategoryName = view.findViewById(R.id.etCategory);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name = etExpenseCategoryName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    etExpenseCategoryName.setError("Enter Category");
                    etExpenseCategoryName.requestFocus();
                    return;
                } else {
                    if (Utility.isNumericWithSpace(name)) {
                        etExpenseCategoryName.setError("Invalid Category");
                        etExpenseCategoryName.requestFocus();
                        return;
                    }else {
                        String Name = name.replaceAll("\\s+", "");
                        System.out.println("Name " + Name);
                        for (int i = 0; i < alreadyCategoryList.size(); i++) {
                            String expenseCategoryName = alreadyCategoryList.get(i).replaceAll("\\s+", "");
                            if (Name.equalsIgnoreCase(expenseCategoryName)) {
                                System.out.println("equal");
                                etExpenseCategoryName.setError("Already this category is saved");
                                etExpenseCategoryName.requestFocus();
                                return;
                            }
                        }
                    }
                }
                if (!pDialog.isShowing() && pDialog == null) {
                    pDialog.show();
                }
                category = new Category();
                category.setCategory(name);
                category.setCreatorId(loggedInUserId);
                category.setModifierId(loggedInUserId);
                category.setCreatorType("A");
                category.setModifierType("A");
                addCategory();
            }
        });
        return view;
    }

    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.MyViewHolder> {
        private List<Category> categoryList;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView tvCategory;
            public ImageView ivDeleteCategory;

            public MyViewHolder(View view) {
                super(view);
                tvCategory = view.findViewById(R.id.tvCategory);
                ivDeleteCategory = view.findViewById(R.id.ivDeleteCategory);
            }
        }


        public CategoryAdapter(List<Category> categoryList) {
            this.categoryList = categoryList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.row_category, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            final Category category = categoryList.get(position);
            holder.tvCategory.setText("" + category.getCategory());
            holder.ivDeleteCategory.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    deleteCategory(category);
                }
            });


        }

        @Override
        public int getItemCount() {
            return categoryList.size();
        }
    }

    private void deleteCategory(Category category) {
        if (pDialog == null && !pDialog.isShowing()) {
            pDialog.show();
        }
        expenseCategoryCollectionRef
                .whereEqualTo("categoryId", category.getId())
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
                                    .setContentText("Do you want to delete " + category.getCategory() + "?")
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
                                            categoryCollectionRef.document(category.getId())
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            if (pDialog != null) {
                                                                pDialog.dismiss();
                                                            }
                                                            SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                                                                    .setTitleText("Deleted")
                                                                    .setContentText("Category has been deleted.")
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
                                                                    .setTitleText("Unable to delete category")
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
                                    .setTitleText("Unable to delete category")
                                    .setContentText("In this category some expenses are there.")
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

    private void addCategory() {
        categoryCollectionRef
                .add(category)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        if (pDialog != null) {
                            pDialog.dismiss();
                        }
                        SweetAlertDialog dialog = new SweetAlertDialog(getContext(), SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Successfully")
                                .setContentText("Category has been successfully added.")
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
                                .setTitleText("Unable to add category")
                                .setContentText("Network issue, please check it.")
                                .setConfirmText("Ok");
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                });
        etExpenseCategoryName.setText("");
    }

}