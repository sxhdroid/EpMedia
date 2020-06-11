package xsh.ijkplaycmd.api;

import android.support.annotation.Keep;

import xsh.ijkplaycmd.core.EpText;

/**
 * @author xiaosh
 * @date 2020-06-11 15:23
 */
@Keep
public final class TextWrapper extends EpText {

    public TextWrapper(int x, int y, float size, Color color, String ttf, String text, Time time) {
        super(x, y, size, color, ttf, text, time);
    }
}
