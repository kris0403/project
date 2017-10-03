package anran.hdcode2.physical

import android.graphics.Point
import anran.hdcode2.library.CaptureProperty
import anran.hdcode2.library.PixelReader
import anran.hdcode2.library.MemoryBuffer
import android.util.Log
import anran.hdcode2.library.YUVColor
import anran.hdcode2.library.Utils
import anran.hdcode2.GlobalProperty
import scala.Immutable


/**
 * locate the black area
 * include the center black pattern and the distributed black data unit
 */
class PatternLocater(data:Array[Byte],prop:CaptureProperty) {
  var log=false
  
  val reader:PixelReader=new PixelReader(data,prop)
  
  val THRES=3 //The distance threshold of MeanShift
  var MINWINDOW_DIST=16 //The minimum radius of searching area of MeanShift-ing distributed locators
  var MINWINDOW_CENTER=50 //The minimum radius of searching area of MeanShift-ing center locator
  val MINBLACK=8 //The minimum black pixels count within a searching area
  
  
  var currentSidelength=7.0
  
  private var bufferc=new YUVColor(0,0,0)
  
  def abs(x:Int)=if(x>0)x else -x
  def max(a:Int,b:Int)=if(a>b)a else b
  def min(a:Int,b:Int)=if(a>b)b else a
  def dist(p1x:Int,p1y:Int,p2x:Int,p2y:Int)=
    abs(p1x-p2x)+abs(p1y-p2y)
    
  
  private var meanShift_resX=0
  private var meanShift_resY=0
  private var meanShift_minR=0
  private def meanShift_mean(px:Int,py:Int,currr:Int,bc:BlackClassifier)={
    var npc=0
    var npx=0
    var npy=0
    
    val minx=max(px-currr,0)&0xfffffffe
    val maxx=min(px+currr,prop.Width-1)
    val miny=max(py-currr,0)&0xfffffffe
    val maxy=min(py+currr,prop.Height-1)
    
    var x=minx
    var y=miny
    var bt=0
    var nt=0
    var btn=0
    var ntn=0
    var p=0
    
    while(x<=maxx){
      y=miny
      while(y<=maxy){
        reader.GetPixelYUVTo(x,y,bufferc)
        
        p=bc.GetProbMul4(bufferc.Y)
        if(p==0){
          nt+=bufferc.Y
          ntn+=1
        }
        else if(p==4){
          bt+=bufferc.Y
          btn+=1
        }
          
        npx+=x*p
        npy+=y*p
        npc+=p
          
        y+=2
      } 
      x+=2
    }
    
    if(npc!=0){
      npx/=npc
      npy/=npc
    }
    if(btn!=0&&ntn!=0)bc.adjust(bt/btn, nt/ntn)
    meanShift_resX=npx
    meanShift_resY=npy
    npc/4
  }
  
  private def meanShift_iter(px:Int,py:Int,maxr:Int,bc:BlackClassifier):Int={
    val res=meanShift_mean(px,py,maxr,bc)
    if(res<MINBLACK)res
    else
    {
      val d=dist(meanShift_resX,meanShift_resY,px,py)
      val newr=max(meanShift_minR,maxr*2/3)
      if(d<THRES){
        res
      }
      else
        meanShift_iter(meanShift_resX,meanShift_resY,newr,bc)
    }
  }
  private def getBlackTill(max:Int,min:Int,dirx:Int,diry:Int,bc:BlackClassifier)={
    var i=min
    var nonblack=false
    var ok=false
    while(!ok&&i<max){
      val x=dirx*i+meanShift_resX
      val y=diry*i+meanShift_resY
      if(x<0||y<0||x>=prop.Width||y>=prop.Height)i=max
      else{
        if(bc.IsNonBlack(reader.GetPixelYUVTo(x, y,bufferc).Y)){
          if(nonblack)
            nonblack=true 
          else 
            ok=true
        }
        i=i+1
      }
    }
    if(ok)i
    else -1
  }
  private def adjust(min:Int,max:Int,bc:BlackClassifier)={
    val l=getBlackTill(max,min,-1,0,bc)
    val r=getBlackTill(max,min,1,0,bc)
    val t=getBlackTill(max,min,0,-1,bc)
    val b=getBlackTill(max,min,0,1,bc)
  
  
    if((l|r|t|b)>=0){
      meanShift_resX-=(l-r)/2
      meanShift_resY-=(t-b)/2
      
      true
    }
    else Utils.debugwarn("[PatternLocater.adjust]adjust failed",false)
  }

