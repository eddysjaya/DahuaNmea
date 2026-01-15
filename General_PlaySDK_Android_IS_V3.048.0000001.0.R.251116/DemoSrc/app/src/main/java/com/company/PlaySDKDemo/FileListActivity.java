package com.company.PlaySDKDemo;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView;
import java.util.Stack;
import java.io.File;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Collections;
import java.util.Comparator;

public class FileListActivity extends ListActivity 
{
	private ArrayList<String> fileList;
	private ArrayAdapter<String> adapter;
	private static Stack<String> pathStack;

	static{
		pathStack = new Stack<>();
		String sDStateString = Environment.getExternalStorageState();
		if (sDStateString.equals(Environment.MEDIA_MOUNTED)) {
			File SDFile = Environment.getExternalStorageDirectory();
			pathStack.push(SDFile.getAbsolutePath() + "/PlaySDK");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		fileList = new ArrayList<>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = super.getView(position, convertView, parent);
				String filePath = pathStack.peek() + "/" + getItem(position);
				File file = new File(filePath);
				TextView textView = view.findViewById(android.R.id.text1);
				textView.setText(file.getName()); // 只显示文件名
				if (file.isDirectory()) {
					textView.setTextColor(Color.BLUE);
				} else {
					textView.setTextColor(Color.BLACK);
				}
				return view;
			}
		};
		setListAdapter(adapter);

		String path = pathStack.peek();
		showFiles(path);
	}

	private void showFiles(String path) {
		File directory = new File(path);
		File[] files = directory.listFiles();

		if (files != null) {
			fileList.clear();

			for (File file : files) {
				fileList.add(file.getName());
			}
			// 按照文件夹和文件的名称进行排序
			fileList.sort(new Comparator<String>() {
                @Override
                public int compare(String filePath1, String filePath2) {
                    File file1 = new File(pathStack.peek() + "/" + filePath1);
                    File file2 = new File(pathStack.peek() + "/" + filePath2);
                    // 如果是文件夹，将其优先于文件
                    if (file1.isDirectory() && file2.isFile()) {
                        return -1; // file1 在前
                    } else if (file1.isFile() && file2.isDirectory()) {
                        return 1; // file2 在前
                    }
                    return file1.getName().compareToIgnoreCase(file2.getName()); // 按名称排序
                }
            });
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		String selectedItem = fileList.get(position);
		selectedItem = pathStack.peek() + "/" + selectedItem;
		File fileOrPath = new File(selectedItem);
		if (fileOrPath.exists())
		{
			if (fileOrPath.isDirectory())
			{
				pathStack.push(selectedItem);
				showFiles(selectedItem);
			}else if (fileOrPath.isFile())
			{
				jumpToPlayDemoActivity(selectedItem);
			}
		}
	}

	@Override
	public void onBackPressed() {
		if (pathStack.size() > 1) {
			pathStack.pop();
			String previousPath = pathStack.peek();
			showFiles(previousPath);
		} else {
			super.onBackPressed();
		}
	}


    public void showLog(String strLog)
    {
    	Toast.makeText(this, strLog, Toast.LENGTH_SHORT).show();
    }

	protected void onDestroy() 
	{
		super.onDestroy();
	}
	
    public void jumpToPlayDemoActivity(String selectfile)
    {
    	Intent intent = new Intent();
		intent.putExtra("selectabspath", selectfile);
		setResult(RESULT_OK, intent);
		finish();
    }
}