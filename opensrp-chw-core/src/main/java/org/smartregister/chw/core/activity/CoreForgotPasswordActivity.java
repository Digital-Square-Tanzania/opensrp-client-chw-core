package org.smartregister.chw.core.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.smartregister.CoreLibrary;
import org.smartregister.chw.core.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

public class CoreForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";
    private final OkHttpClient client = new OkHttpClient();
    private EditText editTextUsername;
    private Button buttonResetPassword;
    private TextView textViewStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        Toolbar toolbar = findViewById(R.id.back_to_nav_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
        }

        editTextUsername = findViewById(R.id.editTextUsername);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);
        textViewStatus = findViewById(R.id.textViewStatus);
        progressBar = findViewById(R.id.progressBar);

        buttonResetPassword.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                editTextUsername.setError("Username cannot be empty");
                return;
            }
            hideStatus(); // Clear previous status
            attemptPasswordReset(username);
        });

        findViewById(R.id.back_to_login).setOnClickListener(v -> {
            finish();
        });
    }

    private void attemptPasswordReset(String username) {
        showLoading(true);
        textViewStatus.setText(""); // Clear previous messages

        // Construct the full URL
        String url = getFormattedUrl() + username; // Simple concatenation works here

        Request request = new Request.Builder().url(url).get() // Explicitly state GET, though it's the default
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Timber.e(e, "Network request failed.");
                // Ensure UI updates run on the main thread
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Network error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Ensure UI updates run on the main thread
                runOnUiThread(() -> showLoading(false));

                int statusCode = response.code();
                String responseBodyString = ""; // Read body only if needed, ensure it's closed

                try (ResponseBody responseBody = response.body()) {
                    // You might want to read the body for specific error messages from the API
                    if (responseBody != null) {
                        // responseBodyString = responseBody.string(); // Uncomment if needed
                    }

                    Timber.i("Response code: %s", statusCode);

                    if (statusCode == 200) {
                        // Success
                        runOnUiThread(() -> showSuccess("Password reset instructions sent. Please check your email."));
                    } else if (statusCode == 404) {
                        // Username not found
                        runOnUiThread(() -> showError("Error: Username not found."));
                    } else if (statusCode >= 400 && statusCode < 500) {
                        // Other client-side errors (e.g., 400 Bad Request, 401 Unauthorized, 403 Forbidden)
                        Timber.i("Client error response: " + statusCode + ", Body: " + responseBodyString);
                        runOnUiThread(() -> showError("Error: Could not process request (Code: " + statusCode + "). Please try again."));
                    } else {
                        // Server errors (5xx) or unexpected codes
                        Timber.i("Server or unexpected error response: " + statusCode + ", Body: " + responseBodyString);
                        runOnUiThread(() -> showError("An unexpected error occurred (Code: " + statusCode + "). Please try again later."));
                    }
                } catch (
                        Exception e) { // Catch potential exceptions during body reading or processing
                    Timber.e(e, "Error processing response");
                    runOnUiThread(() -> showError("Error processing server response."));
                }
            }
        });
    }

    // Helper methods for UI updates (run on UI thread)

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            buttonResetPassword.setEnabled(false); // Disable button while loading
        } else {
            progressBar.setVisibility(View.GONE);
            buttonResetPassword.setEnabled(true); // Re-enable button
        }
    }

    private void hideStatus() {
        textViewStatus.setText("");
        textViewStatus.setVisibility(View.GONE);
    }

    private void showSuccess(String message) {
        textViewStatus.setText(message);
        textViewStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark)); // Example success color
        textViewStatus.setVisibility(View.VISIBLE);
        // Optionally use Toast for brief messages
        // Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showError(String message) {
        textViewStatus.setText(message);
        textViewStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // Example error color
        textViewStatus.setVisibility(View.VISIBLE);
        // Optionally use Toast for brief messages
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing requests if the activity is destroyed
        // This is important to avoid leaks and unnecessary work
        client.dispatcher().cancelAll();
    }

    public String getFormattedUrl() {
        String baseUrl = CoreLibrary.getInstance().context().configuration().dristhiBaseURL();
        String endString = "/";
        if (baseUrl.endsWith(endString)) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf(endString));
        }
        return baseUrl;
    }
}