  private def meanShift(init:Point,initr:Int,minr:Int,dest:Point,fast:Boolean=false,bc:BlackClassifier):Int={
    if(fast&&bc.IsBlack(reader.GetPixelGaussYUVBuffered(init.x,init.y).Y)){ 
      meanShift_resX=init.x
      meanShift_resY=init.y
      if(adjust(1,minr,bc)){
        dest.x=meanShift_resX
        dest.y=meanShift_resY
        return 200
      }
    }
      
    val c=meanShift_iter(init.x,init.y,minr,bc)
    adjust(1,minr,bc)
    dest.x=meanShift_resX
    dest.y=meanShift_resY
    c
  }
  
  private val pmem=new MemoryBuffer[Point](100)({new Point()})
  /**
   * p1,p2,p3 are clockwise
   */
  private def getFourthPoint(p1:Point,p2:Point,p3:Point)={
    val res=pmem.newObj
    res.x=p1.x-p2.x+p3.x
    res.y=p1.y-p2.y+p3.y
    res
  }
  /**
   * p1-p2=p2-p3
   */
  private def getThirdPoint(p1:Point, p2:Point)={
    val res=pmem.newObj
    res.x=2*p2.x-p1.x
    res.y=2*p2.y-p1.y
    res
  }
  /**
   * get the black-most area around the center point
   * return a function. pass true to it will reset the center point
   * or false to use the previous black-most area for the center point
   * the function will return None if no enough black point is around the center point.
   */
  def GetCenterPointLocater(center:Point,bc:BlackClassifier):(Boolean=>Point)={
    var back:Point=if(center==null)new Point(prop.Width/2,prop.Height/2) else center
    var curr=back
    var initial=true
    var currr=0
    def reset(){
      curr=back
      currr=prop.MinSide/6
      initial=true
      bc.Reset()
    }
    reset()
    (r)=>{
      if(r)
        reset()
      
      val res=pmem.newObj
      val count=meanShift(curr,currr,MINWINDOW_CENTER,res,!initial,bc)
      initial=false
      if(count<MINBLACK){
        reset()
        null
      }
      else{
        curr.x=res.x
        curr.y=res.y
        curr
      }
    }
  }
  
