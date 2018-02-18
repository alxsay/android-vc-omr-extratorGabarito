package net.johnhany.moaap_chp2_r3;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

/**
 * Created by marcus on 16/01/18.
 */

public class SampleView extends View
{
    private Movie mMovie;
    private InputStream mStream;
    private long mMoviestart;

    public SampleView(Context context, InputStream stream)
    {
        super(context);

        mStream = stream;
        mMovie = Movie.decodeStream(mStream);
    }



    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.drawColor(Color.TRANSPARENT);
        super.onDraw(canvas);
        final long now = SystemClock.uptimeMillis();
        if (mMoviestart == 0) {
            mMoviestart = now;
        }
        final int relTime = (int)((now - mMoviestart) % mMovie.duration());
        mMovie.setTime(relTime);
        mMovie.draw(canvas, 10, 10);
        this.invalidate();
    }
}
