package kagura.project.com.a8.memory;


import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.GridView;

import com.plattysoft.leonids.ParticleSystem;

import java.util.Calendar;
import java.util.List;

import java.util.Timer;
import java.util.TimerTask;

import kagura.project.com.a8.Association;
import kagura.project.com.a8.AssociationPicturale;
import kagura.project.com.a8.ImageAdapter;
import kagura.project.com.a8.R;
import kagura.project.com.a8.database.ResultDAO;
import kagura.project.com.a8.objects.Card;
import kagura.project.com.a8.objects.Result;

public class MemoryGame extends AppCompatActivity {

    private static int level;

    private boolean isClickable = true;

    private int finish;
    private int tries = 0;

    private String name;
    private UpdateCardsHandler handler;
    private Card firstCard, secondCard;
    private AnimatorSet setRightOutFirst, setRightInFirst, setRightOutSecond, setRightInSecond;
    private static final Object lock = new Object();

    GridView gridviewFront, gridviewBack;
    Fragment fragmentResult;

    Boolean isTimerStarted = false;
    Chronometer timer;
    Calendar calendar;

    FragmentManager fragmentManager;
    Association association;


    public MemoryGame() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setRightOutFirst = (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.card_flip_right_out);

        setRightInFirst = (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.card_flip_right_in);

        setRightOutSecond = (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.card_flip_right_out);

        setRightInSecond = (AnimatorSet) AnimatorInflater.loadAnimator(getApplicationContext(),
                R.animator.card_flip_right_in);

        handler = new UpdateCardsHandler();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        name = preferences.getString(getString(R.string.name), null);


        setContentView(R.layout.activity_memory_game);

        gridviewFront = (GridView) findViewById(R.id.gridviewFront);
        gridviewBack = (GridView) findViewById(R.id.gridviewBack);

        level = getIntent().getIntExtra("level", 0);
        Log.i("level", Integer.toString(level));

        newGame();



        gridviewFront.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                if(!isTimerStarted){
                    timer = new Chronometer(getApplicationContext());
                    timer.start();
                    isTimerStarted = true;
                }

