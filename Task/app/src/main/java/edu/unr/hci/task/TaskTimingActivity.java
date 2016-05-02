/**
 * Get some sample tasks going
 * Thinking that a practice task is a good idea
 * Maybe it could indicate where the relevant bookmark in the task environment is
 * Determine how to go about timing everything
 * Thinking that just timing from start-up/task finishes is fine to help evaluate navigability
 * Add introduction screen (might use that intro/tutorial library for this)
 * Add survey screen (might use that one library for this)
 * JSON schema:
 * {
 * quantitative: {timings}
 * qualitative: {stuff...}
 * }
 * <p/>
 * Later:
 * Confirm when the task is ready to be completed/given up on with a popup or something?
 * Alter the layout to confirm when the user is ready to start the task?
 */

package edu.unr.hci.task;

import android.content.Intent;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class TaskTimingActivity extends AppCompatActivity {
    public static final String TAG = TaskTimingActivity.class.toString();


    public static final String FILE_IO_TAG = "FILE_IO";

    public static final String PARTICIPANT_EXTRA = "PARTICIPANT_EXTRA";

    public static final long GIVE_UP_TIME = 1000 * 60 * 3; // milliseconds

    public static final String DATA_DIR = "HCI-Task-Results";

    public static final float DISABLED_ALPHA = 0.4f;

    public static final int N_TRAINING_TASKS = 3;
    public static final int N_FIRST_ROUND_TASKS = 3;
    public static final int N_SECOND_ROUND_TASKS = 2;

    Intent mActivatingIntent;

    private ViewPager mTaskViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private Button mTaskFinishedButton;
    private Button mTaskGiveUpButton;

    Integer mTaskRound;
    String mInterfaceVersion;
    Integer mNTasks;
    ArrayList<String> mTaskDescriptionTexts;
    ArrayList<String> mTaskImages;
    private int mCurrentTask;
    private long mTaskStartTime;
    private SessionResult mSessionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_timing);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.mActivatingIntent = this.getIntent();

        this.mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        this.setupTaskViewPager(this.mSectionsPagerAdapter);
        this.setupFinishedButton();
        this.setupGiveUpButton();

        String participantId = this.mActivatingIntent.getStringExtra(PARTICIPANT_EXTRA);
        this.mInterfaceVersion = this.mActivatingIntent.getStringExtra(IntentKeys.INTERFACE_VERSION_EXTRA);
        this.mTaskRound = this.mActivatingIntent.getIntExtra(IntentKeys.TASK_ROUND_EXTRA, 0);
        this.mCurrentTask = 0;
        this.setupTaskParameters(this.mTaskRound, this.mInterfaceVersion);

        this.setupNewTask(this.mCurrentTask);
        this.mSessionResult = new SessionResult(participantId);
        this.setupNewTask(this.mCurrentTask);
    }

    public void setupTaskViewPager(SectionsPagerAdapter pagerAdapter) {
        // Set up the ViewPager with the sections adapter.
        this.mTaskViewPager = (ViewPager) findViewById(R.id.view_task_display);
        if (mTaskViewPager != null) {
            this.mTaskViewPager.setAdapter(pagerAdapter);
        }
    }

    public void setupFinishedButton() {
        this.mTaskFinishedButton = (Button) findViewById(R.id.button_task_complete);
        this.mTaskFinishedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Task Complete.");
                completeTask(true);
            }
        });
    }

    public void setupGiveUpButton() {
        this.mTaskGiveUpButton = (Button) findViewById(R.id.button_task_give_up);
        this.mTaskGiveUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeTask(false);
            }
        });
    }

    public void setupTaskParameters(int round, String interfaceVersion) {
        switch (round) {
            case 0:
                this.mTaskDescriptionTexts = TRAINING_TASK_TEXTS;
                this.mNTasks = N_TRAINING_TASKS;
                break;
            case 1:
                this.mTaskDescriptionTexts = TASK_SET_1_TEXTS;
                this.mNTasks = N_FIRST_ROUND_TASKS;
                break;
            case 2:
                this.mTaskDescriptionTexts = TASK_SET_2_TEXTS;
                this.mNTasks = N_SECOND_ROUND_TASKS;
                break;
            default:
                throw new IllegalStateException("This app only uses 3 rounds.");
        }
    }

    public void completeTask(boolean taskFinished) {
        this.storeTaskResult(taskFinished);

        this.mCurrentTask++;

        if (this.mCurrentTask < this.mNTasks) {
            this.setupNewTask(this.mCurrentTask);
        } else {
            this.proceedToNextTransition();
        }
    }

    public void storeTaskResult(boolean taskFinished) {
        long taskDuration = SystemClock.elapsedRealtime() - this.mTaskStartTime;

        SessionResult.CompletionStatus status =
                taskFinished ? SessionResult.CompletionStatus.COMPLETE :
                        SessionResult.CompletionStatus.GIVE_UP;

        this.mSessionResult.addTaskResult(taskDuration, status);
    }

    public void setupNewTask(int taskNumber) {
        String taskDescription = this.mTaskDescriptionTexts.get(taskNumber);
        String screenshotName = taskScreenshots.get(taskNumber);
        this.mSectionsPagerAdapter.setupNewTask(taskDescription, screenshotName);

        this.mTaskGiveUpButton.setAlpha(DISABLED_ALPHA);
        this.mTaskGiveUpButton.setEnabled(false);
        this.mTaskGiveUpButton.postDelayed(new Runnable() {
                                               @Override
                                               public void run() {
                                                   mTaskGiveUpButton.setEnabled(true);
                                                   mTaskGiveUpButton.setAlpha(1.0f);
                                               }
                                           },
                GIVE_UP_TIME);

        this.mTaskFinishedButton.setEnabled(false);
        this.mTaskFinishedButton.postDelayed(new Runnable() {
                                               @Override
                                               public void run() {
                                                   mTaskFinishedButton.setEnabled(true);
                                                   mTaskFinishedButton.setAlpha(1.0f);
                                               }
                                           },
                1000);

        this.mTaskStartTime = SystemClock.elapsedRealtime();
    }

    public void saveTaskData() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), DATA_DIR);

            try {
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File file = new File(directory, String.format(Locale.US, "%s.json", this.mSessionResult.participant));
                if (!file.exists()) {
                    file.createNewFile();
                }

                BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                bw.write(this.mSessionResult.toJsonString());
                bw.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        } else {
            Log.wtf(FILE_IO_TAG, "No external storage is mounted - unable to write data to file");
        }
    }

    public void proceedToNextTransition() {
        saveTaskData();

        this.mTaskRound++;

        Intent intent;
        if (this.mTaskRound < 3) {
            intent = new Intent(this, TransitionActivity.class);
            intent.putExtras(this.mActivatingIntent);
            intent.putExtra(TransitionActivity.TRANSITION_MESSAGE_EXTRA, "Ok, get ready for the real thing!");
            intent.putExtra(IntentKeys.TASK_ROUND_EXTRA, this.mTaskRound);
        } else {
            intent = new Intent(this, ThankYouActivity.class);
        }

        this.finish();
        startActivity(intent);
    }


    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public static final int N_TABS = 2; // only the description page and the screenshot page

        FragmentManager mFragmentManager;
        TaskTextFragment mTaskDisplayFragment;
        TaskExampleFragment mTaskExampleFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            this.mFragmentManager = fm;

            this.mTaskDisplayFragment = new TaskTextFragment();
