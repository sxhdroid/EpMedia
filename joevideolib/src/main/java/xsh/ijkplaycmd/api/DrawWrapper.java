package xsh.ijkplaycmd.api;

import android.support.annotation.Keep;

import xsh.ijkplaycmd.core.EpDraw;

/**
 * @author xiaosh
 * @date 2020-06-11 15:21
 */
@Keep
public final class DrawWrapper extends EpDraw {

    public DrawWrapper(String picPath, int picX, int picY, float picWidth, float picHeight, boolean isAnimation) {
        super(picPath, picX, picY, picWidth, picHeight, isAnimation);
    }

    public DrawWrapper(String picPath, int picX, int picY, float picWidth, float picHeight, boolean isAnimation, int start, int end) {
        super(picPath, picX, picY, picWidth, picHeight, isAnimation, start, end);
    }
}