                if(isClickable){
                    synchronized (lock) {

                        if(firstCard == null){
                            setRightOutFirst.setTarget(v);
                            setRightInFirst.setTarget(gridviewBack.getChildAt(position));
                            setRightOutFirst.start();
                            setRightInFirst.start();
                        }else{

                            if(firstCard.position == position){
                                return; //the user pressed the same card
                            }

                            setRightOutSecond.setTarget(v);
                            setRightInSecond.setTarget(gridviewBack.getChildAt(position));
                            setRightOutSecond.start();
                            setRightInSecond.start();
                        }




                        Log.i("position", Integer.toString(position));
                        turnCard(v,position);
                    }
                }

            }
        });
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("Quitter")
                .setMessage("Êtes vous sûrs de vouloir quitter ce jeu ?")
                .setIcon(android.R.drawable.ic_menu_help)
                .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Non", null).show();

    }

    private void newGame() {

        association = new AssociationPicturale(this);

        // On initialise un tableau d'entiers contenant en position 0 le nombre de colonnes que fera la gridview, et en position 1 le nombre de cartes dans le jeu
        int levelParams[] = association.setLevelParams(level);
        gridviewBack.setNumColumns(levelParams[0]);
        gridviewFront.setNumColumns(levelParams[0]);
        finish = levelParams[1] / 2;


        List<Integer[]> idDrawablesFrontAndBack = association.loadCards();

        // à la position 0 sont placés tous les dos de cartes, à la 1, les différents légumes chargés
        gridviewFront.setAdapter(new ImageAdapter(this, idDrawablesFrontAndBack.get(0)));
        gridviewBack.setAdapter(new ImageAdapter(this, idDrawablesFrontAndBack.get(1)));


    }

    private void turnCard(View v, int position) {



        if(firstCard==null){
            firstCard = new Card(v, gridviewBack.getChildAt(position), position);
        }
        else{

            isClickable = false;

            secondCard = new Card(v, gridviewBack.getChildAt(position), position);


            TimerTask tt = new TimerTask() {

                @Override
                public void run() {
                    try{
                        synchronized (lock) {
                            handler.sendEmptyMessage(0);
                        }
                    }
                    catch (Exception e) {
                        Log.e("E1", e.getMessage());
                    }
                }
            };

            Timer t = new Timer(false);
            t.schedule(tt, 1300);

        }


    }

    @SuppressLint("HandlerLeak")
    class UpdateCardsHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            synchronized (lock) {
                checkCards();
            }
        }
        void checkCards(){
            tries++;

            if(association.getImagePositions().get(firstCard.position).equals(association.getImagePositions().get(secondCard.position))){
                Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
                new ParticleSystem(MemoryGame.this, 1000, getResources().getIdentifier("star", "drawable", getPackageName()), 1000)
                        .setSpeedRange(0.2f, 0.5f)
                        .oneShot(firstCard.viewBack, 100);
                firstCard.viewBack.startAnimation(animFadeOut);
                firstCard.viewFront.setVisibility(View.INVISIBLE);
                firstCard.viewBack.setVisibility(View.INVISIBLE);
                new ParticleSystem(MemoryGame.this, 1000, getResources().getIdentifier("star_pink", "drawable", getPackageName()), 1000)
                        .setSpeedRange(0.2f, 0.5f)
                        .oneShot(secondCard.viewBack, 100);
                secondCard.viewBack.startAnimation(animFadeOut);
                secondCard.viewFront.setVisibility(View.INVISIBLE);
                secondCard.viewBack.setVisibility(View.INVISIBLE);

                finish--;
                isClickable = true;
            }else{
                setRightOutFirst.setTarget(firstCard.viewBack);
                setRightInFirst.setTarget(firstCard.viewFront);

                setRightOutSecond.setTarget(secondCard.viewBack);
                setRightInSecond.setTarget(secondCard.viewFront);


                setRightOutFirst.start();
                setRightInFirst.start();
                setRightOutSecond.start();
                setRightInSecond.start();

                final Handler handlerFlip = new Handler();
                handlerFlip.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isClickable = true;
                    }
                }, 1000);
            }

            firstCard = null;
            secondCard = null;

            if(finish == 0){
                timer.stop();
                result();
            }
        }
    }

    private void result(){

        calendar = Calendar.getInstance();
        String monthString;
        int calendarMonth = calendar.get(Calendar.MONTH) + 1;
        if(calendarMonth < 10){
            monthString = "0" + Integer.toString(calendarMonth);
        }else{
            monthString = Integer.toString(calendarMonth);
        }
        String date = calendar.get(Calendar.DAY_OF_MONTH) + "/" + monthString + "/" + calendar.get(Calendar.YEAR);
        long time = (SystemClock.elapsedRealtime() - timer.getBase()) / 1000;
        Log.i("nb coups", Integer.toString(tries));
        Log.i("timer : ", Long.toString(time));

        Result result = new Result();
        result.setName(name);
        result.setGame("Memory");
        result.setLevel(level);
        result.setTries(tries);
        result.setTime(time);
        result.setDate(date);

        ResultDAO resultDAO = new ResultDAO(this);
        resultDAO.add(result);


        Bundle bundle = new Bundle();
        bundle.putInt("level", level);
        fragmentResult = new MemoryEndFragment();
        fragmentResult.setArguments(bundle);
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.push_up_in, R.anim.push_up_out)
                .replace(R.id.fragment_container, fragmentResult).commit();


    }

    public void replayLevel(View view) {

        fragmentManager.beginTransaction().remove(fragmentResult).commit();
        newGame();

    }

    public void nextLevel(View view) {

        fragmentManager.beginTransaction().remove(fragmentResult).commit();
        isTimerStarted = false;
        tries = 0;
        level++;
        newGame();

    }

    public void goHome(View view) {

        Intent intentHomeMenu = new Intent();
        setResult(66, intentHomeMenu);
        finish();
    }

    public void goLevelMenu(View view) {
        finish();
    }
}
