package anran.hdcode2

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.view.Menu
import android.content.Intent
import anran.hdcode2.datalink.FrameConstructor
import android.graphics.Bitmap
import android.widget.EditText

class MainActivity extends Activity {
	override def onCreate(savedInstanceState:Bundle) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		val btnsend=findViewById(anran.hdcode2.R.id.btnSend)
		val txtBx=findViewById(anran.hdcode2.R.id.txtBlockX).asInstanceOf[EditText]
		val txtBy=findViewById(anran.hdcode2.R.id.txtBlockY).asInstanceOf[EditText]
		val txtnpar=findViewById(anran.hdcode2.R.id.txtInterBlockEC).asInstanceOf[EditText]
		btnsend.setOnClickListener(new android.view.View.OnClickListener(){
		  def onClick(view:android.view.View){
		  }
		})
		val btnreceive=findViewById(anran.hdcode2.R.id.btnReceive)
		btnreceive.setOnClickListener(new android.view.View.OnClickListener(){
		  def onClick(view:android.view.View){
        GlobalProperty.Initialize(Integer.parseInt(txtBx.getText().toString), Integer.parseInt(txtBy.getText().toString), Integer.parseInt(txtnpar.getText().toString))
		    startActivity(new Intent(MainActivity.this,classOf[ReceiverActivity]))
		  }
		})
	}
	
	
	override def onCreateOptionsMenu(menu:Menu):Boolean ={
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu)
		true
	}

}