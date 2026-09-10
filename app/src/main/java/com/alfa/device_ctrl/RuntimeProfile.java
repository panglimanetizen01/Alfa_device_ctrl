package com.alfa.device_ctrl;

/** Immutable metadata describing one installable Linux runtime. */
public final class RuntimeProfile {
    private final String id;
    private final String displayName;
    private final String version;
    private final String architecture;
    private final String rootfsUrl;
    private final String rootfsSha256;
    private final String archiveFormat;
    private final String shell;
    private final String packageManager;
    private final String promptContract;
    private final String[] environment;
    private final String[] requiredPaths;
    private final String[] capabilities;
    private final boolean rootfsGzip;

    public RuntimeProfile(String id,String displayName,String architecture,String rootfsUrl,String rootfsSha256,boolean rootfsGzip) {
        this(id,displayName,"unknown",architecture,rootfsUrl,rootfsSha256,rootfsGzip?"tar.gz":"tar.xz","/bin/sh","unknown","alfa:<runtime-id>:<cwd>",new String[]{"HOME=/root","TERM=xterm-256color"},new String[]{"bin","etc","usr","usr/bin/env","bin/sh"},new String[]{"rootless-proot","pty","storage-bridge","network-evidence","process-evidence"});
    }

    public RuntimeProfile(String id,String displayName,String version,String architecture,String rootfsUrl,String rootfsSha256,boolean rootfsGzip,String archiveFormat,String shell,String packageManager,String promptContract,String[] environment,String[] requiredPaths,String[] capabilities) {
        this.id=require(id,"id");this.displayName=require(displayName,"displayName");this.version=require(version,"version");this.architecture=require(architecture,"architecture");this.rootfsUrl=require(rootfsUrl,"rootfsUrl");this.rootfsSha256=require(rootfsSha256,"rootfsSha256");this.rootfsGzip=rootfsGzip;this.archiveFormat=require(archiveFormat,"archiveFormat");this.shell=require(shell,"shell");this.packageManager=require(packageManager,"packageManager");this.promptContract=require(promptContract,"promptContract");this.environment=copy(environment,"environment");this.requiredPaths=copy(requiredPaths,"requiredPaths");this.capabilities=copy(capabilities,"capabilities");
    }
    public String id(){return id;} public String displayName(){return displayName;} public String version(){return version;} public String architecture(){return architecture;} public String rootfsUrl(){return rootfsUrl;} public String rootfsSha256(){return rootfsSha256;} public boolean rootfsGzip(){return rootfsGzip;} public String archiveFormat(){return archiveFormat;} public String shell(){return shell;} public String packageManager(){return packageManager;} public String promptContract(){return promptContract;} public String[] environment(){return environment.clone();} public String[] requiredPaths(){return requiredPaths.clone();} public String[] capabilities(){return capabilities.clone();}
    private static String require(String value,String name){if(value==null||value.trim().isEmpty()||value.indexOf('\0')>=0)throw new IllegalArgumentException(name+" is invalid");return value;}
    private static String[] copy(String[] values,String name){if(values==null||values.length==0)throw new IllegalArgumentException(name+" is empty");String[] result=values.clone();for(String value:result)require(value,name);return result;}
}