package com.example.todolist.ui.home;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.graphics.Paint;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.todolist.DAO.DateTask;
import com.example.todolist.DAO.Task;
import com.example.todolist.R;
import com.example.todolist.databinding.FragmentHomeBinding;
import com.example.todolist.tools.TomToolkit;
import com.example.todolist.ui.calendar.PopOutTaskDialog;
import com.example.todolist.ui.calendar.TaskListItemView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private LinearLayout layout;

    private int dutyAmount = 0;
    private int dutyFinished = 0;

    private TextView dateText;
    private TextView dutyText;
    private String currentDate;
    private Handler handler;
    private DateTask dateTask;
    private DatabaseReference databaseTable = TomToolkit.getDatabaseTable();
    private ValueEventListener valueEventListener;
    int i = 0;



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);


        View root = binding.getRoot();

        //final TextView textView = binding.textHome;
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        layout = binding.eventLayout;

        dateText = binding.dateText;
        dutyText =  binding.dutyText;

        CalendarView homeCalendar = binding.HomeCalender;
        LinearLayout pulldown = binding.pulldown;


        // initial date and duty status
        Calendar calendar = Calendar.getInstance();
        //获取系统的日期
        //年
        int year = calendar.get(Calendar.YEAR);
        //月
        int month = calendar.get(Calendar.MONTH);
        //日
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        //initialize currentDate
        currentDate = createCurrentDate(day,month,year);

        //initialize datetask
        try {
            dateTask = new DateTask(TomToolkit.getDate(currentDate),TomToolkit.getCurrentUserID());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        removeBindDateTask();
        bindDateTask();
        updateView();
        homeCalendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                //month++;

                //Toast.makeText(getContext(), currentDate, Toast.LENGTH_SHORT).show();

                removeBindDateTask();
                currentDate = createCurrentDate(dayOfMonth,month,year);
                try {
                    dateTask = new DateTask(TomToolkit.getDate(currentDate), TomToolkit.getCurrentUserID());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                bindDateTask();
                updateView();
                //Toast.makeText(getContext(),"You selected :"+date,Toast.LENGTH_SHORT).show();
                //homeCalendar.setVisibility(View.GONE);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.height = 0;
                homeCalendar.setLayoutParams(lp);
                pulldown.setVisibility(View.VISIBLE);

                dateText.setText("Date: "+currentDate);

                layout.removeAllViews();
            }
        });

        handler=new Handler(Looper.getMainLooper()){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Bundle data = msg.getData();
                char actionTag = data.getChar("actionTag");
                if(actionTag=='a'){
                    com.example.todolist.DAO.Task task = (Task)data.getSerializable("task");
//                    Toast.makeText(getContext(),"handler report task: "+task.toString(),Toast.LENGTH_LONG).show();
//                    Toast.makeText(getContext(), dateTask==null?"true":"false", Toast.LENGTH_SHORT).show();
                    dateTask.addTask(task);
                }else if(actionTag=='d'){
                    Toast.makeText(getContext(),Boolean.toString(dateTask.deleteTask(data.getString("ID"))), Toast.LENGTH_SHORT).show();
                }else if(actionTag=='u'){
                    Task task = (Task)data.getSerializable("task");
//                    Toast.makeText(getContext(),"handler report task: "+task.toString(),Toast.LENGTH_LONG).show();
                    dateTask.downTask(task);
                }
//                Toast.makeText(getContext(), dateTask.toString(), Toast.LENGTH_SHORT).show();
                saveData();
                updateView();
            }
        };



        FloatingActionButton fab =  binding.fab;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getContext(),"clicked fab", Toast.LENGTH_SHORT).show();
                PopOutTaskDialog popOutTaskDialog = new PopOutTaskDialog(currentDate,handler);
                popOutTaskDialog.show(getParentFragmentManager(),"Task Editor Dialog");