  /**
   * Get all distributed locater by recursion
   * FramePosition must has 4 center distributed locaters ready.
   * if the point exists, the call to that function will adjust the point.
   * So keep the result. 
   */
  private object queue{
    class MutableTuple4{
      var a:Point=null
      var b:Point=null
      var c:Int=0
      var d:Int=0
    }
    private val distqueue=Array.fill(400)(new MutableTuple4())
    var queuepeak=0
    def addItem(p1:Point,p2:Point,bx:Int,by:Int)={
      val m=distqueue(queuepeak)
      m.a=p1
      m.b=p2
      m.c=bx
      m.d=by
      queuepeak+=1
    }
    def popItem()={
      queuepeak-=1
      distqueue(queuepeak)
    }
    def clear()=queuepeak=0
    def empty()=queuepeak==0
    def size()=queuepeak
  }
  def GetDistributedLocater(fp:FramePosition):(Int,Int)=>Point={
    //step 0:refresh
    fp.Refresh
    //step 1:ensure 4 center locater
    def update(x:Int,y:Int)={
      val bp=fp.BlockPositions(x)(y)
      if(bp.State==BlockPosition.TOADJUST){
        if(meanShift(bp.LTPosition,MINWINDOW_DIST,MINWINDOW_DIST,bp.LTPosition,true,bp.BClassifier)<MINBLACK)
          fp.BlockPositions(x)(y).State=BlockPosition.FAILED
          else
            fp.BlockPositions(x)(y).State=BlockPosition.LOCATED
      }
      else if(fp.BlockPositions(x)(y).State==BlockPosition.TOLOCATE){
        if(meanShift(bp.LTPosition,MINWINDOW_DIST,MINWINDOW_DIST,bp.LTPosition,false,bp.BClassifier)<MINBLACK)
          fp.BlockPositions(x)(y).State=BlockPosition.FAILED
          else
            fp.BlockPositions(x)(y).State=BlockPosition.LOCATED
      }
      fp.BlockPositions(x)(y)
    }
    def switch(x:Int,y:Int)={
      val bp=fp.BlockPositions(x)(y)
      if(bp.State==BlockPosition.NOTADJUSTED)bp.State=BlockPosition.TOADJUST
      else if(bp.State==BlockPosition.NOTLOCATED)bp.State=BlockPosition.TOLOCATE
    }
    switch(fp.XCount/2,fp.YCount/2)
    val lt=update(fp.XCount/2,fp.YCount/2)
    switch(fp.XCount/2,fp.YCount/2+1)
    val lb=update(fp.XCount/2,fp.YCount/2+1)
    switch(fp.XCount/2+1,fp.YCount/2)
    val rt=update(fp.XCount/2+1,fp.YCount/2)
    switch(fp.XCount/2+1,fp.YCount/2+1)
    val rb=update(fp.XCount/2+1,fp.YCount/2+1)
    
    //Utils.debuginfo("blockposition", lt.State+" ", 0)
    
    if(lt.State==BlockPosition.LOCATED&&lt.State==lb.State&&lb.State==rt.State&&rt.State==rb.State){
      
      //step 2:establish a queue
      queue.clear()
      val xc=fp.XCount/2
      val yc=fp.YCount/2
      
      def findadj(x:Int,y:Int)={
        if(x>0&&fp.BlockPositions(x-1)(y).State==BlockPosition.LOCATED){
          if(x<fp.XCount&&BlockPosition.isTo(fp.BlockPositions(x+1)(y)))
            queue.addItem(fp.BlockPositions(x-1)(y).LTPosition, fp.BlockPositions(x)(y).LTPosition, x+1, y)
          if(x>1&&BlockPosition.isTo(fp.BlockPositions(x-2)(y)))
            queue.addItem(fp.BlockPositions(x)(y).LTPosition, fp.BlockPositions(x-1)(y).LTPosition, x-2, y)
        }
        else if(x<fp.XCount&&fp.BlockPositions(x+1)(y).State==BlockPosition.LOCATED){
          if(x>0&&BlockPosition.isTo(fp.BlockPositions(x-1)(y)))
            queue.addItem(fp.BlockPositions(x+1)(y).LTPosition, fp.BlockPositions(x)(y).LTPosition, x-1, y)
          if(x<fp.XCount-1&&BlockPosition.isTo(fp.BlockPositions(x+2)(y)))
            queue.addItem(fp.BlockPositions(x)(y).LTPosition, fp.BlockPositions(x+1)(y).LTPosition, x+2, y)
        }
            
        if(y>0&&fp.BlockPositions(x)(y-1).State==BlockPosition.LOCATED){
          if(y<fp.YCount&&BlockPosition.isTo(fp.BlockPositions(x)(y+1)))
            queue.addItem(fp.BlockPositions(x)(y-1).LTPosition, fp.BlockPositions(x)(y).LTPosition, x, y+1)
          if(y>1&&BlockPosition.isTo(fp.BlockPositions(x)(y-2)))
            queue.addItem(fp.BlockPositions(x)(y).LTPosition, fp.BlockPositions(x)(y-1).LTPosition, x, y-2)
        }
            
        else if(y<fp.YCount&&fp.BlockPositions(x)(y+1).State==BlockPosition.LOCATED){
          if(y>0&&BlockPosition.isTo(fp.BlockPositions(x)(y-1)))
            queue.addItem(fp.BlockPositions(x)(y+1).LTPosition, fp.BlockPositions(x)(y).LTPosition, x, y-1)
          if(y<fp.YCount-1&&BlockPosition.isTo(fp.BlockPositions(x)(y+2)))
            queue.addItem(fp.BlockPositions(x)(y).LTPosition, fp.BlockPositions(x)(y+1).LTPosition, x, y+2)
        }
      }
      
      findadj(xc,yc)
      findadj(xc+1,yc)
      findadj(xc,yc+1)
      findadj(xc+1,yc+1)
      
      //step 3:recursive
      while(!queue.empty()){
        val stuff=queue.popItem()
        val dest=fp.BlockPositions(stuff.c)(stuff.d)
        var succ=false
        if(dest.State==BlockPosition.TOADJUST)
          if(meanShift(dest.LTPosition,MINWINDOW_DIST*3,MINWINDOW_DIST*2,dest.LTPosition,true,dest.BClassifier)>=MINBLACK)
            succ=true
        if(!succ){
          val tp=getThirdPoint(stuff.a,stuff.b)
          val temp=new Point()
          if(meanShift(tp,MINWINDOW_DIST*3,MINWINDOW_DIST*2,temp,false,dest.BClassifier)>=MINBLACK){
            dest.LTPosition.x=temp.x
            dest.LTPosition.y=temp.y
            succ=true
          }
          
        }
        if(succ){
          //Utils.debuginfo("queue success", stuff.c+" "+stuff.d+" "+dest.LTPosition, 0)
          dest.State=BlockPosition.LOCATED
          findadj(stuff.c,stuff.d)
        }
        else 
          Utils.debuginfo("queue failed", stuff.c+" "+stuff.d, dest.State=BlockPosition.FAILED)
      
      }
      
      //step 4:return (for upward compatible)
      (i:Int,j:Int)=>{
        if(fp.BlockPositions(i)(j).State==BlockPosition.LOCATED)fp.BlockPositions(i)(j).LTPosition
        else null
      }
    }
    else null
  }
  /*def GetDistributedLocater(fp:FramePosition):(Int,Int)=>Point={
    def getProbDist(center:Position=>Point)={
      def dist(x:Point,y:Point)=math.sqrt((x.x-y.x)*(x.x-y.x)+(x.y-y.y)*(x.y-y.y))
      def avg(a:Double,b:Double)=(a+b)/2
      val plt=center(LT)
      val prt=center(RT)
      val plb=center(LB)
      val prb=center(RB)
      
      avg(avg(dist(plt,prt),dist(plb,prb)),avg(dist(plt,plb),dist(prt,prb)))
    }
    //val center=fp.GetBlockPosition(fp.XCount/2, fp.YCount/2) _
    //val dist=getProbDist(center)
    adjust +=1
    
    def make(x:Int,y:Int):Point={
      val bp=fp.BlockPositions(x)(y)
      val xyp=bp.LTPosition
      if(bp.Ready){
        
        if(bp.Adjust==adjust)return xyp
        else{
          bp.Adjust=adjust
          val count=meanShift(xyp,MAXSIDELENGTH,xyp,true)
          if(count>=MINBLACK)
            return Utils.debugverb("make ok "+x+" "+y,xyp)
          else{
            fp.BlockPositions(x)(y).Ready=false
            //currently not add it to a queue for further recovery
            
            return Utils.debugwarn("[PatternLocater.GetDistributedLocater]Block "+x+", "+y+" failed to adjust, relocate it",null)
            
          }
        }
      }
      bp.Adjust=adjust
      var p1:Point=null
      var p2:Point=null
      def mk3={
        val p=getThirdPoint(p2,p1)
        val count=meanShift(p,MINWINDOW,xyp)
        if(count<MINBLACK)
          null//Utils.debuginfo("mk3",x+","+y+":"+count, null)
        else{
          fp.BlockPositions(x)(y).Ready=true
          xyp
        }
      }
      if(x==fp.XCount/2){
        if(y>fp.YCount/2)
        {
          p1=make(x,y-1)
          p2=make(x,y-2)
        }
        else{
          p1=make(x,y+1)
          p2=make(x,y+2)
        }
        mk3
      }
      else if(y==fp.YCount/2){
        if(x>fp.XCount/2)
        {
          p1=make(x-1,y)
          p2=make(x-2,y)
        }
        else{
          p1=make(x+1,y)
          p2=make(x+2,y)
        }
        
        mk3
      }
      
      else{
        val (p1dtx,p1dty,
            p2dtx,p2dty,
            p3dtx,p3dty)=(x-fp.XCount/2,y-fp.YCount/2) match{
          case (x,y) if(x<0 && y<0)=> (1,0,1,1,0,1)
          case (x,y) if(x<0 && y>0)=> (0,-1,1,-1,1,0)
          case (x,y) if(x>0 && y<0)=> (-1,0,-1,1,0,1)
          case (x,y) if(x>0 && y>0)=> (-1,0,-1,-1,0,-1)
        }
        val p1=make(x+p1dtx,y+p1dty)
        val p2=make(x+p2dtx,y+p2dty)
        val p3=make(x+p3dtx,y+p3dty)
        if(p1==null||p2==null||p3==null)return null//return Utils.debuginfo(x+","+y+":"+(p1==null)+","+(p2==null)+","+(p3==null), null)
        val res=getFourthPoint(p1,p2,p3)
        val count=meanShift(res,MINWINDOW,xyp)
        
        if(count<MINBLACK)return Utils.debugwarn("[PatternLocater.GetDistributedLocater]Block "+x+", "+y+" failed to locate",null)
        else{
          fp.BlockPositions(x)(y).Ready=true
          return xyp
        }
      
      }
      
    }
    make
  }*/
  
  
  def getEstimatedSidelength(center:Point,bc:BlackClassifier)={
    meanShift_resX=center.x
    meanShift_resY=center.y
    val l=getBlackTill(MINWINDOW_CENTER,2,-1,0,bc)
    val r=getBlackTill(MINWINDOW_CENTER,2,1,0,bc)
    val t=getBlackTill(MINWINDOW_CENTER,2,0,-1,bc)
    val b=getBlackTill(MINWINDOW_CENTER,2,0,1,bc)
  
    Utils.debuginfo("lrtb", l+" "+r+" "+t+" "+b, 0)
    if((l|r|t|b)>=0){
      ((l+r)+(t+b))/6.0
    }
    else -1
  }
  /**
   * Get four center locaters
   * return a function that receives a Position and return a Point
   */
  def GetCenterBlockLocater(center:Point,bc:BlackClassifier):(Position=>Point)={
    
    /*def getPat2Dir:Double={
      val xmin=max(0,center.x-sidelength)
      val ymin=max(0,center.y-sidelength)
      
      val xmax=min(prop.Width-1,center.x+sidelength)
      val ymax=min(prop.Height-1,center.y+sidelength)
      
      var (xdir,ydir,c)=(0,0,0)
      
      for(x<-xmin to xmax)
        for(y<-ymin to ymax){
          val color=reader.GetPixelYUVTo(x, y,bufferc)
          if(!prop.IsPatternColor1(color) && prop.IsPatternColor3(color)){
            c+=1
            xdir+=x-center.x
            ydir+=y-center.y
          }
        }
      if(c==0)Utils.debugwarn("[PatternLocater.GetCenterBlockLocater]No direction pattern detected", 0)//rarely happened
      else math.atan(ydir.asInstanceOf[Double]/xdir)
    }
    */
    
    //assume pattern color 2 directs LT
    def getRT(lt:Point)={
      val (xd,yd)=(lt.x-center.x,lt.y-center.y)
      val res=pmem.newObj
      res.x=center.x-xd
      res.y=center.y+yd
      res
    }
    def getLB(lt:Point)={
      val (xd,yd)=(lt.x-center.x,lt.y-center.y)
      val res=pmem.newObj
      res.x=center.x+xd
      res.y=center.y-yd
      res
    }
    def getRB(lt:Point)={
      val (xd,yd)=(lt.x-center.x,lt.y-center.y)
      val res=pmem.newObj
      res.x=center.x-xd
      res.y=center.y-yd
      res
    }
    
    currentSidelength=getEstimatedSidelength(center,bc)
    if(currentSidelength<0)return Utils.debugwarn("[PatternLocater.GetCenterBlockLocater]side length<0",null)
    else Utils.debuginfo("Sidelength",currentSidelength+"",0)
    
    MINWINDOW_CENTER=(currentSidelength*5).asInstanceOf[Int]
    MINWINDOW_DIST=currentSidelength.asInstanceOf[Int]
    
    //four direction:
    val ldir=0
    val ltdir=ldir+(if(GlobalProperty.HEXAGON)math.atan(5.0/6) else math.atan(1))
    val ltdirx=math.cos(ltdir)
    val ltdiry=math.sin(ltdir)
    
    val ltp=pmem.newObj
    ltp.x=center.x-(7*currentSidelength).asInstanceOf[Int]
    ltp.y=center.y-(7*currentSidelength).asInstanceOf[Int]
    
    val rtp=getRT(ltp)
    val lbp=getLB(ltp)
    val rbp=getRB(ltp)
    
    
    val ltPf=pmem.newObj
    val rtPf=pmem.newObj
    val rbPf=pmem.newObj
    val lbPf=pmem.newObj
    
    val ltCt=meanShift(ltp,(currentSidelength*3).asInstanceOf[Int],(currentSidelength*2).asInstanceOf[Int],ltPf,false,bc)
    val rtCt=meanShift(rtp,(currentSidelength*3).asInstanceOf[Int],(currentSidelength*2).asInstanceOf[Int],rtPf,false,bc)
    val lbCt=meanShift(lbp,(currentSidelength*3).asInstanceOf[Int],(currentSidelength*2).asInstanceOf[Int],lbPf,false,bc)
    val rbCt=meanShift(rbp,(currentSidelength*3).asInstanceOf[Int],(currentSidelength*2).asInstanceOf[Int],rbPf,false,bc)
    
    if(ltCt<MINBLACK||rtCt<MINBLACK||lbCt<MINBLACK||rbCt<MINBLACK)
      Utils.debugwarn("[PatternLocater.GetCenterBlockLocater]Cannot locate four corners", null)
    else
      return (pos:Position)=>pos match{
        case LT=>ltPf
        case RT=>rtPf
        case LB=>lbPf
        case RB=>rbPf
      }
      
    /*
    val minR=sidelength*6
    val maxR=prop.MinSide/4
    val minr=MINWINDOW
    val Rdelta=MINWINDOW
    val rdelta=MINWINDOW/4
    
    var currR=minR
    var currr=minr
    
    var ok=false
    val ltP=pmem.newObj
    val ldir=0//getPat2Dir
    //Log.i("Pat2Dir",ldir+"")
    val ltdir=ldir+(if(GlobalProperty.HEXAGON)math.atan(5.0/6) else math.atan(1))
    val ltdirx=math.cos(ltdir)
    val ltdiry=math.sin(ltdir)
    
    
    while(!ok){
      ltP.x=center.x-(currR*ltdirx).asInstanceOf[Int]
      ltP.y=center.y-(currR*ltdiry).asInstanceOf[Int]
      val lbP=getLB(ltP)
      val rbP=getRB(ltP)
      val rtP=getRT(ltP)
      
      val ltC=getBlackCount(ltP,currr)
      val rtC=getBlackCount(rtP,currr)
      val lbC=getBlackCount(lbP,currr)
      val rbC=getBlackCount(rbP,currr)
      if(ltC>MINBLACK||rtC>MINBLACK||lbC>MINBLACK||rbC>MINBLACK)
      {
        ok=true
        val ltPf=pmem.newObj
        val rtPf=pmem.newObj
        val rbPf=pmem.newObj
        val lbPf=pmem.newObj
        val ltCt=meanShift(ltP,currr,ltPf,false,bc)
        val rtCt=meanShift(rtP,currr,rtPf,false,bc)
        val lbCt=meanShift(lbP,currr,lbPf,false,bc)
        val rbCt=meanShift(rbP,currr,rbPf,false,bc)
        //Log.i("four black",ltCt+","+rtCt+","+lbCt+","+rbCt)
        if(ltCt<MINBLACK||rtCt<MINBLACK||lbCt<MINBLACK||rbCt<MINBLACK)
          Utils.debugwarn("[PatternLocater.GetCenterBlockLocater]Cannot locate four corners", null)
        else
          return (pos:Position)=>pos match{
            case LT=>ltPf
            case RT=>rtPf
            case LB=>lbPf
            case RB=>rbPf
          }
      }
      else
      {
        currR+=Rdelta
        currr+=rdelta
        
        if(currR>MAXR)
          ok=true
      }
    }
    
    */
  }
}