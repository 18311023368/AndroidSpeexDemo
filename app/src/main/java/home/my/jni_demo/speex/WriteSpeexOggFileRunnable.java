package home.my.jni_demo.speex;

import android.util.Log;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;


public class WriteSpeexOggFileRunnable implements Runnable {
    private final static String TAG = "WriteSpeexOggFileRunnable";
    
    private final File mFile;
    private SpeexWriteClient mSpeexWriteClient = new SpeexWriteClient();
    private volatile boolean mIsRecording;
    private processedData mData;
    private LinkedBlockingQueue<processedData> mDataQueue;

    public static int sWritePackageSize = 1024;

    public WriteSpeexOggFileRunnable(File file) {
        this.mFile = file;
        mDataQueue = new LinkedBlockingQueue<>();
        mSpeexWriteClient.start(file, SpeexWriteClient.MODE_NB, SpeexWriteClient.SAMPLERATE_8000, true);
    }

    public void run() {
        Log.d(TAG, "write thread runing");

        mIsRecording = true;
        while (this.isRecording()) {
            try {
                mData = mDataQueue.take();
            } catch (InterruptedException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                mData = null;
            }
            if (mData == sStopFlagData) {
                mIsRecording = false;
            }else if (mData != null) {
                Log.d(TAG, "mData size=" + mData.size);
                mSpeexWriteClient.writePacket(mData.processed, mData.size);
            }
        }
        mSpeexWriteClient.stop();

        Log.d(TAG, "write thread exit");
    }

    public boolean putData(final byte[] buf, int size) {
        processedData data = new processedData(buf, size);
        try {
            mDataQueue.put(data);
        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    public void stop() {
        try {
            mDataQueue.put(sStopFlagData);
        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public File getOutputFile() {
        return this.mFile;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    class processedData {
        processedData(byte[] buf, int size) {
            if(buf != null)
                System.arraycopy(buf, 0, this.processed, 0, size);
            this.size = size;
        }
        private int size;
        private byte[] processed = new byte[sWritePackageSize];
    }
    
    private static final ProcessedData sStopFlagData = new ProcessedData(null, -1);
}
