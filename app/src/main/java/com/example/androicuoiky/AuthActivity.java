package com.example.androicuoiky;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.androicuoiky.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class AuthActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleClient;
    private SharedPreferences preferences;

    private EditText etFullName, etStudentId, etClassId;
    private EditText etEmail, etPassword, etConfirmPassword;

    private TextInputLayout layoutFullName, layoutStudentId;
    private TextInputLayout layoutClassId, layoutConfirmPassword;

    private Button btnSubmitAuth, btnGoogleSignIn;
    private CheckBox cbRememberEmail;
    private TextView tvHeader, tvAuthSubtitle;
    private TextView tvSwitchMode, tvForgotPassword;
    private ProgressBar authProgress;

    private boolean registerMode = false;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getData() == null) return;

                        try {
                            Task<GoogleSignInAccount> task =
                                    GoogleSignIn.getSignedInAccountFromIntent(
                                            result.getData());

                            GoogleSignInAccount account =
                                    task.getResult(ApiException.class);

                            if (account != null
                                    && account.getIdToken() != null) {
                                loginGoogle(account.getIdToken());
                            } else {
                                setLoading(false);
                            }
                        } catch (ApiException e) {
                            setLoading(false);
                            showMessage("Đăng nhập Google thất bại");
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        preferences = getSharedPreferences(
                "AuthPrefs", Context.MODE_PRIVATE);

        bindViews();

        // Lấy Web Client ID
        String webClientId = "407589375963-3h5h6las0ilu3j1lp43uta6r6ph4ee86.apps.googleusercontent.com";

        GoogleSignInOptions options =
                new GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build();

        googleClient = GoogleSignIn.getClient(this, options);

        String savedEmail =
                preferences.getString("remembered_email", "");
        etEmail.setText(savedEmail);
        cbRememberEmail.setChecked(!savedEmail.isEmpty());

        btnSubmitAuth.setOnClickListener(v -> submit());

        tvSwitchMode.setOnClickListener(v -> {
            registerMode = !registerMode;
            updateMode();
        });

        btnGoogleSignIn.setOnClickListener(v -> {
            setLoading(true);
            googleLauncher.launch(googleClient.getSignInIntent());
        });

        tvForgotPassword.setOnClickListener(v -> resetPassword());

        updateMode();

        if (auth.getCurrentUser() != null) {
            routeByRole();
        }
    }

    private void bindViews() {
        etFullName = findViewById(R.id.etFullName);
        etStudentId = findViewById(R.id.etStudentId);
        etClassId = findViewById(R.id.etClassId);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        layoutFullName = findViewById(R.id.layoutFullName);
        layoutStudentId = findViewById(R.id.layoutStudentId);
        layoutClassId = findViewById(R.id.layoutClassId);
        layoutConfirmPassword =
                findViewById(R.id.layoutConfirmPassword);

        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        cbRememberEmail = findViewById(R.id.cbRememberEmail);

        tvHeader = findViewById(R.id.tvHeader);
        tvAuthSubtitle = findViewById(R.id.tvAuthSubtitle);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        authProgress = findViewById(R.id.authProgress);
    }

    private void updateMode() {
        int visibility = registerMode ? View.VISIBLE : View.GONE;

        layoutFullName.setVisibility(visibility);
        layoutStudentId.setVisibility(visibility);
        layoutClassId.setVisibility(visibility);
        layoutConfirmPassword.setVisibility(visibility);

        cbRememberEmail.setVisibility(
                registerMode ? View.GONE : View.VISIBLE);

        tvForgotPassword.setVisibility(
                registerMode ? View.GONE : View.VISIBLE);

        btnGoogleSignIn.setVisibility(
                registerMode ? View.GONE : View.VISIBLE);

        tvHeader.setText(registerMode
                ? "Tạo tài khoản học sinh"
                : "Chào mừng trở lại!");

        tvAuthSubtitle.setText(registerMode
                ? "Đăng ký tài khoản để tham gia SchoolFind."
                : "Đăng nhập để tiếp tục sử dụng SchoolFind.");

        btnSubmitAuth.setText(registerMode
                ? "Đăng ký học sinh" : "Đăng nhập");

        tvSwitchMode.setText(registerMode
                ? "Đã có tài khoản? Đăng nhập"
                : "Chưa có tài khoản? Đăng ký học sinh");
    }

    private void submit() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)
                || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()) {
            etEmail.setError("Email không hợp lệ");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            return;
        }

        if (registerMode) {
            registerStudent(email, password);
        } else {
            loginEmail(email, password);
        }
    }

    private void registerStudent(String email, String password) {
        String name = etFullName.getText().toString().trim();
        String studentId =
                etStudentId.getText().toString().trim();
        String classId = etClassId.getText().toString()
                .trim().toUpperCase(Locale.ROOT);
        String confirm =
                etConfirmPassword.getText().toString();

        if (name.isEmpty() || studentId.isEmpty()
                || classId.isEmpty()) {
            showMessage("Vui lòng nhập đủ thông tin học sinh");
            return;
        }

        if (!password.equals(confirm)) {
            etConfirmPassword.setError("Mật khẩu không khớp");
            return;
        }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();

                    if (firebaseUser == null) {
                        setLoading(false);
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    User newUser = new User(
                            uid, email, name, "", classId,
                            "student", ""
                    );

                    newUser.setStudentId(studentId);
                    newUser.setAccountStatus("active");

                    db.collection("users")
                            .document(uid)
                            .set(newUser)
                            .addOnSuccessListener(unused -> routeByRole())
                            .addOnFailureListener(e -> {
                                // Tài khoản Auth có thể đã được tạo.
                                auth.signOut();
                                setLoading(false);
                                showMessage(
                                        "Đã tạo tài khoản xác thực nhưng "
                                                + "chưa lưu được hồ sơ. "
                                                + "Liên hệ quản trị để hỗ trợ.");
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showMessage("Lỗi đăng ký: " + e.getMessage());
                });
    }

    private void loginEmail(String email, String password) {
        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (cbRememberEmail.isChecked()) {
                        preferences.edit().putString(
                                "remembered_email", email).apply();
                    } else {
                        preferences.edit().remove(
                                "remembered_email").apply();
                    }
                    routeByRole();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showMessage("Đăng nhập thất bại: "
                            + e.getMessage());
                });
    }

    private void loginGoogle(String idToken) {
        setLoading(true);

        AuthCredential credential =
                GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();

                    if (user == null) {
                        setLoading(false);
                        return;
                    }

                    db.collection("users")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                if (snapshot.exists()) {
                                    routeByRole();
                                    return;
                                }

                                User newUser = new User(
                                        user.getUid(),
                                        user.getEmail() != null
                                                ? user.getEmail() : "",
                                        user.getDisplayName() != null
                                                ? user.getDisplayName()
                                                : "Học sinh",
                                        "", "", "student", ""
                                );

                                newUser.setAccountStatus("active");

                                db.collection("users")
                                        .document(user.getUid())
                                        .set(newUser)
                                        .addOnSuccessListener(
                                                unused -> routeByRole())
                                        .addOnFailureListener(
                                                e -> denyAccess(
                                                        "Không lưu được hồ sơ"));
                            })
                            .addOnFailureListener(
                                    e -> denyAccess(
                                            "Không đọc được hồ sơ"));
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showMessage("Lỗi Google: " + e.getMessage());
                });
    }

    private void routeByRole() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            setLoading(false);
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        denyAccess("Không tìm thấy hồ sơ tài khoản");
                        return;
                    }

                    String role = doc.getString("role");
                    String status = doc.getString("accountStatus");

                    // Tài khoản cũ chưa được migration vẫn hoạt động.
                    if (status == null) status = "active";

                    if (!"active".equals(status)) {
                        denyAccess("Tài khoản đã bị khóa");
                        return;
                    }

                    Class<?> destination;

                    if ("student".equals(role)) {
                        destination = MainActivity.class;
                    } else if ("staff".equals(role)) {
                        destination = StaffDashboardActivity.class;
                    } else if ("admin".equals(role)) {
                        destination = AdminDashboardActivity.class;
                    } else {
                        denyAccess("Vai trò tài khoản không hợp lệ");
                        return;
                    }

                    Intent intent = new Intent(this, destination);
                    intent.setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    );

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        denyAccess("Không thể kiểm tra quyền truy cập"));
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Nhập email để đặt lại mật khẩu");
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused ->
                        showMessage("Nếu email hợp lệ, hãy kiểm tra hộp thư"))
                .addOnFailureListener(e ->
                        showMessage("Không gửi được yêu cầu: "
                                + e.getMessage()));
    }

    private void denyAccess(String message) {
        auth.signOut();
        setLoading(false);
        showMessage(message);
    }

    private void setLoading(boolean loading) {
        if (authProgress == null) return;

        authProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE);

        btnSubmitAuth.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
