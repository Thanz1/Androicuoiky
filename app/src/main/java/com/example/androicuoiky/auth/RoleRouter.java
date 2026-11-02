package com.example.androicuoiky.auth;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.example.androicuoiky.AdminDashboardActivity;
import com.example.androicuoiky.AuthActivity;
import com.example.androicuoiky.MainActivity;
import com.example.androicuoiky.StaffDashboardActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RoleRouter {

    public static void openHome(Activity activity) {
        RoleManager.checkRole(new RoleManager.RoleCallback() {

            @Override
            public void onSuccess(String role) {
                if (activity.isFinishing()) return;

                Class<?> destination;

                switch (role) {
                    case RoleManager.STAFF:
                        destination = StaffDashboardActivity.class;
                        break;

                    case RoleManager.ADMIN:
                        destination = AdminDashboardActivity.class;
                        break;

                    default:
                        destination = MainActivity.class;
                        break;
                }

                Intent intent = new Intent(activity, destination);
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );

                activity.startActivity(intent);
                activity.finish();
            }

            @Override
            public void onError(String message) {
                if (activity.isFinishing()) return;

                Toast.makeText(activity,
                        message, Toast.LENGTH_LONG).show();

                FirebaseAuth.getInstance().signOut();

                if (!(activity instanceof AuthActivity)) {
                    Intent intent = new Intent(
                            activity, AuthActivity.class
                    );
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );
                    activity.startActivity(intent);
                    activity.finish();
                }
            }
        });
    }
}
