package hmi.parkinglot.render;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import com.here.android.mpa.common.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hmi.parkinglot.Application;

/**
 * Map rendering utilities
 */
public class RenderUtilities {
    // Splits the string to avoid too long lines
    private static List<String> splitString(String str) {
        List<String> out = new ArrayList<>();
        String[] tokens = str.split(" ");

        int maxTokenLength = 7;
        int minTokenLength = 4;

        StringBuffer currentToken = new StringBuffer();
        for (String token : tokens) {
            int newLength = currentToken.length() + token.length();
            if (newLength > maxTokenLength && currentToken.length() > minTokenLength) {
                out.add(currentToken.toString());
                currentToken = new StringBuffer(token);
            } else {
                if (currentToken.length() > 0) {
                    currentToken.append(" ");
                }
                currentToken.append(token);
            }
        }

        // Last token has to be added
        if (out.size() > 0 && currentToken.length() > 0 && currentToken.length() < minTokenLength) {
            String last = out.get(out.size() - 1);
            last += (" " + currentToken.toString());
            out.set(out.size() - 1, last);
        } else if (currentToken.length() > 0) {
            out.add(currentToken.toString());
        }

        return out;
    }

    private static List<Integer> calculateTextWidths(Paint paint, List<String> strings) {
        List<Integer> out = new ArrayList<>();

        for (String text : strings) {
            out.add((int) (paint.measureText(text) + 0.5f));
        }

        return out;
    }

    private static int calculateTextHeight(Paint paint, List<String> text) {
        float baseline = -paint.ascent();
        int textHeight = ((int) (baseline + paint.descent() + 0.5f)) * text.size();

        return textHeight;
    }


    public static Image createLabeledIcon(Context ctx, String text1, RenderStyle style,
                                          int drawable) {
        // First of all string is splitted
        List<String> strings = splitString(text1);
        Image out = new Image();

        try {
            Bitmap iconBitmap = BitmapFactory.decodeResource(ctx.getResources(), drawable);
            Paint paint = createPaint(ctx, style.textSize, style.textColor, style.textStyle);

            List<Integer> textWidths = calculateTextWidths(paint, strings);
            int textWidth = Collections.max(textWidths);
            int textHeight = calculateTextHeight(paint, strings);

            int width = Math.max(iconBitmap.getWidth(), textWidth);
            int height = iconBitmap.getHeight() + textHeight;

            Bitmap resBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas resCanvas = new Canvas(resBitmap);
            resCanvas.drawBitmap(iconBitmap, calculateLeft(width, iconBitmap.getWidth()), 0, null);

            float baseline = -paint.ascent();
            int lineHeight = (int) (baseline + paint.descent() + 0.5f);
            int tIndex = 0;
            for (String t : strings) {
                resCanvas.drawText(t, calculateLeft(width, textWidths.get(tIndex)),
                        baseline + iconBitmap.getHeight() + lineHeight * tIndex, paint);
                tIndex++;
            }

            out.setBitmap(resBitmap);
        } catch (Exception e) {
            Log.e(Application.TAG, "Error while creating map maker: " + e.toString());
            e.printStackTrace();
        }
        return out;
    }

    private static Paint createPaint(Context ctx, float textSize, int textColor, int textStyle) {
        Paint paint = new Paint();
        paint.setTextSize(dipToPixels(ctx, textSize));
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, textStyle));

        return paint;
    }

    private static float dipToPixels(Context ctx, float dip) {
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (dip * scale);
    }

    private static int calculateLeft(int globalWidth, int elementWidth) {
        return (globalWidth - elementWidth) / 2;
    }
}
