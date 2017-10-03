package anran.hdcode2.library

import android.util.Log
import java.io.BufferedOutputStream
import java.io.FileWriter
import java.io.FileOutputStream
object Utils {
  val DEBUG=false
  val VERBOSE=false
  def debugwarn[T](str: =>String,res: T):T=if(DEBUG)debugwarn("DEBUG",str,res) else res
  def debugwarn[T](tag: =>String,str: =>String,res:T):T={
    if(DEBUG)Log.w(tag,str)
    res
  }
  def debuginfo[T](str: =>String,res: T):T=if(DEBUG)debuginfo("DEBUG",str,res) else res
  def debuginfo[T](tag: =>String,str: =>String,res:T):T={
    if(DEBUG)Log.i(tag,str)
    res
  }
  def debugverb[T](str: =>String,res:T):T={
    if(VERBOSE)Log.i("VERBOSE",str)
    res
  }
  
  var writer:FileWriter=null
  def openLog(){
    writer=new FileWriter("/sdcard/RDCode_log.txt")
  }
  def writeLog(s:String){
    if(writer!=null){
      writer.write(s)
      writer.write("\n")
    }
  }
  def closeLog(){
    writer.flush()
    writer.close()
    writer=null
  }
}