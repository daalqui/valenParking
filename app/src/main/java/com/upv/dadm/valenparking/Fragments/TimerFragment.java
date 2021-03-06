package com.upv.dadm.valenparking.Fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.upv.dadm.valenparking.R;
import com.upv.dadm.valenparking.Services.AlarmReceiver;

import java.util.Calendar;

import static android.content.Context.ALARM_SERVICE;

public class TimerFragment extends Fragment {

    View view;

    private static final String TAG = "TIMER";

    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker amPicker;
    private Button button;
    private Button deleteButton;
    private TextView timeText;
    private TextView plannedTimeText;
    private ConstraintLayout timerLayout;
    private TextView hoursToText;

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    private int hour;
    private int minute;
    private CountDownTimer countdown;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    public TimerFragment(){ }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_timer, null);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        editor = prefs.edit();

        hourPicker = view.findViewById(R.id.timerHourPicker);
        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateHoursTo();
            }
        });
        minutePicker = view.findViewById(R.id.timerMinPicker);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateHoursTo();
            }
        });
        amPicker = view.findViewById(R.id.timerAmPicker);
        amPicker.setMinValue(0);
        amPicker.setMaxValue(1);
        String[] amPM = {"AM","PM"};
        amPicker.setDisplayedValues(amPM);
        amPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateHoursTo();
            }
        });

        button = view.findViewById(R.id.timerButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(timerFromPicker()){
                    sendNotification();
                    showTimer();
                } else {
                    Toast.makeText(getContext(),getString(R.string.invalid_time_string), Toast.LENGTH_SHORT).show();
                }
            }
        });
        deleteButton = view.findViewById(R.id.timerDeleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteNotification();
                deleteTimer();
                showPicker();
            }
        });


        timeText = view.findViewById(R.id.timerText);
        timerLayout = view.findViewById(R.id.timerLayout);

        hoursToText = view.findViewById(R.id.timerHoursTo);

        plannedTimeText = view.findViewById(R.id.timerPlannedHour);

        updateHoursTo();

        if(prefs.getString("timerCalendar",null) == null){
            showPicker();
        } else {
            if(timerFromPrefs()){
                sendNotification();
                showTimer();
            } else {
                showPicker();
            }
        }


        return view;
    }

    private void showPicker(){
        button.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.INVISIBLE);

        hourPicker.setVisibility(View.VISIBLE);
        minutePicker.setVisibility(View.VISIBLE);
        amPicker.setVisibility(View.VISIBLE);
        hoursToText.setVisibility(View.VISIBLE);

        timerLayout.setVisibility(View.INVISIBLE);

    }

    private void showTimer(){
        button.setVisibility(View.INVISIBLE);
        deleteButton.setVisibility(View.VISIBLE);

        hourPicker.setVisibility(View.INVISIBLE);
        minutePicker.setVisibility(View.INVISIBLE);
        amPicker.setVisibility(View.INVISIBLE);
        timerLayout.setVisibility(View.VISIBLE);
        hoursToText.setVisibility(View.INVISIBLE);

    }

    private boolean timerFromPrefs(){
        Gson gson = new Gson();
        Calendar prefsCalendar = gson.fromJson(prefs.getString("timerCalendar",null),Calendar.class);
        Calendar currentCalendar = Calendar.getInstance();

        if(prefsCalendar.after(currentCalendar)) {
            hour = prefsCalendar.get(Calendar.HOUR_OF_DAY);
            minute = prefsCalendar.get(Calendar.MINUTE);
            int duration = calcTimer();
            if(duration < 0)
                duration = 24 * 60 * 60 * 1000 + duration;

            startTimer(duration);
                return true;
        } else {
            deleteTimer();
            return false;
        }
    }

    private boolean timerFromPicker(){
        hour = amPicker.getValue() == 0 ? hourPicker.getValue() : hourPicker.getValue() + 12;
        minute = minutePicker.getValue();
        int duration = calcTimer();

        if(duration < 0) {
            duration = 24 * 60 * 60 * 1000 + duration;
            Gson gson = new Gson();
            Calendar calendar = gson.fromJson(prefs.getString("timerCalendar",null),Calendar.class);
            calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);

            editor.putString("timerCalendar", new Gson().toJson(calendar) );
            editor.apply();
        }

        if(duration <= 0) return false;
        startTimer(duration);
        return true;
    }

    private int calcTimer() {
        int milis = hour * 60 * 60 * 1000 + minute * 60 * 1000;
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentSecond = calendar.get(Calendar.SECOND);
        int currentMilis = currentHour * 60 * 60 * 1000 + currentMinute * 60 * 1000 + currentSecond * 1000;

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        Gson gson = new Gson();
        Calendar prefsCalendar = gson.fromJson(prefs.getString("timerCalendar",null),Calendar.class);
        if(prefsCalendar != null) {
            calendar.set(Calendar.DATE, prefsCalendar.get(Calendar.DATE));
        }

        editor.putString("timerCalendar", new Gson().toJson(calendar) );
        editor.apply();

        return milis - currentMilis;
    }

    public void startTimer(int diff){
        if(diff < 0) {
            diff = 24 * 60 * 60 * 1000 + diff;
            }

        if(diff > 0) {
            if(countdown != null) countdown.cancel();

            String plannedTime = getString(R.string.planned_time_string);
            plannedTime += hour < 10 ? " 0" + hour + ":" : " " + hour + ":";
            plannedTime += minute >= 10 ? minute : minute == 0 ? "00" : "0" + minute;

            plannedTimeText.setText(plannedTime);

            countdown = new CountDownTimer(diff, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int seconds = (int) (millisUntilFinished / 1000) % 60 ;
                    int minutes = (int) ((millisUntilFinished / (1000*60)) % 60);
                    int hours   = (int) ((millisUntilFinished / (1000*60*60)) % 24);

                    String time = "";
                    if(hours > 0)
                        time = hours < 10 ? "0" + hours + ":" : hours + ":";
                    if(minutes > 0)
                        time += minutes < 10 ? "0" + minutes + ":" : minutes + ":";
                    else
                        time += "00:";
                    if(seconds > 0)
                        time += seconds < 10 ? "0" + seconds : seconds;
                    if(seconds == 0)
                        time += "00";

                    timeText.setText(time);
                }

                @Override
                public void onFinish() {
                    deleteTimer();
                }
            };
            countdown.start();
        } else Toast.makeText(getContext(),getString(R.string.invalid_time_string),Toast.LENGTH_LONG);
    }

    private void deleteTimer() {
        editor.putString("timerCalendar",null);
        editor.apply();
    }

    public void sendNotification() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY,hour);
        calendar.set(Calendar.MINUTE, minute);

        Intent i = new Intent(getActivity(), AlarmReceiver.class);

        pendingIntent = PendingIntent.getBroadcast(getActivity(),11111,i,0);
        Toast.makeText(getActivity(),R.string.toast_notification_message, Toast.LENGTH_SHORT).show();
        alarmManager = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
        long updateInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, (calendar.getTimeInMillis() - AlarmManager.INTERVAL_HALF_HOUR)  /1000, updateInterval, pendingIntent);


    }

    public void deleteNotification(){
        alarmManager.cancel(pendingIntent);
    }

    public void updateHoursTo(){
        hour = amPicker.getValue() == 0 ? hourPicker.getValue() : hourPicker.getValue() + 12;
        minute = minutePicker.getValue();

        int milis = hour * 60 * 60 * 1000 + minute * 60 * 1000;
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);
        int currentSecond = calendar.get(Calendar.SECOND);
        int currentMilis = currentHour * 60 * 60 * 1000 + currentMinute * 60 * 1000 + currentSecond * 1000;

        int duration = milis - currentMilis;

        if(duration < 0)
            duration = 24 * 60 * 60 * 1000 + duration;


        int minutes = (int) ((duration / (1000*60)) % 60);
        int hours   = (int) ((duration / (1000*60*60)) % 24);

        hoursToText.setText(getString(R.string.time_left_string,hours,minutes));
    }
}