//                addView(root);
            }
        });



        pulldown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                homeCalendar.setLayoutParams(lp);
                pulldown.setVisibility(View.GONE);
            }
        });


        dateText.setText("Date: "+currentDate);
        setDutyText();
        return root;
    }

    private void updateView() {
        if(dateTask!=null && dateTask.getTasks()!=null){
            if(binding!=null && binding.eventLayout!=null){
                binding.eventLayout.removeAllViews();
                for (Map.Entry<String,Task> entry: dateTask.getTasks().entrySet()) {
                    TaskListItemView taskListItemView = new TaskListItemView(getContext(),entry.getValue(),currentDate,handler,this);
                    binding.eventLayout.addView(taskListItemView);
                }
            }
            setDutyText();
        }else{
            this.dutyAmount = 0;
            this.dutyFinished = 0;
            setDutyText();
        }
    }

    private String createCurrentDate(int day, int month, int year){
        month++;
        return String.format("%02d-%02d-%04d",day,month,year);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // 未启用
//    public void addView(View view) {
//        TextView child = new TextView(getContext());
//        child.setTextSize(20);
//        // 获取当前的时间并转换为时间戳格式,
//        i++;
//        String currentTime = "event " + i;
//        child.setText(currentTime);
//        // 调用一个参数的addView方法
//        layout.addView(child);
//        i++;
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        layout.addView(createEventView(),lp);
//        dutyAmount++;
//        setDutyText();
//
//        // 添加分割线
//        TextView splitLine = new TextView(getContext());
//        splitLine.setWidth(layout.getWidth());
//        splitLine.setHeight(1);
//        splitLine.setBackgroundColor(Color.parseColor("#666666"));
//        layout.addView(splitLine);
//    }

    // 未启用
//    private LinearLayout createEventView(){
//        LinearLayout.LayoutParams lp;
//
//        // 信息中的标题
//        TextView title = new TextView(getContext());
//
//        // 事件主体
//        LinearLayout event =  new LinearLayout(getContext());
//        event.setOrientation(LinearLayout.HORIZONTAL);
//        event.setBackgroundColor(Color.parseColor("#00ffff"));
//
//        // 复选框
//        CheckBox checkBox = new CheckBox(getContext());
//        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked){
//                    // 选中（已完成），背景为灰色
//                    event.setBackgroundColor(Color.parseColor("#cccccc"));
//                    title.setTextColor(Color.parseColor("#999999"));
//                    // 添加删除线
//                    title.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);  //中划线，会有锯齿
//                    title.getPaint().setAntiAlias(true);
//
//                    dutyFinished++;
//                } else {
//                    // 没选中（未完成），背景为蓝色
//                    event.setBackgroundColor(Color.parseColor("#00ffff"));
//                    title.setTextColor(Color.parseColor("#000000"));
//                    // 清除删除线
//                    title.getPaint().setFlags(0);
//                    title.invalidate();
//
//                    dutyFinished--;
//                }
//                // 重新设置字体加粗
//                title.getPaint().setFakeBoldText(true);
//
//                setDutyText();
//            }
//        });
//        lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
//        lp.weight=1;
//        lp.leftMargin = 20;
//        event.addView(checkBox,lp);
//
//        // 事件信息layout
//        LinearLayout info = new LinearLayout(getContext());
//        info.setOrientation(LinearLayout.VERTICAL);
//
//        // 事件标题
//        title.setText("title"+i);
//        title.setTextColor(Color.parseColor("#000000"));
//        title.getPaint().setFakeBoldText(true);
//
//        lp = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        lp.topMargin=20;
//        info.addView(title,lp);
//
//        // 事件简介
//        TextView msg = new TextView(getContext());
//        msg.setText("This is msg");
//        lp = new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        lp.topMargin = 10;
//        lp.bottomMargin=20;
//        info.addView(msg,lp);
//
//        lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
//        lp.weight=6;
//        event.addView(info,lp);
//        event.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getContext(),"clicked "+title.getText(), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        ImageView timer =  new ImageView(getContext());
//        timer.setImageResource(R.drawable.ic_timer);
//        lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
//        lp.gravity= Gravity.CENTER;
//        lp.rightMargin=20;
//        lp.weight=1;
//
//        timer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Toast.makeText(getContext(), "You cliked timer icon", Toast.LENGTH_SHORT).show();
//            }
//        });
//        event.addView(timer,lp);
//
//        return event;
//    }

    private void setDutyText(){
//        int finished = 0;
//        HashMap<String,Task> tasks = dateTask.getTasks();
//        for (String key : tasks.keySet()){
//            if (tasks.get(key).isTaskDown()){
//                finished++;
//            }
//        }
//        dutyFinished = finished;
        dutyText.setText(getDutyInfo());
    }

    private String getDutyInfo(){
        dutyFinished = dateTask.getFinishedTaskNum();
        dutyAmount = dateTask.getAllTaskNum();
        return dutyFinished==dutyAmount?
                "All Finished, cheers!":"Finished Duties: "+dutyFinished+"/"+dutyAmount;
    }
    private void bindDateTask(){
        ValueEventListener valueEventListener = new ValueEventListener() {
            /**
             * when the data in database changed, read the data into dateTask and update view
             * @param snapshot
             */
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Object value = snapshot.getValue();
                if (value != null) {
                    Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create();
                    dateTask = gson.fromJson(value.toString(), DateTask.class);
                    if (dateTask == null) {
                        Toast.makeText(getContext(), "read data == null", Toast.LENGTH_SHORT).show();
                    }
                    updateView();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "loadPost:onCancelled", error.toException());
            }
        };

        databaseTable.child(currentDate).addValueEventListener(valueEventListener);

    }
//    private void bindDateTask(){
//        ValueEventListener valueEventListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Object value = snapshot.getValue();
//                if (value != null) {
//                    Gson gson = new GsonBuilder().setDateFormat("MMM dd, yyyy HH:mm:ss").create();
//                    dateTask = gson.fromJson(value.toString(), DateTask.class);
//                    if (dateTask == null || dateTask.getTasks() == null) {
//                        Toast.makeText(getContext(), "read data == null", Toast.LENGTH_SHORT).show();
////                        saveData();
//                    }
////                    Toast.makeText(getContext(), dateTask.toString(), Toast.LENGTH_SHORT).show();
//                    updateView();
//                }
//
//
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.w(TAG, "loadPost:onCancelled", error.toException());
//            }
//        };
//        databaseTable.child(currentDate).addValueEventListener(valueEventListener);
//    }

    private void removeBindDateTask(){
        if(valueEventListener!=null && currentDate!=null){
            databaseTable.child(currentDate).removeEventListener(valueEventListener);
        }
    }

    private void saveData(){
        TomToolkit.saveToFireBase(currentDate,dateTask);
    }

}