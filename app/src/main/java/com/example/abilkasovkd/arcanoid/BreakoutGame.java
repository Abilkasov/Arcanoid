package com.example.abilkasovkd.arcanoid;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class BreakoutGame extends Activity {

    BreakoutView breakoutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        breakoutView = new BreakoutView(this);
        setContentView(breakoutView);

    }

    class BreakoutView extends SurfaceView implements Runnable {

        // поток
        Thread gameThread = null;

        SurfaceHolder ourHolder;

        volatile boolean playing;

        // Game is paused at the start
        boolean paused = true;

        // A Canvas and a Paint object
        Canvas canvas;
        Paint paint;

        long fps;

        // Поможет рассчитать fps игры
        private long timeThisFrame;

        int screenX;
        int screenY;

        Paddle paddle;

        Ball ball;

        // Up to 200 bricks
        Brick[] bricks = new Brick[200];
        int numBricks = 0;

        /*// For sound FX
        SoundPool soundPool;
        int beep1ID = -1;
        int beep2ID = -1;
        int beep3ID = -1;
        int loseLifeID = -1;
        int explodeID = -1;
        */


        int score = 0;

        int lives = 3;

        public BreakoutView(Context context) {

            super(context);

            // Initialize ourHolder and paint objects
            ourHolder = getHolder();
            paint = new Paint();

            // Получить объект дисплея для доступа к информации на экране
            Display display = getWindowManager().getDefaultDisplay();
            // Загрузите разрешение в объект точки
            Point size = new Point();
            display.getSize(size);

            screenX = size.x;
            screenY = size.y;

            paddle = new Paddle(screenX, screenY);

            // Создаем мяч
            ball = new Ball(screenX, screenY);

            // Load the sounds

            // This SoundPool is deprecated but don't worry
            /*soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

            try {
                // Create objects of the 2 required classes
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                // Load our fx in memory ready for use
                descriptor = assetManager.openFd("beep1.ogg");
                beep1ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep2.ogg");
                beep2ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep3.ogg");
                beep3ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("loseLife.ogg");
                loseLifeID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("explode.ogg");
                explodeID = soundPool.load(descriptor, 0);

            } catch (IOException e) {
                // Print an error message to the console
                Log.e("error", "failed to load sound files");
            }
            */

            createBricksAndRestart();

        }

        public void createBricksAndRestart() {

            // Стартовая позиция
            ball.reset(screenX, screenY);

            int brickWidth = screenX / 8;
            int brickHeight = screenY / 10;

            // Построение кирпичей
            numBricks = 0;
            for (int column = 0; column < 8; column++) {
                for (int row = 0; row < 3; row++) {
                    bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                    numBricks++;
                }
            }

            if (lives == 0) {
                score = 0;
                lives = 3;
            }
        }

        @Override
        public void run() {
            while (playing) {
                // Захват текущее время в миллисекундах в начальный кадр времени
                long startFrameTime = System.currentTimeMillis();
                // Обновление кадра
                if (!paused) {
                    update();
                }
                // Прорисовка рамки
                draw();
                // Рассчитать количество кадров в секунду этот кадр
                // затем мы можем использовать результат
                // время анимации и многое другое.
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }

            }

        }

        // Все, что нужно обновить идет здесь
        // Движение, обнаружение столкновений и т.д.
        public void update() {

            // Move the paddle if required
            paddle.update(fps);

            ball.update(fps);

            // Проверяем мяч когда он сталкивается с кирпичом
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                       // soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }
            }
            // Проверяем мяч когда он сталкивается с веслом
            if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
                //soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }
            // Возврат мяча назад, когда он попадает в нижнюю часть экрана
            if (ball.getRect().bottom > screenY) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - 2);


                lives--;
                //soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

                if (lives == 0) {
                    paused = true;
                    createBricksAndRestart();
                }
            }

            // Возврат мяч назад, когда он попадает в верхнюю часть экрана
            if (ball.getRect().top < 0)

            {
                ball.reverseYVelocity();
                ball.clearObstacleY(12);

                //soundPool.play(beep2ID, 1, 1, 0, 0, 1);
            }

            // Если мяч отскакивает от левой стенки
            if (ball.getRect().left < 0)

            {
                ball.reverseXVelocity();
                ball.clearObstacleX(2);
                //soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // Если мяч отскакивает от правой стенки
            if (ball.getRect().right > screenX - 10) {

                ball.reverseXVelocity();
                ball.clearObstacleX(screenX - 22);

                //soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // Пауза когда очищается экран
            if (score == numBricks * 10)

            {
                paused = true;
                createBricksAndRestart();
            }

        }

        // Draw the newly updated scene
        public void draw() {

            // Make sure our drawing surface is valid or we crash
            if (ourHolder.getSurface().isValid()) {
                // Lock the canvas ready to draw
                canvas = ourHolder.lockCanvas();

                // Цвет фона
                canvas.drawColor(Color.argb(255, 245, 245, 245));

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 41, 49, 51));

                // Draw the paddle
                canvas.drawRect(paddle.getRect(), paint);

                // Draw the ball
                canvas.drawRect(ball.getRect(), paint);

                // Change the brush color for drawing
                paint.setColor(Color.argb(255, 227, 38, 54));

                // Draw the bricks if visible
                for (int i = 0; i < numBricks; i++) {
                    if (bricks[i].getVisibility()) {
                        canvas.drawRect(bricks[i].getRect(), paint);
                    }
                }

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 0, 255, 0));

                // Draw the score
                paint.setTextSize(40);
                canvas.drawText("Score: " + score + "   Lives: " + lives, 10, 50, paint);

                // Has the player cleared the screen?
                if (score == numBricks * 10) {
                    paint.setTextSize(45);
                    canvas.drawText("Вы выиграли!", 10, screenY / 2, paint);
                }

                // Has the player lost?
                if (lives <= 0) {
                    paint.setTextSize(45);
                    canvas.drawText("Вы проиграли!", 10, screenY / 2, paint);
                }

                // Прорисовка все на экране
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        // If SimpleGameEngine Activity is paused/stopped
        // shutdown our thread.
        public void pause() {
            playing = false;
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        // If SimpleGameEngine Activity is started then
        // start our thread.
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
        }

        // Касание экрана
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {


                case MotionEvent.ACTION_DOWN:
                    paused = false;
                    if (motionEvent.getX() > screenX / 2) {

                        paddle.setMovementState(paddle.RIGHT);
                    } else

                    {
                        paddle.setMovementState(paddle.LEFT);
                    }

                    break;


                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }

            return true;
        }

    }

    // Игрок начал игру
    @Override
    protected void onResume() {
        super.onResume();

        breakoutView.resume();
    }

    // Игрок выходит из игры
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        breakoutView.pause();
    }

}
