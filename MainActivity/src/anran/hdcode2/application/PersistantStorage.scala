package anran.hdcode2.application

import scala.actors.Actor
import anran.hdcode2.physical.BlockState
import anran.hdcode2.library.FileTransmitProperty
import java.nio.ByteBuffer
import java.io.RandomAccessFile
import java.util.concurrent.Semaphore
import android.util.Log
import anran.hdcode2.library.Utils

class PersistantStorage(ftProp:FileTransmitProperty,temp:String) {
  case object Error
  case object Success
  val tmpStream=new java.io.BufferedOutputStream(new java.io.FileOutputStream(temp))
  val dlProp=ftProp.DataLinkProperty
  var over=false
  val sem=new Semaphore(1)
    
  def Write(windowsid:Int,frameid:Int,blockid:Int,block:BlockState){
    sem.acquire()
    if(!over){
      var i=1
      tmpStream.write(windowsid)
      tmpStream.write(frameid)
      tmpStream.write(blockid)
      while(i<block.Data.length-dlProp.RSStrengthInFrame*2-1){
        tmpStream.write(block.Data(i))
        i+=1
      }
    }
    sem.release()
  }
  def StopAndSave(dest:String):Boolean={
    sem.acquire()
    Log.i("final-dest",dest)
    val file=new java.io.File(dest)
    if(file.exists())file.delete()
    val randomfile=new RandomAccessFile(dest,"rw")
    
    
    val buffer=new Array[Byte](dlProp.GetBlockDataBytesCount-2)
    tmpStream.flush()
    tmpStream.close()
    val tmpStream2=new java.io.FileInputStream(temp)
    var ok=true
    
    while(ok){
      val windowsid=tmpStream2.read()
      if(windowsid>=0){
        val frameid=tmpStream2.read()
        val blockid=tmpStream2.read()
        
        tmpStream2.read(buffer)
        
        var pos=ftProp.GetPosition(windowsid,frameid,blockid)
        randomfile.seek(pos)
        
        Utils.debuginfo("Persistant",windowsid+" "+frameid+" "+blockid+" "+pos,0)
        
        if(blockid==0)
          randomfile.write(buffer,ftProp.GetHeaderLength,dlProp.GetBlockDataBytesCount-ftProp.GetHeaderLength-2)
        else
          randomfile.write(buffer)
      }
      else ok=false
    }
    randomfile.setLength(ftProp.FileLength)
    tmpStream2.close()
    randomfile.close()
    over=true
    sem.release()
    return true
  }
  
  
  def Stop(){
    sem.acquire()
    tmpStream.close()
    sem.release()
  }
}