package org.onosproject.vcpe;

import java.util.Vector;
import java.util.Hashtable;
import java.io.*;

public class ParseStructure{

   private static String DOMAIN_FILE="domain";
   private static Hashtable<String,Vector<String>> domains=new Hashtable<>();
   private static byte[] lock=new byte[0];

   public static void buildVector(){
      File base=new File("/home/sdn/BL/");
      String[] categories = base.list();
      for(int i=0;i<categories.length;i++){
         Vector<String> catTree=new Vector<String>();
         catTree.add(categories[i]);
          System.out.println(categories[i]);
         File catFile=new File(base,categories[i]);
         if(catFile.isDirectory()){
            recurseDirectory(catFile, catTree);
         }
      }
       System.out.println(domains.size());

   }
   public static void initiate(){
      try {
         ParseStructure.buildVector();
         VcpeComponent.log.info("blacklist hashtable created");
      }catch(Exception e){
         VcpeComponent.log.error("blacklist creation exception", e);
      }
   }
   public static Hashtable<String,Vector<String>> getTable(){
      synchronized(lock){
         return domains;
      }
   }

   private static void recurseDirectory(File directory,Vector<String> catTree){
      String[] contents=directory.list();
      for(int i=0;i<contents.length;i++){
         File innerFile=new File(directory,contents[i]);
         if(innerFile.isFile()){
            if("domains".equals(contents[i])){
               try{
                  BufferedReader reader=new BufferedReader(new FileReader(innerFile));
                  while(true){
                      String line=reader.readLine();
                      if(line==null)break;
                      Vector<String> vals=domains.get(line);
                      if(vals==null){
                        Vector<String> newVals=new Vector<String>();
                        newVals.addAll(catTree);
                        domains.put(line,newVals);
                      }else{
                        vals.addAll(catTree);
                        domains.put(line,vals);

                      }

                   }
                   reader.close();
               }catch(Throwable t){
                  t.printStackTrace();
               }
               catTree.remove(catTree.size()-1);
            }
         }else{
             System.out.println("> " + contents[i]);
            catTree.add(contents[i]);
            recurseDirectory(innerFile,catTree);
         }
      }

   }
}
