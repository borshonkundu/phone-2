package com.pastedialer.app;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

@TargetApi(Build.VERSION_CODES.M)
public class InCallActivity extends Activity implements CallManager.Listener {

    private TextView tvName, tvNumber, tvState, tvTimer;
    private Button btnMute, btnSpeaker, btnRec, btnAnswer, btnEnd, btnPasteDtmf, btnSendDtmf;
    private EditText etDtmfNumber;
    private View rowControls;

    private final Handler h = new Handler(Looper.getMainLooper());
    private String cachedNumber;
    private String cachedName;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshTimer();
            h.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_incall);
        tvName = findViewById(R.id.tvName);
        tvNumber = findViewById(R.id.tvNumber);
        tvState = findViewById(R.id.tvState);
        tvTimer = findViewById(R.id.tvTimer);
        btnMute = findViewById(R.id.btnMute);
        btnSpeaker = findViewById(R.id.btnSpeaker);
        btnRec = findViewById(R.id.btnRec);
        btnAnswer = findViewById(R.id.btnAnswer);
        btnEnd = findViewById(R.id.btnEnd);
        rowControls = findViewById(R.id.rowControls);
        etDtmfNumber = findViewById(R.id.etDtmfNumber);
        btnPasteDtmf = findViewById(R.id.btnPasteDtmf);
        btnSendDtmf = findViewById(R.id.btnSendDtmf);

        btnPasteDtmf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pasteNumberFromClipboard(); }
        });
        btnSendDtmf.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { sendDtmfSequence(); }
        });

        btnAnswer.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { CallManager.answer(); }
        });
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { CallManager.rejectOrHangup(); }
        });
        btnMute.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { CallManager.toggleMute(); }
        });
        btnSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { CallManager.toggleSpeaker(); }
        });
        btnRec.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { CallManager.toggleRecording(); }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallManager.setListener(this);
        refresh();
        h.post(tick);
    }

    @Override
    protected void onStop() {
        super.onStop();
        CallManager.setListener(null);
        h.removeCallbacks(tick);
    }

    @Override
    public void onChanged() {
        refresh();
    }

    @Override
    public void onBackPressed() {
        // কল চলাকালীন ব্যাক চাপলে অ্যাক্টিভিটি লুকাবে, কল কাটবে না
        moveTaskToBack(true);
    }

    private void pasteNumberFromClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData clip = cm.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence value = clip.getItemAt(0).coerceToText(this);
                if (value != null) {
                    String digits = value.toString().replaceAll("[^0-9*#]", "");
                    etDtmfNumber.setText(digits);
                    etDtmfNumber.setSelection(digits.length());
                    return;
                }
            }
        }
        Toast.makeText(this, "ক্লিপবোর্ডে কোনো নম্বর নেই", Toast.LENGTH_SHORT).show();
    }

    private void sendDtmfSequence() {
        String digits = etDtmfNumber.getText().toString().replaceAll("[^0-9*#]", "");
        if (digits.length() == 0) {
            Toast.makeText(this, "আগে নম্বর পেস্ট বা লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT < 23) {
            Toast.makeText(this, "Android 5.1.1-এ অ্যাপ থেকে DTMF টোন পাঠানোর পাবলিক API নেই। নম্বর পেস্ট হবে, কিন্তু টোন পাঠানো যাবে না।", Toast.LENGTH_LONG).show();
            return;
        }
        final Call call = CallManager.current();
        if (call == null || call.getState() != Call.STATE_ACTIVE) {
            Toast.makeText(this, "কোনো সক্রিয় কল নেই", Toast.LENGTH_SHORT).show();
            return;
        }
        final String sequence = digits;
        final Handler toneHandler = new Handler(Looper.getMainLooper());
        final int[] index = {0};
        final Runnable[] step = new Runnable[1];
        step[0] = new Runnable() {
            @Override public void run() {
                if (index[0] >= sequence.length()) {
                    call.stopDtmfTone();
                    Toast.makeText(InCallActivity.this, "টোন পাঠানো শেষ", Toast.LENGTH_SHORT).show();
                    return;
                }
                call.playDtmfTone(sequence.charAt(index[0]++));
                toneHandler.postDelayed(new Runnable() {
                    @Override public void run() {
                        call.stopDtmfTone();
                        toneHandler.postDelayed(step[0], 100);
                    }
                }, 150);
            }
        };
        toneHandler.post(step[0]);
    }

    // ------------------------------------------------------------------ UI
    private void refresh() {
        Call c = CallManager.current();
        if (c == null) {
            finish();
            return;
        }
        String num = CallManager.number(c);
        if (!num.equals(cachedNumber)) {
            cachedNumber = num;
            cachedName = lookupName(num);
        }
        tvName.setText(cachedName != null ? cachedName : (num.isEmpty() ? "অজানা নম্বর" : num));
        tvNumber.setText(num);

        int st = c.getState();
        tvState.setText(stateText(st));

        boolean ringing = st == Call.STATE_RINGING;
        btnAnswer.setVisibility(ringing ? View.VISIBLE : View.GONE);
        rowControls.setVisibility(ringing ? View.GONE : View.VISIBLE);
        btnEnd.setText(ringing ? "প্রত্যাখ্যান" : "কল কেটে দিন");

        btnMute.setText(CallManager.isMuted() ? "মিউট বন্ধ" : "মিউট");
        btnSpeaker.setText(CallManager.isSpeaker() ? "স্পিকার বন্ধ" : "স্পিকার");
        btnRec.setText(CallRecorder.isRecording() ? "রেকর্ডিং বন্ধ" : "রেকর্ড");
        refreshTimer();
    }

    private void refreshTimer() {
        Call c = CallManager.current();
        if (c == null) return;
        int st = c.getState();
        Call.Details d = c.getDetails();
        long ct = d == null ? 0 : d.getConnectTimeMillis();
        if ((st == Call.STATE_ACTIVE || st == Call.STATE_HOLDING) && ct > 0) {
            long s = Math.max(0, (System.currentTimeMillis() - ct) / 1000);
            tvTimer.setText(String.format(Locale.US, "%02d:%02d", s / 60, s % 60));
        } else {
            tvTimer.setText("");
        }
    }

    private String stateText(int st) {
        switch (st) {
            case Call.STATE_RINGING:       return "ইনকামিং কল...";
            case Call.STATE_DIALING:       return "ডায়াল হচ্ছে...";
            case Call.STATE_CONNECTING:    return "সংযোগ হচ্ছে...";
            case Call.STATE_ACTIVE:        return "কথা হচ্ছে";
            case Call.STATE_HOLDING:       return "হোল্ডে আছে";
            case Call.STATE_DISCONNECTING: return "কল শেষ হচ্ছে...";
            case Call.STATE_DISCONNECTED:  return "কল শেষ";
            default:                       return "";
        }
    }

    private String lookupName(String num) {
        if (num == null || num.isEmpty()) return null;
        if (checkCallingOrSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) return null;
        Cursor c = null;
        try {
            Uri uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(num));
            c = getContentResolver().query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }
}
