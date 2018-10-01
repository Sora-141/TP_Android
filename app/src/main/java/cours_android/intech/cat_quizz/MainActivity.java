package cours_android.intech.cat_quizz;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.animation.DynamicAnimation;
import android.support.animation.FloatPropertyCompat;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.annotation.RequiresPermission;
import android.support.annotation.FloatRange;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Vibrator;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import pl.droidsonroids.gif.GifDrawable;

public class MainActivity extends AppCompatActivity {

    List<Question> mylist = new ArrayList<Question>();
    String[] answerList;
    Random r = new Random();
    State state = new State();
    int j;
    TextView text;
    TextView score;
    public MediaPlayer playr;
    ObjectMapper maper = new ObjectMapper();

    ImageView catGif;
    ImageView flingCat;
    int gifRes;
    float dX = 0f;
    float dY = 0f;
    float STIFFNESS = SpringForce.STIFFNESS_MEDIUM;
    float DAMPING_RATIO = SpringForce.DAMPING_RATIO_LOW_BOUNCY;
    SpringAnimation xAnimation;
    SpringAnimation yAnimation;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        catGif = findViewById(R.id.cat);
        flingCat = findViewById(R.id.flingCat);

        gifRes = R.raw.cat;
        showGif(100,100, catGif, gifRes);
        gifRes =R.raw.cat_fly_neutral;
        showGif(100, 100, flingCat, gifRes);

        catGif.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                xAnimation = createSpringAnimation(
                        catGif, SpringAnimation.X, catGif.getX(), STIFFNESS, DAMPING_RATIO);
                yAnimation = createSpringAnimation(
                        catGif, SpringAnimation.Y, catGif.getY(), STIFFNESS, DAMPING_RATIO);
                catGif.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        dragCat();

        InputStream is = getResources().openRawResource(R.raw.quiz);

        text = findViewById(R.id.question);
        score = findViewById(R.id.score);

        try {
            ReadJson json = maper.readValue(is,ReadJson.class);
            mylist = json.getQuestions();

            File file = new File(getFilesDir(),"state.json");
            boolean exists = file.exists();
            if(exists){
                state = maper.readValue(new File(getFilesDir(), "state.json"), State.class);
                if(state.getQuestionList().size() == 0){
                    state.setScore(0);
                    state.setQuestionList(mylist);
                    maper.writeValue(new File(getFilesDir(),"state.json"), state);
                }
            }
            else{
                state.setQuestionList(mylist);
                maper.writeValue(new File(getFilesDir(),"state.json"), state);
            }
            showQuestion();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showQuestion(){
        //j = r.nextInt(mylist.size());
        j = r.nextInt(state.getQuestionList().size());
        //answerList = mylist.get(j).getAnswers();
        answerList = state.getQuestionList().get(j).getAnswers();
        //text.setText(mylist.get(j).getQuestion());
        text.setText(state.getQuestionList().get(j).getQuestion());
        score.setText(""+state.getScore());


        int[]  ids = new int[]{R.id.resp1, R.id.resp2, R.id.resp3, R.id.resp4};
        //final int answerBTid = ids[mylist.get(j).getGoodAnswer()];
        final int answerBTid = ids[state.getQuestionList().get(j).getGoodAnswer()];

        for(int i = 0; i < ids.length; i++) {

            Button temp = findViewById(ids[i]);
            temp.setText(answerList[i]);
            temp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    verifyAnswer(answerBTid, v.getId());
                }
            });
        }

        Button btHelp = findViewById(R.id.helping);
        if(state.getFishOptain() >= 1) {
            btHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int goodanswer = state.getQuestionList().get(j).getGoodAnswer()+1;
                    Toast.makeText(v.getContext(), "might be the n° " + goodanswer, Toast.LENGTH_LONG).show();
                    state.setFishOptain(0);
                }
            });
        }
        else{
            btHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(v.getContext(), "I want a Fish Mow", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    private void verifyAnswer(int answerBTid, int curentBTid){
        if(answerBTid == curentBTid) {
            if (j < state.getQuestionList().size() && j >= 0) {
                state.getQuestionList().remove(j);
                if(state.getQuestionList().size() > 0) {
                    state.setScore(state.getScore()+1);
                    if(state.getScore() >= 5){
                        giveFish();
                    }
                    showQuestion();
                }
                else{
                    endGame();
                }
            }
            else {
                endGame();
            }
        }
        else {
            //cat image change
            playSoud(R.raw.catsoud1);
            MakeVibrate(500);
            state.setScore(state.getScore()-1);
        }
        saveState();
    }

    private void giveFish(){
        state.setFishOptain(state.getFishOptain() + 1);
        state.setScore(state.getScore() - 5);
        Toast.makeText(this,"test",Toast.LENGTH_LONG).show();
    }

    public void endGame(){
        setContentView(R.layout.activity_end_quiz);
        ImageView catClap = findViewById(R.id.clappingCat);
        gifRes = R.raw.cat_clap;
        TextView endScore = findViewById(R.id.end_score);
        endScore.setText("score total : "+state.getScore());
        showGif(400,400, catClap, gifRes);
        ImageView cat_dance = findViewById(R.id.cat_dancing);
        gifRes = R.raw.cat_dancing;
        showGif(200,200, cat_dance, gifRes);
        int[]  winSounds = new int[]{R.raw.winsong1, R.raw.winsong2, R.raw.winsong3,};
        int x = r.nextInt(winSounds.length);
        playSoud(winSounds[x]);
    }

    public void MakeVibrate(int mlliseconds) {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(mlliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(mlliseconds);
        }
    }

    //Show the gif on the layout
    //int width ; int height is for the size of the gif
    //imageView img is the must contain the id of the gif on the layout
    //gifRes must contain the ressources
    private void showGif(int width, int height, ImageView img, int gifRes){
        RequestOptions myOptions = new RequestOptions()
                .fitCenter()
                .override(width, height);

        Glide.with(this)
                .load(gifRes)
                .apply(myOptions)
                .into(img);
    }
    public void playSoud(int sound){
        playr = MediaPlayer.create(this,sound);
        playr.start();
        //playr.pause();
        //playr.stop();
    }

    private void saveState(){
        try {
            maper.writeValue(new File(getFilesDir(),"state.json"), state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        saveState();
        if(playr != null) {
            playr.stop();
        }
        super.onDestroy();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void dragCat(){


        catGif.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        flingCat.setVisibility(flingCat.VISIBLE);

                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();

                        flingCat.setX(view.getX() - event.getRawX());
                        flingCat.setY(view.getY() - event.getRawY());

                        xAnimation.cancel();
                        yAnimation.cancel();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        catGif.setVisibility(catGif.INVISIBLE);
                        catGif.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();

                        flingCat.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        break;
                    case MotionEvent.ACTION_UP:
                        catGif.setVisibility(catGif.VISIBLE);
                        flingCat.setVisibility(flingCat.INVISIBLE);
                        yAnimation.start();
                        break;
                }
                return true;
            }
        });
    }

    SpringAnimation createSpringAnimation(View view, DynamicAnimation.ViewProperty property, Float finalPosition, @FloatRange(from = 0.0) Float stiffness, @FloatRange(from = 0.0) Float dampingRatio)
    {
        SpringAnimation animation = new SpringAnimation(view, property);
        SpringForce spring = new SpringForce(finalPosition);
        spring.setStiffness(stiffness);
        spring.setDampingRatio(dampingRatio);
        animation.setSpring(spring);
        return animation;
    }

}
