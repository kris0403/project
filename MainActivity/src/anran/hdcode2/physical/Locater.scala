package anran.hdcode2.physical

import android.graphics.Point

trait Locater {
  type DataUnitLocater=Int=>((Int,Int)=>Point)
  //def GetLocaterByCorner(lt:Point,rt:Point,rb:Point,lb:Point):DataUnitLocater
  
  def AssignCorners(lt:Point,rt:Point,rb:Point,lb:Point,blocksize:Int)
  def AssignBlockSize(blocksize:Int)
  def LocateTo(y:Int,x:Int,dest:Point):Point
}