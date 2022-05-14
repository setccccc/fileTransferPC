/**
 * 将速度计算的代码都放在这里
 */

public class speedCheck {
    public long preT,curT;
    public String speedStr;
    public speedCheck(){

    }
    public void checkStart(){
        preT = System.currentTimeMillis();
        speedStr = "0 KB/s";
    }
    // 传入当前字节数
    // 返回速度字符串,比如10KB/s或者1MB/s或者500Byte/s
    public static float CHECK_TIME = 1000.0f;
    public static float CHECK_TIME_INV = 1000.0f/CHECK_TIME;
    public String speed(long curbm){
        curbm*=CHECK_TIME_INV;
        if(curbm<1024){
            speedStr = curbm+" Byte/s";
        }else if(curbm>1024 && curbm<1048579){
            speedStr = format((float)curbm/1024.0) + " KB/s";
        }else{
            speedStr = format((float)curbm/1048579.0) + " MB/s";
        }
        return speedStr;
    }
    //精确到小数点后三位
    public String format(double f){
        java.text.DecimalFormat df = new java.text.DecimalFormat("#.###");
        return df.format(f);
    }
    public String 百分比(long 分子,long 分母){
        int i = (int)((float)分子*100.0/(float)分母);
        return i+"%";
    }
}
