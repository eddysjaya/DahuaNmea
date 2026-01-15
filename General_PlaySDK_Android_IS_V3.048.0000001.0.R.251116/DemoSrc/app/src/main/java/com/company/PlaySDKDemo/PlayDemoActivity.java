package com.company.PlaySDKDemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;   
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.EditText;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.media.AudioManager;
import android.graphics.SurfaceTexture;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.opengl.GLES20;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import com.company.PlaySDK.IPlaySDK;
import com.company.PlaySDK.IPlaySDK.CUSTOMRECT;
import com.company.PlaySDK.Constants;
import com.company.PlaySDK.IPlaySDKCallBack.*;

import static com.company.PlaySDK.IPlaySDK.PLAYPlay;
import static com.company.PlaySDK.IPlaySDK.PLAYSetEngine;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;

import android.graphics.Matrix;

public class PlayDemoActivity extends Activity {

	private AudioManager mAudioManager;
	final static boolean EnableDrawFun = false;
	
	public  static final int FILE = 0;		
	public  static final int FILESTREAM = 1;
	private static final int FILE_PATH = 101;
	
	static int port = 0;
	int mode = FILE; 
	int decodeType = Constants.DECODE_SW;
	int aecParam = Constants.AEC_SW;
	String curfile;
	
	private TextureView sv1;
	private SurfaceTexture sfTexture;
	private volatile FastRender m_fastRender;
	private int nTextureId;
	private Surface decodeSurface;
	private SeekBar ProceseekBar;
	private EditText etFile;
	private Button btMode;
	private Button btDecodeType;
	private Button BtnPlay;
	private Button btStop;
	private Button btSnapPict;
	private Button btFast;
	private Button btSlow;
	private Button btNormal;
	private Button btQuality;
	private Button btCapture;
	private Button btAecParam;
	private Button btRule;
	private Button btTrack;
	private Button btPosEvent;
	private View layoutQuality;
	private PopupWindow popQuality;
	private Button btSwapCamera;
	private Spinner spResolution;
    private Button btfisheyeDewarp;
	private Button btAntiAliasing;
	private Button btIotBox;

	private boolean bIotBox = false;
	private boolean bAntiAliasing = false;
	private boolean bPlay = false;
	private boolean bPause = false;
	private boolean bCapture = false;
	private boolean bRule = true;
	private boolean bTrack = true;
	private boolean bPosEvent = true;
	private boolean bFrontCamera = true;
	private int nCameraWidth = 720;
	private int nCameraHeight = 1080;
	
	private String strSpeed[] = new String[]{"1/64X", "1/32X", "1/16X", "1/8X", "1/4X", "1/2X", "1X", "2X", "4X", "8X", "16X", "32X", "64X"};
	private int nSpeedCur = 6;

    private boolean bEnableFisheye = false;

	public class TestAudioRecord implements pCallFunction
	{
		String fileName;

		public TestAudioRecord()
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String currentTime = sdf.format(new Date());
			fileName = "/mnt/sdcard/PlaySDK/audio_record_" +currentTime + ".dav";
		}

		public void invoke(byte[] pDataBuffer,int nBufferLen, long pUserData)
		{
			try
			{
				File file = new File(fileName);
				FileOutputStream fout = new FileOutputStream(file, true);
				fout.write(pDataBuffer);
				fout.close();
			}
			catch(Exception e)
			{
			}
		}

