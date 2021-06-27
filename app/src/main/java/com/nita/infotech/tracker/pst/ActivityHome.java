package com.nita.infotech.tracker.pst;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.nita.infotech.tracker.pst.model.User;
import com.nita.infotech.tracker.pst.util.SessionManager;
import com.nita.infotech.tracker.pst.util.Utility;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ActivityHome extends AppCompatActivity {

    private SessionManager sessionManager;
    private Gson gson;
    private User loggedInUser;
    private String loggedInUserId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    SweetAlertDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        sessionManager = new SessionManager(getApplicationContext());
        gson = Utility.getGson();
        String userJson = sessionManager.getString("loggedInUser");
        loggedInUser = gson.fromJson(userJson, User.class);
        loggedInUserId = loggedInUser.getId();
        pDialog = Utility.createSweetAlertDialog(getApplicationContext());
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_expense, R.id.navigation_category)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.contentLayout);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_salary:
                getSupportActionBar().setTitle(R.string.salary);
                FragmentSalary fragmentSalary = new FragmentSalary();
                replaceFragment(fragmentSalary, getString(R.string.salary));
                break;

            case R.id.nav_logout:
                SweetAlertDialog dialog = new SweetAlertDialog(ActivityHome.this, SweetAlertDialog.WARNING_TYPE)
                        .setTitleText("Logout?")
                        .setContentText("Do you really want to logout from the App? ")
                        .setConfirmText("OK")
                        .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismissWithAnimation();

                            }
                        })
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                sDialog.dismissWithAnimation();

                                SessionManager session = new SessionManager(getApplication());
                                session.remove("loggedInUser");
                                session.remove("loggedInUserId");
                                session.remove("orgId");
                                session.clear();
                                Intent intent = new Intent(getApplicationContext(), ActivityLogin.class);
                                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                                startActivity(intent);
                                finish();
                            }
                        });
                dialog.setCancelable(false);
                dialog.show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        fragmentTransaction.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        fragmentTransaction.replace(R.id.contentLayout, fragment, tag);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() <= 0) {
            SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Exit App")
                    .setContentText("Do you really want to exit the App? ")
                    .setConfirmText("Ok")
                    .setCancelButton("Cancel", new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.dismissWithAnimation();
                        }
                    })
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            sDialog.dismissWithAnimation();
                            finish();
                        }
                    });
            dialog.setCancelable(false);
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}