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

    btn_insert.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        EPermission.requestPermissions(MainActivity.this,
            new String[]{DangerPermission.READ_CALENDAR, DangerPermission.WRITE_CALENDAR},
            new PermissionCallback() {
              @Override
              public void onPermissionsGranted(List<String> list) {
                CalendarManager.addCalendarEvent(MainActivity.this, "测试日程标题title", "测试地点description", Calendar.getInstance().getTimeInMillis());
              }

              @Override
              public void onPermissionsDenied(List<String> list) {
                Toast.makeText(MainActivity.this, "没有权限" + list.get(0), Toast.LENGTH_LONG).show();
              }
            });
      }
    });

    btn_delete.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {

      }
    });
  }
}