//            Bundle bundle = new Bundle();
//            bundle.putString(TaskTextFragment.TASK_DESCRIPTION, "hello");
//            this.mTaskDisplayFragment.setArguments(bundle);

            this.mTaskExampleFragment = new TaskExampleFragment();
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return this.mTaskDisplayFragment;
            } else /*if (position == 1)*/ {
                return this.mTaskExampleFragment;
            }
        }

        @Override
        public int getItemPosition(Object object) {
            if (object != null) {
                ((IUpdateableFragment) object).update();
            }

            return super.getItemPosition(object);
        }

        @Override
        public int getCount() {
            return N_TABS;
        }

        public void setupNewTask(String description, String screenshot) {

            this.mTaskDisplayFragment.setDescription(description);
            this.mTaskExampleFragment.setScreenshotId(R.drawable.wilfred);

            this.notifyDataSetChanged();
        }
    }


    public static final ArrayList<String> TRAINING_TASK_TEXTS = new ArrayList<>(Arrays.asList(
            "Your first training task will be to press the green \"Finished!\" button. This is the button you should press when you have completed a task.",
            "Your next task will be to try out swiping between a screenshot and an image. Go on, swipe this text to the left, and swipe right to bring it back. Click the green button when you are done.",
            "Finally, there is a red button that is currently disabled. You can either wait 3 minutes for it to become available, or just remember that it's there if things take too long. Click either button to move on."
    ));

    public static final ArrayList<String> TASK_SET_1_TEXTS = new ArrayList<>(Arrays.asList(
            "Please perform task 1",
            "Please perform task 2",
            "Please perform task 3"
    ));

    public static final ArrayList<String> TASK_SET_2_TEXTS = new ArrayList<>(Arrays.asList(
            "Please perform task 1",
            "Please perform task 2"
    ));

    public ArrayList<String> taskDescriptions = new ArrayList<>(Arrays.asList(
            "Use the Geospatial Data Search to find the mean tree radial growth in the Snake Range East Sagebrush location.",
            "Vinh help me come up with tasks!",
            "There's darkness everywhere Ryan. You just can't see it because the sun is such an attention whore."
    ));
    public ArrayList<String> taskScreenshots = new ArrayList<>(Arrays.asList(
            "@drawable/geospatial_task",
            "@drawable/web_image_archive_task",
            "@drawable/wilfred"
    ));
}
