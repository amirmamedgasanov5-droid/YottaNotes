package com.yotta.fc;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

/** Hosts the game loop and forwards touch input. */
final class GameView extends View {
    final App app;
    long last;

    GameView(Context ctx, App app) {
        super(ctx);
        this.app = app;
        setFocusable(true);
        setFocusableInTouchMode(true);
        last = System.nanoTime();
    }

    @Override protected void onDraw(Canvas c) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - last) / 1e9f);
        last = now;
        app.frame(c, dt, getWidth(), getHeight());
        postInvalidateOnAnimation();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int w = getWidth(), h = getHeight();
        int act = e.getActionMasked();
        int idx = e.getActionIndex();
        switch (act) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                app.touch(0, e.getPointerId(idx), e.getX(idx), e.getY(idx), w, h);
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) app.touch(2, e.getPointerId(i), e.getX(i), e.getY(i), w, h);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                app.touch(1, e.getPointerId(idx), e.getX(idx), e.getY(idx), w, h);
                break;
            case MotionEvent.ACTION_CANCEL:
                for (int i = 0; i < e.getPointerCount(); i++) app.touch(1, e.getPointerId(i), e.getX(i), e.getY(i), w, h);
                break;
            default: break;
        }
        return true;
    }
}
