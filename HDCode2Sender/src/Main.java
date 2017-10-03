import anran.hdcode2.sender.SenderApplication;


public class Main {
  public static void main(String[] args){
    if(args.length==1&&args[0].toLowerCase().equals("help")){
      System.out.println("RDCode Sender\n================\nUsage: java -jar RDCode_sender.jar BlockCountX(an odd number) BlockCountY(an odd number) ParityBlockCount(1-3) MarginLeft(>0) MarginTop(>0) SymbolSize(>0)\nDefault:11 7 3 50 20 8");
    }
    else
      SenderApplication.main(args);
  }
}
