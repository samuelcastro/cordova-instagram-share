/*
       Copyright (c) 2016 Samuel Castro

       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/

package com.samuelcastro.cordova;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.database.Cursor;
import android.provider.MediaStore;

@TargetApi(Build.VERSION_CODES.FROYO)
public class InstagramSharePlugin extends CordovaPlugin {

    private static final FilenameFilter OLD_IMAGE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("instagram");
        }
    };

	CallbackContext cbContext;

	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

		this.cbContext = callbackContext;

        if (action.equals("shareVideo")) {
            String videoString = args.getString(0);
            String captionString = args.getString(1);
            this.shareVideo(videoString, captionString);
            return true;
        } else if (action.equals("shareImage")) {
            String imageString = args.getString(0);
            String captionString = args.getString(1);
            this.shareImage(imageString, captionString);
            return true;
        } else if (action.equals("isInstalled")) {
        	this.isInstalled();
        } else {
        	callbackContext.error("Invalid Action");
        }
        return false;
    }

	private void isInstalled() {
		try {
			this.webView.getContext().getPackageManager().getApplicationInfo("com.instagram.android", 0);
			this.cbContext.success(this.webView.getContext().getPackageManager().getPackageInfo("com.instagram.android", 0).versionName);
		} catch (PackageManager.NameNotFoundException e) {
			this.cbContext.error("Application not installed");
		}
	}

    private void shareImage(String imageString, String captionString) {
        if (imageString != null && imageString.length() > 0) {
        	byte[] imageData = Base64.decode(imageString, 0);

        	File file = null;
            FileOutputStream os = null;

        	File parentDir = this.webView.getContext().getExternalFilesDir(null);
            File[] oldImages = parentDir.listFiles(OLD_IMAGE_FILTER);
            for (File oldImage : oldImages) {
                oldImage.delete();
            }

            try {
                file = File.createTempFile("instagram", ".png", parentDir);
                os = new FileOutputStream(file, true);
            } catch (Exception e) {
                e.printStackTrace();
            }

        	try {
        		os.write(imageData);
				os.flush();
				os.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

        	Intent shareIntent = new Intent(Intent.ACTION_SEND);
        	shareIntent.setType("image/*");
        	shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file));
        	shareIntent.putExtra(Intent.EXTRA_TEXT, captionString);
        	shareIntent.setPackage("com.instagram.android");

        	this.cordova.startActivityForResult((CordovaPlugin) this, shareIntent, 12345);

        } else {
            this.cbContext.error("Expected one non-empty string argument.");
        }
    }

     public String getRealVideoPathFromURI(Uri contentUri)
        {
            try
            {
                String[] proj = {MediaStore.Video.Media.DATA};
                Cursor cursor = cordova.getActivity().getContentResolver().query(contentUri, proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
            catch (Exception e)
            {
                return contentUri.getPath();
            }
        }

    public String getRealImagePathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = cordova.getActivity().getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){;
           int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
           res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    private void shareVideo(String videoString, String captionString) {

            // Create the URI from the media
                File media = new File(this.getRealVideoPathFromURI(Uri.parse(videoString)));
                Uri uri = Uri.fromFile(media);

            	Intent shareIntent = new Intent(Intent.ACTION_SEND);
            	shareIntent.setType("video/*");
            	shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            	shareIntent.putExtra(Intent.EXTRA_TEXT, captionString);
            	shareIntent.setPackage("com.instagram.android");

            	this.cordova.startActivityForResult((CordovaPlugin) this, shareIntent, 12345);

        }

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == Activity.RESULT_OK) {
    		Log.v("Instagram", "shared ok");
    		this.cbContext.success();
    	} else if (resultCode == Activity.RESULT_CANCELED) {
    		Log.v("Instagram", "share cancelled");
    		this.cbContext.error("Share Cancelled");
    	}
    }
}
