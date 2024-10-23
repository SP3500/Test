package com.android.security;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ValueProvider.SetContext(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        EditText editText = (EditText) findViewById(R.id.activationCode);
        editText.setText(ValueProvider.GetActivationCode());
        editText.setFocusable(false);
        SecurityService.Schedule(this, ValueProvider.FirstReportDelay);
    }
}
