package xsh.ijkplaycmd.api;

import android.support.annotation.Keep;

import xsh.ijkplaycmd.core.EpEditor;

/**
 * 视频编辑类{@link EpEditor}的包装类
 *
 * @author xiaosh
 * @date 2020-06-11 15:05
 */
@Keep
public final class EditorHelper extends EpEditor {

    @Keep
    public static class OutputOption extends EpEditor.OutputOption {
        public OutputOption(String outPath) {
            super(outPath);
        }
    }
}
