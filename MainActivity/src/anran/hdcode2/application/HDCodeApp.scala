package anran.hdcode2.application

import scala.actors.Actor
import anran.hdcode2.physical.BlockState
import anran.hdcode2.library.PresentProperty
import anran.hdcode2.library.DataLinkProperty

trait HDCodeReceiverApp{
  def PushOriginalData()
  def write()
  def ReceiveData(frameId:Int)
  def ReceiveMessage(msg:Any)
}
