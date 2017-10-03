package anran.hdcode2.sender

import anran.hdcode2.lib._
import scala.actors.Actor
import java.io.File
import java.io.IOException

trait HDCodeSenderApp{
  def StartGenerating(presentActor:Actor)
}
object FileTransmitApp{
  def CreateAsSender(ftProp:FileTransmitProperty)={
    
    new HDCodeSenderApp(){
      
      def StartGenerating(presentActor:Actor){
        val dlProp=ftProp.DataLinkProperty
        
        val cons=new ReFrameConstructor(dlProp)
        cons.RegisterWindowChangeListener(()=>{
          ftProp.WindowId+=1
        })
        //val rand=new Random()
        val outstream=cons.Start((bitmap,fid)=>{
          if(bitmap==null)presentActor!0
          else presentActor!(bitmap,fid)
        })
//          val ff = new File("C:\\Dropbox\\FYP\\RandomTest\\frame_seq0.csv")
//          var byte = 0
//          var eoff = false
//          println("Read Seq")
//          val instreamf = new java.io.FileInputStream(ff)
//          while (!eoff) {
//            byte=instreamf.read()
//            instreamf.read()
//            if (byte == -1)
//              eoff = true
//            else
//              print(byte + " ")
//          }
        var index = 0
        while (index < GlobalProperty.DataLinkProperty.RSStrengthFramesCount * 2)
        {
          val f = new File("C:\\Dropbox\\FYP\\encodedframes192\\" + index.toString() + ".csv")
          val f1 = new File("C:\\Dropbox\\FYP\\encodedframes192\\" + (index + 1).toString() + ".csv")
          if ((!f.exists() || !f.canRead()) || (!f1.exists() || !f1.canRead())) throw new IOException()
          index += 2
          
          val instream = new java.io.FileInputStream(f)
          val instream1 = new java.io.FileInputStream(f1)
          
          var eof = false
          var byte1 = 0
          var byte2 = 0
          var data_count = 0
          
          while (!eof) {
            if (data_count % 128 == 0)
            {
              outstream(0)
              outstream(0)
              outstream(0)
              outstream(0)
            }
            data_count += 1
            byte1=instream.read()
            instream.read()
            byte2=instream1.read()
            instream1.read()
            byte1 %= 2
            byte2 %= 2
            val symbol = (byte1 << 1) | byte2
            outstream(symbol)
            if (data_count == (144 - 8 - 8) * 116) {
              eof = true
            } 
          }
        }
        cons.trace_close()
        cons.Stop()
      }
    }
  }
}