		@Override
		public void invokeNotify(int i, long l, long l1) {

		}

	}

	public class TestVideoRecord implements pCallFunction
	{
		String fileName;

		public TestVideoRecord()
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String currentTime = sdf.format(new Date());
			fileName = "/mnt/sdcard/PlaySDK/video_record_" +currentTime + ".dav";
		}

		public void invoke(byte[] pDataBuffer,int nBufferLen, long pUserData)
		{
			try
			{
				File file = new File(fileName);
				FileOutputStream fout = new FileOutputStream(file, true);
				fout.write(pDataBuffer);
				fout.close();
			}
			catch(Exception e)
			{
			}
		}

		@Override
		public void invokeNotify(int i, long l, long l1) {

		}

	}

	public class AecCallBackFun implements fAECProcessCBFun
	{
		public void invoke(int nPort, AEC_CALLBACK_INFO pFrameDecodeInfo) {
			int b = 0;
		}
	}

	private TestAudioRecord m_audiorecordcallback;
	private TestVideoRecord m_videorecordcallback;
	private AecCallBackFun m_aecCB;

	public void  startRecord()
	{
		//sound capture.
		int m_nBitPerSample = 16;
		int m_nSamplePerSecond = 8000;
		int lSampleLen = 1024;
		m_audiorecordcallback = new TestAudioRecord();

		File SDFile = Environment.getExternalStorageDirectory();
		String path1 = SDFile.getAbsolutePath() + "/PlaySDK/audio_aec_ans_16k.cfg";
		IPlaySDK.PLAYSetAecDebug(0, path1);
		IPlaySDK.PLAYAecHardwareEnable(aecParam);
		int retValue = IPlaySDK.PLAYOpenAudioRecord(m_audiorecordcallback,m_nBitPerSample,m_nSamplePerSecond, lSampleLen, 1);
		if(0 == retValue)
		{
			Log.d("[playsdk]", "PLAYOpenAudioRecord Failed.");
		}

		//打开对讲时，切换音频播放模式为通话模式
		mAudioManager.setSpeakerphoneOn(false);
		mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		IPlaySDK.PLAYSetAudioPlaybackMode(port, 0);
		PlayDemoActivity.this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

		//video capture
		m_videorecordcallback = new TestVideoRecord();

		IPlaySDK.CAMERA_CAPABILITY_PARAM capParam = new IPlaySDK.CAMERA_CAPABILITY_PARAM();
		capParam.cameraType = bFrontCamera ? 1:0;
		capParam.height = nCameraHeight;
		capParam.width = nCameraWidth;
		IPlaySDK.CAMERA_RECORD_CAPABILITY cap = IPlaySDK.PLAYGetCameraCapability(capParam);

		boolean hasFind = false;
		for (int i = 0; i < cap.dimensions.length; i++)
		{
			if(cap.dimensions[i].pixelHeight == nCameraHeight && cap.dimensions[i].pixelWidth == nCameraWidth)
			{
				hasFind = true;
				break;
			}
		}

		if(hasFind)
		{
			IPlaySDK.CAMERA_RECORD_PARAM param = new IPlaySDK.CAMERA_RECORD_PARAM();
			param.cameraType = bFrontCamera ? 1:0;
			param.captureHeight = nCameraHeight;
			param.captureWidth = nCameraWidth;
			param.frameRate = 30;
			param.outPutHeight = nCameraHeight;
			param.outPutWidth = nCameraWidth;
			param.isRotateFrame = 1;

			IPlaySDK.PLAYOpenCameraVideoRecord(m_videorecordcallback, param);
		}else
		{
			new AlertDialog.Builder(this)
					.setTitle("警告")
					.setMessage("分辨率不支持")
					.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// 确定按钮的点击事件处理
						}
					})
					.setNegativeButton("取消", null)
					.show();
		}
	}

	public void stopRecord()
	{
		IPlaySDK.PLAYCloseAudioRecord();
		IPlaySDK.PLAYCloseCameraVideoRecord();

		//关闭对讲时，切换音频播放模式为媒体模式
		mAudioManager.setSpeakerphoneOn(true);
		mAudioManager.setMode(AudioManager.MODE_NORMAL);
		IPlaySDK.PLAYSetAudioPlaybackMode(port, 3);
		PlayDemoActivity.this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 执行与应用程序从后台返回到前台相关的操作
		// 比如重新加载数据、注册监听器等
		if(bCapture)
		{
			startRecord();
		}

		IPlaySDK.PLAYSetIVSIotBoxConfig(port, 0, "/storage/emulated/0/PlaySDK/iotbox_assets", 16);

	}

	@Override
	protected void onPause() {
		super.onPause();
		// 执行与应用程序从前台切换到后台相关的操作
		// 比如释放资源、取消注册监听器等
		if(bCapture)
		{
			stopRecord();
		}
		IPlaySDK.PLAYSetIVSIotBoxConfig(port, 1, "", 0);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
		{

		}else
		{

		}
	}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mAudioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
		
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
            		Manifest.permission.WRITE_EXTERNAL_STORAGE,
					Manifest.permission.RECORD_AUDIO,
					Manifest.permission.CAMERA}, 2);
        }

        IPlaySDK.PLAYSetPrintLogSwitch(1);
		
		sv1 = (TextureView) findViewById(R.id.sv_demo_view);
		sv1.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			@Override
			public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
				Log.d("[playsdk]", "surfaceCreated: " + surfaceTexture.toString());
				if(bPlay)
				{
					if(Constants.DECODE_HW_TEXTURE != decodeType)
						IPlaySDK.PLAYSetDisplayRegion(port, 0, null, new Surface(surfaceTexture), 1);
					IPlaySDK.PLAYRenderPrivateData(port, 1);
					IPlaySDK.PLAYViewResolutionChanged(port, sv1.getWidth(), sv1.getHeight(), 0);
				}
			}

			@Override
			public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
				if(bPlay) {

					int _width = sv1.getWidth();
					int _height = sv1.getHeight();

					IPlaySDK.PLAYViewResolutionChanged(port, _width , _height, 0);
				}
			}

			@Override
			public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
				Log.d("[playsdk]", "surfaceDestroyed: " + surfaceTexture.toString());

				if(bPlay)
				{
					if(Constants.DECODE_HW_TEXTURE != decodeType)
						IPlaySDK.PLAYSetDisplayRegion(port, 0, null, new Surface(surfaceTexture), 0);
				}

				return true;
			}

			@Override
			public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

			}
		});
		     
		Button BtnOpenFile = (Button)findViewById(R.id.bt_demo_file);
		BtnOpenFile.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				Toast.makeText(getApplicationContext(), "Select file...", Toast.LENGTH_LONG).show();
			
					jumpToFileListActivity();
			}
		});
		
		etFile = (EditText)findViewById(R.id.et_demo_file);
		
		btMode = (Button)findViewById(R.id.bt_demo_mode);
		btMode.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mode = (mode + 1) % 2;
				if (FILE == mode) {
					btMode.setText(R.string.demo_activity_mode_file);
				} else {
					btMode.setText(R.string.demo_activity_mode_stream);
				}
			}
		});
		
		btDecodeType = (Button)findViewById(R.id.bt_demo_decode_type);
		btDecodeType.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				decodeType = decodeType % 4 + 1;
				if (Constants.DECODE_SW == decodeType) {
					btDecodeType.setText(R.string.demo_activity_decode_sw);
				} else if(Constants.DECODE_HW == decodeType){
					btDecodeType.setText(R.string.demo_activity_decode_hw);
				} else if(Constants.DECODE_HW_FAST == decodeType){
					btDecodeType.setText(R.string.demo_activity_decode_hwFast);
				} else if(Constants.DECODE_HW_TEXTURE == decodeType){
					btDecodeType.setText(R.string.demo_activity_decode_hwTexture);
				}
			}
		});

		btAecParam = (Button)findViewById(R.id.bt_demo_aec);
		btAecParam.setEnabled(false);
		btAecParam.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                aecParam = (aecParam + 1) % 2;
                if (Constants.AEC_SW == aecParam)
                {
                    btAecParam.setText(R.string.demo_activity_aec_sw);
                }
                else
                {
                    btAecParam.setText(R.string.demo_activity_aec_hw);
                }
            }
        });
		 
		btCapture = (Button)findViewById(R.id.bt_demo_capture);
		btCapture.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (bCapture == false) {

					startRecord();

					btCapture.setText(R.string.demo_activity_stopcapture);
					bCapture = true;
				} else {

					stopRecord();

					btCapture.setText(R.string.demo_activity_capture);
					bCapture = false;
				}
			}
		});
        
		BtnPlay = (Button)findViewById(R.id.bt_demo_play);
		BtnPlay.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!bPlay) {
					curfile = etFile.getText().toString();
					if (curfile == null || curfile.equals(""))
			    	{
						Toast.makeText(getApplicationContext(), "Please select/input one file to play", Toast.LENGTH_LONG).show();
			    		return;
			    	}
					
					bPlay = true;
					if(Constants.DECODE_HW_TEXTURE == decodeType)
					{
						m_fastRender = new FastRender(port);
						m_fastRender.start();
					}
					else
					{
						StartPlaySDK();
					}
					//start process control timer
					new Thread(new ThreadProcess()).start(); 
				 				
	 				BtnPlay.setText(R.string.demo_activity_pause);
	 				
	 				btMode.setEnabled(false);
	 				btDecodeType.setEnabled(false);
	 				btStop.setEnabled(true);
	 				btSnapPict.setEnabled(true);
	 				
	 				btFast.setEnabled(true);
					btSlow.setEnabled(true);
					btNormal.setEnabled(true);
					btQuality.setEnabled(true);

					btCapture.setEnabled(true);
					btAecParam.setEnabled(true);
					btRule.setEnabled(true);
					btTrack.setEnabled(true);
					btPosEvent.setEnabled(true);
					btSwapCamera.setEnabled(true);
					btAntiAliasing.setEnabled(true);
					btfisheyeDewarp.setEnabled(true);
					btIotBox.setEnabled(true);

					btRule.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
					btTrack.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
					btPosEvent.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));

					nSpeedCur = 6;
	 			} else {
	 				if (!bPause) {
	 					IPlaySDK.PLAYPause(port, (short)1);
	 					
	 					bPause = true;
		 				BtnPlay.setText(R.string.demo_activity_play);
	 				} else {
	 					IPlaySDK.PLAYPause(port, (short)0);
	 					
	 					bPause = false;
		 				BtnPlay.setText(R.string.demo_activity_pause);
	 				}
	 			}
			}
		});
		
		btStop = (Button)findViewById(R.id.bt_demo_stop);
		btStop.setEnabled(false);
		btStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				//stop play
	    		try {
	                Message msg = new Message();   
	                msg.what = 2;   
	                handler.sendMessage(msg);   
	            } catch (Exception e) {   
	                e.printStackTrace();   
	            }
			}
		});
		
		btSnapPict = (Button)findViewById(R.id.bt_demo_SnapPict);
		btSnapPict.setEnabled(false);
		btSnapPict.setOnClickListener(new OnClickListener() {
			@SuppressLint("UseValueOf") 
			public void onClick(View v) {

				Log.d("[playsdk]", "bt_demo_SnapPict onClick");
				//Get time
				File SDFile = Environment.getExternalStorageDirectory();
				File sdPath = new File(SDFile.getAbsolutePath() + "/PlaySDK");
				if (sdPath.canWrite())
				{
					byte[] rr = new byte[24];  // array size must large than 24 , because it is used to store 6 int. if not , QueryInfo will return false.
					long year = 0;
					long month = 0;
					long day = 0;
					long hour = 0;
					long minute = 0;
					long second = 0;
					Integer gf = new Integer(0);
					if(IPlaySDK.PLAYQueryInfo(port, Constants.PLAY_CMD_GetTime, rr, rr.length, gf) != 0)
					{
						year 	= ((long)(rr[3]  & 0xff) << 24) + ((long)(rr[2]  & 0xff) << 16) + ((long)(rr[1]  & 0xff) << 8) + (long)(rr[0]  & 0xff);
						month 	= ((long)(rr[7]  & 0xff) << 24) + ((long)(rr[6]  & 0xff) << 16) + ((long)(rr[5]  & 0xff) << 8) + (long)(rr[4]  & 0xff);
						day 	= ((long)(rr[11] & 0xff) << 24) + ((long)(rr[10] & 0xff) << 16) + ((long)(rr[9]  & 0xff) << 8) + (long)(rr[8]  & 0xff);
						hour 	= ((long)(rr[15] & 0xff) << 24) + ((long)(rr[14] & 0xff) << 16) + ((long)(rr[13] & 0xff) << 8) + (long)(rr[12] & 0xff);
						minute 	= ((long)(rr[19] & 0xff) << 24) + ((long)(rr[18] & 0xff) << 16) + ((long)(rr[17] & 0xff) << 8) + (long)(rr[16] & 0xff);
						second 	= ((long)(rr[23] & 0xff) << 24) + ((long)(rr[22] & 0xff) << 16) + ((long)(rr[21] & 0xff) << 8) + (long)(rr[20] & 0xff);
					}

					if (year == 0 || month == 0 || day == 0)
					{
						Calendar calendar = Calendar.getInstance();
						year = calendar.get(Calendar.YEAR);
						month = calendar.get(Calendar.MONTH) + 1;
						day = calendar.get(Calendar.DAY_OF_MONTH);
						hour = calendar.get(Calendar.HOUR_OF_DAY);
						minute = calendar.get(Calendar.MINUTE);
						second = calendar.get(Calendar.SECOND);
					}
					String name = year + "_" + month + "_" + day + "_" + hour + "_" + minute + "_" + second + ".bmp";
					String snapPictFile = sdPath.getAbsolutePath() + "/" + name;
					Log.d("[playsdk]", "SnapPictFile :" + snapPictFile);
					if (decodeType == Constants.DECODE_HW_TEXTURE){
						new Thread(new CatchPicThread(snapPictFile)).start();
						Log.d("[playsdk]", "bt_demo_SnapPict finish");
					}
					else{
						//Snap Picture
						if(IPlaySDK.PLAYCatchPic(port, snapPictFile) != 0)
						{
							Log.d("[playsdk]", "PLAYCatchPic Success" );
						}
					}
				}

			}
		});
		

		btFast = (Button)findViewById(R.id.bt_demo_fast);
		btFast.setEnabled(false);
		btFast.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {

				int b = IPlaySDK.PLAYFast(port);
				if (0 != b) {
					if (nSpeedCur < strSpeed.length - 1) {
						nSpeedCur++;
						Toast.makeText(getApplicationContext(), strSpeed[nSpeedCur], Toast.LENGTH_LONG).show();
					}
				}

        	}
        });
		
		btSlow = (Button)findViewById(R.id.bt_demo_slow);
		btSlow.setEnabled(false);
		btSlow.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
				int b = IPlaySDK.PLAYSlow(port);
				if (0 != b) {
					if (nSpeedCur > 0) {
						nSpeedCur--;
						Toast.makeText(getApplicationContext(), strSpeed[nSpeedCur], Toast.LENGTH_LONG).show();
					}
				}
        	}
        });
		
		btNormal = (Button)findViewById(R.id.bt_demo_normal);
		btNormal.setEnabled(false);
		btNormal.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
				int b = IPlaySDK.PLAYPlay(port, new Surface(sv1.getSurfaceTexture()));
				if (0 != b) {
					BtnPlay.setText(R.string.demo_activity_pause);
					bPause = false;
					
					nSpeedCur = 6;
					Toast.makeText(getApplicationContext(), strSpeed[nSpeedCur], Toast.LENGTH_LONG).show();
				}
			}
        });
		
		layoutQuality = View.inflate(PlayDemoActivity.this, R.layout.qualityview, null);
		btQuality = (Button)findViewById(R.id.bt_demo_quality);
		btQuality.setEnabled(false);
        btQuality.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
	        	Integer stBright = new Integer(0);
	        	Integer stContrast = new Integer(0);
	        	Integer stSaturation = new Integer(0);
	        	Integer stHuen = new Integer(0);
	        	int nRet = IPlaySDK.PLAYGetColor(port, 0, stBright, stContrast, stSaturation, stHuen);
	        	if (0 == nRet) {
	        		Toast.makeText(getApplicationContext(), "Get color failed", Toast.LENGTH_LONG).show();
	        		return;
	        	}
	        	
	        	if (null == popQuality) {
	        		popQuality = new PopupWindow(layoutQuality, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	    		}
	    		
	    		if (popQuality.isShowing()) {
	    			popQuality.dismiss();
	    			return;
	    		} else {
	    			popQuality.showAtLocation(findViewById(R.id.layout_demo), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 150);
	    		}
	    		
	    		SeekBar sbBright = (SeekBar)layoutQuality.findViewById(R.id.sb_quality_bright);
	    		sbBright.setMax(128);
	    		sbBright.incrementProgressBy(1);
	    		sbBright.setProgress(stBright);
	    		sbBright.setOnSeekBarChangeListener(new ColorSeekBarListenner());
	    		
	    		SeekBar sbContrast = (SeekBar)layoutQuality.findViewById(R.id.sb_quality_contrast);
	    		sbContrast.setMax(128);
	    		sbContrast.incrementProgressBy(1);
	    		sbContrast.setProgress(stContrast);
	    		sbContrast.setOnSeekBarChangeListener(new ColorSeekBarListenner());
	    		
	    		SeekBar sbSaturation = (SeekBar)layoutQuality.findViewById(R.id.sb_quality_saturation);
	    		sbSaturation.setMax(128);
	    		sbSaturation.incrementProgressBy(1);
	    		sbSaturation.setProgress(stSaturation);
	    		sbSaturation.setOnSeekBarChangeListener(new ColorSeekBarListenner());
	    		
	    		SeekBar sbHuen = (SeekBar)layoutQuality.findViewById(R.id.sb_quality_huen);
	    		sbHuen.setMax(128);
	    		sbHuen.incrementProgressBy(1);
	    		sbHuen.setProgress(stHuen);
	    		sbHuen.setOnSeekBarChangeListener(new ColorSeekBarListenner());
        	}
        });
        
        ProceseekBar =(SeekBar)findViewById(R.id.sb_demo_process);
        ProceseekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) {
				
			}
			
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				
		           if (fromUser) {
		                int SeekPosition=seekBar.getProgress();
		                float fpos = (float)SeekPosition/100;
		        	   if(mode == FILE){
			                IPlaySDK.PLAYSetPlayPos(port, fpos);
		        	   }else{
		        		   IPlaySDK.PLAYResetBuffer(port, 1);
		        		   IPlaySDK.PLAYResetBuffer(port, 3);
			       			try {
			       				fis.seek((long) (fpos*fileLength));
			       			} catch (IOException e) {
			       				e.printStackTrace();
			       			}
			       			bResetStreamPos = true;
		        	   }
		            }

				
			}
		});

        btRule = (Button)findViewById(R.id.bt_demo_rule);
		btRule.setEnabled(false);
		btRule.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(bRule == false){
					IPlaySDK.PLAYSetIvsEnable(port, Constants.IVSDRAWER_RULE, 1);
					bRule = true;
					btRule.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
				}
				else {
					IPlaySDK.PLAYSetIvsEnable(port, Constants.IVSDRAWER_RULE, 0);
					bRule = false;
					btRule.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
				}
			}
		});

		btTrack = (Button)findViewById(R.id.bt_demo_track);
		btTrack.setEnabled(false);
		btTrack.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(bTrack == false){

					//do not show track Tex.
					//IPlaySDK.IVSDRAWER_TrackEx2Config  textDisable = null;
					//IPlaySDK.PLAYSetIVSTrackEx2Config(port, textDisable);

					IPlaySDK.PLAYSetIvsEnable(port,Constants.IVSDRAWER_TRACK,1);
					IPlaySDK.PLAYSetIvsEnable(port,Constants.IVSDRAWER_TRACKEX2,1);
					bTrack = true;
					btTrack.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));

				}
				else  {
					IPlaySDK.PLAYSetIvsEnable(port,Constants.IVSDRAWER_TRACK,0);
					IPlaySDK.PLAYSetIvsEnable(port,Constants.IVSDRAWER_TRACKEX2,0);
					bTrack = false;
					btTrack.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
				}
			}
		});

		btPosEvent = (Button)findViewById(R.id.bt_demo_posevent);
		btPosEvent.setEnabled(false);
		btPosEvent.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if(bPosEvent == false){
					IPlaySDK.PLAYSetIvsEnable(port,Constants.IVSDRAWER_ALARM,1);
					bPosEvent = true;
					btPosEvent.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
				}
				else {
					IPlaySDK.PLAYSetIvsEnable(port,Constants.IVSDRAWER_ALARM,0);
					bPosEvent = false;
					btPosEvent.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
				}
			}
		});

		btSwapCamera = (Button)findViewById(R.id.bt_demo_swap);
		btSwapCamera.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				bFrontCamera = !bFrontCamera;
				if (bFrontCamera)
				{
					btSwapCamera.setText("Front");
				}else
				{
					btSwapCamera.setText("Back");
				}
			}
		});

		spResolution = (Spinner) findViewById(R.id.sp_demo_resolution);
		String[] items = {"CIF", "480p", "720p", "1080p", "4k", "CIF_NEW"};

		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		spResolution.setAdapter(adapter);

		spResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				switch (position)
				{
					case 0:
					{
						nCameraWidth = 352;
						nCameraHeight = 288;
					}
						break;
					case 1:
					{
						nCameraWidth = 640;
						nCameraHeight = 480;
					}
						break;
					case 2:
					{
						nCameraWidth = 1280;
						nCameraHeight = 720;
					}
						break;
					case 3:
					{
						nCameraWidth = 1920;
						nCameraHeight = 1080;
					}
						break;
					case 4:
					{
						nCameraWidth = 3840;
						nCameraHeight = 2160;
					}
						break;
					case 5:
					{
						nCameraWidth = 320;
						nCameraHeight = 240;
					}
					break;
					default:
						break;
				}

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// 当没有选项被选择时执行的操作
			}
		});

        btfisheyeDewarp = (Button)findViewById(R.id.fisheye_dewarp);
		btfisheyeDewarp.setEnabled(false);
        btfisheyeDewarp.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                bEnableFisheye = !bEnableFisheye;
                if (bEnableFisheye)
                {
                	IPlaySDK.PLAYStartFisheye(port);
                	IPlaySDK.PLAYSetFisheyeParam(port, Constants.FISHEYEMOUNT_MODE_CEIL, Constants.FISHEYECALIBRATE_MODE_ORIGINAL_PLUS_THREE_EPTZ_REGION);
                    btfisheyeDewarp.setText("dewarp");
                }else
                {
					IPlaySDK.PLAYStopFisheye(port);
                    btfisheyeDewarp.setText("origin");
    			}
            }
        });

		btAntiAliasing = (Button)findViewById(R.id.bt_anti_aliasing);
		btAntiAliasing.setEnabled(false);
		btAntiAliasing.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				bAntiAliasing = !bAntiAliasing;
				int intValue = bAntiAliasing ? 1 : 0;
				IPlaySDK.PLAYAntiAliasEnable(port, intValue);
				if(bAntiAliasing){
					btAntiAliasing.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
				}else{
					btAntiAliasing.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
				}
			}
		});

		btIotBox = (Button)findViewById(R.id.bt_iotbox);
		btIotBox.setEnabled(false);
		btIotBox.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				bIotBox = !bIotBox;
				if(bIotBox){
					IPlaySDK.PLAYSetIVSIotBoxConfig(port, 0, "/storage/emulated/0/PlaySDK/iotbox_assets", 16);
					IPlaySDK.PLAYSetIvsEnable(port,41, 1);
					btIotBox.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
				}else{
					IPlaySDK.PLAYSetIvsEnable(port,41, 0);
					IPlaySDK.PLAYSetIVSIotBoxConfig(port, 1, "/storage/emulated/0/PlaySDK/iotbox_assets", 16);
					btIotBox.setBackgroundTintList(ColorStateList.valueOf(Color.LTGRAY));
				}
			}
		});
	
    }
	
    public void StartPlaySDK()
    {
    	int retValue = 0;
    	// file mode
		if(mode == FILE){
			long lUsrdata = 0;
			// optional: add this call back to get invoke when file play finish.
			retValue = IPlaySDK.PLAYSetFileEndCallBack(port, new PlayEndCallBack(), lUsrdata);
			if(0 == retValue){
				return;
			}
			retValue = IPlaySDK.PLAYOpenFile(port,curfile);
			if(0 == retValue){
				return;
			}
		}else{
			IPlaySDK.PLAYSetStreamOpenMode(port, Constants.STREAME_FILE);
			retValue = IPlaySDK.PLAYOpenStream(port, null, 0, 5*1024*1024);
			if(0 == retValue){
				return;
			}
		}
		
		// optional: DrawCallBack Test
		if(EnableDrawFun)
		{
			retValue = IPlaySDK.PLAYRigisterDrawFun(port, 0, new DrawCallback(), 0);
			if(0 == retValue)
			{
				return;
			}
		}
		
		//draw IVS enable
		IPlaySDK.PLAYRenderPrivateData(port, 1);

		// optional: this interface can be called to accelerate when decode h264 and hevc.
		Log.d("playsdk", "decodeType:" + decodeType);
		if(decodeType == Constants.DECODE_SW)
		{
			IPlaySDK.PLAYSetDecodeThreadNum(port, 4);
		}
		// optional: this interface will use ndkmediacodec to decode h264 and hevc.
		else
		{
			int nDecodeType = decodeType;
			if (decodeType == Constants.DECODE_HW_TEXTURE){
				nDecodeType = 6;
			}
			retValue = IPlaySDK.PLAYSetEngine(port, nDecodeType, Constants.RENDER_NOTSET);
			if(0 == retValue){
				return;
			}
		}

		if (Constants.DECODE_HW_TEXTURE == decodeType) {
			retValue = IPlaySDK.PLAYSetGLESTextureCallBack(port, new GLESTextureCallBack(), 0, 0);
			if(0 == retValue){
				return;
			}
		}

		IPlaySDK.PLAYSetFishEyeInfoCallBack(port, 0 , 0);		String sDStateString = Environment.getExternalStorageState();
		if (sDStateString.equals(Environment.MEDIA_MOUNTED)) {
			try {
				File SDFile = Environment.getExternalStorageDirectory();
				String path = SDFile.getAbsolutePath() + "/PlaySDK/audio_hds_ans.cfg";
				m_aecCB = new AecCallBackFun();
				IPlaySDK.PLAYSetAecPlayConfig(port, path, m_aecCB);
			}
			catch (Exception e)
			{

			}
		}		//硬解直显方式Play_Play传入渲染窗口surface初始化
		retValue = IPlaySDK.PLAYPlay(port, new Surface(sv1.getSurfaceTexture()));
		if(0 == retValue){
			return;
		}
		// optional: open sound
		retValue = IPlaySDK.PLAYPlaySound(port);
		if(0 == retValue){
			Log.d("[playsdk]", "PLAYPlaySound Failed.");
		}
		
		// stream mode need PLAYInputdata to fill streamdata into playsdk.
		if(mode == FILESTREAM){
			new Thread(new FileStreamDataFill()).start();
		}
    }
    
    public void Stop()
    {
    	if(Constants.DECODE_HW_TEXTURE == decodeType) {
			m_fastRender.stopRender();
    	}
    	else {
			StopPlaySDK();
		}
    }

    public void StopPlaySDK()
	{
		IPlaySDK.PLAYRigisterDrawFun(port, 0, null, 0);
		if(0 != mProgram)
		{
			GLES20.glDeleteProgram(mProgram);
			mProgram = 0;
		}

		IPlaySDK.PLAYStopSound();
		//clear Screen
		IPlaySDK.PLAYCleanScreen(port, 0/*red*/, 0/*green*/, 0/*blue*/, 1/*alpha*/, 0);
		IPlaySDK.PLAYStop(port);
			
		if(mode == FILE)
		{
			IPlaySDK.PLAYCloseFile(port);
		}
		else
		{
			IPlaySDK.PLAYCloseStream(port);
		}
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
    	if (bPlay) {
			Stop();
    	}
    	
    	if (null != popQuality && popQuality.isShowing()) {
    		popQuality.dismiss();
    	}
    }

    //implement timer
    Handler handler = new Handler() {   
    public void handleMessage(Message msg) {   
                if (msg.what == 1) {   
                	   if(mode == FILE){
                		   float fproc = IPlaySDK.PLAYGetPlayPos(port);
                		   if(fproc < 0.01)
                		   {
                			   Log.d("","");
                		   }
                		   ProceseekBar.setProgress((int)(fproc*100));
		        	   }
                	   else{                		   
                		   long curPos = 0;
							try {
								if (fis != null)
									curPos = fis.getFilePointer() - IPlaySDK.PLAYGetSourceBufferRemain(port);
							} catch (IOException e) {
								e.printStackTrace();
							}
                		   if(fileLength != 0){
                			   ProceseekBar.setProgress((int)(curPos*100 / fileLength));
                		   }
		        	   }
               } else if (msg.what == 2) {
					Stop();
					bPlay = false;
					btStop.setEnabled(false);
					btSnapPict.setEnabled(false);
					BtnPlay.setText(R.string.demo_activity_play);
					bPause = false;

					btDecodeType.setEnabled(true);
					btMode.setEnabled(true);
					
					btFast.setEnabled(false);
					btSlow.setEnabled(false);
					btNormal.setEnabled(false);
					btQuality.setEnabled(false);

					btAecParam.setEnabled(false);
					btRule.setEnabled(false);
					btTrack.setEnabled(false);
					btPosEvent.setEnabled(false);
					btAntiAliasing.setEnabled(false);
					btfisheyeDewarp.setEnabled(false);
					btIotBox.setEnabled(false);

					bIotBox = false;
					bAntiAliasing = false;
					bRule = true;
					bTrack = true;
					bPosEvent = true;
					int color = Color.parseColor("#FFECECEC");
					btRule.setBackgroundTintList(ColorStateList.valueOf(color));
					btTrack.setBackgroundTintList(ColorStateList.valueOf(color));
					btPosEvent.setBackgroundTintList(ColorStateList.valueOf(color));
					btAntiAliasing.setBackgroundTintList(ColorStateList.valueOf(color));
					btIotBox.setBackgroundTintList(ColorStateList.valueOf(color));




					if (null != popQuality && popQuality.isShowing()) {
						popQuality.dismiss();
					}
               }
            };   
      };

	//implement frameListener
	class FastRender extends Thread implements SurfaceTexture.OnFrameAvailableListener {
		private final Object mFrameSyncObject = new Object();
		private boolean mFrameAvailable;
		private volatile boolean renderStop = false;
		private int  nPort;
		// 命令队列（线程安全）
    	private final BlockingQueue<Runnable> commandQueue = new LinkedBlockingQueue<>();
		private float[] m_transMat = new float[16];

		public FastRender(int port) {
			nPort = port;
		}
		@Override
		public void run() {
			super.run();

			//start play
			StartPlaySDK();
			while(!renderStop){

				// 1. 处理待执行命令
                Runnable cmd = commandQueue.poll();
                if (cmd != null) {
                    cmd.run();
                }

				awaitNewImage();
				//updateMatrix();
				drawImage(nPort);
				//判断是否需要截图，激活截图线程
			}

			StopPlaySDK();

		}

		// 外部线程调用：发送命令到渲染线程
		// 添加命令到队列
		public void postCommand(Runnable command) {
			if (command != null) {
				commandQueue.offer(command);
			}
		}

		@Override
		public void onFrameAvailable(SurfaceTexture decodeSurface) {
			synchronized (mFrameSyncObject) {
				Log.d("[PlaySDK]","onFrameAvailable enter!");
				if (mFrameAvailable) {
					Log.d("[PlaySDK]", "mFrameAvailable already set, this frame dropped");
				}
				//此刻的sfTexture.getTimestamp为上次updateTexImage获取的时间戳
				//https://developer.android.google.cn/reference/android/graphics/SurfaceTexture?hl=zh-cn
				//Retrieve the timestamp associated with the texture image set by the most recent call to updateTexImage().
				Log.d("[PlaySDK]", "onFrameAvailable port:"+String.valueOf(nPort)+" timeStamp:"+String.valueOf(decodeSurface.getTimestamp()/1000));
				mFrameAvailable = true;
				mFrameSyncObject.notifyAll();
			}
		}
		//绘制单帧画面
		public void drawImage(int nPort){
			synchronized (mFrameSyncObject) {
				//IPlaySDK.PLAYOutsideRender(nPort, 0.0f, 0.0f, 0, 0, 0);
				IPlaySDK.PLAYOutsideRender(nPort, Math.abs(m_transMat[0]), Math.abs(m_transMat[5]), 0, 0, 0);
			}
		}
		//等待单帧解码
		public void awaitNewImage(){
			final int TIMEOUT_MS = 2000;
			synchronized (mFrameSyncObject){
				while (!mFrameAvailable){
					try {
						mFrameSyncObject.wait(TIMEOUT_MS);
						if(!mFrameAvailable){
							Log.d("[PlaySDK]","frame wait time out.");
							return ;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				mFrameAvailable = false;
			}

			try {
				//更新纹理
				//Log.d("[PlaySDK]","updateTexImage.");
				Log.d("[PlaySDK]","before updateTexImage port:"+String.valueOf(nPort)+" timeStamp:"+ String.valueOf(sfTexture.getTimestamp()/1000));
				//Update the texture image to the most recent frame from the image stream.
				//更新之后时间戳为解码后的最新帧
				//应用调用 updateTexImage()，这会释放先前占有的缓冲区，从队列中获取新缓冲区并执行 EGL 调用，从而使 GLES 可将此缓冲区作为外部纹理使用。
				//onFrameAvailable回调可以发生在任意线程，所以不能在回调中直接调用updateTexImage，而是必须切换到OpenGL线程调用updateTexImage
				sfTexture.updateTexImage();
				sfTexture.getTransformMatrix(m_transMat);
				Log.d("[PlaySDK]","after updateTexImage port:"+String.valueOf(nPort)+" timeStamp:"+ String.valueOf(sfTexture.getTimestamp()/1000));
				//Log.d("[PlaySDK]","Leave updateTexImage.");
			}catch (IllegalStateException e){

			}
		}
		//停止渲染
		public void stopRender() {
			Log.d("[PlaySDK]","stop render...");
			synchronized (mFrameSyncObject){
				renderStop = true;
				mFrameAvailable = false;
				mFrameSyncObject.notifyAll();
			}
		}
	}


    //implement timer
    class ThreadProcess implements Runnable {   

       public void run() {   
    	   
    	   Thread.interrupted();
            while (bPlay) {   
               try {   
                    Thread.sleep(10);   
                    Message msg = new Message();   
                    msg.what = 1;   
                    handler.sendMessage(msg);   
                } catch (Exception e) {   
                    e.printStackTrace();   
                }
            } 
	   		 Log.d("[playsdk]", "ThreadProcess End");
       }
    }

	class CatchPicThread implements Runnable {
		String m_picFile;
		public CatchPicThread(String fileDir) {
			m_picFile = fileDir;
		}
		public void run() {
            int ret = 0;
			if (bPlay) {
				try {
					Log.d("[playsdk]", "CatchPicThread start");
					ret = IPlaySDK.PLAYCatchPic(port, m_picFile);

					Log.d("[playsdk]", "CatchPicThread return:"+ String.valueOf(ret));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (ret == 1) {
				Log.d("[playsdk]", "CatchPicThread Success");
			}
		}
	}

    long fileLength;
    boolean bResetStreamPos;
    RandomAccessFile fis = null;
	class FileStreamDataFill implements Runnable {

		public void run() {
			try
			{
				fis = new RandomAccessFile(curfile,"rw");
				fileLength = fis.length();
				int readsize = 1024;
				byte[] buffer = new byte[readsize];
				int readlen = 0;
				int ret = 1;
				bResetStreamPos = false;
				while(bPlay){
					if(bResetStreamPos){
						bResetStreamPos = false;
						ret = 1;
					}
					if(ret == 1){
						readlen = fis.read(buffer);
					}
					if(readlen == -1){
						Thread.sleep(100);
						continue;
					}

					ret = IPlaySDK.PLAYInputData(port, buffer, readlen);
					if(ret == 0){
						Thread.sleep(10); 
						Log.d("[playsdk]", "PLAYInputData Failed.");
					}
				}
				fis.close();
				fis = null;
			}
			catch(Exception e)
			{
			}
		}
	}

    private class PlayEndCallBack implements fpFileEndCBFun {
    	@Override
    	public void invoke(int nPort, long pUserData) {
    		try {
                Message msg = new Message();   
                msg.what = 2;   
                handler.sendMessage(msg);   
            } catch (Exception e) {   
                e.printStackTrace();   
            }
    	}
    }

	private class GLESTextureCallBack implements fGLESTextureCallback {
		@Override
		public Surface invoke(int TextureID, long pUserData) {
			nTextureId = TextureID;
			sfTexture = new SurfaceTexture(nTextureId);
			if ( sfTexture != null) {
				sfTexture.setOnFrameAvailableListener(m_fastRender);
			}
			else{
				Log.d("[PlaySDK]", "wrong SurfaceTexture!");
			}
			decodeSurface = new Surface(sfTexture);
			if (decodeSurface  == null ||  m_fastRender == null) {
				return null;
			}
			return decodeSurface;
		}
	}

    private final String vertexShader = 
    		"attribute vec4 aColor;\n" + 
    		"varying vec4 vColor;\n" + 
    		"attribute vec4 vPosition;\n" +
    		"void main(){ \n" +
    		"gl_Position = vPosition;\n" +
    		"vColor = aColor;\n" +
    		"}\n";
    
    private final String fragShader = 
    		"precision mediump float; \n" + 
    		"varying vec4 vColor;\n" + 
    		"void main(){ \n" +
    		"gl_FragColor = vColor;\n" +
    		"}\n";
    
    private final float[] vertexArray= new float[]{
			-0.5f, 0.5f, 0.0f,
			-0.5f, -0.5f, 0.0f,
			0.5f, -0.5f, 0.0f,
			0.5f, 0.5f, 0.0f
	};
		
    private final float[] colorArray= new float[]{
			255.0f, 0.0f, 0.0f, 1.0f,
			255.0f, 0.0f, 0.0f, 1.0f,
			255.0f, 0.0f, 0.0f, 1.0f,
			255.0f, 0.0f, 0.0f, 1.0f
	};
		
    private int mProgram = 0;
	private int maPositionHandle = 0;
	private int maColorHandle = 0;
	FloatBuffer vertexBuffer;
	FloatBuffer colorBuffer;
	
    private class DrawCallback implements fDrawCBFun {
    	@Override
    	public void invoke(int nPort,int regionnum, long eglContext, long pUserData)
		{		
     		if(mProgram == 0)
     		{        		
        		int vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
         		GLES20.glShaderSource(vshader, vertexShader);
         		GLES20.glCompileShader(vshader);
         		int compiled[] = new int[1];
         		GLES20.glGetShaderiv(vshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        		if(compiled[0] != GLES20.GL_TRUE)
        		{
        			Log.d("[playsdk]", "compile vsharder failed.");
        		}
         		
         		int fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
         		GLES20.glShaderSource(fshader, fragShader);
         		GLES20.glCompileShader(fshader);
         		GLES20.glGetShaderiv(fshader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        		if(compiled[0] != GLES20.GL_TRUE)
        		{
        			Log.d("[playsdk]", "compile fsharder failed.");
        		}
         		
        		mProgram = GLES20.glCreateProgram();
        		GLES20.glAttachShader(mProgram, vshader);
        		GLES20.glAttachShader(mProgram, fshader);
        		GLES20.glLinkProgram(mProgram);
        		GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, compiled, 0);
        		if(compiled[0] != GLES20.GL_TRUE)
        		{
        			Log.d("[playsdk]", "link program failed.");
					GLES20.glDeleteShader(vshader);
            		GLES20.glDeleteShader(fshader);
        			GLES20.glDeleteProgram(mProgram);
        			mProgram = 0;
        			return;
        		}
      
            	GLES20.glDeleteShader(vshader);
            	GLES20.glDeleteShader(fshader);
        
        		maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        		maColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
        		
     			ByteBuffer vbb = ByteBuffer.allocateDirect(vertexArray.length*4);
        		vbb.order(ByteOrder.nativeOrder());
        		vertexBuffer = vbb.asFloatBuffer();
        		vertexBuffer.put(vertexArray);
        		vertexBuffer.position(0);
        		
        		ByteBuffer cbb = ByteBuffer.allocateDirect(colorArray.length*4);
        		cbb.order(ByteOrder.nativeOrder());
        		colorBuffer = cbb.asFloatBuffer();
        		colorBuffer.put(colorArray);
        		colorBuffer.position(0);
     		}
    		
    		GLES20.glUseProgram(mProgram);
    			
    		GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
    		GLES20.glEnableVertexAttribArray(maPositionHandle);
    		
    		GLES20.glVertexAttribPointer(maColorHandle, 4, GLES20.GL_FLOAT, false, 16, colorBuffer);
    		GLES20.glEnableVertexAttribArray(maColorHandle);
    		
    		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 3);
    	
		}	
	}
    
    
    public void jumpToFileListActivity()
    {
		Intent intent = new Intent();
		intent.setClass(this, FileListActivity.class);
		startActivityForResult(intent, FILE_PATH);
    }
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (data == null) {
			return;
		}
		
		if (requestCode == FILE_PATH && resultCode == RESULT_OK) {
			curfile = data.getStringExtra("selectabspath");
			etFile.setText(curfile);
			etFile.setSelection(etFile.length());
		}
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_BACK)
    	{
			Stop();
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    private class ColorSeekBarListenner implements OnSeekBarChangeListener {
		@Override
    	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    		if (fromUser) {
	        	Integer stBright = new Integer(0);
	        	Integer stContrast = new Integer(0);
	        	Integer stSaturation = new Integer(0);
	        	Integer stHuen = new Integer(0);
	        	IPlaySDK.PLAYGetColor(port, 0, stBright, stContrast, stSaturation, stHuen);
    			if (seekBar == (SeekBar)layoutQuality.findViewById(R.id.sb_quality_bright)) {
    				IPlaySDK.PLAYSetColor(port, 0, progress, stContrast, stSaturation, stHuen);
    			} else if (seekBar == (SeekBar)layoutQuality.findViewById(R.id.sb_quality_contrast)) {
    				IPlaySDK.PLAYSetColor(port, 0, stBright, progress, stSaturation, stHuen);
    			} else if (seekBar == (SeekBar)layoutQuality.findViewById(R.id.sb_quality_saturation)) {
    				IPlaySDK.PLAYSetColor(port, 0, stBright, stContrast, progress, stHuen);
    			} else if (seekBar == (SeekBar)layoutQuality.findViewById(R.id.sb_quality_huen)) {
    				IPlaySDK.PLAYSetColor(port, 0, stBright, stContrast, stSaturation, progress);
    			}
    		}
    	}
    	
		

    	@Override
    	public void onStartTrackingTouch(SeekBar seekBar) {
    		
    	}
    	
    	@Override
    	public void onStopTrackingTouch(SeekBar seekBar) {
    		
    	}
	}
}