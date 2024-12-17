package com.example.nfc;

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

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
            Toast.makeText(this, "NFC not supported.", Toast.LENGTH_LONG).show();
            finish();
        }

        writeButton.setOnClickListener(v -> {
            if (currentTag != null) {
                String data = inputData.getText().toString();
                if (!data.isEmpty()) {
                    writeToTag(currentTag, data);
                } else {
                    Toast.makeText(this, "Введите данные для записи.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Поднесите NFC-метку перед записью.", Toast.LENGTH_SHORT).show();
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
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
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
                if (ndefMessage != null) {
                    for (NdefRecord record : ndefMessage.getRecords()) {
                        String payload = new String(record.getPayload(), StandardCharsets.UTF_8);
                        textView.setText("Прочитанные данные: " + payload);
                    }
                } else {
                    textView.setText("Метка пустая.");
                }
            } catch (Exception e) {
                Log.e("NFC", "Ошибка при чтении метки.", e);
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "Метка не поддерживает NDEF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeToTag(Tag tag, String data) {
        NdefRecord record = NdefRecord.createTextRecord("en", data);
        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                ndef.writeNdefMessage(message);
                Toast.makeText(this, "Данные успешно записаны!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("NFC", "Ошибка при записи на метку.", e);
            } finally {
                try {
                    ndef.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "Метка не поддерживает запись.", Toast.LENGTH_SHORT).show();
        }
    }
}
