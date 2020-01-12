package com.github.ppartisan.simplealarms.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.github.ppartisan.simplealarms.R;
import com.github.ppartisan.simplealarms.data.DatabaseHelper;
import com.github.ppartisan.simplealarms.model.Alarm;
import com.github.ppartisan.simplealarms.service.AlarmReceiver;
import com.github.ppartisan.simplealarms.service.LoadAlarmsService;
import com.github.ppartisan.simplealarms.util.ViewUtils;

import java.util.Calendar;
import java.util.Random;

public final class AddEditAlarmFragment extends Fragment {

    private TimePicker mTimePicker;
    private TimePicker mTimePicker2;
    private EditText mLabel;
    private CheckBox mMon, mTues, mWed, mThurs, mFri, mSat, mSun;

    public int getID(int start_time, int end_time) {return (int)(Math.random() * (7*(start_time+123) + 3*(end_time+456)));}
    public int getTimeCode(int h, int m){return h*60 + m;}

    public static AddEditAlarmFragment newInstance(Alarm alarm) {

        Bundle args = new Bundle();
        args.putParcelable(AddEditAlarmActivity.ALARM_EXTRA, alarm);

        AddEditAlarmFragment fragment = new AddEditAlarmFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.fragment_add_edit_alarm, container, false);

        setHasOptionsMenu(true);

        final Alarm alarm = getAlarm();

        mTimePicker = (TimePicker) v.findViewById(R.id.edit_alarm_time_picker);
        mTimePicker2 = (TimePicker) v.findViewById(R.id.edit_alarm_time_picker2);
        ViewUtils.setTimePickerTime(mTimePicker, alarm.getTime());

        mLabel = (EditText) v.findViewById(R.id.edit_alarm_label);
        mLabel.setText(alarm.getLabel());

        mMon = (CheckBox) v.findViewById(R.id.edit_alarm_mon);
        mTues = (CheckBox) v.findViewById(R.id.edit_alarm_tues);
        mWed = (CheckBox) v.findViewById(R.id.edit_alarm_wed);
        mThurs = (CheckBox) v.findViewById(R.id.edit_alarm_thurs);
        mFri = (CheckBox) v.findViewById(R.id.edit_alarm_fri);
        mSat = (CheckBox) v.findViewById(R.id.edit_alarm_sat);
        mSun = (CheckBox) v.findViewById(R.id.edit_alarm_sun);

        setDayCheckboxes(alarm);

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_alarm_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                save();
                break;
            case R.id.action_delete:
                delete();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private Alarm getAlarm() {
        return getArguments().getParcelable(AddEditAlarmActivity.ALARM_EXTRA);
    }

    private void setDayCheckboxes(Alarm alarm) {
        mMon.setChecked(alarm.getDay(Alarm.MON));
        mTues.setChecked(alarm.getDay(Alarm.TUES));
        mWed.setChecked(alarm.getDay(Alarm.WED));
        mThurs.setChecked(alarm.getDay(Alarm.THURS));
        mFri.setChecked(alarm.getDay(Alarm.FRI));
        mSat.setChecked(alarm.getDay(Alarm.SAT));
        mSun.setChecked(alarm.getDay(Alarm.SUN));
    }

    private void save() {

        final Alarm alarm = getAlarm();

        final Calendar time = Calendar.getInstance();

        int startH = ViewUtils.getTimePickerHour(mTimePicker);
        int startM = ViewUtils.getTimePickerMinute(mTimePicker);

        int endH = ViewUtils.getTimePickerHour(mTimePicker2);
        int endM = ViewUtils.getTimePickerMinute(mTimePicker2);

        int startTimeCode = getTimeCode(startH, startM);
        int endTimeCode = getTimeCode(endH,endM);

        if(endTimeCode < startTimeCode) endTimeCode += 1440; //add a day if the endTime < startTime

        Random rand = new Random();
        int rnd = rand.nextInt(endTimeCode-startTimeCode+1) + startTimeCode;


        //int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
        int randomH = rnd/60;
        int randomM = rnd%60;

        if(randomH > 23) randomH-=24; //in case when the hours was > 24, subtract a day

        //time.set(Calendar.MINUTE, ViewUtils.getTimePickerMinute(mTimePicker));
        //time.set(Calendar.HOUR_OF_DAY, ViewUtils.getTimePickerHour(mTimePicker));

        time.set(Calendar.MINUTE, randomM);
        time.set(Calendar.HOUR_OF_DAY, randomH);
        alarm.setTime(time.getTimeInMillis());

        //alarm.setLabel(mLabel.getText().toString());
        alarm.setLabel("Hour: " + randomH + " Minute: " + randomM);

        alarm.setDay(Alarm.MON, mMon.isChecked());
        alarm.setDay(Alarm.TUES, mTues.isChecked());
        alarm.setDay(Alarm.WED, mWed.isChecked());
        alarm.setDay(Alarm.THURS, mThurs.isChecked());
        alarm.setDay(Alarm.FRI, mFri.isChecked());
        alarm.setDay(Alarm.SAT, mSat.isChecked());
        alarm.setDay(Alarm.SUN, mSun.isChecked());

        final int rowsUpdated = DatabaseHelper.getInstance(getContext()).updateAlarm(alarm);
        final int messageId = (rowsUpdated == 1) ? R.string.update_complete : R.string.update_failed;

        Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();

        AlarmReceiver.setReminderAlarm(getContext(), alarm);

        getActivity().finish();

    }

    private void delete() {

        final Alarm alarm = getAlarm();

        final AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext(), R.style.DeleteAlarmDialogTheme);
        builder.setTitle(R.string.delete_dialog_title);
        builder.setMessage(R.string.delete_dialog_content);
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //Cancel any pending notifications for this alarm
                AlarmReceiver.cancelReminderAlarm(getContext(), alarm);

                final int rowsDeleted = DatabaseHelper.getInstance(getContext()).deleteAlarm(alarm);
                int messageId;
                if(rowsDeleted == 1) {
                    messageId = R.string.delete_complete;
                    Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();
                    LoadAlarmsService.launchLoadAlarmsService(getContext());
                    getActivity().finish();
                } else {
                    messageId = R.string.delete_failed;
                    Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.no, null);
        builder.show();

    }

}
