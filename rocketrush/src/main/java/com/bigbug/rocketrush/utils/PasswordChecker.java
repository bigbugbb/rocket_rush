package com.bigbug.rocketrush.utils;

public class PasswordChecker {

    private StringBuffer mPassword;
    private int i = 0;

    public PasswordChecker(String aTargetPassword) {
        mPassword = new StringBuffer(aTargetPassword);
    }

    public boolean isMatch(int keyCode) {
        // 68 is the offsetfor the Keycode values to ascii
        if ((keyCode + 68) == ((int) mPassword.charAt(i))) {
            ++i;
        } else {
            i = 0;
        }

        if (i == mPassword.length()) {
            i = 0;
            return true;
        }

        return false;
    }
}