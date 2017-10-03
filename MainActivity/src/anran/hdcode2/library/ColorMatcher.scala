package anran.hdcode2.library
/**
 * match a color to bits
 */
object ColorMatcher{
  /**
   * match a given color to one of the color pattern array
   * always return the index
   */
  def MatchColor(colors:Array[YUVColor],count:Int)=(color:YUVColor)=>{
    var i=0
    var res=0
    var resd=1000000
    while(i<count){
      val d=colors(i).Dist(color)
      if(resd>d){
        res=i
        resd=d
      }
      i+=1
    }
    res
  }
  /**
   * match a given color to one of the two pattern colors in capture property
   * return 0 if color==patterncolor2
   * 1 if color==patterncolor3
   * -1 if none
   */
  def MatchPatternColor(capProp:CaptureProperty,color:YUVColor)={
    if(capProp.IsPatternColor2(color))0
    else if(capProp.IsPatternColor3(color))1
    else -1
  }
  /**
   * determine whether a given color is black 
   * true if black
   */
  def MatchPattern(capProp:CaptureProperty,color:YUVColor)={
    if(capProp.IsPatternColor1(color))true else false
  }
  
  /**
   * Below are no-allocation methods for accelerating
   */
  private var colors:Array[YUVColor]=null
  private var count:Int=0
  def AssignColors(colors:Array[YUVColor],count:Int){
    this.colors=colors
    this.count=count
  }
  def Match(color:YUVColor)={
    var i=0
    var res=0
    var resd=1000000
    while(i<count){
      val d=colors(i).Dist(color)
      if(resd>d){
        res=i
        resd=d
      }
      i+=1
    }
    res
  }
}