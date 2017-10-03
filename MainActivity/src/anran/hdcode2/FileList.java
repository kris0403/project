package anran.hdcode2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FileList extends ListActivity
{	
	private List<String> items = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.directory_list);
        File f=null;
        String sdcard=Environment.getExternalStorageDirectory().getPath();
        if(!sdcard.endsWith("/"))sdcard=sdcard+"/";
        
        ///////////////
        System.out.println(sdcard+"sdcard");
        ///////////////////
        
        if(getIntent().getExtras()!=null&&getIntent().getExtras().containsKey("Receive")){
          sdcard+="HDCodeReceived";
        }

        Log.i("specific sdcard",(icicle==null)+"");
        if((f=new java.io.File(sdcard)).exists())
        	fill(f.listFiles());
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) 
    {
		int selectionRowID = (int) id;
		if (selectionRowID == 0) 
		{
			fillWithRoot();
		}
		else 
		{
			File file = new File(items.get(selectionRowID));
			if (file.isDirectory())
			{
				fill(file.listFiles());
			}
			else
			{
				Intent result = new Intent();
				result.putExtra("filename", file.getAbsolutePath());
				setResult(RESULT_OK, result);
				finish();
			}			
		}
	}

    private void fillWithRoot() {
    	fill(new File(Environment.getExternalStorageDirectory().getPath()).listFiles());
    }

	private void fill(File[] files) 
	{
		items = new ArrayList<String>();
		items.add("Back to SD Card root folder..");
		if(files!=null)
			for (File file : files)
				items.add(file.getPath());
		
		ArrayAdapter<String> fileList = new ArrayAdapter<String>(this,
				R.layout.file_row, items);
		setListAdapter(fileList);
	}
}
