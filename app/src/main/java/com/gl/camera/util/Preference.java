package com.gl.camera.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Liang on 2016/4/7.
 */
public class Preference {
    private static Preference preference;
    private SharedPreferences sharedPreferences;

    public static Preference getInstance(Context context) {
        if (preference == null) {
            preference = new Preference(context);
        }
        return preference;
    }

    private Preference(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setPort(int port) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("port", port);
        editor.apply();
    }

    public int getPort() {
        return sharedPreferences.getInt("port", -1);
    }

    public void setPassword(String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("password", password);
        editor.apply();
    }

    public String getPassword() {
        return sharedPreferences.getString("password", "no password");
    }

    public void setQuality(int quality) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("quality", quality);
        editor.apply();
    }

    public int getQuality() {
        return sharedPreferences.getInt("quality", 20);
    }

    public void setResolution(int resolution) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("resolution", resolution);
        editor.apply();
    }

    public int getResolution() {
        return sharedPreferences.getInt("resolution", 20);
    }

    public void setFPS(int fps) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("fps", fps);
        editor.apply();
    }

    public int getFPS() {
        return sharedPreferences.getInt("fps", 15);
    }

    public void setDeviceId(String deviceId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("DeviceId", deviceId);
        editor.apply();
    }

    public String getDeviceId() {
        return sharedPreferences.getString("DeviceId", "");
    }

    public void setEmail(String email) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.apply();
    }

    public String getEmail() {
        return sharedPreferences.getString("email", "");
    }

    public void setFrequency(int frequency) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("frequency", frequency);
        editor.apply();
    }

    public int getFrequency() {
        return sharedPreferences.getInt("frequency", 15);
    }

    public void setDrop(boolean drop) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("drop", drop);
        editor.apply();
    }

    public boolean getDrop() {
        return sharedPreferences.getBoolean("drop", false);
    }

    public void setDropSimilarity(int dropSimilarity) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("DropSimilarity", dropSimilarity);
        editor.apply();
    }

    public int getDropSimilarity() {
        return sharedPreferences.getInt("DropSimilarity", 80);
    }
}
