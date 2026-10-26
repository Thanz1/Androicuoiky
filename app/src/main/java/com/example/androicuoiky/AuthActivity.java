package com.example.androicuoiky;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AuthPrefs";
    private static final String KEY_REMEMBER_EMAIL = "remembered_email";
    private static final String KEY_IS_REMEMBERED = "is_remembered";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private boolean isRegisterMode = false;

    private EditText etFullName, etStudentId, etEmail, etPassword;
    private CheckBox cbRememberEmail;
    private Button btnSubmitAuth, btnGoogleSignIn;
    private TextView tvSwitchMode, tvHeader;

    private SharedPreferences sharedPreferences;

    // Bộ nhận kết quả phản hồi khi chọn tài khoản Google
    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null && account.getIdToken() != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Toast.makeText(this, "Đăng nhập Google thất bại (Mã lỗi: " + e.getStatusCode() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Nếu người dùng đã đăng nhập từ trước thì vào thẳng MainActivity
        if (mAuth.getCurrentUser() != null) {
            goToMainActivity();
            return;
        }

        // Cấu hình Google Sign-In với Web Client ID chính xác
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("407589375963-3h5h6las0ilu3j1lp43uta6r6ph4ee86.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Ánh xạ View
        etFullName = findViewById(R.id.etFullName);
        etStudentId = findViewById(R.id.etStudentId);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberEmail = findViewById(R.id.cbRememberEmail);
        btnSubmitAuth = findViewById(R.id.btnSubmitAuth);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        tvHeader = findViewById(R.id.tvHeader);

        // Khôi phục email đăng nhập lần cuối nếu đã được ghi nhớ trước đó
        loadRememberedEmail();

        // Chuyển đổi giữa chế độ Đăng nhập và Đăng ký
        tvSwitchMode.setOnClickListener(v -> toggleMode());

        // Bấm nút Đăng nhập / Đăng ký bằng Email
        btnSubmitAuth.setOnClickListener(v -> handleAuthAction());

        // Bấm nút Đăng nhập bằng Google
        btnGoogleSignIn.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void loadRememberedEmail() {
        boolean isRemembered = sharedPreferences.getBoolean(KEY_IS_REMEMBERED, false);
        if (isRemembered) {
            String savedEmail = sharedPreferences.getString(KEY_REMEMBER_EMAIL, "");
            etEmail.setText(savedEmail);
            cbRememberEmail.setChecked(true);
            etPassword.requestFocus(); // Chuyển con trỏ xuống ô mật khẩu cho tiện thao tác
        }
    }

    private void saveEmailPreference(String email, boolean shouldRemember) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (shouldRemember) {
            editor.putString(KEY_REMEMBER_EMAIL, email);
            editor.putBoolean(KEY_IS_REMEMBERED, true);
        } else {
            editor.remove(KEY_REMEMBER_EMAIL);
            editor.putBoolean(KEY_IS_REMEMBERED, false);
        }
        editor.apply();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        String name = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Sinh viên";
                        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";

                        // Lưu lại email Google nếu checkbox đang được bật
                        if (cbRememberEmail.isChecked()) {
                            saveEmailPreference(email, true);
                        }

                        // Kiểm tra xem user này đã tồn tại trong Firestore chưa
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(snapshot -> {
                                    if (!snapshot.exists()) {
                                        User newUser = new User(uid, email, name, "", "", "student", "");
                                        db.collection("users").document(uid).set(newUser);
                                    }
                                    Toast.makeText(this, "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi xác thực: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            tvHeader.setText("ĐĂNG KÝ TÀI KHOẢN");
            etFullName.setVisibility(View.VISIBLE);
            etStudentId.setVisibility(View.VISIBLE);
            cbRememberEmail.setVisibility(View.GONE); // Đăng ký thì không cần hiện ô ghi nhớ
            btnSubmitAuth.setText("Đăng ký");
            btnGoogleSignIn.setVisibility(View.GONE);
            tvSwitchMode.setText("Đã có tài khoản? Bấm để Đăng nhập");
        } else {
            tvHeader.setText("ĐĂNG NHẬP");
            etFullName.setVisibility(View.GONE);
            etStudentId.setVisibility(View.GONE);
            cbRememberEmail.setVisibility(View.VISIBLE);
            btnSubmitAuth.setText("Đăng nhập");
            btnGoogleSignIn.setVisibility(View.VISIBLE);
            tvSwitchMode.setText("Chưa có tài khoản? Bấm để Đăng ký");
        }
    }

    private void handleAuthAction() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Email không được trống, Mật khẩu tối thiểu 6 ký tự!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRegisterMode) {
            String name = etFullName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            if (name.isEmpty() || studentId.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập họ tên và mã số sinh viên!", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        User newUser = new User(uid, email, name, "", studentId, "student", "");
                        db.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                    goToMainActivity();
                                });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi đăng ký: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            // Đăng nhập bằng Email/Password
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(authResult -> {
                        // Lưu hoặc xóa email dựa trên trạng thái checkbox
                        saveEmailPreference(email, cbRememberEmail.isChecked());

                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Đăng nhập thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void goToMainActivity() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}