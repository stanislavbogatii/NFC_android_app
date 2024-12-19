package com.example.nfc;
import com.example.nfc.EncryptionUtils;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView textView;
    private EditText inputData;
    private Button writeButton;
    private Tag currentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        inputData = findViewById(R.id.inputData);
        writeButton = findViewById(R.id.writeButton);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            Toast.makeText(this, "Dispozitivul nu suportă NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        writeButton.setOnClickListener(v -> {
            authenticateUser();
            if (currentTag != null) {
                String data = inputData.getText().toString();
                if (!data.isEmpty()) {
                    try {
                        writeToTag(currentTag, secureData(data));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Toast.makeText(this, "Introduceți datele pentru scriere.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Apropiați o etichetă NFC înainte de a scrie.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatch();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            currentTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            readFromTag(currentTag);
        }
    }

    private void enableForegroundDispatch() {
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE);
        IntentFilter[] filters = new IntentFilter[]{new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)};
        String[][] techList = new String[][]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, techList);
    }

    private void disableForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void readFromTag(Tag tag) {
        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                String secretKey = "1234567890123456";
                if (ndefMessage != null) {
                    for (NdefRecord record : ndefMessage.getRecords()) {
                        String encryptedPayload = new String(record.getPayload(), StandardCharsets.UTF_8);
                        String decryptedData = EncryptionUtils.decrypt(encryptedPayload, secretKey);
                        textView.setText("Date citite (securizate): " + decryptedData);
                    }
                } else {
                    textView.setText("Eticheta NFC este goală.");
                }

            } catch (Exception e) {
                Log.e("NFC", "Eroare la citirea etichetei.", e);
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "Eticheta nu suportă formatul NDEF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeToTag(Tag tag, String data) throws Exception {


        String secretKey = "1234567890123456";
        String encryptedData = EncryptionUtils.encrypt(data, secretKey);

        NdefRecord record = NdefRecord.createTextRecord("en", encryptedData);
        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
        Ndef ndef = Ndef.get(tag);

        if (ndef != null) {
            try {
                ndef.connect();
                ndef.writeNdefMessage(message);
                Toast.makeText(this, "Date scrise cu succes!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("NFC", "Eroare la scriere.", e);
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "Eticheta nu suportă scrierea.", Toast.LENGTH_SHORT).show();
        }
    }

    private String secureData(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Arrays.toString(hash);
        } catch (Exception e) {
            Log.e("NFC", "Eroare la criptarea datelor.", e);
            return data;
        }
    }


    private void authenticateUser() {
        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Toast.makeText(MainActivity.this, "Аутентификация успешна!", Toast.LENGTH_SHORT).show();
                        // Разрешить выполнение NFC-операции
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(MainActivity.this, "Аутентификация не удалась!", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Биометрическая аутентификация")
                .setSubtitle("Используйте отпечаток пальца или распознавание лица.")
                .setNegativeButtonText("Отмена")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

}
