// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.TexturedButton;
import com.spoiledmilk.ibikecph.util.AsyncImageFetcher;
import com.spoiledmilk.ibikecph.util.ImageData;
import com.spoiledmilk.ibikecph.util.ImagerPrefetcherListener;

public class RegisterActivity extends Activity implements ImagerPrefetcherListener {
	TextView textTitle;
	ImageButton btnBack;
	Button btnLogout;
	EditText textName;
	EditText textEmail;
	EditText textNewPassword;
	EditText textPasswordConfirm;
	TexturedButton btnRegister;

	Handler handler;

	UserData userData;

	ProgressBar progressBar;

	String validationMessage;
	String base64Image = "";
	private static final int IMAGE_REQUEST = 1888;

	public static final int RESULT_USER_DELETED = 101;
	public static final int RESULT_ACCOUNT_REGISTERED = 102;
	public static final int RESULT_NO_ACTION = 103;

	boolean inProgress = false;

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.register_activity);
		textTitle = (TextView) findViewById(R.id.textTitle);
		textTitle.setVisibility(View.VISIBLE);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		btnBack = (ImageButton) findViewById(R.id.btnBack);
		btnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_NO_ACTION);
				finish();
				overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
			}

		});

		textName = (EditText) findViewById(R.id.textName);
		textEmail = (EditText) findViewById(R.id.textEmail);
		textNewPassword = (EditText) findViewById(R.id.textNewPassword);
		textPasswordConfirm = (EditText) findViewById(R.id.textPasswordConfirm);
		btnRegister = (TexturedButton) findViewById(R.id.btnRegister);
		btnRegister.setTextureResource(R.drawable.btn_pattern_repeteable);
		btnRegister.setBackgroundResource(R.drawable.btn_blue_selector);
		btnRegister.setTextColor(Color.WHITE);
		btnRegister.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (validateInput() && !inProgress) {
					inProgress = true;
					new Thread(new Runnable() {
						@Override
						public void run() {
							Looper.myLooper();
							Looper.prepare();

							RegisterActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									progressBar.setVisibility(View.VISIBLE);
								}
							});

							Message message = HTTPAccountHandler.performRegister(userData, RegisterActivity.this);
							handler.sendMessage(message);
							IbikeApplication.getTracker().sendEvent("Register", "Completed", userData.getEmail(), (long) 0);
							RegisterActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									progressBar.setVisibility(View.GONE);
								}
							});

						}
					}).start();
				} else if (!inProgress) {
					launchAlertDialog(validationMessage);
				}
			}
		});

		if (handler == null) {
			handler = new Handler(new Handler.Callback() {

				@Override
				public boolean handleMessage(Message msg) {

					Bundle data = msg.getData();
					int msgType = data.getInt("type");
					Boolean success = false;
					inProgress = false;
					switch (msgType) {
					case HTTPAccountHandler.REGISTER_USER:
						success = data.getBoolean("success");
						if (!success) {
							launchAlertDialog(data.getString("info"));
						} else {
							setResult(RESULT_ACCOUNT_REGISTERED);
							finish();
							overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
						}
						break;
					case HTTPAccountHandler.ERROR:
						launchAlertDialog("Error");
						break;
					}
					return true;
				}
			});
		}

	}

	@Override
	public void onResume() {
		super.onResume();
		initStrings();
	}

	@Override
	public void onPause() {
		super.onPause();
		progressBar.setVisibility(View.GONE);
	}

	public void onImageContainerClick(View v) {
		Intent pickIntent = new Intent();
		pickIntent.setType("image/*");
		pickIntent.setAction(Intent.ACTION_GET_CONTENT);
		Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		String pickTitle = "";
		Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
		chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { takePhotoIntent });
		startActivityForResult(chooserIntent, IMAGE_REQUEST);
	}

	private void initStrings() {
		textTitle.setText(IbikeApplication.getString("create_account"));
		textTitle.setTypeface(IbikeApplication.getNormalFont());
		textTitle.setVisibility(View.VISIBLE);
		textNewPassword.setHint(IbikeApplication.getString("register_password_placeholder"));
		textNewPassword.setHintTextColor(getResources().getColor(R.color.HintColor));
		textNewPassword.setTypeface(IbikeApplication.getNormalFont());
		textPasswordConfirm.setHint(IbikeApplication.getString("register_password_repeat_placeholder"));
		textPasswordConfirm.setHintTextColor(getResources().getColor(R.color.HintColor));
		textPasswordConfirm.setTypeface(IbikeApplication.getNormalFont());
		btnRegister.setText(IbikeApplication.getString("register_save"));
		btnRegister.setTypeface(IbikeApplication.getNormalFont());
		textName.setHint(IbikeApplication.getString("register_name_placeholder"));
		textName.setHintTextColor(getResources().getColor(R.color.HintColor));
		textName.setTypeface(IbikeApplication.getNormalFont());
		textEmail.setHint(IbikeApplication.getString("register_email_placeholder"));
		textEmail.setHintTextColor(getResources().getColor(R.color.HintColor));
		textEmail.setTypeface(IbikeApplication.getNormalFont());
	}

	private void launchAlertDialog(String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(RegisterActivity.this);
		builder.setMessage(msg).setTitle("Error");
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private boolean validateInput() {
		boolean ret = true;
		if (textName.getText().toString().length() == 0 || textEmail.getText().toString().length() == 0
				|| textNewPassword.getText().toString().length() == 0 || textPasswordConfirm.getText().toString().length() == 0) {
			validationMessage = IbikeApplication.getString("register_error_fields");
			ret = false;
		} else if (!textNewPassword.getText().toString().equals(textPasswordConfirm.getText().toString())) {
			validationMessage = IbikeApplication.getString("register_error_passwords");
			ret = false;
		} else if (textNewPassword.getText().toString().length() < 3) {
			validationMessage = IbikeApplication.getString("register_error_passwords_short");
			ret = false;
		} else {
			int atIndex = textEmail.getText().toString().indexOf('@');
			if (atIndex < 1) {
				validationMessage = IbikeApplication.getString("register_error_invalid_email");
				ret = false;
			}
			int pointIndex = textEmail.getText().toString().indexOf('.', atIndex);
			if (pointIndex < atIndex || pointIndex == textEmail.getText().toString().length() - 1) {
				validationMessage = IbikeApplication.getString("register_error_invalid_email");
				ret = false;
			}
		}
		userData = new UserData(textName.getText().toString(), textEmail.getText().toString(), textNewPassword.getText().toString(),
				textPasswordConfirm.getText().toString(), base64Image, "image.png");
		return ret;
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
			progressBar.setVisibility(View.VISIBLE);
			AsyncImageFetcher aif = new AsyncImageFetcher(this, this);
			aif.execute(data);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance().activityStart(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance().activityStop(this);
	}

	@Override
	public void onImagePrefetched(ImageData imageData) {
		if (imageData != null && imageData.bmp != null && imageData.base64 != null) {
			base64Image = imageData.base64;
			((ImageView) findViewById(R.id.pictureContainer)).setImageDrawable(imageData.bmp);
		} else {
			Toast.makeText(this, "Error fetching the image", Toast.LENGTH_SHORT).show();
		}
		progressBar.setVisibility(View.GONE);
	}

}
