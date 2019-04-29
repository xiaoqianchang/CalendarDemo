package com.changxiao.calendardemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import com.mobile2345.epermission.DangerPermission;
import com.mobile2345.epermission.EPermission;
import com.mobile2345.epermission.callback.PermissionCallback;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private Button btn_insert;
  private Button btn_delete;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    btn_insert = findViewById(R.id.btn_insert);
    btn_delete = findViewById(R.id.btn_delete);

//    btn_insert.setOnClickListener(new OnClickListener() {
//      @Override
//      public void onClick(View v) {
//        EPermission.requestPermissions(MainActivity.this,
//            new String[]{DangerPermission.READ_CALENDAR, DangerPermission.WRITE_CALENDAR},
//            new PermissionCallback() {
//              @Override
//              public void onPermissionsGranted(List<String> list) {
                CalendarManager.getInstance().addCalendarEvent(MainActivity.this, create());
//              }
//
//              @Override
//              public void onPermissionsDenied(List<String> list) {
//                Toast.makeText(MainActivity.this, "没有权限" + list.get(0), Toast.LENGTH_LONG).show();
//              }
//            });
//      }
//    });

    btn_delete.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

      }
    });
  }

  private CalendarRemindModel create() {
    CalendarRemindModel model = new CalendarRemindModel();
    model.setTitle("测试提醒title");
    model.setDescription("https://www.baidu.com/");
    Calendar calendar = Calendar.getInstance();
    Calendar startCalendar = Calendar.getInstance();
    startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)
        , calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY)
        , calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND) + 30);
    model.setStartDate(startCalendar.getTimeInMillis());
    model.setEndDate(startCalendar.getTimeInMillis());
    model.setLocation("");
    model.setTimeZone("");
    model.setHasAlarm(1);
    model.setAllDay(0);
    model.setEventStatus(1);
    model.setAvailability(1);
    model.setAccessLevel(3);
    return model;
  }
}
