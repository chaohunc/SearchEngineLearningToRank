import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

public class generatedQuery {

  public static void main(String[] args) throws IOException {
    // TODO Auto-generated method stub
    // 10:#AND (#WSUM( cheap.url 0 cheap.title 0 cheap.inlink 0 cheap.body 0.5 cheap.keywords)
    // #WSUM(0.3 internet.url 0 internet.title 0 internet.inlink 0 internet.body 0.5
    // internet.keywords))
    int[] a = new int[25];
    String[] fields = new String[5];

    /*
     * 10:#AND(cheap internet) 12:#AND(djs) 26:#AND(lower heart rate) 29:#AND(ps 2 games)
     * 33:#AND(elliptical trainer) 52:#AND(avp) 71:#AND(living in india) 102:#AND(fickle creek farm)
     * 149:#AND(uplift at yellowstone national park) 190:#AND(brooks brothers clearance)
     */
    BufferedReader bf =new BufferedReader(new FileReader("queries.txt"));
    
    String[] arr = new String[25];

    for (int i=0;i<25;i++)
    {
      String str = bf.readLine();
      String[] strlist=str.split(":");
      a[i] = Integer.parseInt(strlist[0]);
      arr[i] = strlist[1];

    }
    /*
    a[0] = 10;
    a[1] = 12;
    a[2] = 26;
    a[3] = 29;
    a[4] = 33;
    a[5] = 52;
    a[6] = 71;
    a[7] = 102;
    a[8] = 149;
    a[9] = 190;
    */
    double[] w = new double[5];
    /*
    arr[0] = "cheap internet";
    arr[1] = "djs";
    arr[2] = "lower heart rate";
    arr[3] = "ps 2 games";
    arr[4] = "elliptical trainer";
    arr[5] = "avp";
    arr[6] = "living in india";
    arr[7] = "fickle creek farm";
    arr[8] = "uplift at yellowstone national park";
    arr[9] = "brooks brothers clearance";
*/
    w[0] = 0.1;
    w[1] = 0.1;
    w[2] = 0.1;
    w[3] = 0.6;
    w[4] = 0.1;
    
    double[] ww = new double[2];
    ww[0]= 0.9;
    ww[1]= 0.1;
    double[] s = new double[3];
    s[0] = 0.7;
    s[1] = 0.2;
    s[2] = 0.1;
    fields[0] = "url";
    fields[1] = "keywords";
    fields[2] = "title";
    fields[3] = "body";
    fields[4] = "inlink";

    for (int i = 0; i < 25; i++) {
      String[] str = arr[i].split(" ");
      System.out.print(a[i]+ ": ");
//      System.out.print(ww[0]+ " #AND(");
      
  //      for (int j=0;j<str.length;j++) { System.out.print(" #WSUM( "); for (int z=0;z<5;z++)
  //     System.out.print(w[z]+" "+str[j]+"."+fields[z]+" "); System.out.print(")"); }
  //     System.out.print(")");
      
       
  //     System.out.print(ww[1]);
       
      System.out.print(" #WSUM(");
      System.out.print(s[0]);
      System.out.print(" #AND(");
      for (int j = 0; j < str.length; j++)
        System.out.print(str[j] + " ");
      System.out.print(") ");

      if (str.length != 1)
      {      
        System.out.print(s[1]);
        System.out.print(" #AND(");
      }
      for (int j = 0; j < str.length - 1; j++) {
        System.out.print(" #Near/1(");
        System.out.print(str[j] + " " + str[j + 1] + " ");
        System.out.print(")");
      }
      if (str.length != 1)
        System.out.print(") ");
      if (str.length != 1)
      {
        System.out.print(s[2]);
        System.out.print(" #AND(");
      }
      for (int j = 0; j < str.length - 1; j++) {
        System.out.print(" #Window/8(");
        System.out.print(str[j] + " " + str[j + 1] + " ");
        System.out.print(")");
      }
      if (str.length != 1)
        System.out.print(")");
       System.out.println(")");

      //  System.out.println("))");
    }
  }

}
