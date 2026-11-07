package com.pastedialer.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_CALL = 41, REQ_CONTACTS = 42, REQ_LOG = 43;
    private LinearLayout root, content, header;
    private EditText numberBox;
    private String number = "";
    private String tab = "keypad";
    private boolean dark = false;
    private int bg, surface, text, muted, accent, line;
    private final ArrayList<Person> people = new ArrayList<>();
    private final ArrayList<Person> recent = new ArrayList<>();

    private static class Person {
        String name, phone, detail;
        Person(String n, String p, String d) { name=n; phone=p; detail=d; }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) { number = state.getString("number", ""); dark = state.getBoolean("dark", false); }
        String incoming = getIntent() != null ? getIntent().getDataString() : null;
        if (incoming != null && incoming.startsWith("tel:")) number = Uri.decode(incoming.substring(4));
        buildScreen();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("number", number); out.putBoolean("dark", dark);
    }

    private void colors() {
        bg = dark ? Color.rgb(18,20,24) : Color.rgb(247,248,250);
        surface = dark ? Color.rgb(31,34,40) : Color.WHITE;
        text = dark ? Color.rgb(245,246,248) : Color.rgb(25,28,33);
        muted = dark ? Color.rgb(166,173,184) : Color.rgb(111,119,130);
        accent = dark ? Color.rgb(139,183,255) : Color.rgb(28,100,210);
        line = dark ? Color.rgb(57,62,71) : Color.rgb(230,233,238);
    }

    private void buildScreen() {
        colors();
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(22),dp(16),dp(18),dp(12));
        TextView title = new TextView(this); title.setText(tab.equals("contacts") ? "Contacts" : tab.equals("recent") ? "Recents" : "Phone"); title.setTextColor(text); title.setTextSize(29); title.setTypeface(null,Typeface.BOLD);
        header.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView more = new TextView(this); more.setText("⋮"); more.setTextSize(27); more.setTextColor(text); more.setGravity(Gravity.CENTER); header.addView(more,new LinearLayout.LayoutParams(dp(38),dp(46)));
        more.setOnClickListener(v->showMore());
        TextView theme = new TextView(this); theme.setText(dark ? "☀" : "☾"); theme.setTextSize(24); theme.setTextColor(text); theme.setGravity(Gravity.CENTER); theme.setBackground(round(surface,dp(28))); header.addView(theme,new LinearLayout.LayoutParams(dp(46),dp(46)));
        theme.setOnClickListener(v->{ dark=!dark; buildScreen(); }); root.addView(header);
        getWindow().setStatusBarColor(dark ? Color.rgb(18,20,24) : Color.rgb(247,248,250));
        getWindow().setNavigationBarColor(dark ? Color.rgb(18,20,24) : Color.WHITE);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(16),dp(4),dp(16),dp(8)); root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        if (tab.equals("keypad")) showKeypad(); else if (tab.equals("contacts")) showContacts(); else showRecents();
        LinearLayout nav = new LinearLayout(this); nav.setBackgroundColor(surface); nav.setElevation(dp(8)); nav.setPadding(dp(8),dp(8),dp(8),dp(8));
        addNav(nav,"☷","Recents","recent"); addNav(nav,"◉","Contacts","contacts"); addNav(nav,"⌕","Keypad","keypad");
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(66)));
        setContentView(root);
    }

    private void addNav(LinearLayout nav,String icon,String label,String id) {
        LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
        TextView i = new TextView(this); i.setText(icon); i.setTextSize(23); i.setGravity(Gravity.CENTER); i.setTextColor(tab.equals(id)?accent:muted);
        TextView t = new TextView(this); t.setText(label); t.setTextSize(11); t.setTypeface(null,tab.equals(id)?Typeface.BOLD:Typeface.NORMAL); t.setTextColor(tab.equals(id)?accent:muted);
        item.addView(i); item.addView(t); nav.addView(item,new LinearLayout.LayoutParams(0,-1,1)); item.setOnClickListener(v->{tab=id; buildScreen();});
    }

    private void showKeypad() {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackground(round(surface,dp(24)));
        numberBox = new EditText(this); numberBox.setSingleLine(true); numberBox.setText(number); numberBox.setTextSize(29); numberBox.setTextColor(text); numberBox.setHintTextColor(muted); numberBox.setHint("Enter number"); numberBox.setGravity(Gravity.CENTER); numberBox.setBackgroundColor(Color.TRANSPARENT); numberBox.setInputType(3);
        numberBox.setSelection(numberBox.getText().length()); numberBox.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){number=s.toString();} public void afterTextChanged(android.text.Editable e){}});
        card.addView(numberBox,new LinearLayout.LayoutParams(-1,dp(66)));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER); TextView paste = smallAction("Paste"); TextView clear = smallAction("⌫"); actions.addView(paste,new LinearLayout.LayoutParams(dp(100),dp(38))); actions.addView(clear,new LinearLayout.LayoutParams(dp(100),dp(38)));
        paste.setOnClickListener(v->pasteNumber()); clear.setOnClickListener(v->{ if(number.length()>0){number=number.substring(0,number.length()-1);numberBox.setText(number);numberBox.setSelection(number.length());}}); card.addView(actions);
        content.addView(card);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(3); grid.setRowCount(4); grid.setUseDefaultMargins(false); LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,0,1); gp.topMargin=dp(8); content.addView(grid,gp);
        String[][] keys={{"1","",""},{"2","ABC",""},{"3","DEF",""},{"4","GHI",""},{"5","JKL",""},{"6","MNO",""},{"7","PQRS",""},{"8","TUV",""},{"9","WXYZ",""},{"*","",""},{"0","+",""},{"#","",""}};
        for(int x=0;x<12;x++) { final String key=keys[x][0]; LinearLayout cell=new LinearLayout(this); cell.setGravity(Gravity.CENTER); cell.setOrientation(LinearLayout.VERTICAL); TextView big=new TextView(this); big.setText(key); big.setTextSize(27); big.setTextColor(text); big.setGravity(Gravity.CENTER); TextView sub=new TextView(this); sub.setText(keys[x][1]); sub.setTextSize(9); sub.setTextColor(muted); sub.setGravity(Gravity.CENTER); cell.addView(big); cell.addView(sub); GridLayout.LayoutParams p=new GridLayout.LayoutParams(); p.width=0;p.height=0;p.columnSpec=GridLayout.spec(x%3,1,1f);p.rowSpec=GridLayout.spec(x/3,1,1f);p.setMargins(dp(4),dp(2),dp(4),dp(2));grid.addView(cell,p); cell.setOnClickListener(v->{number+=key;numberBox.setText(number);numberBox.setSelection(number.length());}); }
        LinearLayout callRow=new LinearLayout(this); callRow.setGravity(Gravity.CENTER); callRow.setPadding(0,dp(8),0,dp(4)); TextView call=new TextView(this); call.setText("☎"); call.setTextSize(29); call.setTextColor(Color.WHITE); call.setGravity(Gravity.CENTER); call.setBackground(round(Color.rgb(31,164,103),dp(40))); callRow.addView(call,new LinearLayout.LayoutParams(dp(72),dp(58))); call.setOnClickListener(v->makeCall()); content.addView(callRow);
    }

    private TextView smallAction(String s){TextView v=new TextView(this);v.setText(s);v.setTextColor(accent);v.setGravity(Gravity.CENTER);v.setTextSize(14);return v;}
    private void pasteNumber(){try{ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm!=null&&cm.hasPrimaryClip()){ClipData d=cm.getPrimaryClip();if(d!=null&&d.getItemCount()>0){CharSequence c=d.getItemAt(0).coerceToText(this);if(c!=null){number=c.toString().replaceAll("[^0-9+*#]","");numberBox.setText(number);numberBox.setSelection(number.length());}}}}catch(Exception e){Toast.makeText(this,"Could not paste",Toast.LENGTH_SHORT).show();}}

    private void makeCall(){String n=number.trim();if(n.length()==0){Toast.makeText(this,"Enter a phone number",Toast.LENGTH_SHORT).show();return;} if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CALL_PHONE},REQ_CALL);return;} placeCall(n);}
    private void placeCall(String n){try{Intent i=new Intent(Intent.ACTION_CALL,Uri.fromParts("tel",n,null));startActivity(i);}catch(Exception e){Intent i=new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",n,null));startActivity(i);}}

    private void showContacts(){ if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},REQ_CONTACTS); TextView m=message("Allow Contacts permission to show your contacts.");content.addView(m);return;} people.clear(); Cursor c=null;try{c=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME+" COLLATE LOCALIZED ASC");if(c!=null)while(c.moveToNext())people.add(new Person(c.getString(0),c.getString(1),"Contact"));}catch(Exception ignored){}finally{if(c!=null)c.close();} showPeople(people,"No contacts found"); }

    private void showRecents(){if(Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG},REQ_LOG);content.addView(message("Allow Call Log permission to show recent calls."));return;} recent.clear();Cursor c=null;try{c=getContentResolver().query(CallLog.Calls.CONTENT_URI,new String[]{CallLog.Calls.CACHED_NAME,CallLog.Calls.NUMBER,CallLog.Calls.DATE,CallLog.Calls.TYPE},null,null,CallLog.Calls.DATE+" DESC LIMIT 60");if(c!=null)while(c.moveToNext()){String name=c.getString(0);String phone=c.getString(1);long date=c.getLong(2);int type=c.getInt(3);String d=new SimpleDateFormat("dd MMM, hh:mm a",Locale.getDefault()).format(new Date(date));recent.add(new Person(name==null||name.length()==0?phone:name,phone,d));}}catch(Exception ignored){}finally{if(c!=null)c.close();}showPeople(recent,"No recent calls");}

    private TextView message(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(15);t.setTextColor(muted);t.setGravity(Gravity.CENTER);return t;}
    private void showPeople(ArrayList<Person> data,String empty){if(data.size()==0){content.addView(message(empty),new LinearLayout.LayoutParams(-1,dp(180)));return;}ListView list=new ListView(this);list.setDivider(new android.graphics.drawable.ColorDrawable(line));list.setDividerHeight(dp(1));list.setBackground(round(surface,dp(20)));list.setAdapter(new BaseAdapter(){public int getCount(){return data.size();}public Object getItem(int p){return data.get(p);}public long getItemId(int p){return p;}public View getView(int p,View old,ViewGroup parent){Person person=data.get(p);LinearLayout row=new LinearLayout(MainActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(16),dp(11),dp(12),dp(11));TextView avatar=new TextView(MainActivity.this);avatar.setText(person.name.length()>0?person.name.substring(0,1).toUpperCase():"•");avatar.setTextColor(accent);avatar.setTextSize(18);avatar.setGravity(Gravity.CENTER);avatar.setBackground(round(dark?Color.rgb(44,58,78):Color.rgb(230,239,255),dp(28)));row.addView(avatar,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout labels=new LinearLayout(MainActivity.this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,0,0);TextView name=new TextView(MainActivity.this);name.setText(person.name);name.setTextSize(16);name.setTextColor(text);name.setTypeface(null,Typeface.BOLD);TextView detail=new TextView(MainActivity.this);detail.setText(person.detail==null?person.phone:person.detail+"  •  "+person.phone);detail.setTextSize(12);detail.setTextColor(muted);labels.addView(name);labels.addView(detail);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView call=new TextView(MainActivity.this);call.setText("☎");call.setTextSize(20);call.setTextColor(accent);row.addView(call);row.setOnClickListener(v->{number=person.phone;tab="keypad";buildScreen();});return row;}});content.addView(list,new LinearLayout.LayoutParams(-1,-1));}

    private void requestDefaultDialer(){
        if(Build.VERSION.SDK_INT>=24){
            try {
                Intent i=new Intent("android.telecom.action.CHANGE_DEFAULT_DIALER");
                i.putExtra("android.telecom.extra.CHANGE_DEFAULT_DIALER_PACKAGE_NAME",getPackageName());
                startActivity(i);
            } catch(Exception e) {
                Toast.makeText(this,"ডিফল্ট ফোন অ্যাপ সেটিংস খোলা যায়নি",Toast.LENGTH_LONG).show();
            }
        } else {
            new AlertDialog.Builder(this)
                .setTitle("ডিফল্ট ফোন অ্যাপ")
                .setMessage("Android 5.1.1-এ Android 6 বা পরের সংস্করণের মতো Default Dialer role নেই। এই অ্যাপটি নম্বরের DIAL লিংক খুলতে পারে; ফোনের Settings থেকে Default app/Opening links অপশন থাকলে PasteDialer নির্বাচন করুন। চলমান কলের আসল স্ক্রিন Android সিস্টেমই নিয়ন্ত্রণ করবে।")
                .setPositiveButton("ঠিক আছে",null).show();
        }
    }
    private void showMore(){
        final String[] choices={"Set as default phone app","Paste number","About"};
        new AlertDialog.Builder(this).setItems(choices,(dialog,which)->{
            if(which==0) requestDefaultDialer();
            else if(which==1) pasteNumber();
            else new AlertDialog.Builder(this).setTitle("One UI Phone").setMessage("A lightweight dialer designed for Android 5.1.1 and newer. On Android 5.1.1, the system keeps control of the in-call screen.").setPositiveButton("OK",null).show();
        }).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED){if(requestCode==REQ_CALL)placeCall(number);else buildScreen();}else Toast.makeText(this,"Permission is needed for this feature",Toast.LENGTH_SHORT).show();}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
}
