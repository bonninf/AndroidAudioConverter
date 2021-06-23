package cafe.adriel.androidaudioconverter;

import android.content.Context;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;

import java.io.File;
import java.io.IOException;

import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class AndroidAudioConverter {

    private static boolean loaded;

    private Context context;
    private File audioFile;
    private AudioFormat format;
    private IConvertCallback callback;

    private AndroidAudioConverter(Context context){
        this.context = context;
    }

    public static boolean isLoaded(){
        return loaded;
    }

    public static void load(Context context, final ILoadCallback callback){
        loaded = true;
        callback.onSuccess();

/*
        try {

            if (FFmpegKit.getInstance(context).isSupported()) {
                loaded = true;
                callback.onSuccess();
            }
            else {
                loaded = false;
                callback.onFailure(new Exception("Failed to loaded FFmpeg lib"));
            }

        } catch (Exception e){
            loaded = false;
            callback.onFailure(e);
        }
*/
    }

    public static AndroidAudioConverter with(Context context) {
        return new AndroidAudioConverter(context);
    }

    public AndroidAudioConverter setFile(File originalFile) {
        this.audioFile = originalFile;
        return this;
    }

    public AndroidAudioConverter setFormat(AudioFormat format) {
        this.format = format;
        return this;
    }

    public AndroidAudioConverter setCallback(IConvertCallback callback) {
        this.callback = callback;
        return this;
    }

    public void convert() {
        if(!isLoaded()){
            callback.onFailure(new Exception("FFmpeg not loaded"));
            return;
        }
        if(audioFile == null || !audioFile.exists()){
            callback.onFailure(new IOException("File not exists"));
            return;
        }
        if(!audioFile.canRead()){
            callback.onFailure(new IOException("Can't read the file. Missing permission?"));
            return;
        }
        final File convertedFile = getConvertedFile(audioFile, format);
        final String ffmpegCommand = String.format("-hide_banner -y -i %s -c:a libmp3lame -qscale:a 2 %s", audioFile.getPath(), convertedFile.getPath());
//        final String[] cmd = new String[]{"-y", "-i", audioFile.getPath(), convertedFile.getPath()};
        try {

            FFmpegKit.executeAsync(ffmpegCommand, new ExecuteCallback() {

                @Override
                public void apply(Session session) {
                    final SessionState state = session.getState();
                    final ReturnCode returnCode = session.getReturnCode();

                    if (ReturnCode.isSuccess(returnCode)) {
                        callback.onSuccess(convertedFile);
                    }
                    else {
                        final String message = String.format("Encode failed with state %s and rc %s.%s", state, returnCode, notNull(session.getFailStackTrace(), "\n"));
                        callback.onFailure(new IOException(message));
                    }


                }

            });

/*
            FFmpeg.getInstance(context).execute(cmd, new ExecuteBinaryResponseHandler() {
                @Override
                public void onStart() {}

                @Override
                public void onProgress(String message) {}

                @Override
                public void onFailure(String message) {
                    callback.onFailure(new IOException(message));
                }

                @Override
                public void onSuccess(String message) {
                    callback.onSuccess(convertedFile);
                }

                @Override
                public void onFinish() {}

            });
*/

        } catch (Exception e){
            callback.onFailure(e);
        }
    }

    private static File getConvertedFile(File originalFile, AudioFormat format){
        String[] f = originalFile.getPath().split("\\.");
        String filePath = originalFile.getPath().replace(f[f.length - 1], format.getFormat());
        return new File(filePath);
    }

    private static String notNull(final String string, final String valuePrefix) {
        return (string == null) ? "" : String.format("%s%s", valuePrefix, string);
    }
}