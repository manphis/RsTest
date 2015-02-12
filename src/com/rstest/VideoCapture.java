package com.rstest;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;


public class VideoCapture extends SurfaceView implements SurfaceHolder.Callback,
														 Camera.PreviewCallback
{
	private static final String TAG = "VideoCapture";
	private static final int H264_SAMPLE_RATE = 90000;
	private static final int BACK_CAMERA = 0;
	private static final int FRONT_CAMERA = 1;
	private static VideoCapture mVideoCapture = null;
	private static Camera mCamera = null;
	private static SurfaceHolder mHolder = null;
	private static int mNativeHandle = 0;
	private static int mWidth = 640;					// default setting
	private static int mHeight = 480;					// default setting
	private static int mFps = 10;						// default setting
	private static int mFpsFilter = 0;						// default setting
	private static long mTimestamp = 0;
	private static long mCounter = 0;
	private static final int BUFFER_NUM = 3;
	private static final int PIXEL_FORMAT = ImageFormat.NV21;
	private static PixelFormat mPixelFormat = new PixelFormat();
	
	public static int state = 0;
	private static int[] rgb_pixels;
	private static ImageView mView;
	private static boolean useRenderScript = false;
	private static RenderScript    mRS;
	private Bitmap outputBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
	private static int counter = 0;
	private static int total_time_ms = 0;

	private static Status mStatus = Status.UNKNOWN;
	private enum Status {
	    INTIALIZE, DESTORY, OPEN, CLOSE, START, STOP, PAUSE, RESUME, UNKNOWN
	}

	/***************************************************************************
	 *  				Public function implement
	 **************************************************************************/
	public static void Inintialize() {
		Log.e(TAG, "Inintialize");
		// in order to load libandroid_video_capture.so ASAP
		mStatus = Status.INTIALIZE;
		return ;
	}

	public static void Destroy() {
		if (mNativeHandle != 0) {
			Log.d(TAG, "Destroy VideoCapture");
//			NativeDestroy(mNativeHandle);
		}

		if (mCamera != null) {
			mCamera.setPreviewCallbackWithBuffer(null);
			mCamera.release();
		}

		mWidth = 0;
		mHeight = 0;
		mFps = 0;
		mTimestamp = 0;
		mNativeHandle = 0;
		mHolder = null;
		mCamera = null;
		mVideoCapture = null;
		mStatus = Status.DESTORY;
	}
	
    public static void Start() {
    	Log.d(TAG, "Start VideoCapture");
	}
    
    public static void Stop() {
		Log.d(TAG, "Stop VideoCapture");
		_Stop();
	}
    
    public static void Pause() {
		Log.d(TAG, "Pause VideoCapture");
		_Pause();
	}

	public static void Resume() {
		Log.d(TAG, "Resume VideoCapture");
		_Resume();
	}
    
    public static void Restart() {
    	Log.d(TAG, "Restart VideoCapture");
    	_Start();
//    	try{
//    	mCamera.setPreviewDisplay(mHolder);
//    	mCamera.startPreview();
//    	} catch(Exception e ){
//    		
//    		e.printStackTrace();
//    	}
//    	state = 1;
//    	mCounter = 0;
    }
    
    public static void setImageView(ImageView view) {
    	mView = view;
    }
    
    public static void setRenderScript(RenderScript script) {
    	mRS = script;
    }

    public VideoCapture(Context context, AttributeSet attrs) {
        super(context, attrs);
        
    	Log.e(TAG, " new VideoCapture");
        mHolder = this.getHolder();
        mHolder.addCallback(this);
//    	mNativeHandle = NativeCreate();
		mVideoCapture = this;
		Log.d(TAG, "VideoCapture mNativeHandle = "+mNativeHandle);
    }
    
    public VideoCapture(Context context) {
        super(context);
        
    	Log.e(TAG, " new VideoCapture");
        mHolder = this.getHolder();
        mHolder.addCallback(this);
//    	mNativeHandle = NativeCreate();
		mVideoCapture = this;
		Log.d(TAG, "VideoCapture mNativeHandle = "+mNativeHandle);
    }


	/***************************************************************************
	 *  				Private function implement
	 **************************************************************************/
	private static Camera getCameraInstance(){
	    Camera camera = null;
	    try {
//	    	camera = Camera.open(FRONT_CAMERA);
	    	camera = Camera.open(FRONT_CAMERA);
	    } catch (Exception e){
	    	Log.d(TAG, "Camera is not available: " + e.getMessage());
	    }
	    return camera;
	}
	
	private static void _Start() {		
//		if (mNativeHandle == 0) {
//			Log.d(TAG, "mNativeHandle doesn't create");
//			return ;
//		}
		try {
        	if (mCamera != null) {
        		mCamera.setPreviewDisplay(mHolder);
        	} else {
        		mCamera = getCameraInstance();
        		if(mCamera!=null)
        		mCamera.setPreviewDisplay(mHolder);
        	}
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }

		if (mCamera != null) {
			Log.d(TAG, "_Start VideoCapture");
			Camera.Parameters params = mCamera.getParameters();
			Camera.Size sizes = params.getPreviewSize();
			
			Log.d(TAG, "camera size = " + sizes.width + ": " + sizes.height);
			Log.d(TAG, "preview format = " + params.getPreviewFormat());
			
//			params.setPictureFormat(PIXEL_FORMAT);
//			params.setPreviewSize(mWidth, mHeight);
			
			rgb_pixels = new int[mWidth * mHeight];

			int[] fpsrange = new int[2];
			params.getPreviewFpsRange(fpsrange);

			params.setPreviewFpsRange(fpsrange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], 
			                          fpsrange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

			try{
			    mCamera.setParameters(params);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			
			
			PixelFormat.getPixelFormatInfo(PIXEL_FORMAT, mPixelFormat);
			byte[] buffer = null;
			for (int i=0; i<BUFFER_NUM; i++) {
				buffer = new byte[mWidth*mHeight*mPixelFormat.bitsPerPixel/8];
				mCamera.addCallbackBuffer(buffer);
			}

			mCamera.setPreviewCallbackWithBuffer(mVideoCapture);
			mCamera.startPreview();
			
			state = 1;

		}
		else
			Log.d(TAG, "_Start: camera = null");
		mCounter = 0;
		mStatus = Status.START;
	}
	
	private static void _Stop() {		
		if (mCamera != null) {
			Log.d(TAG, "_Stop VideoCapture");
			mCamera.stopPreview();
			try {
				mCamera.setPreviewDisplay(null);
				mCamera.release();
				mCamera = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
			state = 0;
		}
		mCounter = 0;
		mStatus = Status.STOP;
	}
	
	private static void _Pause() {
		mStatus = Status.PAUSE;
	}

	private static void _Resume() {
		mStatus = Status.RESUME;
	}

	private static void ConfigCallback(int width, int height, int fps) {
		Log.e(TAG, "ConfigCallback VideoCapture width " + width + " height "
	          + height + " fps " + fps);
		mWidth = width;
		mHeight = height;
		mFps = fps;
		mFpsFilter = 30/fps;
		mTimestamp = 0;
	}

	private static void StartCallbcak() {
		Log.d(TAG, "StartCallbcak VideoCapture");
		_Start();
	}

	private static void StopCallback() throws IOException {
		Log.d(TAG, "StopCallback VideoCapture");
		_Stop();
	}

	/***************************************************************************
	 *  			implement SurfaceHolder.Callback
	 **************************************************************************/
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d(TAG, "surfaceChanged VideoCapture width " + width + " height "
	          + height + " format " + format);
        try {
        	if (mCamera != null) {
        		StopCallback();
        	}
        } catch (Exception e){
        	Log.d(TAG, "tried to stop a non-existent preview: " + e.getMessage());
        }
        mHolder = holder;
        _Start();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated VideoCapture");
		mHolder = holder;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed VideoCapture");
		mHolder = null;
	}
	

	/***************************************************************************
	 *  			implement Camera.PreviewCallba
	 **************************************************************************/
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mStatus == Status.START || mStatus == Status.RESUME) {
//		    if (mFpsFilter !=0 && 0 == mCounter%mFpsFilter) {
//			    NativeEncode(data, data.length);
//		    }
			if (mCounter % 10 == 0 && mView != null) {
				counter++;
				long time = 0;
				long duringTime = 0;
				time = System.currentTimeMillis();
				
				if (!useRenderScript) {
				
					decodeYUV420SP(rgb_pixels, data, mWidth, mHeight);

					Bitmap bitmap = Bitmap.createBitmap(rgb_pixels, 0, mWidth,
							mWidth, mHeight, Bitmap.Config.ARGB_8888);
					duringTime = (System.currentTimeMillis() - time);
					Log.d(TAG,
							"NV21 --> YUV420 time = " + duringTime);
					total_time_ms += duringTime;
					
					mView.setImageBitmap(bitmap);
				} else {
					byte[] rgba_byte_array = N21ToRGBA(data);
					duringTime = (System.currentTimeMillis() - time);
					Log.d(TAG,
							"NV21 --> YUV420 time = " + duringTime);
					
					total_time_ms += duringTime;
//					int[] rgb = convertByteToColor(rgba_byte_array);
//					Bitmap bitmap = Bitmap.createBitmap(rgb, 0, mWidth,
//							mWidth, mHeight, Bitmap.Config.ARGB_8888);
					mView.setImageBitmap(outputBitmap);
				}
				
				if (counter == 100)
					Log.d(TAG, "average time = " + total_time_ms/counter);
			}
		}
		
		camera.addCallbackBuffer(data);
		mCounter++;
		//Log.d(TAG, "onPreviewFrame mCounter = " + mCounter);
	}
	
	// Format transfer
	//Method from Ketai project! Not mine! See below...  
	private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

		final int frameSize = width * height;

		for (int j = 0, yp = 0; j < height; j++) {
			int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
			for (int i = 0; i < width; i++, yp++) {
				int y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				int y1192 = 1192 * y;
				int r = (y1192 + 1634 * v);
				int g = (y1192 - 833 * v - 400 * u);
				int b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
						| ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
			}
		}
	}
	
	public byte[] convertColorToByte(int color[])
    {  
        if (color == null)  
        {  
            return null;  
        }  
          
        byte[] data = new byte[color.length * 3];  
        for(int i = 0; i < color.length; i++)  
        {  
            data[i * 3] = (byte) (color[i] >> 16 & 0xff);  
            data[i * 3 + 1] = (byte) (color[i] >> 8 & 0xff);  
            data[i * 3 + 2] =  (byte) (color[i] & 0xff);  
        }  
          
        return data;  
          
    }
	
