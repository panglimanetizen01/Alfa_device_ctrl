package com.alfa.device_ctrl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Executes only the fixed, non-mutating Rish identity probe and returns bounded evidence. */
public final class SecurityExecutionProbe {
    private static final int MAX_OUTPUT=4096;
    private SecurityExecutionProbe(){}
    public static Result probeRish(){
        String executable=findRish(); if(executable==null)return new Result("NOT_FOUND","rish executable was not found in canonical probe locations");
        Process process=null;
        try{
            process=new ProcessBuilder(executable,"-c","id").redirectErrorStream(true).start();
            final Process active=process; final StringBuilder out=new StringBuilder();
            Thread reader=new Thread(()->{try(BufferedReader input=new BufferedReader(new InputStreamReader(active.getInputStream(),StandardCharsets.UTF_8))){char[] buffer=new char[512];int count;while((count=input.read(buffer))!=-1){synchronized(out){if(out.length()<MAX_OUTPUT)out.append(buffer,0,Math.min(count,MAX_OUTPUT-out.length()));}}}catch(Exception ignored){}} ,"alfa-rish-output");
            reader.start();
            boolean finished=process.waitFor(5,TimeUnit.SECONDS);
            if(!finished){process.destroyForcibly();process.waitFor(1,TimeUnit.SECONDS);reader.join(1000);return new Result("TIMEOUT",snapshot(out));}
            reader.join(1000); return new Result(process.exitValue()==0?"PASS":"FAIL:"+process.exitValue(),snapshot(out));
        }catch(Exception error){if(process!=null)process.destroyForcibly();return new Result("ERROR",error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage()));}
    }
    private static String snapshot(StringBuilder out){synchronized(out){return out.toString().trim();}}
    private static String findRish(){String[] candidates={"/data/local/tmp/rish","/data/local/tmp/shizuku/rish","/system/bin/rish","/system/xbin/rish"};for(String candidate:candidates){File file=new File(candidate);if(file.isFile()&&file.canExecute())return candidate;}String path=System.getenv("PATH");if(path!=null)for(String directory:path.split(":",-1)){if(directory.isEmpty())continue;File file=new File(directory,"rish");if(file.isFile()&&file.canExecute())return file.getAbsolutePath();}return null;}
    public static final class Result{public final String status;public final String output;Result(String status,String output){this.status=status;this.output=output;}}
}
