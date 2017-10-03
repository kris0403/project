package anran.hdcode2.sender

import scala.swing.SimpleSwingApplication
import scala.swing.MainFrame
import scala.swing.Button
import scala.swing.Dimension
import scala.swing.Graphics2D
import java.awt.Color
import scala.swing.FileChooser
import scala.swing.Component
import java.io.File
import anran.hdcode2.lib.GlobalProperty
import anran.hdcode2.lib.FileTransmitProperty
import anran.hdcode2.lib.FileTypes
import scala.actors.Actor
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.ArrayList
import javax.imageio.ImageIO
import anran.hdcode2.zip.XZip
object SenderApplication extends SimpleSwingApplication{
  var bx = 13
  var by = bx match {
    case 15 => 9
    case 13 => 9
    case 11 => 7
    case 9 => 7
    case 7 => 5
    case 5 => 5
  }
  var npar = 2
  var xoffset = bx match {
    case 5 => 320
    case 7 => 210
    case 11 => 150
    case 13 => 200
    case _ => 0
  }
  var symbolsize = by match {
    case 5 => 11
    case 7 => 8
    case 9 => 6
    case _ => 0
  }
  var yoffset = symbolsize match {
    case 11 => 25
    case 8 => 8
    case 6 => 20
    case _ => 0
  }
  
  override def main(args:Array[String]){
    if(args.length>2){
      bx=Integer.parseInt(args(0))
      by=Integer.parseInt(args(1))
      npar=Integer.parseInt(args(2))
      xoffset=Integer.parseInt(args(3))
      yoffset=Integer.parseInt(args(4))
      symbolsize=Integer.parseInt(args(5))
    }
    top.open()
  }
  def top=new MainFrame {
    val imageList=new ArrayList[BufferedImage]()
      
    preferredSize = new Dimension(1366,768)
    title="HDCode 2 Sender"
    this.background=Color.WHITE
    var started=false
    def isStarted()=started
    val panel=new scala.swing.Panel(){
      var current=0
      override def paintComponent(g:Graphics2D){
        if(isStarted)
        {
          g.drawImage(imageList.get(current), 0,0,null)
          current=(current+1)%imageList.size()
          Thread.sleep(100)
          this.repaint
        }
      }
      preferredSize=new Dimension(720,1280)
      peer.setLocation(50, 50)
    }
    this.contents = panel
    val th2=new Thread(){
      override def run(){
        val ftprop=new FileTransmitProperty(GlobalProperty.DataLinkProperty,null, 0)
        val sender=FileTransmitApp.CreateAsSender(ftprop)
        sender.StartGenerating(new Actor(){
          def act()
          {
            while(true)
              receive{
              case (b:BufferedImage,id:Int)=>{
                ImageIO.write(b,"PNG",new File(imageList.size()+"("+id+").png"))/////////////
                if (imageList.size() % 20 == 0)
                  println("")
                print(imageList.size()+"("+id+") ")
                imageList.add(b)
                if(imageList.size() >= ftprop.DataLinkProperty.RSStrengthFramesCount && !isStarted)
                {
                  println("start")
                  started = true
                  panel.repaint
                }
                if(imageList.size() == ftprop.DataLinkProperty.RSStrengthFramesCount)
                {
                  println("stop")
                  exit()
                }
              }
              
            }
          }
          start()
        })
      }
    }
    {
      th2.start()
    }
  }
}