//	private int[] convertByteToColor(byte[] data) {
//		int size = data.length;
//		if (size == 0) {
//			return null;
//		}
//
//		// 理论上data的长度应该是3的倍数，这里做个兼容
//		int arg = 0;
//		if (size % 3 != 0) {
//			arg = 1;
//		}
//
//		int[] color = new int[size / 3 + arg];
//		int red, green, blue;
//
//		if (arg == 0) { // 正好是3的倍数
//			for (int i = 0; i < color.length; ++i) {
//
//				color[i] = (data[i * 3] << 16 & 0x00FF0000)
//						| (data[i * 3 + 1] << 8 & 0x0000FF00)
//						| (data[i * 3 + 2] & 0x000000FF) | 0xFF000000;
//			}
//		} else { // 不是3的倍数
//			for (int i = 0; i < color.length - 1; ++i) {
//				color[i] = (data[i * 3] << 16 & 0x00FF0000)
//						| (data[i * 3 + 1] << 8 & 0x0000FF00)
//						| (data[i * 3 + 2] & 0x000000FF) | 0xFF000000;
//			}
//
//			color[color.length - 1] = 0xFF000000; // 最后一个像素用黑色填充
//		}
//
//		return color;
//	}
	
	private int[] convertByteToColor(byte[] data) {
		int size = data.length;
		if (size == 0) {
			return null;
		}

		int[] color = new int[size / 4];

		
			for (int i = 0; i < color.length; ++i) {

				color[i] = (data[i * 4] << 16 & 0x00FF0000)
						| (data[i * 4 + 1] << 8 & 0x0000FF00)
						| (data[i * 4 + 2] & 0x000000FF) | 0xFF000000;
			}

		return color;
	}
    
    
	private byte[] N21ToRGBA(final byte[] n21ByteArray) {
		byte[] rgbaArray = new byte[4*mWidth*mHeight];
	    ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
	    Type.Builder            n21Type;
	    Type.Builder            rgbaType;
	    Allocation              in;
	    Allocation              out;
	    
	    yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(mRS, Element.U8_4(mRS));
	    
	    n21Type = new Type.Builder(mRS, Element.U8(mRS));
	    n21Type.setX(n21ByteArray.length);
	    in = Allocation.createTyped(mRS, n21Type.create());

	    rgbaType = new Type.Builder(mRS, Element.RGBA_8888(mRS));
	    rgbaType.setX(mWidth);
	    rgbaType.setY(mHeight);
	    out = Allocation.createTyped(mRS, rgbaType.create());

	    in.copyFrom(n21ByteArray);
	    yuvToRgbIntrinsic.setInput(in);
	    yuvToRgbIntrinsic.forEach(out);
	    out.copyTo(rgbaArray);
	    
	    
	    out.copyTo(outputBitmap);
	    
	    return rgbaArray;
	}
}
