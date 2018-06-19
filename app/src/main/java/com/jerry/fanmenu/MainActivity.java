package com.jerry.fanmenu;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.jerry.fanmenu.view.FanContainerLinearLayout;
import com.jerry.fanmenu.view.FanMenu;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FanContainerLinearLayout llFanContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initListener();
    }

    private void initListener() {
        llFanContainer.setOnFanSelectedListener(new FanMenu.OnFanSelectedListener() {
            @Override
            public void onFanSelected(int selIndex) {
                if (selIndex >= 0) {
                    Toast.makeText(MainActivity.this, "你选中了第" + (selIndex + 1) + "个菜单", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initView() {
        llFanContainer = (FanContainerLinearLayout) findViewById(R.id.ll_container);
    }
}
