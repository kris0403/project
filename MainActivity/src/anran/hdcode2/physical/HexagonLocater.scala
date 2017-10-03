package anran.hdcode2.physical

import android.graphics.Point

class HexagonLocater extends SquareLocater {
  
  override def LocateTo(y:Int,x:Int,dest:Point)={
    val fm=blockSize
    val fzxl=x
    val fzyl=y
    val fzx=fm-fzxl
    val fzy=fm-fzyl
    if(y%2==0)
      dest.x=(fzx*fzy*lt.x+fzxl*fzy*rt.x+fzx*fzyl*lb.x+fzxl*fzyl*rb.x)/(fm*fm)
    else
      dest.x=(fzx*fzy*lt.x+fzxl*fzy*rt.x+fzx*fzyl*lb.x+fzxl*fzyl*rb.x-
          fzy*lt.x/2-fzyl*lb.x/2+fzy*rt.x/2+fzyl*rb.x/2)/(fm*fm)
      
    dest.y=(fzx*fzy*lt.y+fzxl*fzy*rt.y+fzx*fzyl*lb.y+fzxl*fzyl*rb.y)/(fm*fm)
    dest
  }
}